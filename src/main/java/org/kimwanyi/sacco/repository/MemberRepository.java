package org.kimwanyi.sacco.repository;

import org.kimwanyi.sacco.entity.Member;

public interface MemberRepository extends GenericRepository<Member, Long> {
    boolean existsByNationalId(String nationalId);
    boolean existsByMembershipNumber(String membershipNumber);
    Member findByMemberNumber(String membershipNumber);
    Member findByNationalId(String nationalId);
}
