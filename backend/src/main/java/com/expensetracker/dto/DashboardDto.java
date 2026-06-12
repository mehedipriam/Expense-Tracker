package com.expensetracker.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardDto {

    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netBalance;

    // Last 6 months: { "2024-01": { income: X, expense: Y } }
    private List<MonthlyData> monthlySummary;

    // Category breakdown for expenses
    private Map<String, BigDecimal> categoryBreakdown;

    // Top 3 spending categories
    private List<CategoryStat> topCategories;

    // 5 most recent transactions
    private List<TransactionDto.Response> recentTransactions;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MonthlyData {
        private String month;
        private BigDecimal income;
        private BigDecimal expense;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategoryStat {
        private String category;
        private BigDecimal amount;
        private double percentage;
    }
}
