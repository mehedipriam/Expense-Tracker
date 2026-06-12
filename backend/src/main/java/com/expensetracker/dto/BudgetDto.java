package com.expensetracker.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

public class BudgetDto {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Category is required")
        private String category;

        @NotNull(message = "Monthly limit is required")
        @DecimalMin(value = "0.01", message = "Monthly limit must be positive")
        private BigDecimal monthlyLimit;

        @NotBlank(message = "Month is required (format: YYYY-MM)")
        @Pattern(regexp = "\\d{4}-\\d{2}", message = "Month must be in format YYYY-MM")
        private String month;
    }

    @Data
    public static class Response {
        private String id;
        private String category;
        private BigDecimal monthlyLimit;
        private BigDecimal spent;
        private String month;
        private double usagePercentage;
        private boolean overBudget;
    }
}
