package com.fairing.fairplay.support;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.concurrent.atomic.AtomicInteger;

public class RecordingTransactionManager implements PlatformTransactionManager {

    private final AtomicInteger activeTransactions = new AtomicInteger();

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) {
        activeTransactions.incrementAndGet();
        return new SimpleTransactionStatus();
    }

    @Override
    public void commit(TransactionStatus status) {
        activeTransactions.decrementAndGet();
    }

    @Override
    public void rollback(TransactionStatus status) {
        activeTransactions.decrementAndGet();
    }

    public int activeCount() {
        return activeTransactions.get();
    }
}
