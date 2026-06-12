package com.expensetracker.service;

import com.expensetracker.dto.TransactionDto;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.model.Transaction;
import com.expensetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final MongoTemplate mongoTemplate;

    public TransactionDto.Response create(String userId, TransactionDto.CreateRequest request) {
        if (request.isRecurring() && (request.getRecurringFrequency() == null || request.getRecurringFrequency().isBlank())) {
            throw new BadRequestException("Recurring frequency is required for recurring transactions");
        }

        Transaction transaction = Transaction.builder()
                .userId(userId)
                .title(request.getTitle())
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory())
                .date(request.getDate())
                .note(request.getNote())
                .isRecurring(request.isRecurring())
                .recurringFrequency(request.getRecurringFrequency())
                .build();

        return toResponse(transactionRepository.save(transaction));
    }

    public TransactionDto.Response update(String userId, String id, TransactionDto.UpdateRequest request) {
        Transaction transaction = getOwnedTransaction(userId, id);

        if (request.getTitle() != null) transaction.setTitle(request.getTitle());
        if (request.getAmount() != null) transaction.setAmount(request.getAmount());
        if (request.getType() != null) transaction.setType(request.getType());
        if (request.getCategory() != null) transaction.setCategory(request.getCategory());
        if (request.getDate() != null) transaction.setDate(request.getDate());
        if (request.getNote() != null) transaction.setNote(request.getNote());
        if (request.getIsRecurring() != null) transaction.setRecurring(request.getIsRecurring());
        if (request.getRecurringFrequency() != null) transaction.setRecurringFrequency(request.getRecurringFrequency());
        transaction.setUpdatedAt(LocalDateTime.now());

        return toResponse(transactionRepository.save(transaction));
    }

    public void delete(String userId, String id) {
        Transaction transaction = getOwnedTransaction(userId, id);
        transactionRepository.delete(transaction);
    }

    public TransactionDto.Response getById(String userId, String id) {
        return toResponse(getOwnedTransaction(userId, id));
    }

    public Page<TransactionDto.Response> filter(String userId, TransactionDto.FilterRequest filter) {
        Criteria criteria = Criteria.where("userId").is(userId);

        if (filter.getFromDate() != null) {
            criteria = criteria.and("date").gte(filter.getFromDate());
        }
        if (filter.getToDate() != null) {
            if (filter.getFromDate() != null) {
                // date is already added, use andOperator for range
                criteria = Criteria.where("userId").is(userId)
                        .and("date").gte(filter.getFromDate()).lte(filter.getToDate());
            } else {
                criteria = criteria.and("date").lte(filter.getToDate());
            }
        }
        if (filter.getCategory() != null && !filter.getCategory().isBlank()) {
            criteria = criteria.and("category").is(filter.getCategory());
        }
        if (filter.getType() != null && !filter.getType().isBlank()) {
            criteria = criteria.and("type").is(filter.getType());
        }
        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            String kw = filter.getKeyword();
            criteria = criteria.orOperator(
                    Criteria.where("title").regex(kw, "i"),
                    Criteria.where("note").regex(kw, "i")
            );
        }
        if (filter.getMinAmount() != null) {
            criteria = criteria.and("amount").gte(filter.getMinAmount());
        }
        if (filter.getMaxAmount() != null) {
            criteria = criteria.and("amount").lte(filter.getMaxAmount());
        }

        Query query = new Query(criteria).with(Sort.by(Sort.Direction.DESC, "date"));
        long total = mongoTemplate.count(query, Transaction.class);

        query.with(PageRequest.of(filter.getPage(), filter.getSize()));
        List<Transaction> transactions = mongoTemplate.find(query, Transaction.class);

        List<TransactionDto.Response> responses = transactions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(responses, PageRequest.of(filter.getPage(), filter.getSize()), total);
    }

    public List<TransactionDto.Response> getRecurring(String userId) {
        return transactionRepository.findByUserIdAndIsRecurringTrueOrderByDateDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<TransactionDto.Response> getAllForExport(String userId, TransactionDto.FilterRequest filter) {
        // Reuse filter logic but return all (no paging)
        Criteria criteria = Criteria.where("userId").is(userId);
        if (filter.getFromDate() != null && filter.getToDate() != null) {
            criteria = Criteria.where("userId").is(userId)
                    .and("date").gte(filter.getFromDate()).lte(filter.getToDate());
        } else if (filter.getFromDate() != null) {
            criteria = criteria.and("date").gte(filter.getFromDate());
        } else if (filter.getToDate() != null) {
            criteria = criteria.and("date").lte(filter.getToDate());
        }
        if (filter.getCategory() != null && !filter.getCategory().isBlank()) {
            criteria = criteria.and("category").is(filter.getCategory());
        }
        if (filter.getType() != null && !filter.getType().isBlank()) {
            criteria = criteria.and("type").is(filter.getType());
        }

        Query query = new Query(criteria).with(Sort.by(Sort.Direction.DESC, "date"));
        return mongoTemplate.find(query, Transaction.class)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Transaction getOwnedTransaction(String userId, String id) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        if (!t.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Transaction not found");
        }
        return t;
    }

    public TransactionDto.Response toResponse(Transaction t) {
        TransactionDto.Response r = new TransactionDto.Response();
        r.setId(t.getId());
        r.setTitle(t.getTitle());
        r.setAmount(t.getAmount());
        r.setType(t.getType());
        r.setCategory(t.getCategory());
        r.setDate(t.getDate());
        r.setNote(t.getNote());
        r.setRecurring(t.isRecurring());
        r.setRecurringFrequency(t.getRecurringFrequency());
        r.setCreatedAt(t.getCreatedAt());
        r.setUpdatedAt(t.getUpdatedAt());
        return r;
    }
}
