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
