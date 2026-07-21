package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.SavingsTransaction;
import org.kimwanyi.sacco.repository.SavingsTransactionRepository;

import java.util.List;

public class SavingsTransactionRepositoryImpl extends GenericRepositoryImpl<SavingsTransaction, Long> implements SavingsTransactionRepository {

    public SavingsTransactionRepositoryImpl() {
        super(SavingsTransaction.class);
    }

    @Override
    public List<SavingsTransaction> findBySavingsAccountId(Session session, Long accountId) {
        return session.createQuery(
                        "FROM SavingsTransaction st WHERE st.savingsAccount.id = :accountId ORDER BY st.createdAt DESC",
                        SavingsTransaction.class)
                .setParameter("accountId", accountId)
                .getResultList();
    }

    @Override
    public boolean existsByReferenceNumber(Session session, String referenceNumber) {
        if (referenceNumber == null || referenceNumber.isBlank()) {
            return false;
        }
        Long count = session.createQuery(
                        "SELECT COUNT(st) FROM SavingsTransaction st WHERE st.referenceNumber = :referenceNumber", Long.class)
                .setParameter("referenceNumber", referenceNumber.trim())
                .uniqueResult();
        return count != null && count > 0;
    }
}
