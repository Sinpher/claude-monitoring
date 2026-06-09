package com.sinpher.claudemonitor.repository;

import com.sinpher.claudemonitor.model.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SessionRepository 集成测试，验证会话数据访问逻辑。
 * 使用 H2 内存数据库替代 SQLite 进行测试。
 *
 * @author sinpher
 */
@DataJpaTest
@ActiveProfiles("test")
class SessionRepositoryTest {

    @Autowired
    private SessionRepository sessionRepository;

    /**
     * 测试保存和查询会话。
     * 验证保存后的会话能通过 ID 正确查询出来，且字段值一致。
     */
    @Test
    @DisplayName("保存会话后应能通过 ID 查询到，且字段值一致")
    void shouldFindSessionById() {
        // 准备测试数据
        Session session = Session.builder()
                .id("test-session-001")
                .project("test-project")
                .model("claude-sonnet-4-6")
                .startedAt(LocalDateTime.of(2026, 6, 9, 10, 0))
                .totalInputTokens(1000L)
                .totalOutputTokens(500L)
                .totalCacheTokens(200L)
                .estimatedCost(new BigDecimal("0.015000"))
                .toolCallCount(5)
                .status("ACTIVE")
                .build();

        // 执行保存
        sessionRepository.save(session);

        // 查询并验证
        Session found = sessionRepository.findById("test-session-001")
                .orElse(null);

        assertThat(found)
                .as("保存的会话应能通过 ID 查询到")
                .isNotNull();
        assertThat(found.getProject())
                .as("项目名称应与保存时一致")
                .isEqualTo("test-project");
        assertThat(found.getTotalInputTokens())
                .as("输入 token 数应与保存时一致")
                .isEqualTo(1000L);
        assertThat(found.getStatus())
                .as("状态应与保存时一致")
                .isEqualTo("ACTIVE");
    }

    /**
     * 测试按状态查询活跃会话。
     * 插入 ACTIVE 和 COMPLETED 两种状态的会话，验证只返回 ACTIVE 的。
     */
    @Test
    @DisplayName("按状态查询应只返回对应状态的会话")
    void shouldFindOnlyActiveSessions() {
        // 准备测试数据：一个活跃、一个已完成
        Session active = Session.builder()
                .id("session-active")
                .project("project-a")
                .startedAt(LocalDateTime.now())
                .status("ACTIVE")
                .build();
        Session completed = Session.builder()
                .id("session-completed")
                .project("project-b")
                .startedAt(LocalDateTime.now().minusHours(1))
                .endedAt(LocalDateTime.now())
                .status("COMPLETED")
                .build();

        // 执行保存
        sessionRepository.save(active);
        sessionRepository.save(completed);

        // 查询活跃会话
        List<Session> activeSessions = sessionRepository.findByStatus("ACTIVE");

        // 验证只返回活跃会话
        assertThat(activeSessions)
                .as("查询 ACTIVE 状态应只返回活跃会话")
                .hasSize(1)
                .allSatisfy(s -> assertThat(s.getStatus())
                        .as("返回的会话状态应为 ACTIVE")
                        .isEqualTo("ACTIVE"));
    }
}
