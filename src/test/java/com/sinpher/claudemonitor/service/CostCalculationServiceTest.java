package com.sinpher.claudemonitor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
        // 使用测试构造函数，绕过 Spring 注入
        costCalculationService = new CostCalculationService(pricing);
    }

    @Test
    @DisplayName("已知模型应按对应定价计算费用")
    void shouldCalculateCostForKnownModel() {
        // 执行：1M input + 1M output 的 sonnet 计算
        BigDecimal cost = costCalculationService.calculateCost(
                "claude-sonnet-4-6", 1_000_000L, 1_000_000L, 0L, 0L);

        // 验证：3.0 + 15.0 = 18.0 USD
        assertThat(cost)
                .withFailMessage("1M input + 1M output 的 sonnet 费用应为 18.0 USD，实际为 %s", cost)
                .isEqualByComparingTo(BigDecimal.valueOf(18.0));
    }

    @Test
    @DisplayName("未知模型应使用默认定价计算费用")
    void shouldUseDefaultPricingForUnknownModel() {
        // 执行：1M input 的未知模型计算
        BigDecimal cost = costCalculationService.calculateCost(
                "unknown-model", 1_000_000L, 0L, 0L, 0L);

        // 验证：未知模型应使用默认 input 定价 3.0 USD/1M tokens
        assertThat(cost)
                .withFailMessage("未知模型应使用默认 input 定价 3.0 USD/1M tokens，实际为 %s", cost)
                .isEqualByComparingTo(BigDecimal.valueOf(3.0));
    }

    @Test
    @DisplayName("缓存 token 应按对应定价计算费用")
    void shouldCalculateCacheTokenCost() {
        // 执行：1M cache_creation + 1M cache_read 的 sonnet 计算
        BigDecimal cost = costCalculationService.calculateCost(
                "claude-sonnet-4-6", 0L, 0L, 1_000_000L, 1_000_000L);

        // 验证：3.75 + 0.30 = 4.05 USD
        assertThat(cost)
                .withFailMessage("1M cache_creation + 1M cache_read 的 sonnet 费用应为 4.05 USD，实际为 %s", cost)
                .isEqualByComparingTo(BigDecimal.valueOf(4.05));
    }

    @Test
    @DisplayName("零 token 用量应产生零费用")
    void shouldReturnZeroForZeroTokens() {
        // 执行：所有 token 数为 0
        BigDecimal cost = costCalculationService.calculateCost(
                "claude-sonnet-4-6", 0L, 0L, 0L, 0L);

        // 验证：零用量应产生零费用
        assertThat(cost)
                .withFailMessage("零 token 用量应产生零费用，实际为 %s", cost)
                .isEqualByComparingTo(BigDecimal.ZERO);
    }
}
