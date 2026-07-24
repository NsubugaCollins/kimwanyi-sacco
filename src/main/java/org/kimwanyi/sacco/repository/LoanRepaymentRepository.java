package org.kimwanyi.sacco.repository;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.LoanRepayment;

import java.util.List;

public interface LoanRepaymentRepository extends GenericRepository<LoanRepayment, Long> {
    List<LoanRepayment> findByLoanId(Session session, Long loanId);
    boolean existsByReferenceNumber(Session session, String referenceNumber);
}
