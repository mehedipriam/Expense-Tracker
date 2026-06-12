package com.expensetracker.repository;

import com.expensetracker.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    List<Transaction> findByUserIdOrderByDateDesc(String userId);

    Page<Transaction> findByUserIdOrderByDateDesc(String userId, Pageable pageable);

    List<Transaction> findByUserIdAndTypeOrderByDateDesc(String userId, String type);

    List<Transaction> findByUserIdAndDateBetweenOrderByDateDesc(String userId, LocalDate from, LocalDate to);

    List<Transaction> findByUserIdAndIsRecurringTrueOrderByDateDesc(String userId);

    // Top 5 recent
    List<Transaction> findTop5ByUserIdOrderByDateDesc(String userId);

    // Count by type
    long countByUserIdAndType(String userId, String type);

    // For monthly summary
    @Query("{ 'userId': ?0, 'date': { $gte: ?1, $lte: ?2 } }")
    List<Transaction> findByUserIdAndDateBetween(String userId, LocalDate from, LocalDate to);

    @Query("{ 'userId': ?0, 'date': { $gte: ?1, $lte: ?2 }, 'type': ?3 }")
    List<Transaction> findByUserIdAndDateBetweenAndType(String userId, LocalDate from, LocalDate to, String type);

    @Query("{ 'userId': ?0, 'date': { $gte: ?1, $lte: ?2 }, 'category': ?3 }")
    List<Transaction> findByUserIdAndDateBetweenAndCategory(String userId, LocalDate from, LocalDate to, String category);
}
