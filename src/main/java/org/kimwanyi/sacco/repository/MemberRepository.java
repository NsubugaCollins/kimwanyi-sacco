package org.kimwanyi.sacco.repository;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.Member;

public interface MemberRepository extends GenericRepository<Member, Long> {
    boolean existsByNationalId(Session session, String nationalId);
    boolean existsByMembershipNumber(Session session, String membershipNumber);
    default boolean existsByEmail(Session session, String email) {
        return false;
    }
    Member findByMemberNumber(Session session, String membershipNumber);
    Member findByNationalId(Session session, String nationalId);
    default Member findByVerificationToken(Session session, String token) {
        return null;
    }
    default Member findByMemberNumberOrEmailOrPhone(Session session, String identifier) {
        return findByMemberNumber(session, identifier);
    }
}
