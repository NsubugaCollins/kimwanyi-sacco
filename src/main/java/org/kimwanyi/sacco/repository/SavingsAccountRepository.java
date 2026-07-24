package org.kimwanyi.sacco.repository;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.SavingsAccount;

import java.util.Optional;

public interface SavingsAccountRepository extends GenericRepository<SavingsAccount, Long> {
    Optional<SavingsAccount> findByAccountNumber(Session session, String accountNumber);
    Optional<SavingsAccount> findByMemberId(Session session, Long memberId);
    boolean existsByAccountNumber(Session session, String accountNumber);
}
