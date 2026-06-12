package com.expensetracker.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TransactionDto {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Title is required")
        private String title;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        private BigDecimal amount;

        @NotBlank(message = "Type is required")
        @Pattern(regexp = "income|expense", message = "Type must be 'income' or 'expense'")
        private String type;

        @NotBlank(message = "Category is required")
        private String category;

        @NotNull(message = "Date is required")
        private LocalDate date;

        private String note;
        private boolean isRecurring;

        @Pattern(regexp = "daily|weekly|monthly|yearly", message = "Frequency must be daily, weekly, monthly, or yearly")
        private String recurringFrequency;
    }

    @Data
    public static class UpdateRequest {
        private String title;
        private BigDecimal amount;

        @Pattern(regexp = "income|expense", message = "Type must be 'income' or 'expense'")
        private String type;

        private String category;
        private LocalDate date;
        private String note;
        private Boolean isRecurring;

        @Pattern(regexp = "daily|weekly|monthly|yearly", message = "Frequency must be daily, weekly, monthly, or yearly")
        private String recurringFrequency;
    }

    @Data
    public static class FilterRequest {
        private LocalDate fromDate;
        private LocalDate toDate;
        private String category;
        private String type;
        private String keyword;
        private BigDecimal minAmount;
        private BigDecimal maxAmount;
        private int page = 0;
        private int size = 10;
    }

    @Data
    public static class Response {
        private String id;
        private String title;
        private BigDecimal amount;
        private String type;
        private String category;
        private LocalDate date;
        private String note;
        private boolean isRecurring;
        private String recurringFrequency;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
