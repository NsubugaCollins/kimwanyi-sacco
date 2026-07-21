package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.kimwanyi.sacco.entity.SavingsAccount;
import org.kimwanyi.sacco.repository.SavingsAccountRepository;

import java.util.Optional;

public class SavingsAccountRepositoryImpl extends GenericRepositoryImpl<SavingsAccount, Long> implements SavingsAccountRepository {

    public SavingsAccountRepositoryImpl(SessionFactory sessionFactory) {
        super(SavingsAccount.class, sessionFactory);
    }

    @Override
    public Optional<SavingsAccount> findByAccountNumber(String accountNumber) {
        Session session = getSession();
        SavingsAccount account = session.createQuery(
                        "FROM SavingsAccount sa WHERE sa.accountNumber = :accountNumber", SavingsAccount.class)
                .setParameter("accountNumber", accountNumber)
                .uniqueResult();
        return Optional.ofNullable(account);
    }

    @Override
    public Optional<SavingsAccount> findByMemberId(Long memberId) {
        Session session = getSession();
        SavingsAccount account = session.createQuery(
                        "FROM SavingsAccount sa WHERE sa.member.id = :memberId", SavingsAccount.class)
                .setParameter("memberId", memberId)
                .uniqueResult();
        return Optional.ofNullable(account);
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        Long count = getSession().createQuery(
                        "SELECT COUNT(sa) FROM SavingsAccount sa WHERE sa.accountNumber = :accountNumber", Long.class)
                .setParameter("accountNumber", accountNumber)
                .uniqueResult();
        return count != null && count > 0;
    }
}
