package com.expensetracker.service;

import com.expensetracker.dto.TransactionDto;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final TransactionService transactionService;

    public String exportToCsv(String userId, TransactionDto.FilterRequest filter) {
        List<TransactionDto.Response> transactions = transactionService.getAllForExport(userId, filter);

        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            // Header
            writer.writeNext(new String[]{"Date", "Title", "Category", "Type", "Amount", "Note", "Recurring", "Frequency"});

            // Rows
            for (TransactionDto.Response t : transactions) {
                writer.writeNext(new String[]{
                        t.getDate() != null ? t.getDate().toString() : "",
                        t.getTitle() != null ? t.getTitle() : "",
                        t.getCategory() != null ? t.getCategory() : "",
                        t.getType() != null ? t.getType() : "",
                        t.getAmount() != null ? t.getAmount().toPlainString() : "",
                        t.getNote() != null ? t.getNote() : "",
                        String.valueOf(t.isRecurring()),
                        t.getRecurringFrequency() != null ? t.getRecurringFrequency() : ""
                });
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV", e);
        }

        return sw.toString();
    }
}
