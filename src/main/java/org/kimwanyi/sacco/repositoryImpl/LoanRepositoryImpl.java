package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.Loan;
import org.kimwanyi.sacco.enums.LoanStatus;
import org.kimwanyi.sacco.repository.LoanRepository;

import java.util.List;

public class LoanRepositoryImpl extends GenericRepositoryImpl<Loan, Long> implements LoanRepository {

    public LoanRepositoryImpl() {
        super(Loan.class);
    }

    @Override
    public List<Loan> findByMemberId(Session session, Long memberId) {
        return session.createQuery(
                "FROM Loan l WHERE l.member.id = :memberId ORDER BY l.createdAt DESC", Loan.class
        ).setParameter("memberId", memberId).getResultList();
    }

    @Override
    public List<Loan> findByStatus(Session session, LoanStatus status) {
        if (status == null) {
            return session.createQuery(
                    "FROM Loan l ORDER BY l.createdAt DESC", Loan.class
            ).getResultList();
        }
        return session.createQuery(
                "FROM Loan l WHERE l.status = :status ORDER BY l.createdAt DESC", Loan.class
        ).setParameter("status", status).getResultList();
    }

    @Override
    public boolean existsActiveLoanForMember(Session session, Long memberId) {
        Long count = session.createQuery(
                "SELECT COUNT(l) FROM Loan l WHERE l.member.id = :memberId AND (l.status = :active OR l.status = :pending)", Long.class
        ).setParameter("memberId", memberId)
         .setParameter("active", LoanStatus.ACTIVE)
         .setParameter("pending", LoanStatus.PENDING)
         .uniqueResult();
        return count != null && count > 0;
    }
}
