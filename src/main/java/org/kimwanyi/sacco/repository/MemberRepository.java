package org.kimwanyi.sacco.repository;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.Member;

public interface MemberRepository extends GenericRepository<Member, Long> {
    boolean existsByNationalId(Session session, String nationalId);
    boolean existsByMembershipNumber(Session session, String membershipNumber);
    Member findByMemberNumber(Session session, String membershipNumber);
    Member findByNationalId(Session session, String nationalId);
}
