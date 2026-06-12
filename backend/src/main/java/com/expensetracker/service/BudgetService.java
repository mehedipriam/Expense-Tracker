package com.expensetracker.service;

import com.expensetracker.dto.BudgetDto;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.model.Budget;
import com.expensetracker.model.Transaction;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    public BudgetDto.Response createOrUpdate(String userId, BudgetDto.CreateRequest request) {
        Budget budget = budgetRepository
                .findByUserIdAndCategoryAndMonth(userId, request.getCategory(), request.getMonth())
                .orElse(Budget.builder()
                        .userId(userId)
                        .category(request.getCategory())
                        .month(request.getMonth())
                        .build());

        budget.setMonthlyLimit(request.getMonthlyLimit());
        Budget saved = budgetRepository.save(budget);
        return toResponse(saved, userId);
    }

    public void delete(String userId, String id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found"));
        if (!budget.getUserId().equals(userId)) throw new ResourceNotFoundException("Budget not found");
        budgetRepository.delete(budget);
    }

    public List<BudgetDto.Response> getBudgetsForMonth(String userId, String month) {
        return budgetRepository.findByUserIdAndMonth(userId, month)
                .stream().map(b -> toResponse(b, userId))
                .collect(Collectors.toList());
    }

    public List<BudgetDto.Response> getAllBudgets(String userId) {
        return budgetRepository.findByUserId(userId)
                .stream().map(b -> toResponse(b, userId))
                .collect(Collectors.toList());
    }

    private BudgetDto.Response toResponse(Budget budget, String userId) {
        // Calculate spent for this category this month
        BigDecimal spent = getSpentForCategoryMonth(userId, budget.getCategory(), budget.getMonth());

        double usagePct = 0;
        if (budget.getMonthlyLimit().compareTo(BigDecimal.ZERO) > 0) {
            usagePct = spent.divide(budget.getMonthlyLimit(), 4, RoundingMode.HALF_UP)
                    .doubleValue() * 100;
            usagePct = Math.min(usagePct, 200); // cap display at 200%
        }

        BudgetDto.Response r = new BudgetDto.Response();
        r.setId(budget.getId());
        r.setCategory(budget.getCategory());
        r.setMonthlyLimit(budget.getMonthlyLimit());
        r.setSpent(spent);
        r.setMonth(budget.getMonth());
        r.setUsagePercentage(Math.round(usagePct * 10.0) / 10.0);
        r.setOverBudget(spent.compareTo(budget.getMonthlyLimit()) > 0);
        return r;
    }

    private BigDecimal getSpentForCategoryMonth(String userId, String category, String month) {
        try {
            YearMonth ym = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
            LocalDate from = ym.atDay(1);
            LocalDate to = ym.atEndOfMonth();

            return transactionRepository
                    .findByUserIdAndDateBetweenAndCategory(userId, from, to, category)
                    .stream()
                    .filter(t -> "expense".equals(t.getType()))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
