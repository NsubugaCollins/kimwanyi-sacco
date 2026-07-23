package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.Member;
import org.kimwanyi.sacco.repository.MemberRepository;

import java.util.List;

public class MemberRepositoryImpl extends GenericRepositoryImpl<Member, Long> implements MemberRepository {

    public MemberRepositoryImpl() {
        super(Member.class);
    }

    @Override
    public boolean existsByNationalId(Session session, String nationalId) {
        if (nationalId == null || nationalId.trim().isEmpty()) return false;
        Long count = session.createQuery(
                "SELECT COUNT(m) FROM Member m WHERE m.nationalId = :nationalId", Long.class
        ).setParameter("nationalId", nationalId.trim()).uniqueResult();
        return count != null && count > 0;
    }

    @Override
    public boolean existsByEmail(Session session, String email) {
        if (email == null || email.trim().isEmpty()) return false;
        Long count = session.createQuery(
                "SELECT COUNT(m) FROM Member m WHERE m.email = :email", Long.class
        ).setParameter("email", email.trim()).uniqueResult();
        return count != null && count > 0;
    }

    @Override
    public boolean existsByMembershipNumber(Session session, String membershipNumber) {
        Long count = session.createQuery(
                "SELECT COUNT(m) FROM Member m WHERE m.membershipNumber = :membershipNumber", Long.class
        ).setParameter("membershipNumber", membershipNumber).uniqueResult();
        return count != null && count > 0;
    }

    @Override
    public Member findByMemberNumber(Session session, String membershipNumber) {
        return session.createQuery(
                "FROM Member m WHERE m.membershipNumber = :membershipNumber", Member.class
        ).setParameter("membershipNumber", membershipNumber).uniqueResult();
    }

    @Override
    public Member findByNationalId(Session session, String nationalId) {
        return session.createQuery(
                "FROM Member m WHERE m.nationalId = :nationalId", Member.class
        ).setParameter("nationalId", nationalId).uniqueResult();
    }

    @Override
    public Member findByVerificationToken(Session session, String token) {
        if (token == null || token.trim().isEmpty()) return null;
        List<Member> results = session.createQuery(
                "FROM Member m WHERE m.verificationToken = :token", Member.class
        ).setParameter("token", token.trim()).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public Member findByMemberNumberOrEmailOrPhone(Session session, String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return null;
        String val = identifier.trim();
        List<Member> results = session.createQuery(
                "FROM Member m WHERE m.membershipNumber = :val OR m.email = :val OR m.phoneNumber = :val OR m.username = :val", Member.class
        ).setParameter("val", val).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}
