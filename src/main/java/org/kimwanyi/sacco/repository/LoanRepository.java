package org.kimwanyi.sacco.repository;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.Loan;
import org.kimwanyi.sacco.enums.LoanStatus;

import java.util.List;

public interface LoanRepository extends GenericRepository<Loan, Long> {
    List<Loan> findByMemberId(Session session, Long memberId);
    List<Loan> findByStatus(Session session, LoanStatus status);
    boolean existsActiveLoanForMember(Session session, Long memberId);
}
