package org.kimwanyi.sacco.repositoryImpl;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.JournalEntry;
import org.kimwanyi.sacco.repository.JournalEntryRepository;

import java.util.Optional;

public class JournalEntryRepositoryImpl extends GenericRepositoryImpl<JournalEntry, Long> implements JournalEntryRepository {

    public JournalEntryRepositoryImpl() {
        super(JournalEntry.class);
    }

    @Override
    public Optional<JournalEntry> findByReferenceNumber(Session session, String referenceNumber) {
        if (session == null || referenceNumber == null) return Optional.empty();
        return session.createQuery("FROM JournalEntry j WHERE j.referenceNumber = :ref", JournalEntry.class)
                .setParameter("ref", referenceNumber.trim())
                .uniqueResultOptional();
    }

    @Override
    public boolean existsByReferenceNumber(Session session, String referenceNumber) {
        if (session == null || referenceNumber == null) return false;
        Long count = session.createQuery("SELECT COUNT(j) FROM JournalEntry j WHERE j.referenceNumber = :ref", Long.class)
                .setParameter("ref", referenceNumber.trim())
                .getSingleResult();
        return count > 0;
    }
}
