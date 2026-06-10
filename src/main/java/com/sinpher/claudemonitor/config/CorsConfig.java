package com.sinpher.claudemonitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 跨域配置，允许前端（Tauri WebView 或浏览器）访问后端 API。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 配置 CORS 允许规则。
     * 允许所有来源访问 REST API，因为本应用是本地桌面工具。
     *
     * @param registry CORS 注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*");
    }
}