package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.LedgerAccount;
import org.kimwanyi.sacco.repository.LedgerAccountRepository;

import java.util.Optional;

public class LedgerAccountRepositoryImpl extends GenericRepositoryImpl<LedgerAccount, Long> implements LedgerAccountRepository {

    public LedgerAccountRepositoryImpl() {
        super(LedgerAccount.class);
    }

    @Override
    public Optional<LedgerAccount> findByAccountCode(Session session, String accountCode) {
        if (session == null || accountCode == null) return Optional.empty();
        return session.createQuery("FROM LedgerAccount a WHERE a.accountCode = :code", LedgerAccount.class)
                .setParameter("code", accountCode.trim())
                .uniqueResultOptional();
    }

    @Override
    public boolean existsByAccountCode(Session session, String accountCode) {
        if (session == null || accountCode == null) return false;
        Long count = session.createQuery("SELECT COUNT(a) FROM LedgerAccount a WHERE a.accountCode = :code", Long.class)
                .setParameter("code", accountCode.trim())
                .getSingleResult();
        return count > 0;
    }
}
