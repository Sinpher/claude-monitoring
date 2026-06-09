# Claude Monitor 后端实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 Claude Monitor 的 SpringBoot 后端，包含 JSONL 解析器、Hooks 接收端、数据存储、REST API 和 WebSocket 实时推送。

**Architecture:** SpringBoot 3.0 + JDK 21 后端，SQLite 嵌入式数据库。定时任务扫描 Claude Code 的 JSONL 会话文件增量解析 token 和工具调用数据；Hooks 端点接收实时事件驱动 Agent 状态变更；WebSocket 推送状态到前端。

**Tech Stack:** JDK 21, SpringBoot 3.0, SQLite (via sqlite-jdbc), Spring WebSocket, Jackson, Spring Scheduling

**Spec:** `docs/superpowers/specs/2026-06-08-claude-monitor-design.md`

---

## 文件结构

```
claude-monitoring/
├── build.gradle                                    # Gradle 构建配置
├── settings.gradle                                 # Gradle 设置
├── gradle/
│   └── wrapper/                                    # Gradle Wrapper
├── src/
│   ├── main/
│   │   ├── java/com/sinpher/claudemonitor/
│   │   │   ├── ClaudeMonitorApplication.java       # 启动类
│   │   │   ├── config/
│   │   │   │   ├── WebSocketConfig.java            # WebSocket 配置
│   │   │   │   └── SchedulingConfig.java           # 定时任务配置
│   │   │   ├── model/
│   │   │   │   ├── Session.java                    # 会话实体
│   │   │   │   ├── ToolCall.java                   # 工具调用实体
│   │   │   │   ├── DailyUsage.java                 # 日用量实体
│   │   │   │   └── AgentStatus.java                # 实时状态实体
│   │   │   ├── repository/
│   │   │   │   ├── SessionRepository.java          # 会话 DAO
│   │   │   │   ├── ToolCallRepository.java         # 工具调用 DAO
│   │   │   │   ├── DailyUsageRepository.java       # 日用量 DAO
│   │   │   │   └── AgentStatusRepository.java      # 实时状态 DAO
│   │   │   ├── service/
│   │   │   │   ├── JsonlParserService.java         # JSONL 解析服务
│   │   │   │   ├── JsonlScannerService.java        # JSONL 扫描调度服务
│   │   │   │   ├── HookEventService.java           # Hook 事件处理服务
│   │   │   │   ├── CostCalculationService.java     # 成本计算服务
│   │   │   │   ├── UsageService.java               # 用量统计服务
│   │   │   │   └── AgentStatusService.java         # Agent 状态服务
│   │   │   ├── controller/
│   │   │   │   ├── SessionController.java          # 会话 API
│   │   │   │   ├── UsageController.java            # 用量 API
│   │   │   │   ├── HookEventController.java        # Hook 接收端点
│   │   │   │   └── ConfigController.java           # 配置 API
│   │   │   ├── websocket/
│   │   │   │   └── AgentStatusHandler.java         # WebSocket 处理器
│   │   │   └── dto/
│   │   │       ├── HookEventDto.java               # Hook 事件 DTO
│   │   │       ├── SessionDetailDto.java           # 会话详情 DTO
│   │   │       ├── DailyUsageDto.java              # 日用量 DTO
│   │   │       └── AgentStatusDto.java             # Agent 状态 DTO
│   │   └── resources/
│   │       ├── application.yml                     # 应用配置
│   │       ├── schema.sql                          # SQLite 建表脚本
│   │       └── model-pricing.yml                   # 模型定价配置
│   └── test/
│       └── java/com/sinpher/claudemonitor/
│           ├── service/
│           │   ├── JsonlParserServiceTest.java      # JSONL 解析测试
│           │   ├── CostCalculationServiceTest.java  # 成本计算测试
│           │   ├── HookEventServiceTest.java        # Hook 事件测试
│           │   ├── UsageServiceTest.java            # 用量统计测试
│           │   └── AgentStatusServiceTest.java      # Agent 状态测试
│           ├── controller/
│           │   ├── SessionControllerTest.java       # 会话 API 测试
│           │   ├── UsageControllerTest.java         # 用量 API 测试
│           │   └── HookEventControllerTest.java     # Hook 端点测试
│           └── repository/
│               └── SessionRepositoryTest.java       # 会话 DAO 测试
```

---

### Task 1: 初始化 Gradle 项目

**Files:**
- Create: `build.gradle`
- Create: `settings.gradle`
- Create: `src/main/java/com/sinpher/claudemonitor/ClaudeMonitorApplication.java`
- Create: `src/main/resources/application.yml`

- [ ] **Step 1: 创建 build.gradle**

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.0.13'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.sinpher'
version = '0.1.0'

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-websocket'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.xerial:sqlite-jdbc:3.45.1.0'
    implementation 'org.hibernate.orm:hibernate-community-dialects:6.1.7.Final'
    implementation 'com.fasterxml.jackson.core:jackson-databind'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'com.h2database:h2'
}

test {
    useJUnitPlatform()
}
```

- [ ] **Step 2: 创建 settings.gradle**

```groovy
rootProject.name = 'claude-monitoring'
```

- [ ] **Step 3: 创建启动类 ClaudeMonitorApplication.java**

```java
package com.sinpher.claudemonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Claude Monitor 应用启动类。
 * 启用定时任务调度，用于定期扫描 JSONL 文件。
 */
@SpringBootApplication
@EnableScheduling
public class ClaudeMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaudeMonitorApplication.class, args);
    }
}
```

- [ ] **Step 4: 创建 application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:sqlite:${user.home}/.claude-monitor/data.db
    driver-class-name: org.sqlite.JDBC
  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: update
    show-sql: false
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    serialization:
      write-dates-as-timestamps: false

claude-monitor:
  claude-data-dir: ${user.home}/.claude
  scan-interval: 5000
  hook-secret: ""
```

- [ ] **Step 5: 验证项目能编译启动**

Run: `cd E:/sinpher/claude-monitoring && GRADLE_USER_HOME=/tmp/codex-gradle21 JAVA_HOME=$JAVA_HOME gradle bootRun`
Expected: 应用启动成功，日志显示 Tomcat started on port 8080

- [ ] **Step 6: Commit**

```bash
git add build.gradle settings.gradle src/
git commit -m "初始化 Gradle 项目：SpringBoot 3.0 + SQLite"
```

---

### Task 2: 数据模型与建表

**Files:**
- Create: `src/main/java/com/sinpher/claudemonitor/model/Session.java`
- Create: `src/main/java/com/sinpher/claudemonitor/model/ToolCall.java`
- Create: `src/main/java/com/sinpher/claudemonitor/model/DailyUsage.java`
- Create: `src/main/java/com/sinpher/claudemonitor/model/AgentStatus.java`
- Create: `src/main/resources/schema.sql`

- [ ] **Step 1: 创建 Session 实体**

```java
package com.sinpher.claudemonitor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Claude Code 会话实体，记录每次会话的 token 用量和费用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sessions")
public class Session {

    /** 会话唯一标识 */
    @Id
    @Column(length = 64)
    private String id;

    /** 所属项目名称 */
    @Column(length = 256)
    private String project;

    /** 使用的模型名称 */
    @Column(length = 128)
    private String model;

    /** 会话开始时间 */
    private LocalDateTime startedAt;

    /** 会话结束时间，进行中为 null */
    private LocalDateTime endedAt;

    /** 输入 token 总量 */
    @Column(nullable = false)
    private Long totalInputTokens = 0L;

    /** 输出 token 总量 */
    @Column(nullable = false)
    private Long totalOutputTokens = 0L;

    /** 缓存 token 总量（含 creation + read） */
    @Column(nullable = false)
    private Long totalCacheTokens = 0L;

    /** 预估费用（USD） */
    @Column(precision = 10, scale = 6)
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    /** 工具调用次数 */
    @Column(nullable = false)
    private Integer toolCallCount = 0;

    /** 会话状态：ACTIVE / COMPLETED */
    @Column(length = 16, nullable = false)
    private String status = "ACTIVE";
}
```

- [ ] **Step 2: 创建 ToolCall 实体**

```java
package com.sinpher.claudemonitor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工具调用记录实体，记录 Claude Code 每次工具调用的详情。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tool_calls")
public class ToolCall {

    /** 自增主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属会话 ID */
    @Column(length = 64, nullable = false)
    private String sessionId;

    /** 工具名称（如 Read、Edit、Bash 等） */
    @Column(length = 64, nullable = false)
    private String toolName;

    /** 工具输入参数（JSON 格式） */
    @Column(columnDefinition = "TEXT")
    private String inputParams;

    /** 调用时间 */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /** 耗时（毫秒），可能为 null */
    private Integer duration;
}
```

- [ ] **Step 3: 创建 DailyUsage 实体**

```java
package com.sinpher.claudemonitor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 日用量汇总实体，按日期聚合 token 用量和费用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "daily_usage")
public class DailyUsage {

    /** 日期（主键） */
    @Id
    private LocalDate date;

    /** 输入 token 总量 */
    @Column(nullable = false)
    private Long totalInputTokens = 0L;

    /** 输出 token 总量 */
    @Column(nullable = false)
    private Long totalOutputTokens = 0L;

    /** 缓存 token 总量 */
    @Column(nullable = false)
    private Long totalCacheTokens = 0L;

    /** 预估费用（USD） */
    @Column(precision = 10, scale = 6)
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    /** 会话数 */
    @Column(nullable = false)
    private Integer sessionCount = 0;

    /** 工具调用次数 */
    @Column(nullable = false)
    private Integer toolCallCount = 0;
}
```

- [ ] **Step 4: 创建 AgentStatus 实体**

```java
package com.sinpher.claudemonitor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent 实时状态实体，记录当前活跃会话的工作状态。
 * 状态值：WORKING / DONE / WAITING / IDLE
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "agent_status")
public class AgentStatus {

    /** 会话 ID（主键） */
    @Id
    @Column(length = 64)
    private String sessionId;

    /** 当前状态：WORKING / DONE / WAITING / IDLE */
    @Column(length = 16, nullable = false)
    private String status = "IDLE";

    /** 当前执行的工具名称 */
    @Column(length = 64)
    private String currentTool;

    /** 最后更新时间 */
    @Column(nullable = false)
    private LocalDateTime lastUpdatedAt;
}
```

- [ ] **Step 5: 创建 schema.sql（备用，JPA ddl-auto=update 会自动建表）**

```sql
-- 仅作参考，实际由 JPA 自动建表
CREATE TABLE IF NOT EXISTS sessions (
    id VARCHAR(64) PRIMARY KEY,
    project VARCHAR(256),
    model VARCHAR(128),
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    total_input_tokens BIGINT NOT NULL DEFAULT 0,
    total_output_tokens BIGINT NOT NULL DEFAULT 0,
    total_cache_tokens BIGINT NOT NULL DEFAULT 0,
    estimated_cost DECIMAL(10,6) DEFAULT 0,
    tool_call_count INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS tool_calls (
    id BIGINT PRIMARY KEY AUTOINCREMENT,
    session_id VARCHAR(64) NOT NULL,
    tool_name VARCHAR(64) NOT NULL,
    input_params TEXT,
    timestamp TIMESTAMP NOT NULL,
    duration INT
);

CREATE TABLE IF NOT EXISTS daily_usage (
    date DATE PRIMARY KEY,
    total_input_tokens BIGINT NOT NULL DEFAULT 0,
    total_output_tokens BIGINT NOT NULL DEFAULT 0,
    total_cache_tokens BIGINT NOT NULL DEFAULT 0,
    estimated_cost DECIMAL(10,6) DEFAULT 0,
    session_count INT NOT NULL DEFAULT 0,
    tool_call_count INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS agent_status (
    session_id VARCHAR(64) PRIMARY KEY,
    status VARCHAR(16) NOT NULL DEFAULT 'IDLE',
    current_tool VARCHAR(64),
    last_updated_at TIMESTAMP NOT NULL
);
```

- [ ] **Step 6: 验证编译通过**

Run: `cd E:/sinpher/claude-monitoring && GRADLE_USER_HOME=/tmp/codex-gradle21 gradle compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/sinpher/claudemonitor/model/ src/main/resources/schema.sql
git commit -m "添加数据模型：Session、ToolCall、DailyUsage、AgentStatus"
```

---

### Task 3: Repository 层

**Files:**
- Create: `src/main/java/com/sinpher/claudemonitor/repository/SessionRepository.java`
- Create: `src/main/java/com/sinpher/claudemonitor/repository/ToolCallRepository.java`
- Create: `src/main/java/com/sinpher/claudemonitor/repository/DailyUsageRepository.java`
- Create: `src/main/java/com/sinpher/claudemonitor/repository/AgentStatusRepository.java`
- Create: `src/test/java/com/sinpher/claudemonitor/repository/SessionRepositoryTest.java`

- [ ] **Step 1: 创建 SessionRepository**

```java
package com.sinpher.claudemonitor.repository;

import com.sinpher.claudemonitor.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话数据访问层，提供会话的 CRUD 和统计查询。
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, String> {

    /**
     * 查询指定时间范围内的会话列表，按开始时间降序排列。
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 会话列表
     */
    List<Session> findByStartedAtBetweenOrderByStartedAtDesc(LocalDateTime start, LocalDateTime end);

    /**
     * 查询状态为 ACTIVE 的会话列表。
     *
     * @return 活跃会话列表
     */
    List<Session> findByStatus(String status);

    /**
     * 统计指定日期范围内的会话数量。
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 会话数量
     */
    long countByStartedAtBetween(LocalDateTime start, LocalDateTime end);
}
```

- [ ] **Step 2: 创建 ToolCallRepository**

```java
package com.sinpher.claudemonitor.repository;

import com.sinpher.claudemonitor.model.ToolCall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工具调用数据访问层，提供工具调用的 CRUD 和统计查询。
 */
@Repository
public interface ToolCallRepository extends JpaRepository<ToolCall, Long> {

    /**
     * 查询指定会话的工具调用列表，按时间升序排列。
     *
     * @param sessionId 会话 ID
     * @return 工具调用列表
     */
    List<ToolCall> findBySessionIdOrderByTimestampAsc(String sessionId);

    /**
     * 统计指定时间范围内各工具的调用次数。
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 工具名和调用次数的投影列表
     */
    @Query("SELECT tc.toolName, COUNT(tc) FROM ToolCall tc WHERE tc.timestamp BETWEEN :start AND :end GROUP BY tc.toolName ORDER BY COUNT(tc) DESC")
    List<Object[]> countByToolNameBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 统计指定时间范围内的工具调用总数。
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 调用总数
     */
    long countByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
```

- [ ] **Step 3: 创建 DailyUsageRepository**

```java
package com.sinpher.claudemonitor.repository;

import com.sinpher.claudemonitor.model.DailyUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 日用量数据访问层，提供日用量汇总的 CRUD 和查询。
 */
@Repository
public interface DailyUsageRepository extends JpaRepository<DailyUsage, LocalDate> {

    /**
     * 查询指定日期范围内的日用量，按日期升序排列。
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return 日用量列表
     */
    List<DailyUsage> findByDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);
}
```

- [ ] **Step 4: 创建 AgentStatusRepository**

```java
package com.sinpher.claudemonitor.repository;

import com.sinpher.claudemonitor.model.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Agent 实时状态数据访问层，提供状态的 CRUD 和查询。
 */
@Repository
public interface AgentStatusRepository extends JpaRepository<AgentStatus, String> {

    /**
     * 查询指定状态的 Agent 列表。
     *
     * @param status 状态值（WORKING / DONE / WAITING / IDLE）
     * @return Agent 状态列表
     */
    List<AgentStatus> findByStatus(String status);
}
```

- [ ] **Step 5: 编写 SessionRepositoryTest**

```java
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
import static org.assertj.core.api.Assertions.withFailMessage;

/**
 * SessionRepository 集成测试，验证会话数据访问逻辑。
 * 使用 H2 内存数据库替代 SQLite 进行测试。
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
```

- [ ] **Step 6: 创建测试配置 src/test/resources/application-test.yml**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
```

- [ ] **Step 7: 运行测试验证通过**

Run: `cd E:/sinpher/claude-monitoring && GRADLE_USER_HOME=/tmp/codex-gradle21 gradle test --tests "SessionRepositoryTest"`
Expected: 2 tests PASSED

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/sinpher/claudemonitor/repository/ src/test/
git commit -m "添加 Repository 层和 SessionRepository 集成测试"
```

---

### Task 4: 成本计算服务

**Files:**
- Create: `src/main/java/com/sinpher/claudemonitor/service/CostCalculationService.java`
- Create: `src/main/resources/model-pricing.yml`
- Create: `src/test/java/com/sinpher/claudemonitor/service/CostCalculationServiceTest.java`

- [ ] **Step 1: 创建模型定价配置 model-pricing.yml**

```yaml
# Anthropic 模型定价（USD per 1M tokens）
# 参考：https://www.anthropic.com/pricing
models:
  claude-sonnet-4-6:
    input: 3.0
    output: 15.0
    cache_creation: 3.75
    cache_read: 0.30
  claude-opus-4-6:
    input: 15.0
    output: 75.0
    cache_creation: 18.75
    cache_read: 1.50
  claude-haiku-4-5:
    input: 0.80
    output: 4.0
    cache_creation: 1.0
    cache_read: 0.08
  # 默认定价（未知模型使用 sonnet 价格）
  default:
    input: 3.0
    output: 15.0
    cache_creation: 3.75
    cache_read: 0.30
```

- [ ] **Step 2: 创建 CostCalculationService**

```java
package com.sinpher.claudemonitor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 成本计算服务，根据模型定价和 token 用量计算费用。
 * 定价数据从 model-pricing.yml 加载，单位为 USD per 1M tokens。
 */
@Service
public class CostCalculationService {

    /** 模型定价配置，key 为模型名，value 为定价明细 */
    private final Map<String, Map<String, Double>> modelPricing;

    /** 默认定价配置 */
    private final Map<String, Double> defaultPricing;

    /**
     * 构造函数，从配置文件加载模型定价。
     *
     * @param modelPricing 模型定价映射
     */
    @SuppressWarnings("unchecked")
    public CostCalculationService(
            @Value("#{${claude-monitor.model-pricing}}") Map<String, Object> modelPricing) {
        this.modelPricing = (Map<String, Map<String, Double>>) (Map<?, ?>) modelPricing;
        this.defaultPricing = this.modelPricing.getOrDefault("default",
                Map.of("input", 3.0, "output", 15.0, "cache_creation", 3.75, "cache_read", 0.30));
    }

    /**
     * 根据模型名和 token 用量计算费用。
     *
     * @param model              模型名称
     * @param inputTokens        输入 token 数
     * @param outputTokens       输出 token 数
     * @param cacheCreationTokens 缓存创建 token 数
     * @param cacheReadTokens    缓存读取 token 数
     * @return 预估费用（USD），保留 6 位小数
     */
    public BigDecimal calculateCost(String model, long inputTokens, long outputTokens,
                                    long cacheCreationTokens, long cacheReadTokens) {
        Map<String, Double> pricing = modelPricing.getOrDefault(model, defaultPricing);

        double inputCost = inputTokens * pricing.getOrDefault("input", 3.0) / 1_000_000;
        double outputCost = outputTokens * pricing.getOrDefault("output", 15.0) / 1_000_000;
        double cacheCreationCost = cacheCreationTokens * pricing.getOrDefault("cache_creation", 3.75) / 1_000_000;
        double cacheReadCost = cacheReadTokens * pricing.getOrDefault("cache_read", 0.30) / 1_000_000;

        return BigDecimal.valueOf(inputCost + outputCost + cacheCreationCost + cacheReadCost)
                .setScale(6, RoundingMode.HALF_UP);
    }
}
```

- [ ] **Step 3: 在 application.yml 中添加定价配置引用**

在 `application.yml` 末尾追加：

```yaml
claude-monitor:
  claude-data-dir: ${user.home}/.claude
  scan-interval: 5000
  hook-secret: ""
  model-pricing: ${model-pricing}
```

- [ ] **Step 4: 编写 CostCalculationServiceTest**

```java
package com.sinpher.claudemonitor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withFailMessage;

/**
 * CostCalculationService 单元测试，验证成本计算逻辑。
 */
class CostCalculationServiceTest {

    private CostCalculationService costCalculationService;

    @BeforeEach
    void setUp() {
        // 准备测试定价数据
        Map<String, Object> pricing = Map.of(
                "claude-sonnet-4-6", Map.of("input", 3.0, "output", 15.0, "cache_creation", 3.75, "cache_read", 0.30),
                "default", Map.of("input", 3.0, "output", 15.0, "cache_creation", 3.75, "cache_read", 0.30)
        );
        costCalculationService = new CostCalculationService(pricing);
    }

    /**
     * 测试已知模型的成本计算。
     * 使用 sonnet-4-6 的定价，验证 1M input + 1M output 的费用。
     */
    @Test
    @DisplayName("已知模型应按对应定价计算费用")
    void shouldCalculateCostForKnownModel() {
        // 执行计算：1M input tokens + 1M output tokens
        BigDecimal cost = costCalculationService.calculateCost(
                "claude-sonnet-4-6",
                1_000_000L, 1_000_000L, 0L, 0L
        );

        // 验证：3.0 + 15.0 = 18.0 USD
        assertThat(cost)
                .as("1M input + 1M output 的 sonnet 费用应为 18.0 USD")
                .isEqualByComparingTo(BigDecimal.valueOf(18.0));
    }

    /**
     * 测试未知模型使用默认定价。
     * 传入不存在的模型名，验证使用 default 定价计算。
     */
    @Test
    @DisplayName("未知模型应使用默认定价计算费用")
    void shouldUseDefaultPricingForUnknownModel() {
        // 执行计算：使用不存在的模型名
        BigDecimal cost = costCalculationService.calculateCost(
                "unknown-model",
                1_000_000L, 0L, 0L, 0L
        );

        // 验证：使用默认 input 定价 3.0
        assertThat(cost)
                .as("未知模型应使用默认 input 定价 3.0 USD/1M tokens")
                .isEqualByComparingTo(BigDecimal.valueOf(3.0));
    }

    /**
     * 测试缓存 token 的成本计算。
     * 验证 cache_creation 和 cache_read 的费用计算正确。
     */
    @Test
    @DisplayName("缓存 token 应按对应定价计算费用")
    void shouldCalculateCacheTokenCost() {
        // 执行计算：1M cache_creation + 1M cache_read
        BigDecimal cost = costCalculationService.calculateCost(
                "claude-sonnet-4-6",
                0L, 0L, 1_000_000L, 1_000_000L
        );

        // 验证：3.75 + 0.30 = 4.05 USD
        assertThat(cost)
                .as("1M cache_creation + 1M cache_read 的 sonnet 费用应为 4.05 USD")
                .isEqualByComparingTo(BigDecimal.valueOf(4.05));
    }

    /**
     * 测试零 token 的费用为零。
     */
    @Test
    @DisplayName("零 token 用量应产生零费用")
    void shouldReturnZeroForZeroTokens() {
        BigDecimal cost = costCalculationService.calculateCost(
                "claude-sonnet-4-6", 0L, 0L, 0L, 0L
        );

        assertThat(cost)
                .as("零 token 用量应产生零费用")
                .isEqualByComparingTo(BigDecimal.ZERO);
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `cd E:/sinpher/claude-monitoring && GRADLE_USER_HOME=/tmp/codex-gradle21 gradle test --tests "CostCalculationServiceTest"`
Expected: 4 tests PASSED

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/sinpher/claudemonitor/service/CostCalculationService.java src/main/resources/model-pricing.yml src/test/java/com/sinpher/claudemonitor/service/CostCalculationServiceTest.java
git commit -m "添加成本计算服务和模型定价配置"
```

---

### Task 5: JSONL 解析服务

**Files:**
- Create: `src/main/java/com/sinpher/claudemonitor/service/JsonlParserService.java`
- Create: `src/test/java/com/sinpher/claudemonitor/service/JsonlParserServiceTest.java`
- Create: `src/test/resources/test-session.jsonl` (测试用 JSONL 文件)

- [ ] **Step 1: 创建测试用 JSONL 文件 test-session.jsonl**

```jsonl
{"type":"user","message":{"role":"user","content":"帮我写一个函数"},"sessionId":"test-001","timestamp":"2026-06-09T10:00:00Z","permissionMode":"default"}
{"type":"assistant","message":{"role":"assistant","content":[{"type":"thinking","thinking":"让我想想..."},{"type":"tool_use","id":"tool-1","name":"Read","input":{"file_path":"/tmp/test.py"}},{"type":"text","text":"好的，我来帮你写"}],"model":"claude-sonnet-4-6"},"usage":{"input_tokens":0,"output_tokens":0},"sessionId":"test-001","timestamp":"2026-06-09T10:00:05Z"}
{"type":"assistant","message":{"role":"assistant","content":[{"type":"text","text":"函数已写好"}],"model":"claude-sonnet-4-6"},"usage":{"input_tokens":5000,"output_tokens":200,"cache_creation_input_tokens":1000,"cache_read_input_tokens":500,"server_tool_use":{"web_search_requests":0,"web_fetch_requests":0}},"sessionId":"test-001","timestamp":"2026-06-09T10:00:10Z","gitBranch":"main","version":"1.0.0"}
{"type":"system","subtype":"turn_duration","durationMs":5000,"sessionId":"test-001","timestamp":"2026-06-09T10:00:15Z"}
```

- [ ] **Step 2: 编写 JsonlParserServiceTest（先写测试）**

```java
package com.sinpher.claudemonitor.service;

import com.sinpher.claudemonitor.model.Session;
import com.sinpher.claudemonitor.model.ToolCall;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withFailMessage;

/**
 * JsonlParserService 单元测试，验证 JSONL 文件解析逻辑。
 */
class JsonlParserServiceTest {

    private final JsonlParserService parserService = new JsonlParserService();

    /**
     * 测试解析包含完整会话数据的 JSONL 文件。
     * 验证能正确提取 session 信息、token 用量和工具调用。
     */
    @Test
    @DisplayName("解析完整 JSONL 文件应正确提取会话和工具调用数据")
    void shouldParseCompleteJsonlFile(@TempDir Path tempDir) throws IOException {
        // 准备测试数据：写入 JSONL 文件
        String jsonlContent = """
                {"type":"user","message":{"role":"user","content":"帮我写一个函数"},"sessionId":"test-001","timestamp":"2026-06-09T10:00:00Z","permissionMode":"default"}
                {"type":"assistant","message":{"role":"assistant","content":[{"type":"thinking","thinking":"让我想想..."},{"type":"tool_use","id":"tool-1","name":"Read","input":{"file_path":"/tmp/test.py"}},{"type":"text","text":"好的，我来帮你写"}],"model":"claude-sonnet-4-6"},"usage":{"input_tokens":0,"output_tokens":0},"sessionId":"test-001","timestamp":"2026-06-09T10:00:05Z"}
                {"type":"assistant","message":{"role":"assistant","content":[{"type":"text","text":"函数已写好"}],"model":"claude-sonnet-4-6"},"usage":{"input_tokens":5000,"output_tokens":200,"cache_creation_input_tokens":1000,"cache_read_input_tokens":500,"server_tool_use":{"web_search_requests":0,"web_fetch_requests":0}},"sessionId":"test-001","timestamp":"2026-06-09T10:00:10Z","gitBranch":"main","version":"1.0.0"}
                """;
        Path jsonlFile = tempDir.resolve("test-001.jsonl");
        Files.writeString(jsonlFile, jsonlContent);

        // 执行解析
        JsonlParserService.ParseResult result = parserService.parseFile(jsonlFile, 0);

        // 验证会话信息
        assertThat(result)
                .as("解析结果不应为 null")
                .isNotNull();
        assertThat(result.getSession())
                .as("应提取到会话信息")
                .isNotNull();
        assertThat(result.getSession().getId())
                .as("会话 ID 应为 test-001")
                .isEqualTo("test-001");
        assertThat(result.getSession().getModel())
                .as("模型应为 claude-sonnet-4-6")
                .isEqualTo("claude-sonnet-4-6");

        // 验证 token 用量（应过滤掉 token=0 的中间消息）
        assertThat(result.getSession().getTotalInputTokens())
                .as("输入 token 应为 5000（过滤掉 token=0 的中间消息）")
                .isEqualTo(5000L);
        assertThat(result.getSession().getTotalOutputTokens())
                .as("输出 token 应为 200")
                .isEqualTo(200L);
        assertThat(result.getSession().getTotalCacheTokens())
                .as("缓存 token 应为 1500（1000 creation + 500 read）")
                .isEqualTo(1500L);

        // 验证工具调用
        assertThat(result.getToolCalls())
                .as("应提取到 1 个工具调用")
                .hasSize(1);
        assertThat(result.getToolCalls().get(0).getToolName())
                .as("工具名应为 Read")
                .isEqualTo("Read");

        // 验证已读取行数
        assertThat(result.getLinesRead())
                .as("应读取 3 行")
                .isEqualTo(3);
    }

    /**
     * 测试增量解析：只读取新增的行。
     * 第一次读取前 1 行，第二次从第 2 行开始读取。
     */
    @Test
    @DisplayName("增量解析应只读取指定行号之后的新增行")
    void shouldParseIncrementally(@TempDir Path tempDir) throws IOException {
        // 准备测试数据
        String line1 = "{\"type\":\"user\",\"message\":{\"role\":\"user\",\"content\":\"hello\"},\"sessionId\":\"test-002\",\"timestamp\":\"2026-06-09T11:00:00Z\"}\n";
        String line2 = "{\"type\":\"assistant\",\"message\":{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"hi\"}],\"model\":\"claude-sonnet-4-6\"},\"usage\":{\"input_tokens\":100,\"output_tokens\":50},\"sessionId\":\"test-002\",\"timestamp\":\"2026-06-09T11:00:05Z\"}\n";
        Path jsonlFile = tempDir.resolve("test-002.jsonl");

        // 第一次写入 1 行
        Files.writeString(jsonlFile, line1);

        // 第一次解析
        JsonlParserService.ParseResult result1 = parserService.parseFile(jsonlFile, 0);
        assertThat(result1.getLinesRead())
                .as("第一次应读取 1 行")
                .isEqualTo(1);

        // 追加第 2 行
        Files.writeString(jsonlFile, line2, java.nio.file.StandardOpenOption.APPEND);

        // 第二次增量解析
        JsonlParserService.ParseResult result2 = parserService.parseFile(jsonlFile, 1);
        assertThat(result2.getLinesRead())
                .as("第二次增量解析应累计读取 2 行")
                .isEqualTo(2);
        assertThat(result2.getSession().getTotalInputTokens())
                .as("增量解析应提取到新增的 token 数据")
                .isEqualTo(100L);
    }

    /**
     * 测试过滤 token=0 的中间消息。
     * JSONL 中可能有多条 assistant 消息，只有最终消息包含真实 token 数据。
     */
    @Test
    @DisplayName("应过滤 token 全为 0 的中间 assistant 消息")
    void shouldFilterZeroTokenMessages(@TempDir Path tempDir) throws IOException {
        // 准备测试数据：包含 token=0 的中间消息和 token>0 的最终消息
        String jsonlContent = """
                {"type":"assistant","message":{"role":"assistant","content":[{"type":"text","text":"thinking..."}],"model":"claude-sonnet-4-6"},"usage":{"input_tokens":0,"output_tokens":0},"sessionId":"test-003","timestamp":"2026-06-09T12:00:00Z"}
                {"type":"assistant","message":{"role":"assistant","content":[{"type":"text","text":"done"}],"model":"claude-sonnet-4-6"},"usage":{"input_tokens":3000,"output_tokens":100},"sessionId":"test-003","timestamp":"2026-06-09T12:00:05Z"}
                """;
        Path jsonlFile = tempDir.resolve("test-003.jsonl");
        Files.writeString(jsonlFile, jsonlContent);

        // 执行解析
        JsonlParserService.ParseResult result = parserService.parseFile(jsonlFile, 0);

        // 验证：只累计 token>0 的消息
        assertThat(result.getSession().getTotalInputTokens())
                .as("应过滤 token=0 的中间消息，只累计 3000")
                .isEqualTo(3000L);
        assertThat(result.getSession().getTotalOutputTokens())
                .as("输出 token 应为 100")
                .isEqualTo(100L);
    }
}
```

- [ ] **Step 3: 运行测试验证失败（方法未实现）**

Run: `cd E:/sinpher/claude-monitoring && GRADLE_USER_HOME=/tmp/codex-gradle21 gradle test --tests "JsonlParserServiceTest"`
Expected: FAIL - JsonlParserService 类不存在

- [ ] **Step 4: 实现 JsonlParserService**

```java
package com.sinpher.claudemonitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinpher.claudemonitor.model.Session;
import com.sinpher.claudemonitor.model.ToolCall;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JSONL 文件解析服务，负责解析 Claude Code 的会话 JSONL 文件，
 * 提取 token 用量、工具调用和会话元信息。
 * 支持增量解析：通过 lastLine 参数指定从第几行开始读取。
 */
@Service
public class JsonlParserService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析单条 JSONL 行的解析结果。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParseResult {
        /** 解析出的会话信息 */
        private Session session;
        /** 解析出的工具调用列表 */
        private List<ToolCall> toolCalls;
        /** 已读取的总行数 */
        private int linesRead;
    }

    /**
     * 解析 JSONL 文件，从指定行号开始增量读取。
     *
     * @param filePath JSONL 文件路径
     * @param lastLine 上次已读取的行号（从 0 开始，0 表示从头读取）
     * @return 解析结果，包含会话信息、工具调用和已读行数
     */
    public ParseResult parseFile(Path filePath, int lastLine) {
        Session.SessionBuilder sessionBuilder = Session.builder();
        List<ToolCall> toolCalls = new ArrayList<>();
        long totalInputTokens = 0;
        long totalOutputTokens = 0;
        long totalCacheTokens = 0;
        String sessionId = null;
        int linesRead = 0;

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            int currentLine = 0;

            while ((line = reader.readLine()) != null) {
                currentLine++;
                // 跳过已读取的行
                if (currentLine <= lastLine) {
                    continue;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    JsonNode node = objectMapper.readTree(line);
                    String type = node.path("type").asText("");

                    if ("user".equals(type)) {
                        // 提取会话 ID 和开始时间
                        sessionId = node.path("sessionId").asText(null);
                        String timestamp = node.path("timestamp").asText(null);
                        if (timestamp != null) {
                            sessionBuilder.startedAt(parseTimestamp(timestamp));
                        }
                    } else if ("assistant".equals(type)) {
                        // 提取 token 用量
                        JsonNode usage = node.path("usage");
                        long inputTokens = usage.path("input_tokens").asLong(0);
                        long outputTokens = usage.path("output_tokens").asLong(0);
                        long cacheCreation = usage.path("cache_creation_input_tokens").asLong(0);
                        long cacheRead = usage.path("cache_read_input_tokens").asLong(0);

                        // 过滤 token=0 的中间消息
                        if (inputTokens > 0 || outputTokens > 0) {
                            totalInputTokens += inputTokens;
                            totalOutputTokens += outputTokens;
                            totalCacheTokens += cacheCreation + cacheRead;
                        }

                        // 提取会话元信息
                        if (sessionId == null) {
                            sessionId = node.path("sessionId").asText(null);
                        }
                        String model = node.path("message").path("model").asText(null);
                        if (model != null) {
                            sessionBuilder.model(model);
                        }
                        String gitBranch = node.path("gitBranch").asText(null);
                        if (gitBranch != null) {
                            // gitBranch 存储在 project 字段中，后续由 Scanner 补充 project 名
                        }

                        // 提取工具调用
                        JsonNode content = node.path("message").path("content");
                        if (content.isArray()) {
                            for (JsonNode item : content) {
                                if ("tool_use".equals(item.path("type").asText(""))) {
                                    String toolName = item.path("name").asText("");
                                    String toolInput = item.path("input").toString();
                                    String toolTimestamp = node.path("timestamp").asText(null);

                                    if (!toolName.isEmpty()) {
                                        ToolCall toolCall = ToolCall.builder()
                                                .sessionId(sessionId != null ? sessionId : "unknown")
                                                .toolName(toolName)
                                                .inputParams(toolInput)
                                                .timestamp(toolTimestamp != null ? parseTimestamp(toolTimestamp) : LocalDateTime.now())
                                                .build();
                                        toolCalls.add(toolCall);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // 跳过无法解析的行，继续处理后续行
                }
            }

            linesRead = currentLine;

        } catch (IOException e) {
            return ParseResult.builder()
                    .linesRead(lastLine)
                    .toolCalls(List.of())
                    .build();
        }

        // 构建会话对象
        if (sessionId != null) {
            sessionBuilder
                    .id(sessionId)
                    .totalInputTokens(totalInputTokens)
                    .totalOutputTokens(totalOutputTokens)
                    .totalCacheTokens(totalCacheTokens)
                    .toolCallCount(toolCalls.size())
                    .status("ACTIVE");
        }

        return ParseResult.builder()
                .session(sessionId != null ? sessionBuilder.build() : null)
                .toolCalls(toolCalls)
                .linesRead(linesRead)
                .build();
    }

    /**
     * 解析 ISO 时间戳字符串为 LocalDateTime。
     *
     * @param timestamp ISO 格式时间戳（如 2026-06-09T10:00:00Z）
     * @return LocalDateTime 对象
     */
    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            return OffsetDateTime.parse(timestamp).toLocalDateTime();
        } catch (Exception e) {
            return LocalDateTime.parse(timestamp);
        }
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `cd E:/sinpher/claude-monitoring && GRADLE_USER_HOME=/tmp/codex-gradle21 gradle test --tests "JsonlParserServiceTest"`
Expected: 3 tests PASSED

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/sinpher/claudemonitor/service/JsonlParserService.java src/test/java/com/sinpher/claudemonitor/service/JsonlParserServiceTest.java
git commit -m "添加 JSONL 解析服务：支持增量解析和 token 过滤"
```

---

### Task 6: JSONL 扫描调度服务

**Files:**
- Create: `src/main/java/com/sinpher/claudemonitor/service/JsonlScannerService.java`
- Create: `src/main/java/com/sinpher/claudemonitor/config/SchedulingConfig.java`

- [ ] **Step 1: 创建 SchedulingConfig**

```java
package com.sinpher.claudemonitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务配置类，启用 Spring Scheduling 支持。
 * 用于 JSONL 文件的定时扫描。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
```

- [ ] **Step 2: 创建 JsonlScannerService**

```java
package com.sinpher.claudemonitor.service;

import com.sinpher.claudemonitor.model.Session;
import com.sinpher.claudemonitor.model.ToolCall;
import com.sinpher.claudemonitor.repository.SessionRepository;
import com.sinpher.claudemonitor.repository.ToolCallRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSONL 文件扫描调度服务，定时扫描 Claude Code 的项目目录，
 * 发现新的会话文件并增量解析，将结果持久化到数据库。
 */
@Slf4j
@Service
public class JsonlScannerService {

    private final JsonlParserService jsonlParserService;
    private final SessionRepository sessionRepository;
    private final ToolCallRepository toolCallRepository;
    private final CostCalculationService costCalculationService;

    /** Claude Code 数据目录路径 */
    @Value("${claude-monitor.claude-data-dir}")
    private String claudeDataDir;

    /** 每个文件的已读取行号缓存，key 为文件绝对路径 */
    private final Map<String, Integer> fileLineCache = new ConcurrentHashMap<>();

    /**
     * 构造函数。
     *
     * @param jsonlParserService     JSONL 解析服务
     * @param sessionRepository      会话仓库
     * @param toolCallRepository     工具调用仓库
     * @param costCalculationService 成本计算服务
     */
    public JsonlScannerService(JsonlParserService jsonlParserService,
                               SessionRepository sessionRepository,
                               ToolCallRepository toolCallRepository,
                               CostCalculationService costCalculationService) {
        this.jsonlParserService = jsonlParserService;
        this.sessionRepository = sessionRepository;
        this.toolCallRepository = toolCallRepository;
        this.costCalculationService = costCalculationService;
    }

    /**
     * 定时扫描任务，每 5 秒执行一次。
     * 扫描 .claude/projects 目录下的所有 JSONL 文件，增量解析并存储。
     */
    @Scheduled(fixedDelayString = "${claude-monitor.scan-interval}")
    public void scanJsonlFiles() {
        Path projectsDir = Paths.get(claudeDataDir, "projects");
        if (!Files.isDirectory(projectsDir)) {
            return;
        }

        try (DirectoryStream<Path> projectDirs = Files.newDirectoryStream(projectsDir)) {
            for (Path projectDir : projectDirs) {
                if (!Files.isDirectory(projectDir)) {
                    continue;
                }
                String projectName = projectDir.getFileName().toString();
                scanProjectDirectory(projectDir, projectName);
            }
        } catch (IOException e) {
            log.error("扫描项目目录失败: {}", e.getMessage());
        }
    }

    /**
     * 扫描单个项目目录下的所有 JSONL 文件。
     *
     * @param projectDir  项目目录路径
     * @param projectName 项目名称
     */
    private void scanProjectDirectory(Path projectDir, String projectName) {
        try (DirectoryStream<Path> jsonlFiles = Files.newDirectoryStream(projectDir, "*.jsonl")) {
            for (Path jsonlFile : jsonlFiles) {
                processJsonlFile(jsonlFile, projectName);
            }
        } catch (IOException e) {
            log.error("扫描项目 {} 的 JSONL 文件失败: {}", projectName, e.getMessage());
        }
    }

    /**
     * 处理单个 JSONL 文件，增量解析并更新数据库。
     *
     * @param jsonlFile   JSONL 文件路径
     * @param projectName 项目名称
     */
    private void processJsonlFile(Path jsonlFile, String projectName) {
        String fileKey = jsonlFile.toAbsolutePath().toString();
        int lastLine = fileLineCache.getOrDefault(fileKey, 0);

        JsonlParserService.ParseResult result = jsonlParserService.parseFile(jsonlFile, lastLine);

        if (result.getSession() == null) {
            fileLineCache.put(fileKey, result.getLinesRead());
            return;
        }

        // 更新会话信息
        Session parsed = result.getSession();
        Session existing = sessionRepository.findById(parsed.getId()).orElse(null);

        if (existing != null) {
            // 累加 token 用量
            existing.setTotalInputTokens(existing.getTotalInputTokens() + parsed.getTotalInputTokens());
            existing.setTotalOutputTokens(existing.getTotalOutputTokens() + parsed.getTotalOutputTokens());
            existing.setTotalCacheTokens(existing.getTotalCacheTokens() + parsed.getTotalCacheTokens());
            existing.setToolCallCount(existing.getToolCallCount() + parsed.getToolCallCount());
            // 重新计算费用
            existing.setEstimatedCost(costCalculationService.calculateCost(
                    existing.getModel(),
                    existing.getTotalInputTokens(),
                    existing.getTotalOutputTokens(),
                    existing.getTotalCacheTokens(), 0L
            ));
            sessionRepository.save(existing);
        } else {
            // 新会话
            parsed.setProject(projectName);
            parsed.setEstimatedCost(costCalculationService.calculateCost(
                    parsed.getModel(),
                    parsed.getTotalInputTokens(),
                    parsed.getTotalOutputTokens(),
                    parsed.getTotalCacheTokens(), 0L
            ));
            sessionRepository.save(parsed);
        }

        // 保存工具调用
        for (ToolCall toolCall : result.getToolCalls()) {
            toolCallRepository.save(toolCall);
        }

        fileLineCache.put(fileKey, result.getLinesRead());
    }
}
```

- [ ] **Step 3: 验证编译通过**

Run: `cd E:/sinpher/claude-monitoring && GRADLE_USER_HOME=/tmp/codex-gradle21 gradle compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/sinpher/claudemonitor/service/JsonlScannerService.java src/main/java/com/sinpher/claudemonitor/config/SchedulingConfig.java
git commit -m "添加 JSONL 扫描调度服务：定时增量扫描和持久化"
```

---

### Task 7: Hook 事件处理服务

**Files:**
- Create: `src/main/java/com/sinpher/claudemonitor/dto/HookEventDto.java`
- Create: `src/main/java/com/sinpher/claudemonitor/service/HookEventService.java`
- Create: `src/test/java/com/sinpher/claudemonitor/service/HookEventServiceTest.java`

- [ ] **Step 1: 创建 HookEventDto**

```java
package com.sinpher.claudemonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Hook 事件数据传输对象，接收 Claude Code Hooks 推送的事件数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HookEventDto {

    /** Hook 事件类型：UserPromptSubmit / PreToolUse / PostToolUse / Stop */
    private String hookEvent;

    /** 触发的工具名称（仅 PreToolUse / PostToolUse 有值） */
    private String toolName;

    /** 工具输入参数（仅 PreToolUse / PostToolUse 有值） */
    private String toolInput;

    /** 会话 ID */
    private String sessionId;

    /** 事件时间戳 */
    private String timestamp;
}
```

- [ ] **Step 2: 编写 HookEventServiceTest（先写测试）**

```java
package com.sinpher.claudemonitor.service;

import com.sinpher.claudemonitor.dto.HookEventDto;
import com.sinpher.claudemonitor.model.AgentStatus;
import com.sinpher.claudemonitor.repository.AgentStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withFailMessage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HookEventService 单元测试，验证 Hook 事件到 Agent 状态的映射逻辑。
 */
class HookEventServiceTest {

    private AgentStatusRepository agentStatusRepository;
    private AgentStatusService agentStatusService;
    private HookEventService hookEventService;

    @BeforeEach
    void setUp() {
        agentStatusRepository = mock(AgentStatusRepository.class);
        agentStatusService = new AgentStatusService(agentStatusRepository);
        hookEventService = new HookEventService(agentStatusService);
    }

    /**
     * 测试 UserPromptSubmit 事件映射为 WORKING 状态。
     */
    @Test
    @DisplayName("UserPromptSubmit 事件应将 Agent 状态设为 WORKING")
    void shouldSetWorkingOnUserPromptSubmit() {
        // 准备测试数据
        HookEventDto event = HookEventDto.builder()
                .hookEvent("UserPromptSubmit")
                .sessionId("session-001")
                .timestamp("2026-06-09T10:00:00Z")
                .build();
        when(agentStatusRepository.save(any(AgentStatus.class))).thenAnswer(i -> i.getArgument(0));

        // 执行处理
        hookEventService.processHookEvent(event);

        // 验证状态被设为 WORKING
        ArgumentCaptor<AgentStatus> captor = ArgumentCaptor.forClass(AgentStatus.class);
        verify(agentStatusRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus())
                .as("UserPromptSubmit 事件应将状态设为 WORKING")
                .isEqualTo("WORKING");
        assertThat(captor.getValue().getSessionId())
                .as("会话 ID 应与事件一致")
                .isEqualTo("session-001");
    }

    /**
     * 测试 Stop 事件映射为 DONE 状态。
     */
    @Test
    @DisplayName("Stop 事件应将 Agent 状态设为 DONE")
    void shouldSetDoneOnStop() {
        HookEventDto event = HookEventDto.builder()
                .hookEvent("Stop")
                .sessionId("session-001")
                .timestamp("2026-06-09T10:05:00Z")
                .build();
        when(agentStatusRepository.save(any(AgentStatus.class))).thenAnswer(i -> i.getArgument(0));

        hookEventService.processHookEvent(event);

        ArgumentCaptor<AgentStatus> captor = ArgumentCaptor.forClass(AgentStatus.class);
        verify(agentStatusRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus())
                .as("Stop 事件应将状态设为 DONE")
                .isEqualTo("DONE");
    }

    /**
     * 测试 PostToolUse 事件保持 WORKING 状态并记录当前工具。
     */
    @Test
    @DisplayName("PostToolUse 事件应保持 WORKING 状态并记录当前工具")
    void shouldKeepWorkingOnPostToolUse() {
        HookEventDto event = HookEventDto.builder()
                .hookEvent("PostToolUse")
                .toolName("Edit")
                .sessionId("session-001")
                .timestamp("2026-06-09T10:01:00Z")
                .build();
        when(agentStatusRepository.save(any(AgentStatus.class))).thenAnswer(i -> i.getArgument(0));

        hookEventService.processHookEvent(event);

        ArgumentCaptor<AgentStatus> captor = ArgumentCaptor.forClass(AgentStatus.class);
        verify(agentStatusRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus())
                .as("PostToolUse 事件应保持 WORKING 状态")
                .isEqualTo("WORKING");
        assertThat(captor.getValue().getCurrentTool())
                .as("当前工具应为 Edit")
                .isEqualTo("Edit");
    }

    /**
     * 测试 PreToolUse 事件映射为 WAITING 状态。
     */
    @Test
    @DisplayName("PreToolUse 事件应将 Agent 状态设为 WAITING")
    void shouldSetWaitingOnPreToolUse() {
        HookEventDto event = HookEventDto.builder()
                .hookEvent("PreToolUse")
                .toolName("Bash")
                .sessionId("session-001")
                .timestamp("2026-06-09T10:02:00Z")
                .build();
        when(agentStatusRepository.save(any(AgentStatus.class))).thenAnswer(i -> i.getArgument(0));

        hookEventService.processHookEvent(event);

        ArgumentCaptor<AgentStatus> captor = ArgumentCaptor.forClass(AgentStatus.class);
        verify(agentStatusRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus())
                .as("PreToolUse 事件应将状态设为 WAITING")
                .isEqualTo("WAITING");
        assertThat(captor.getValue().getCurrentTool())
                .as("当前工具应为 Bash")
                .isEqualTo("Bash");
    }
}
```

- [ ] **Step 3: 创建 AgentStatusService**

```java
package com.sinpher.claudemonitor.service;

import com.sinpher.claudemonitor.model.AgentStatus;
import com.sinpher.claudemonitor.repository.AgentStatusRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent 状态管理服务，负责更新和查询 Agent 的实时工作状态。
 */
@Service
public class AgentStatusService {

    private final AgentStatusRepository agentStatusRepository;

    /**
     * 构造函数。
     *
     * @param agentStatusRepository Agent 状态仓库
     */
    public AgentStatusService(AgentStatusRepository agentStatusRepository) {
        this.agentStatusRepository = agentStatusRepository;
    }

    /**
     * 更新指定会话的 Agent 状态。
     *
     * @param sessionId   会话 ID
     * @param status      新状态（WORKING / DONE / WAITING / IDLE）
     * @param currentTool 当前执行的工具名称，可为 null
     */
    public void updateStatus(String sessionId, String status, String currentTool) {
        AgentStatus agentStatus = agentStatusRepository.findById(sessionId)
                .orElse(AgentStatus.builder()
                        .sessionId(sessionId)
                        .build());
        agentStatus.setStatus(status);
        agentStatus.setCurrentTool(currentTool);
        agentStatus.setLastUpdatedAt(LocalDateTime.now());
        agentStatusRepository.save(agentStatus);
    }

    /**
     * 查询所有活跃的 Agent 状态（非 IDLE）。
     *
     * @return 活跃 Agent 状态列表
     */
    public List<AgentStatus> getActiveStatuses() {
        return agentStatusRepository.findAll().stream()
                .filter(s -> !"IDLE".equals(s.getStatus()))
                .toList();
    }
}
```

- [ ] **Step 4: 创建 HookEventService**

```java
package com.sinpher.claudemonitor.service;

import com.sinpher.claudemonitor.dto.HookEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Hook 事件处理服务，将 Claude Code 的 Hook 事件映射为 Agent 状态变更。
 *
 * 事件到状态的映射规则：
 * - UserPromptSubmit → WORKING
 * - PreToolUse → WAITING
 * - PostToolUse → WORKING（记录当前工具）
 * - Stop → DONE
 */
@Slf4j
@Service
public class HookEventService {

    private final AgentStatusService agentStatusService;

    /**
     * 构造函数。
     *
     * @param agentStatusService Agent 状态服务
     */
    public HookEventService(AgentStatusService agentStatusService) {
        this.agentStatusService = agentStatusService;
    }

    /**
     * 处理接收到的 Hook 事件，更新 Agent 状态。
     *
     * @param event Hook 事件 DTO
     */
    public void processHookEvent(HookEventDto event) {
        String sessionId = event.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            log.warn("收到缺少 sessionId 的 Hook 事件: {}", event.getHookEvent());
            return;
        }

        String status = mapEventToStatus(event.getHookEvent());
        String currentTool = extractCurrentTool(event);

        agentStatusService.updateStatus(sessionId, status, currentTool);
        log.debug("Hook 事件处理完成: event={}, sessionId={}, status={}",
                event.getHookEvent(), sessionId, status);
    }

    /**
     * 将 Hook 事件类型映射为 Agent 状态。
     *
     * @param hookEvent Hook 事件类型
     * @return 对应的 Agent 状态
     */
    private String mapEventToStatus(String hookEvent) {
        return switch (hookEvent) {
            case "UserPromptSubmit" -> "WORKING";
            case "PreToolUse" -> "WAITING";
            case "PostToolUse" -> "WORKING";
            case "Stop" -> "DONE";
            default -> "IDLE";
        };
    }

    /**
     * 从 Hook 事件中提取当前工具名称。
     *
     * @param event Hook 事件 DTO
     * @return 工具名称，无工具时返回 null
     */
    private String extractCurrentTool(HookEventDto event) {
        if ("PostToolUse".equals(event.getHookEvent()) || "PreToolUse".equals(event.getHookEvent())) {
            return event.getToolName();
        }
        return null;
    }
}
```

- [ ] **Step 5: 运行测试验证通过**

Run: `cd E:/sinpher/claude-monitoring && GRADLE_USER_HOME=/tmp/codex-gradle21 gradle test --tests "HookEventServiceTest"`
Expected: 4 tests PASSED

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/sinpher/claudemonitor/dto/ src/main/java/com/sinpher/claudemonitor/service/HookEventService.java src/main/java/com/sinpher/claudemonitor/service/AgentStatusService.java src/test/java/com/sinpher/claudemonitor/service/HookEventServiceTest.java
git commit -m "添加 Hook 事件处理服务和 Agent 状态管理"
```

---

### Task 8: REST API 控制器

**Files:**
- Create: `src/main/java/com/sinpher/claudemonitor/dto/SessionDetailDto.java`
- Create: `src/main/java/com/sinpher/claudemonitor/dto/DailyUsageDto.java`
- Create: `src/main/java/com/sinpher/claudemonitor/dto/AgentStatusDto.java`
- Create: `src/main/java/com/sinpher/claudemonitor/service/UsageService.java`
- Create: `src/main/java/com/sinpher/claudemonitor/controller/SessionController.java`
- Create: `src/main/java/com/sinpher/claudemonitor/controller/UsageController.java`
- Create: `src/main/java/com/sinpher/claudemonitor/controller/HookEventController.java`
- Create: `src/test/java/com/sinpher/claudemonitor/controller/HookEventControllerTest.java`

- [ ] **Step 1: 创建 DTO 类**

SessionDetailDto:
```java
package com.sinpher.claudemonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会话详情数据传输对象，用于 API 响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDetailDto {
    private String id;
    private String project;
    private String model;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long totalInputTokens;
    private Long totalOutputTokens;
    private Long totalCacheTokens;
    private BigDecimal estimatedCost;
    private Integer toolCallCount;
    private String status;
}
```

DailyUsageDto:
```java
package com.sinpher.claudemonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 日用量数据传输对象，用于 API 响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyUsageDto {
    private LocalDate date;
    private Long totalInputTokens;
    private Long totalOutputTokens;
    private Long totalCacheTokens;
    private BigDecimal estimatedCost;
    private Integer sessionCount;
    private Integer toolCallCount;
}
```

AgentStatusDto:
```java
package com.sinpher.claudemonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent 实时状态数据传输对象，用于 API 和 WebSocket 响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStatusDto {
    private String sessionId;
    private String status;
    private String currentTool;
    private LocalDateTime lastUpdatedAt;
}
```

- [ ] **Step 2: 创建 UsageService**

```java
package com.sinpher.claudemonitor.service;

import com.sinpher.claudemonitor.dto.DailyUsageDto;
import com.sinpher.claudemonitor.model.DailyUsage;
import com.sinpher.claudemonitor.model.Session;
import com.sinpher.claudemonitor.repository.DailyUsageRepository;
import com.sinpher.claudemonitor.repository.SessionRepository;
import com.sinpher.claudemonitor.repository.ToolCallRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 用量统计服务，提供日用量和月用量的聚合查询。
 */
@Service
public class UsageService {

    private final SessionRepository sessionRepository;
    private final DailyUsageRepository dailyUsageRepository;
    private final ToolCallRepository toolCallRepository;

    /**
     * 构造函数。
     *
     * @param sessionRepository    会话仓库
     * @param dailyUsageRepository 日用量仓库
     * @param toolCallRepository   工具调用仓库
     */
    public UsageService(SessionRepository sessionRepository,
                        DailyUsageRepository dailyUsageRepository,
                        ToolCallRepository toolCallRepository) {
        this.sessionRepository = sessionRepository;
        this.dailyUsageRepository = dailyUsageRepository;
        this.toolCallRepository = toolCallRepository;
    }

    /**
     * 查询指定日期范围内的日用量统计。
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return 日用量 DTO 列表
     */
    public List<DailyUsageDto> getDailyUsage(LocalDate start, LocalDate end) {
        List<DailyUsage> usages = dailyUsageRepository.findByDateBetweenOrderByDateAsc(start, end);
        return usages.stream().map(this::toDailyUsageDto).toList();
    }

    /**
     * 获取今日用量概览。
     *
     * @return 今日的日用量 DTO
     */
    public DailyUsageDto getTodayUsage() {
        LocalDate today = LocalDate.now();
        List<Session> todaySessions = sessionRepository.findByStartedAtBetweenOrderByStartedAtDesc(
                today.atStartOfDay(), today.atTime(LocalTime.MAX));

        long totalInput = todaySessions.stream().mapToLong(Session::getTotalInputTokens).sum();
        long totalOutput = todaySessions.stream().mapToLong(Session::getTotalOutputTokens).sum();
        long totalCache = todaySessions.stream().mapToLong(Session::getTotalCacheTokens).sum();
        BigDecimal totalCost = todaySessions.stream()
                .map(Session::getEstimatedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long toolCalls = toolCallRepository.countByTimestampBetween(
                today.atStartOfDay(), today.atTime(LocalTime.MAX));

        return DailyUsageDto.builder()
                .date(today)
                .totalInputTokens(totalInput)
                .totalOutputTokens(totalOutput)
                .totalCacheTokens(totalCache)
                .estimatedCost(totalCost)
                .sessionCount(todaySessions.size())
                .toolCallCount((int) toolCalls)
                .build();
    }

    /**
     * 将 DailyUsage 实体转换为 DTO。
     *
     * @param usage DailyUsage 实体
     * @return DailyUsageDto
     */
    private DailyUsageDto toDailyUsageDto(DailyUsage usage) {
        return DailyUsageDto.builder()
                .date(usage.getDate())
                .totalInputTokens(usage.getTotalInputTokens())
                .totalOutputTokens(usage.getTotalOutputTokens())
                .totalCacheTokens(usage.getTotalCacheTokens())
                .estimatedCost(usage.getEstimatedCost())
                .sessionCount(usage.getSessionCount())
                .toolCallCount(usage.getToolCallCount())
                .build();
    }
}
```

- [ ] **Step 3: 创建 SessionController**

```java
package com.sinpher.claudemonitor.controller;

import com.sinpher.claudemonitor.dto.SessionDetailDto;
import com.sinpher.claudemonitor.model.Session;
import com.sinpher.claudemonitor.repository.SessionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 会话 API 控制器，提供会话列表和详情查询。
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionRepository sessionRepository;

    /**
     * 构造函数。
     *
     * @param sessionRepository 会话仓库
     */
    public SessionController(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * 查询会话列表，支持按日期范围筛选。
     *
     * @param start 开始日期（可选，格式 yyyy-MM-dd）
     * @param end   结束日期（可选，格式 yyyy-MM-dd）
     * @return 会话详情列表
     */
    @GetMapping
    public ResponseEntity<List<SessionDetailDto>> listSessions(
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end) {

        List<Session> sessions;
        if (start != null && end != null) {
            sessions = sessionRepository.findByStartedAtBetweenOrderByStartedAtDesc(
                    start.atStartOfDay(), end.atTime(LocalTime.MAX));
        } else {
            sessions = sessionRepository.findAll();
        }

        List<SessionDetailDto> dtos = sessions.stream().map(this::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * 查询单个会话详情。
     *
     * @param id 会话 ID
     * @return 会话详情，不存在返回 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<SessionDetailDto> getSession(@PathVariable String id) {
        return sessionRepository.findById(id)
                .map(session -> ResponseEntity.ok(toDto(session)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 将 Session 实体转换为 DTO。
     *
     * @param session Session 实体
     * @return SessionDetailDto
     */
    private SessionDetailDto toDto(Session session) {
        return SessionDetailDto.builder()
                .id(session.getId())
                .project(session.getProject())
                .model(session.getModel())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .totalInputTokens(session.getTotalInputTokens())
                .totalOutputTokens(session.getTotalOutputTokens())
                .totalCacheTokens(session.getTotalCacheTokens())
                .estimatedCost(session.getEstimatedCost())
                .toolCallCount(session.getToolCallCount())
                .status(session.getStatus())
                .build();
    }
}
```

- [ ] **Step 4: 创建 UsageController**

```java
package com.sinpher.claudemonitor.controller;

import com.sinpher.claudemonitor.dto.DailyUsageDto;
import com.sinpher.claudemonitor.service.UsageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 用量统计 API 控制器，提供日用量和今日概览查询。
 */
@RestController
@RequestMapping("/api/usage")
public class UsageController {

    private final UsageService usageService;

    /**
     * 构造函数。
     *
     * @param usageService 用量统计服务
     */
    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    /**
     * 查询日用量统计。
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return 日用量列表
     */
    @GetMapping("/daily")
    public ResponseEntity<List<DailyUsageDto>> getDailyUsage(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end) {
        return ResponseEntity.ok(usageService.getDailyUsage(start, end));
    }

    /**
     * 查询今日用量概览。
     *
     * @return 今日用量
     */
    @GetMapping("/today")
    public ResponseEntity<DailyUsageDto> getTodayUsage() {
        return ResponseEntity.ok(usageService.getTodayUsage());
    }
}
```

- [ ] **Step 5: 创建 HookEventController**

```java
package com.sinpher.claudemonitor.controller;

import com.sinpher.claudemonitor.dto.HookEventDto;
import com.sinpher.claudemonitor.service.HookEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hook 事件接收控制器，接收 Claude Code Hooks 推送的事件。
 */
@RestController
@RequestMapping("/api/hooks")
public class HookEventController {

    private final HookEventService hookEventService;

    /**
     * 构造函数。
     *
     * @param hookEventService Hook 事件处理服务
     */
    public HookEventController(HookEventService hookEventService) {
        this.hookEventService = hookEventService;
    }

    /**
     * 接收 Hook 事件并处理。
     *
     * @param event Hook 事件 DTO
     * @return 200 OK
     */
    @PostMapping("/event")
    public ResponseEntity<Void> receiveHookEvent(@RequestBody HookEventDto event) {
        hookEventService.processHookEvent(event);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 6: 编写 HookEventControllerTest**

```java
package com.sinpher.claudemonitor.controller;

import com.sinpher.claudemonitor.dto.HookEventDto;
import com.sinpher.claudemonitor.service.AgentStatusService;
import com.sinpher.claudemonitor.service.HookEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HookEventController 测试，验证 Hook 事件接收端点。
 */
@WebMvcTest(HookEventController.class)
class HookEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HookEventService hookEventService;

    /**
     * 测试 POST /api/hooks/event 端点能正确接收事件。
     */
    @Test
    @DisplayName("POST /api/hooks/event 应返回 200 并调用处理服务")
    void shouldReceiveHookEvent() throws Exception {
        // 准备测试数据
        String json = """
                {"hookEvent":"UserPromptSubmit","sessionId":"test-001","timestamp":"2026-06-09T10:00:00Z"}
                """;

        // 执行请求并验证
        mockMvc.perform(post("/api/hooks/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        // 验证服务被调用
        verify(hookEventService).processHookEvent(any(HookEventDto.class));
    }
}
```

- [ ] **Step 7: 运行测试验证通过**

Run: `cd E:/sinpher/claude-monitoring && GRADLE_USER_HOME=/tmp/codex-gradle21 gradle test --tests "HookEventControllerTest"`
Expected: 1 test PASSED

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/sinpher/claudemonitor/dto/ src/main/java/com/sinpher/claudemonitor/service/UsageService.java src/main/java/com/sinpher/claudemonitor/controller/ src/test/java/com/sinpher/claudemonitor/controller/
git commit -m "添加 REST API 控制器：会话、用量、Hook 事件端点"
```

---

### Task 9: WebSocket 实时状态推送

**Files:**
- Create: `src/main/java/com/sinpher/claudemonitor/config/WebSocketConfig.java`
- Create: `src/main/java/com/sinpher/claudemonitor/websocket/AgentStatusHandler.java`

- [ ] **Step 1: 创建 WebSocketConfig**

```java
package com.sinpher.claudemonitor.config;

import com.sinpher.claudemonitor.websocket.AgentStatusHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置类，注册 Agent 状态实时推送端点。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AgentStatusHandler agentStatusHandler;

    /**
     * 构造函数。
     *
     * @param agentStatusHandler Agent 状态 WebSocket 处理器
     */
    public WebSocketConfig(AgentStatusHandler agentStatusHandler) {
        this.agentStatusHandler = agentStatusHandler;
    }

    /**
     * 注册 WebSocket 端点。
     * 前端通过 ws://localhost:8080/ws/agent-status 连接获取实时状态。
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(agentStatusHandler, "/ws/agent-status")
                .setAllowedOrigins("*");
    }
}
```

- [ ] **Step 2: 创建 AgentStatusHandler**

```java
package com.sinpher.claudemonitor.websocket;

import com.sinpher.claudemonitor.dto.AgentStatusDto;
import com.sinpher.claudemonitor.model.AgentStatus;
import com.sinpher.claudemonitor.service.AgentStatusService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Agent 状态 WebSocket 处理器，向前端推送实时状态变更。
 * 当有客户端连接时，定时推送当前所有活跃 Agent 的状态。
 */
@Slf4j
@Component
public class AgentStatusHandler extends TextWebSocketHandler {

    private final AgentStatusService agentStatusService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 当前连接的 WebSocket 会话列表 */
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    /**
     * 构造函数。
     *
     * @param agentStatusService Agent 状态服务
     */
    public AgentStatusHandler(AgentStatusService agentStatusService) {
        this.agentStatusService = agentStatusService;
    }

    /**
     * 新连接建立时，将 session 加入列表。
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.debug("WebSocket 连接建立: {}", session.getId());
    }

    /**
     * 连接关闭时，移除 session。
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.debug("WebSocket 连接关闭: {}", session.getId());
    }

    /**
     * 定时推送 Agent 状态（每 2 秒）。
     * 向所有已连接的客户端广播当前活跃 Agent 状态。
     */
    @Scheduled(fixedRate = 2000)
    public void broadcastAgentStatus() {
        if (sessions.isEmpty()) {
            return;
        }

        List<AgentStatusDto> statuses = agentStatusService.getActiveStatuses().stream()
                .map(this::toDto)
                .toList();

        try {
            String json = objectMapper.writeValueAsString(statuses);
            TextMessage message = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException e) {
            log.error("推送 Agent 状态失败: {}", e.getMessage());
        }
    }

    /**
     * 将 AgentStatus 实体转换为 DTO。
     *
     * @param status AgentStatus 实体
     * @return AgentStatusDto
     */
    private AgentStatusDto toDto(AgentStatus status) {
        return AgentStatusDto.builder()
                .sessionId(status.getSessionId())
                .status(status.getStatus())
                .currentTool(status.getCurrentTool())
                .lastUpdatedAt(status.getLastUpdatedAt())
                .build();
    }
}
```

- [ ] **Step 3: 验证编译通过**

Run: `cd E:/sinpher/claude-monitoring && GRADLE_USER_HOME=/tmp/codex-gradle21 gradle compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/sinpher/claudemonitor/config/WebSocketConfig.java src/main/java/com/sinpher/claudemonitor/websocket/AgentStatusHandler.java
git commit -m "添加 WebSocket 实时状态推送：Agent 状态每 2 秒广播"
```

---

### Task 10: Hook 脚本与配置

**Files:**
- Create: `hooks/claude-monitor-hook.cmd` (Windows Hook 包装脚本)
- Create: `hooks/claude-monitor-hook.sh` (Linux/Mac Hook 脚本)

- [ ] **Step 1: 创建 Windows Hook 包装脚本 claude-monitor-hook.cmd**

```batch
@echo off
REM Claude Monitor Hook 包装脚本（Windows）
REM 从 stdin 读取 Hook 事件数据，POST 到 SpringBoot 后端

setlocal enabledelayedexpansion

REM 读取 stdin 到变量
set "input="
for /f "delims=" %%a in ('more') do set "input=%%a"

REM POST 到后端
curl -s -X POST http://localhost:8080/api/hooks/event -H "Content-Type: application/json" -d "!input!" >nul 2>&1

endlocal
```

- [ ] **Step 2: 创建 Linux/Mac Hook 脚本 claude-monitor-hook.sh**

```bash
#!/bin/bash
# Claude Monitor Hook 脚本（Linux/Mac）
# 从 stdin 读取 Hook 事件数据，POST 到 SpringBoot 后端

input=$(cat)
curl -s -X POST http://localhost:8080/api/hooks/event \
  -H "Content-Type: application/json" \
  -d "$input" > /dev/null 2>&1
```

- [ ] **Step 3: Commit**

```bash
git add hooks/
git commit -m "添加 Claude Code Hook 脚本：Windows 和 Linux/Mac"
```

---

### Task 11: 全量测试与启动验证

**Files:**
- Modify: `src/main/resources/application.yml` (补充 model-pricing 引用)

- [ ] **Step 1: 运行全量测试**

Run: `cd E:/sinpher/claude-monitoring && GRADLE_USER_HOME=/tmp/codex-gradle21 gradle test`
Expected: ALL tests PASSED

- [ ] **Step 2: 启动应用验证**

Run: `cd E:/sinpher/claude-monitoring && GRADLE_USER_HOME=/tmp/codex-gradle21 gradle bootRun`
Expected: 应用启动成功，日志显示 Tomcat started on port 8080

- [ ] **Step 3: 验证 API 端点可用**

Run: `curl http://localhost:8080/api/sessions`
Expected: 返回空数组 `[]`

Run: `curl http://localhost:8080/api/usage/today`
Expected: 返回今日用量 JSON

- [ ] **Step 4: Commit（如有修复）**

```bash
git add -A
git commit -m "全量测试通过，后端 P0 功能完成"
```

---

## 自检结果

**1. Spec 覆盖检查：**
- Token 用量统计 → Task 2 (数据模型) + Task 4 (成本计算) + Task 5/6 (JSONL 解析) + Task 8 (API)
- 实时状态悬浮窗 → Task 7 (Hook 事件) + Task 9 (WebSocket)
- JSONL 解析器 → Task 5 + Task 6
- Hooks 接收端 → Task 7 + Task 8 (HookEventController) + Task 10 (脚本)
- REST API → Task 8
- WebSocket → Task 9
- 成本计算 → Task 4
- 数据模型 → Task 2 + Task 3

**2. Placeholder 扫描：** 无 TBD/TODO/待定内容

**3. 类型一致性：** 所有 DTO、实体、Repository 方法签名在 Task 间保持一致
