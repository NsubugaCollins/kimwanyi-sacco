package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.kimwanyi.sacco.entity.SavingsTransaction;
import org.kimwanyi.sacco.repository.SavingsTransactionRepository;

import java.util.List;

public class SavingsTransactionRepositoryImpl extends GenericRepositoryImpl<SavingsTransaction, Long> implements SavingsTransactionRepository {

    public SavingsTransactionRepositoryImpl(SessionFactory sessionFactory) {
        super(SavingsTransaction.class, sessionFactory);
    }

    @Override
    public List<SavingsTransaction> findBySavingsAccountId(Long accountId) {
        Session session = getSession();
        return session.createQuery(
                        "FROM SavingsTransaction st WHERE st.savingsAccount.id = :accountId ORDER BY st.createdAt DESC",
                        SavingsTransaction.class)
                .setParameter("accountId", accountId)
                .getResultList();
    }

    @Override
    public boolean existsByReferenceNumber(String referenceNumber) {
        if (referenceNumber == null || referenceNumber.isBlank()) {
            return false;
        }
        Long count = getSession().createQuery(
                        "SELECT COUNT(st) FROM SavingsTransaction st WHERE st.referenceNumber = :referenceNumber", Long.class)
                .setParameter("referenceNumber", referenceNumber.trim())
                .uniqueResult();
        return count != null && count > 0;
    }
}
