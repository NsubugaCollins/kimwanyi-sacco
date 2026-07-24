package org.kimwanyi.sacco.repository;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.LedgerAccount;

import java.util.Optional;

public interface LedgerAccountRepository extends GenericRepository<LedgerAccount, Long> {
    Optional<LedgerAccount> findByAccountCode(Session session, String accountCode);
    boolean existsByAccountCode(Session session, String accountCode);
}
