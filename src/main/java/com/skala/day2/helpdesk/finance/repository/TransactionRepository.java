package com.skala.day2.helpdesk.finance.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class TransactionRepository {

    private final Map<String, Transaction> transactions = new ConcurrentHashMap<>();

    public TransactionRepository() {
        transactions.put(
                "TX-123",
                new Transaction(
                        "TX-123",
                        "user1",
                        "REVIEW",
                        1500000L,
                        "PENDING"));

        transactions.put(
                "TX-124",
                new Transaction(
                        "TX-124",
                        "user1",
                        "COMPLETED",
                        320000L,
                        "APPROVED"));

        // 다른 사용자의 거래 — 접근통제 테스트용
        transactions.put(
                "TX-999",
                new Transaction(
                        "TX-999",
                        "user2",
                        "REVIEW",
                        9000000L,
                        "PENDING"));
    }

    public Optional<Transaction> findOwned(String transactionId, String userId) {
        return Optional.ofNullable(transactions.get(transactionId))
                .filter(transaction -> transaction.ownerId().equals(userId));
    }

    public boolean ownsTransaction(String transactionId, String userId) {
        return findOwned(transactionId, userId).isPresent();
    }

    public record Transaction(
            String id,
            String ownerId,
            String status,
            long amount,
            String approvalStatus) {
    }
}
