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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JsonlParserService 单元测试，验证 JSONL 文件解析逻辑。
 */
class JsonlParserServiceTest {

    private final JsonlParserService parserService = new JsonlParserService();

    @Test
    @DisplayName("解析完整 JSONL 文件应正确提取会话和工具调用数据")
    void shouldParseCompleteJsonlFile(@TempDir Path tempDir) throws IOException {
        // 测试数据准备：构造包含 user、assistant（含 tool_use）和 assistant（含 token）的 JSONL 文件
        String jsonlContent = """
                {"type":"user","message":{"role":"user","content":"帮我写一个函数"},"sessionId":"test-001","timestamp":"2026-06-09T10:00:00Z","permissionMode":"default"}
                {"type":"assistant","message":{"role":"assistant","content":[{"type":"thinking","thinking":"让我想想..."},{"type":"tool_use","id":"tool-1","name":"Read","input":{"file_path":"/tmp/test.py"}},{"type":"text","text":"好的，我来帮你写"}],"model":"claude-sonnet-4-6"},"usage":{"input_tokens":0,"output_tokens":0},"sessionId":"test-001","timestamp":"2026-06-09T10:00:05Z"}
                {"type":"assistant","message":{"role":"assistant","content":[{"type":"text","text":"函数已写好"}],"model":"claude-sonnet-4-6"},"usage":{"input_tokens":5000,"output_tokens":200,"cache_creation_input_tokens":1000,"cache_read_input_tokens":500,"server_tool_use":{"web_search_requests":0,"web_fetch_requests":0}},"sessionId":"test-001","timestamp":"2026-06-09T10:00:10Z","gitBranch":"main","version":"1.0.0"}
                """;
        Path jsonlFile = tempDir.resolve("test-001.jsonl");
        Files.writeString(jsonlFile, jsonlContent);

        // 被测动作执行：解析完整 JSONL 文件
        JsonlParserService.ParseResult result = parserService.parseFile(jsonlFile, 0);

        // 结果验证：会话基本信息
        assertThat(result).as("解析结果不应为 null").isNotNull();
        assertThat(result.getSession()).as("应提取到会话信息").isNotNull();
        assertThat(result.getSession().getId()).as("会话 ID 应为 test-001").isEqualTo("test-001");
        assertThat(result.getSession().getModel()).as("模型应为 claude-sonnet-4-6").isEqualTo("claude-sonnet-4-6");

        // 结果验证：token 累计（过滤了 tokens 全为 0 的中间消息）
        assertThat(result.getSession().getTotalInputTokens()).as("输入 token 应为 5000").isEqualTo(5000L);
        assertThat(result.getSession().getTotalOutputTokens()).as("输出 token 应为 200").isEqualTo(200L);
        assertThat(result.getSession().getTotalCacheTokens()).as("缓存 token 应为 1500").isEqualTo(1500L);

        // 结果验证：工具调用
        assertThat(result.getToolCalls()).as("应提取到 1 个工具调用").hasSize(1);
        assertThat(result.getToolCalls().get(0).getToolName()).as("工具名应为 Read").isEqualTo("Read");

        // 结果验证：行数
        assertThat(result.getLinesRead()).as("应读取 3 行").isEqualTo(3);
    }

    @Test
    @DisplayName("增量解析应只读取指定行号之后的新增行")
    void shouldParseIncrementally(@TempDir Path tempDir) throws IOException {
        // 测试数据准备：分两步写入 JSONL 文件以模拟增量追加
        String line1 = "{\"type\":\"user\",\"message\":{\"role\":\"user\",\"content\":\"hello\"},\"sessionId\":\"test-002\",\"timestamp\":\"2026-06-09T11:00:00Z\"}\n";
        String line2 = "{\"type\":\"assistant\",\"message\":{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"hi\"}],\"model\":\"claude-sonnet-4-6\"},\"usage\":{\"input_tokens\":100,\"output_tokens\":50},\"sessionId\":\"test-002\",\"timestamp\":\"2026-06-09T11:00:05Z\"}\n";
        Path jsonlFile = tempDir.resolve("test-002.jsonl");

        // 第一次解析：只读第 1 行
        Files.writeString(jsonlFile, line1);
        JsonlParserService.ParseResult result1 = parserService.parseFile(jsonlFile, 0);
        assertThat(result1.getLinesRead()).as("第一次应读取 1 行").isEqualTo(1);

        // 追加第 2 行后增量解析：从第 2 行开始
        Files.writeString(jsonlFile, line2, java.nio.file.StandardOpenOption.APPEND);
        JsonlParserService.ParseResult result2 = parserService.parseFile(jsonlFile, 1);
        assertThat(result2.getLinesRead()).as("第二次增量解析应累计读取 2 行").isEqualTo(2);
        assertThat(result2.getSession().getTotalInputTokens()).as("增量解析应提取到新增的 token 数据").isEqualTo(100L);
    }

    @Test
    @DisplayName("应过滤 token 全为 0 的中间 assistant 消息")
    void shouldFilterZeroTokenMessages(@TempDir Path tempDir) throws IOException {
        // 测试数据准备：两条 assistant 消息，第一条 token 全为 0 应被过滤
        String jsonlContent = """
                {"type":"assistant","message":{"role":"assistant","content":[{"type":"text","text":"thinking..."}],"model":"claude-sonnet-4-6"},"usage":{"input_tokens":0,"output_tokens":0},"sessionId":"test-003","timestamp":"2026-06-09T12:00:00Z"}
                {"type":"assistant","message":{"role":"assistant","content":[{"type":"text","text":"done"}],"model":"claude-sonnet-4-6"},"usage":{"input_tokens":3000,"output_tokens":100},"sessionId":"test-003","timestamp":"2026-06-09T12:00:05Z"}
                """;
        Path jsonlFile = tempDir.resolve("test-003.jsonl");
        Files.writeString(jsonlFile, jsonlContent);

        // 被测动作执行
        JsonlParserService.ParseResult result = parserService.parseFile(jsonlFile, 0);

        // 结果验证：token=0 的消息不计入累计
        assertThat(result.getSession().getTotalInputTokens()).as("应过滤 token=0 的中间消息").isEqualTo(3000L);
        assertThat(result.getSession().getTotalOutputTokens()).as("输出 token 应为 100").isEqualTo(100L);
    }
}
