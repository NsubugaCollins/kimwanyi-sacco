package org.kimwanyi.sacco.repository;

import org.kimwanyi.sacco.entity.SavingsTransaction;

import java.util.List;

public interface SavingsTransactionRepository extends GenericRepository<SavingsTransaction, Long> {
    List<SavingsTransaction> findBySavingsAccountId(Long accountId);
    boolean existsByReferenceNumber(String referenceNumber);
}
