package org.kimwanyi.sacco.service;

import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kimwanyi.sacco.entity.JournalEntry;
import org.kimwanyi.sacco.entity.LedgerAccount;
import org.kimwanyi.sacco.enums.AccountType;
import org.kimwanyi.sacco.enums.TransactionType;
import org.kimwanyi.sacco.event.EventPublisher;
import org.kimwanyi.sacco.event.LoanDisbursedEvent;
import org.kimwanyi.sacco.event.SavingsTransactionEvent;
import org.kimwanyi.sacco.listener.LedgerEventListener;
import org.kimwanyi.sacco.repository.JournalEntryRepository;
import org.kimwanyi.sacco.repository.LedgerAccountRepository;
import org.kimwanyi.sacco.serviceImpl.LedgerServiceImpl;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class EventPublisherTest {

    private EventPublisher publisher;
    private LedgerService ledgerService;
    private DummyJournalEntryRepository journalEntryRepository;

    @BeforeEach
    void setUp() {
        publisher = EventPublisher.getInstance();
        publisher.clearListeners();

        DummyLedgerAccountRepository accountRepository = new DummyLedgerAccountRepository();
        journalEntryRepository = new DummyJournalEntryRepository();

        ledgerService = new LedgerServiceImpl(accountRepository, journalEntryRepository);

        LedgerEventListener listener = new LedgerEventListener(ledgerService);
        listener.registerListeners(publisher);
    }

    @Test
    void testSavingsDepositEvent_AutomaticallyPostsJournalEntry() {
        SavingsTransactionEvent depositEvent = new SavingsTransactionEvent(
                1L, 10L, TransactionType.DEPOSIT, new BigDecimal("250000.00"), "DEP-8899"
        );

        publisher.publish(depositEvent);

        // Check account balances
        LedgerAccount cash = ledgerService.getAccountByCode(LedgerEventListener.CASH_ACCOUNT_CODE);
        LedgerAccount savings = ledgerService.getAccountByCode(LedgerEventListener.SAVINGS_ACCOUNT_CODE);

        assertEquals(new BigDecimal("250000.00"), cash.getCurrentBalance());
        assertEquals(new BigDecimal("250000.00"), savings.getCurrentBalance());

        // Check Journal Entry created
        assertTrue(journalEntryRepository.existsByReferenceNumber(null, "GL-SAV-DEP-8899"));
    }

    @Test
    void testLoanDisbursedEvent_AutomaticallyPostsJournalEntry() {
        LoanDisbursedEvent disburseEvent = new LoanDisbursedEvent(
                50L, 1L, new BigDecimal("1000000.00"), 200L
        );

        publisher.publish(disburseEvent);

        LedgerAccount loans = ledgerService.getAccountByCode(LedgerEventListener.LOANS_ACCOUNT_CODE);
        LedgerAccount cash = ledgerService.getAccountByCode(LedgerEventListener.CASH_ACCOUNT_CODE);

        // DEBIT Loans (Asset) +1,000,000 / CREDIT Cash (Asset) -1,000,000
        assertEquals(new BigDecimal("1000000.00"), loans.getCurrentBalance());
        assertEquals(new BigDecimal("-1000000.00"), cash.getCurrentBalance());
    }

    private static class DummyLedgerAccountRepository implements LedgerAccountRepository {
        private final Map<Long, LedgerAccount> accountsById = new HashMap<>();
        private final Map<String, LedgerAccount> accountsByCode = new HashMap<>();
        private long idSeq = 1L;

        @Override
        public Optional<LedgerAccount> findByAccountCode(Session session, String accountCode) {
            return Optional.ofNullable(accountsByCode.get(accountCode));
        }

        @Override
        public boolean existsByAccountCode(Session session, String accountCode) {
            return accountsByCode.containsKey(accountCode);
        }

        @Override
        public LedgerAccount save(Session session, LedgerAccount entity) {
            if (entity.getId() == null) entity.setId(idSeq++);
            accountsById.put(entity.getId(), entity);
            accountsByCode.put(entity.getAccountCode(), entity);
            return entity;
        }

        @Override
        public LedgerAccount update(Session session, LedgerAccount entity) {
            accountsById.put(entity.getId(), entity);
            accountsByCode.put(entity.getAccountCode(), entity);
            return entity;
        }

        @Override
        public Optional<LedgerAccount> findById(Session session, Long id) {
            return Optional.ofNullable(accountsById.get(id));
        }

        @Override
        public List<LedgerAccount> findAll(Session session) {
            return new ArrayList<>(accountsById.values());
        }

        @Override
        public void delete(Session session, LedgerAccount entity) {}

        @Override
        public long count(Session session) { return accountsById.size(); }

        @Override
        public boolean existsById(Session session, Long id) { return accountsById.containsKey(id); }
    }

    private static class DummyJournalEntryRepository implements JournalEntryRepository {
        private final Map<String, JournalEntry> entriesByRef = new HashMap<>();
        private long idSeq = 1L;

        @Override
        public Optional<JournalEntry> findByReferenceNumber(Session session, String referenceNumber) {
            return Optional.ofNullable(entriesByRef.get(referenceNumber));
        }

        @Override
        public boolean existsByReferenceNumber(Session session, String referenceNumber) {
            return entriesByRef.containsKey(referenceNumber);
        }

        @Override
        public JournalEntry save(Session session, JournalEntry entity) {
            if (entity.getId() == null) entity.setId(idSeq++);
            entriesByRef.put(entity.getReferenceNumber(), entity);
            return entity;
        }

        @Override
        public JournalEntry update(Session session, JournalEntry entity) {
            entriesByRef.put(entity.getReferenceNumber(), entity);
            return entity;
        }

        @Override
        public Optional<JournalEntry> findById(Session session, Long id) {
            return Optional.empty();
        }

        @Override
        public List<JournalEntry> findAll(Session session) {
            return new ArrayList<>(entriesByRef.values());
        }

        @Override
        public void delete(Session session, JournalEntry entity) {}

        @Override
        public long count(Session session) { return entriesByRef.size(); }

        @Override
        public boolean existsById(Session session, Long id) { return false; }
    }
}
