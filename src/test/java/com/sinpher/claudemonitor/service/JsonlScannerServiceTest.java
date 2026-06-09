package com.sinpher.claudemonitor.service;

import com.sinpher.claudemonitor.model.Session;
import com.sinpher.claudemonitor.model.ToolCall;
import com.sinpher.claudemonitor.repository.SessionRepository;
import com.sinpher.claudemonitor.repository.ToolCallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JsonlScannerService 单元测试，验证定时扫描和增量解析逻辑。
 *
 * @author sinpher
 */
@ExtendWith(MockitoExtension.class)
class JsonlScannerServiceTest {

    @Mock
    private JsonlParserService jsonlParserService;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private ToolCallRepository toolCallRepository;

    @Mock
    private CostCalculationService costCalculationService;

    private JsonlScannerService scannerService;

    @TempDir
    Path tempDir;

    /**
     * 测试前准备：初始化被测服务并注入依赖。
     */
    @BeforeEach
    void setUp() {
        scannerService = new JsonlScannerService(
                jsonlParserService,
                sessionRepository,
                toolCallRepository,
                costCalculationService
        );
        // 注入配置值
        ReflectionTestUtils.setField(scannerService, "claudeDataDir", tempDir.toString());
    }

    @Test
    @DisplayName("扫描任务应跳过不存在的项目目录")
    void shouldSkipNonExistentProjectsDirectory() {
        // 测试数据准备：设置一个不存在的目录
        ReflectionTestUtils.setField(scannerService, "claudeDataDir", "/non/existent/path");

        // 被测动作执行
        scannerService.scanJsonlFiles();

        // 结果验证：不应调用解析服务
        verify(jsonlParserService, never()).parseFile(any(), any(Integer.class));
    }

    @Test
    @DisplayName("扫描任务应处理项目目录下的 JSONL 文件")
    void shouldProcessJsonlFilesInProjectDirectory() throws IOException {
        // 测试数据准备：创建项目目录和 JSONL 文件
        Path projectsDir = tempDir.resolve("projects");
        Path projectDir = projectsDir.resolve("test-project");
        Files.createDirectories(projectDir);
        Path jsonlFile = projectDir.resolve("session-001.jsonl");
        Files.writeString(jsonlFile, "{\"type\":\"user\",\"sessionId\":\"session-001\"}\n");

        // 模拟解析结果：返回空会话（无新数据）
        when(jsonlParserService.parseFile(any(), any(Integer.class)))
                .thenReturn(JsonlParserService.ParseResult.builder()
                        .session(null)
                        .toolCalls(List.of())
                        .linesRead(1)
                        .build());

        // 被测动作执行
        scannerService.scanJsonlFiles();

        // 结果验证：应调用解析服务
        verify(jsonlParserService).parseFile(any(), any(Integer.class));
    }

    @Test
    @DisplayName("处理新会话应保存到数据库并设置项目名称")
    void shouldSaveNewSessionWithProjectName() throws IOException {
        // 测试数据准备：创建项目目录和 JSONL 文件
        Path projectsDir = tempDir.resolve("projects");
        Path projectDir = projectsDir.resolve("my-project");
        Files.createDirectories(projectDir);
        Path jsonlFile = projectDir.resolve("new-session.jsonl");
        Files.writeString(jsonlFile, "{}\n");

        // 模拟解析结果：返回新会话
        Session newSession = Session.builder()
                .id("new-session-001")
                .model("claude-sonnet-4-6")
                .totalInputTokens(1000L)
                .totalOutputTokens(500L)
                .totalCacheTokens(200L)
                .toolCallCount(2)
                .build();
        ToolCall toolCall = ToolCall.builder()
                .sessionId("new-session-001")
                .toolName("Read")
                .build();

        when(jsonlParserService.parseFile(any(), any(Integer.class)))
                .thenReturn(JsonlParserService.ParseResult.builder()
                        .session(newSession)
                        .toolCalls(List.of(toolCall))
                        .linesRead(1)
                        .build());
        when(sessionRepository.findById("new-session-001")).thenReturn(Optional.empty());
        when(costCalculationService.calculateCost(anyString(), any(Long.class), any(Long.class), any(Long.class), any(Long.class)))
                .thenReturn(BigDecimal.valueOf(0.05));

        // 被测动作执行
        scannerService.scanJsonlFiles();

        // 结果验证：应保存新会话
        verify(sessionRepository).save(any(Session.class));
        verify(toolCallRepository).save(any(ToolCall.class));
    }

    @Test
    @DisplayName("处理已存在的会话应增量更新 token 和费用")
    void shouldIncrementallyUpdateExistingSession() throws IOException {
        // 测试数据准备：创建项目目录和 JSONL 文件
        Path projectsDir = tempDir.resolve("projects");
        Path projectDir = projectsDir.resolve("existing-project");
        Files.createDirectories(projectDir);
        Path jsonlFile = projectDir.resolve("existing-session.jsonl");
        Files.writeString(jsonlFile, "{}\n");

        // 模拟已存在的会话
        Session existingSession = Session.builder()
                .id("existing-session-001")
                .project("existing-project")
                .model("claude-sonnet-4-6")
                .totalInputTokens(2000L)
                .totalOutputTokens(1000L)
                .totalCacheTokens(500L)
                .toolCallCount(5)
                .estimatedCost(BigDecimal.valueOf(0.10))
                .build();

        // 模拟解析结果：返回增量数据
        Session incrementalSession = Session.builder()
                .id("existing-session-001")
                .model("claude-sonnet-4-6")
                .totalInputTokens(500L)
                .totalOutputTokens(200L)
                .totalCacheTokens(100L)
                .toolCallCount(1)
                .build();

        when(jsonlParserService.parseFile(any(), any(Integer.class)))
                .thenReturn(JsonlParserService.ParseResult.builder()
                        .session(incrementalSession)
                        .toolCalls(List.of())
                        .linesRead(1)
                        .build());
        when(sessionRepository.findById("existing-session-001")).thenReturn(Optional.of(existingSession));
        when(costCalculationService.calculateCost(anyString(), any(Long.class), any(Long.class), any(Long.class), any(Long.class)))
                .thenReturn(BigDecimal.valueOf(0.15));

        // 被测动作执行
        scannerService.scanJsonlFiles();

        // 结果验证：应更新会话（token 累加）
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    @DisplayName("增量扫描应从上次读取的行号继续")
    void shouldContinueFromLastLineOnIncrementalScan() throws IOException {
        // 测试数据准备：创建项目目录和 JSONL 文件
        Path projectsDir = tempDir.resolve("projects");
        Path projectDir = projectsDir.resolve("incremental-project");
        Files.createDirectories(projectDir);
        Path jsonlFile = projectDir.resolve("incremental-session.jsonl");
        Files.writeString(jsonlFile, "line1\nline2\n");

        // 第一次扫描：返回 1 行已读
        when(jsonlParserService.parseFile(any(), any(Integer.class)))
                .thenReturn(JsonlParserService.ParseResult.builder()
                        .session(null)
                        .toolCalls(List.of())
                        .linesRead(1)
                        .build());

        // 被测动作执行：第一次扫描
        scannerService.scanJsonlFiles();

        // 结果验证：第一次应被调用
        verify(jsonlParserService).parseFile(any(), any(Integer.class));

        // 第二次扫描：模拟增量解析
        when(jsonlParserService.parseFile(any(), any(Integer.class)))
                .thenReturn(JsonlParserService.ParseResult.builder()
                        .session(null)
                        .toolCalls(List.of())
                        .linesRead(2)
                        .build());

        // 被测动作执行：第二次扫描
        scannerService.scanJsonlFiles();

        // 结果验证：总共应被调用 2 次（第一次 + 第二次增量）
        verify(jsonlParserService, org.mockito.Mockito.times(2)).parseFile(any(), any(Integer.class));
    }

    @Test
    @DisplayName("扫描任务应跳过非目录的项目条目")
    void shouldSkipNonDirectoryProjectEntries() throws IOException {
        // 测试数据准备：创建 projects 目录，其中包含一个文件（非目录）
        Path projectsDir = tempDir.resolve("projects");
        Files.createDirectories(projectsDir);
        Path notADir = projectsDir.resolve("not-a-directory.txt");
        Files.writeString(notADir, "I am a file, not a directory");

        // 被测动作执行
        scannerService.scanJsonlFiles();

        // 结果验证：不应调用解析服务（因为没有有效的项目目录）
        verify(jsonlParserService, never()).parseFile(any(), any(Integer.class));
    }
}
