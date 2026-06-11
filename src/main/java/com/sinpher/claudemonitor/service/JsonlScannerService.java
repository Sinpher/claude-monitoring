package com.sinpher.claudemonitor.service;

import com.sinpher.claudemonitor.model.Session;
import com.sinpher.claudemonitor.model.TokenUsage;
import com.sinpher.claudemonitor.model.ToolCall;
import com.sinpher.claudemonitor.repository.SessionRepository;
import com.sinpher.claudemonitor.repository.TokenUsageRepository;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSONL 文件扫描调度服务，定时扫描 Claude Code 的项目目录，
 * 发现新的会话文件并增量解析，将结果持久化到数据库。
 * 同时根据文件增长情况推断 Agent 工作状态。
 *
 * @author sinpher
 */
@Slf4j
@Service
public class JsonlScannerService {

    private final JsonlParserService jsonlParserService;
    private final SessionRepository sessionRepository;
    private final ToolCallRepository toolCallRepository;
    private final TokenUsageRepository tokenUsageRepository;
    private final CostCalculationService costCalculationService;

    /** Claude Code 数据目录路径 */
    @Value("${claude-monitor.claude-data-dir}")
    private String claudeDataDir;

    /** 每个文件的已读取行号缓存，key 为文件绝对路径 */
    private final ConcurrentHashMap<String, Integer> fileLineCache = new ConcurrentHashMap<>();

    /** 标记文件是否已完成首次扫描（首次扫描仅建立基准行号，不触发状态更新） */
    private final ConcurrentHashMap<String, Boolean> fileInitialScanDone = new ConcurrentHashMap<>();

    /**
     * 构造函数。
     *
     * @param jsonlParserService     JSONL 解析服务
     * @param sessionRepository      会话仓库
     * @param toolCallRepository     工具调用仓库
     * @param tokenUsageRepository   Token 用量仓库
     * @param costCalculationService 成本计算服务
     */
    public JsonlScannerService(JsonlParserService jsonlParserService,
                               SessionRepository sessionRepository,
                               ToolCallRepository toolCallRepository,
                               TokenUsageRepository tokenUsageRepository,
                               CostCalculationService costCalculationService) {
        this.jsonlParserService = jsonlParserService;
        this.sessionRepository = sessionRepository;
        this.toolCallRepository = toolCallRepository;
        this.tokenUsageRepository = tokenUsageRepository;
        this.costCalculationService = costCalculationService;
    }

    /**
     * 定时扫描任务，按配置的间隔执行。
     * 扫描 .claude/projects 目录下的所有 JSONL 文件，增量解析并存储。
     */
    @Scheduled(fixedDelayString = "${claude-monitor.scan-interval}")
    public void scanJsonlFiles() {
        Path projectsDir = Paths.get(claudeDataDir, "projects");
        if (!Files.isDirectory(projectsDir)) {
            log.debug("项目目录不存在或不是目录: {}", projectsDir);
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
        boolean isFirstScan = !fileInitialScanDone.containsKey(fileKey);

        JsonlParserService.ParseResult result = jsonlParserService.parseFile(jsonlFile, lastLine);

        if (result.getSession() == null) {
            fileLineCache.put(fileKey, result.getLinesRead());
            fileInitialScanDone.put(fileKey, true);
            return;
        }

        Session parsed = result.getSession();
        Session existing = sessionRepository.findById(parsed.getId()).orElse(null);

        if (existing != null) {
            // 增量更新已存在的会话
            existing.setTotalInputTokens(existing.getTotalInputTokens() + parsed.getTotalInputTokens());
            existing.setTotalOutputTokens(existing.getTotalOutputTokens() + parsed.getTotalOutputTokens());
            existing.setTotalCacheTokens(existing.getTotalCacheTokens() + parsed.getTotalCacheTokens());
            existing.setToolCallCount(existing.getToolCallCount() + parsed.getToolCallCount());
            // 重新计算费用，使用 cacheTokens 作为 cacheCreation，cacheRead 暂为 0
            existing.setEstimatedCost(costCalculationService.calculateCost(
                    existing.getModel(),
                    existing.getTotalInputTokens(),
                    existing.getTotalOutputTokens(),
                    existing.getTotalCacheTokens(), 0L
            ));
            sessionRepository.save(existing);
        } else {
            // 保存新会话
            parsed.setProject(projectName);
            parsed.setEstimatedCost(costCalculationService.calculateCost(
                    parsed.getModel(),
                    parsed.getTotalInputTokens(),
                    parsed.getTotalOutputTokens(),
                    parsed.getTotalCacheTokens(), 0L
            ));
            sessionRepository.save(parsed);
        }

        // 保存工具调用记录
        for (ToolCall toolCall : result.getToolCalls()) {
            toolCallRepository.save(toolCall);
        }

        // 保存 Token 用量记录
        for (TokenUsage tokenUsage : result.getTokenUsages()) {
            tokenUsageRepository.save(tokenUsage);
        }

        fileLineCache.put(fileKey, result.getLinesRead());
        fileInitialScanDone.put(fileKey, true);
    }
}
