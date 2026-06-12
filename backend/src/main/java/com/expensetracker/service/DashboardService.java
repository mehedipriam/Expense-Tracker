package com.expensetracker.service;

import com.expensetracker.dto.DashboardDto;
import com.expensetracker.dto.TransactionDto;
import com.expensetracker.model.Transaction;
import com.expensetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    public DashboardDto getDashboard(String userId) {
        // Current month bounds
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        List<Transaction> currentMonthTx =
                transactionRepository.findByUserIdAndDateBetween(userId, monthStart, monthEnd);

        BigDecimal totalIncome = sumByType(currentMonthTx, "income");
        BigDecimal totalExpense = sumByType(currentMonthTx, "expense");
        BigDecimal netBalance = totalIncome.subtract(totalExpense);

        // Last 6 months summary
        List<DashboardDto.MonthlyData> monthlySummary = buildMonthlySummary(userId);

        // Category breakdown (expenses only, current month)
        Map<String, BigDecimal> categoryBreakdown = currentMonthTx.stream()
                .filter(t -> "expense".equals(t.getType()))
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        // Top 3 categories
        List<DashboardDto.CategoryStat> topCategories = buildTopCategories(categoryBreakdown, totalExpense);

        // 5 most recent
        List<TransactionDto.Response> recentTransactions = transactionRepository
                .findTop5ByUserIdOrderByDateDesc(userId)
                .stream().map(transactionService::toResponse)
                .collect(Collectors.toList());

        return DashboardDto.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netBalance(netBalance)
                .monthlySummary(monthlySummary)
                .categoryBreakdown(categoryBreakdown)
                .topCategories(topCategories)
                .recentTransactions(recentTransactions)
                .build();
    }

    private List<DashboardDto.MonthlyData> buildMonthlySummary(String userId) {
        List<DashboardDto.MonthlyData> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.now().minusMonths(i);
            LocalDate from = ym.atDay(1);
            LocalDate to = ym.atEndOfMonth();

            List<Transaction> monthTx = transactionRepository.findByUserIdAndDateBetween(userId, from, to);
            BigDecimal income = sumByType(monthTx, "income");
            BigDecimal expense = sumByType(monthTx, "expense");

            result.add(DashboardDto.MonthlyData.builder()
                    .month(ym.format(fmt))
                    .income(income)
                    .expense(expense)
                    .build());
        }
        return result;
    }

    private List<DashboardDto.CategoryStat> buildTopCategories(Map<String, BigDecimal> breakdown,
                                                                BigDecimal totalExpense) {
        if (totalExpense.compareTo(BigDecimal.ZERO) == 0) return Collections.emptyList();

        return breakdown.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(3)
                .map(entry -> {
                    double pct = entry.getValue()
                            .divide(totalExpense, 4, RoundingMode.HALF_UP)
                            .doubleValue() * 100;
                    return DashboardDto.CategoryStat.builder()
                            .category(entry.getKey())
                            .amount(entry.getValue())
                            .percentage(Math.round(pct * 10.0) / 10.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private BigDecimal sumByType(List<Transaction> txList, String type) {
        return txList.stream()
                .filter(t -> type.equals(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
