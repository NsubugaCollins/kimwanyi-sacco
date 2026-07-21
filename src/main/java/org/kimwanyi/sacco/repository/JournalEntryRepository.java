package org.kimwanyi.sacco.repository;

import org.hibernate.Session;
import org.kimwanyi.sacco.entity.JournalEntry;

import java.util.Optional;

public interface JournalEntryRepository extends GenericRepository<JournalEntry, Long> {
    Optional<JournalEntry> findByReferenceNumber(Session session, String referenceNumber);
    boolean existsByReferenceNumber(Session session, String referenceNumber);
}
