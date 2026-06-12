package com.expensetracker.controller;

import com.expensetracker.dto.ApiResponse;
import com.expensetracker.dto.TransactionDto;
import com.expensetracker.service.ExportService;
import com.expensetracker.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final ExportService exportService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionDto.Response>> create(
            Authentication auth,
            @Valid @RequestBody TransactionDto.CreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.create(auth.getName(), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionDto.Response>> update(
            Authentication auth,
            @PathVariable String id,
            @RequestBody TransactionDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.update(auth.getName(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            Authentication auth,
            @PathVariable String id) {
        transactionService.delete(auth.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionDto.Response>> getById(
            Authentication auth,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getById(auth.getName(), id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionDto.Response>>> filter(
            Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        TransactionDto.FilterRequest filter = new TransactionDto.FilterRequest();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setCategory(category);
        filter.setType(type);
        filter.setKeyword(keyword);
        filter.setMinAmount(minAmount);
        filter.setMaxAmount(maxAmount);
        filter.setPage(page);
        filter.setSize(size);

        return ResponseEntity.ok(ApiResponse.success(
                transactionService.filter(auth.getName(), filter)));
    }

    @GetMapping("/recurring")
    public ResponseEntity<ApiResponse<List<TransactionDto.Response>>> getRecurring(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getRecurring(auth.getName())));
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type) {

        TransactionDto.FilterRequest filter = new TransactionDto.FilterRequest();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setCategory(category);
        filter.setType(type);

        String csv = exportService.exportToCsv(auth.getName(), filter);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "transactions.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csv.getBytes());
    }
}
