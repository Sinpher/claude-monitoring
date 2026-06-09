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
                        sessionId = node.path("sessionId").asText(null);
                        String timestamp = node.path("timestamp").asText(null);
                        if (timestamp != null) {
                            sessionBuilder.startedAt(parseTimestamp(timestamp));
                        }
                    } else if ("assistant".equals(type)) {
                        JsonNode usage = node.path("usage");
                        long inputTokens = usage.path("input_tokens").asLong(0);
                        long outputTokens = usage.path("output_tokens").asLong(0);
                        long cacheCreation = usage.path("cache_creation_input_tokens").asLong(0);
                        long cacheRead = usage.path("cache_read_input_tokens").asLong(0);

                        if (inputTokens > 0 || outputTokens > 0) {
                            totalInputTokens += inputTokens;
                            totalOutputTokens += outputTokens;
                            totalCacheTokens += cacheCreation + cacheRead;
                        }

                        if (sessionId == null) {
                            sessionId = node.path("sessionId").asText(null);
                        }
                        String model = node.path("message").path("model").asText(null);
                        if (model != null) {
                            sessionBuilder.model(model);
                        }

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
                    // Skip unparseable lines
                }
            }

            linesRead = currentLine;

        } catch (IOException e) {
            return ParseResult.builder()
                    .linesRead(lastLine)
                    .toolCalls(List.of())
                    .build();
        }

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
     * @param timestamp ISO 格式的时间戳字符串
     * @return 解析后的 LocalDateTime，解析失败时返回当前时间
     */
    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            return OffsetDateTime.parse(timestamp).toLocalDateTime();
        } catch (Exception e) {
            return LocalDateTime.parse(timestamp);
        }
    }
}
