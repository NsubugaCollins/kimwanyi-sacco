package org.kimwanyi.sacco.repository;

import org.kimwanyi.sacco.entity.SavingsAccount;

import java.util.Optional;

public interface SavingsAccountRepository extends GenericRepository<SavingsAccount, Long> {
    Optional<SavingsAccount> findByAccountNumber(String accountNumber);
    Optional<SavingsAccount> findByMemberId(Long memberId);
    boolean existsByAccountNumber(String accountNumber);
}
