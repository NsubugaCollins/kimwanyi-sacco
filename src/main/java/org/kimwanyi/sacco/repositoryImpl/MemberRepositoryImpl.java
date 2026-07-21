package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.Member;
import org.kimwanyi.sacco.repository.MemberRepository;

public class MemberRepositoryImpl extends GenericRepositoryImpl<Member, Long> implements MemberRepository {

    public MemberRepositoryImpl() {
        super(Member.class);
    }

    @Override
    public boolean existsByNationalId(Session session, String nationalId) {
        Long count = session.createQuery(
                "SELECT COUNT(m) FROM Member m WHERE m.nationalId = :nationalId", Long.class
        ).setParameter("nationalId", nationalId).uniqueResult();
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
}
