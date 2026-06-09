package com.sinpher.claudemonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 成本计算服务，根据模型定价和 token 用量计算费用。
 * 定价数据从 model-pricing.yml 加载，单位为 USD per 1M tokens。
 */
@Service
public class CostCalculationService {

    /** 模型定价配置，key 为模型名，value 为定价明细 */
    private Map<String, Map<String, Double>> modelPricing;

    /** 默认定价配置 */
    private Map<String, Double> defaultPricing;

    /**
     * 构造函数，初始化定价配置为空。
     * 实际定价数据由 {@link #init()} 从 YAML 文件加载。
     */
    public CostCalculationService() {
        this.modelPricing = Collections.emptyMap();
        this.defaultPricing = Map.of(
                "input", 3.0,
                "output", 15.0,
                "cache_creation", 3.75,
                "cache_read", 0.30
        );
    }

    /**
     * 用于单元测试的构造函数，直接传入定价配置。
     *
     * @param modelPricing 模型定价映射，key 为模型名，value 为定价明细
     */
    @SuppressWarnings("unchecked")
    public CostCalculationService(Map<String, Object> modelPricing) {
        this.modelPricing = new LinkedHashMap<>();
        modelPricing.forEach((key, value) -> {
            if (value instanceof Map) {
                this.modelPricing.put(key, (Map<String, Double>) (Map<?, ?>) value);
            }
        });
        this.defaultPricing = this.modelPricing.getOrDefault("default",
                Map.of("input", 3.0, "output", 15.0, "cache_creation", 3.75, "cache_read", 0.30));
    }

    /**
     * 应用启动后从 classpath 加载 model-pricing.yml 定价配置。
     *
     * @throws RuntimeException 当 YAML 文件读取或解析失败时抛出
     */
    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() {
        try {
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            ClassPathResource resource = new ClassPathResource("model-pricing.yml");
            if (!resource.exists()) {
                return;
            }
            try (InputStream is = resource.getInputStream()) {
                Map<String, Object> root = yamlMapper.readValue(is, Map.class);
                Object modelsObj = root.get("models");
                if (modelsObj instanceof Map) {
                    Map<String, Object> models = (Map<String, Object>) modelsObj;
                    this.modelPricing = new LinkedHashMap<>();
                    models.forEach((key, value) -> {
                        if (value instanceof Map) {
                            this.modelPricing.put(key, (Map<String, Double>) (Map<?, ?>) value);
                        }
                    });
                    this.defaultPricing = this.modelPricing.getOrDefault("default",
                            Map.of("input", 3.0, "output", 15.0, "cache_creation", 3.75, "cache_read", 0.30));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("加载 model-pricing.yml 失败", e);
        }
    }

    /**
     * 根据模型名和 token 用量计算费用。
     *
     * @param model               模型名称
     * @param inputTokens         输入 token 数
     * @param outputTokens        输出 token 数
     * @param cacheCreationTokens 缓存创建 token 数
     * @param cacheReadTokens     缓存读取 token 数
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
