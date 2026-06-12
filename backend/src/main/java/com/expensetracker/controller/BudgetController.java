package com.expensetracker.controller;

import com.expensetracker.dto.ApiResponse;
import com.expensetracker.dto.BudgetDto;
import com.expensetracker.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetDto.Response>> createOrUpdate(
            Authentication auth,
            @Valid @RequestBody BudgetDto.CreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                budgetService.createOrUpdate(auth.getName(), request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            Authentication auth,
            @PathVariable String id) {
        budgetService.delete(auth.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Budget deleted", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetDto.Response>>> getBudgets(
            Authentication auth,
            @RequestParam(required = false) String month) {

        List<BudgetDto.Response> result = month != null
                ? budgetService.getBudgetsForMonth(auth.getName(), month)
                : budgetService.getAllBudgets(auth.getName());

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
