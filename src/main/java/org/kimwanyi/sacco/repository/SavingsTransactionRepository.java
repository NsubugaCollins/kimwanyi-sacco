package org.kimwanyi.sacco.repository;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.SavingsTransaction;

import java.util.List;

public interface SavingsTransactionRepository extends GenericRepository<SavingsTransaction, Long> {
    List<SavingsTransaction> findBySavingsAccountId(Session session, Long accountId);
    boolean existsByReferenceNumber(Session session, String referenceNumber);
}
