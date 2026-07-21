package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.LoanRepayment;
import org.kimwanyi.sacco.repository.LoanRepaymentRepository;

import java.util.List;

public class LoanRepaymentRepositoryImpl extends GenericRepositoryImpl<LoanRepayment, Long> implements LoanRepaymentRepository {

    public LoanRepaymentRepositoryImpl() {
        super(LoanRepayment.class);
    }

    @Override
    public List<LoanRepayment> findByLoanId(Session session, Long loanId) {
        return session.createQuery(
                "FROM LoanRepayment lr WHERE lr.loan.id = :loanId ORDER BY lr.paymentDate DESC", LoanRepayment.class
        ).setParameter("loanId", loanId).getResultList();
    }

    @Override
    public boolean existsByReferenceNumber(Session session, String referenceNumber) {
        Long count = session.createQuery(
                "SELECT COUNT(lr) FROM LoanRepayment lr WHERE lr.referenceNumber = :referenceNumber", Long.class
        ).setParameter("referenceNumber", referenceNumber).uniqueResult();
        return count != null && count > 0;
    }
}
