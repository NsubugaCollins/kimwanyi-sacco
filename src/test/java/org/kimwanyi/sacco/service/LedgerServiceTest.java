package org.kimwanyi.sacco.service;

import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kimwanyi.sacco.entity.JournalEntry;
import org.kimwanyi.sacco.entity.JournalLine;
import org.kimwanyi.sacco.entity.LedgerAccount;
import org.kimwanyi.sacco.enums.AccountType;
import org.kimwanyi.sacco.enums.EntryType;
import org.kimwanyi.sacco.exception.ValidationException;
import org.kimwanyi.sacco.repository.JournalEntryRepository;
import org.kimwanyi.sacco.repository.LedgerAccountRepository;
import org.kimwanyi.sacco.serviceImpl.LedgerServiceImpl;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class LedgerServiceTest {

    private LedgerService ledgerService;
    private DummyLedgerAccountRepository ledgerAccountRepository;
    private DummyJournalEntryRepository journalEntryRepository;

    @BeforeEach
    void setUp() {
        ledgerAccountRepository = new DummyLedgerAccountRepository();
        journalEntryRepository = new DummyJournalEntryRepository();
        ledgerService = new LedgerServiceImpl(ledgerAccountRepository, journalEntryRepository);

        // Pre-populate Chart of Accounts
        ledgerService.createAccount("1010-CASH", "Cash on Hand", AccountType.ASSET);
        ledgerService.createAccount("2010-SAVINGS", "Member Savings Deposits", AccountType.LIABILITY);
    }

    @Test
    void testCreateLedgerAccount_Succeeds() {
        LedgerAccount account = ledgerService.getAccountByCode("1010-CASH");
        assertNotNull(account);
        assertEquals("Cash on Hand", account.getAccountName());
        assertEquals(AccountType.ASSET, account.getAccountType());
        assertEquals(BigDecimal.ZERO, account.getCurrentBalance());
    }

    @Test
    void testPostBalancedJournalEntry_UpdatesAccountBalances() {
        LedgerAccount cash = ledgerService.getAccountByCode("1010-CASH");
        LedgerAccount savings = ledgerService.getAccountByCode("2010-SAVINGS");

        List<JournalLine> lines = Arrays.asList(
                new JournalLine(cash, EntryType.DEBIT, new BigDecimal("100000.00")),
                new JournalLine(savings, EntryType.CREDIT, new BigDecimal("100000.00"))
        );

        JournalEntry entry = ledgerService.postJournalEntry("GL-REF-001", "Member Cash Deposit", lines);

        assertNotNull(entry);
        assertEquals("GL-REF-001", entry.getReferenceNumber());
        assertEquals(2, entry.getLines().size());

        // Asset (Cash) +Debit -> Balance increases to 100,000
        assertEquals(new BigDecimal("100000.00"), ledgerService.getAccountByCode("1010-CASH").getCurrentBalance());

        // Liability (Savings) +Credit -> Balance increases to 100,000
        assertEquals(new BigDecimal("100000.00"), ledgerService.getAccountByCode("2010-SAVINGS").getCurrentBalance());
    }

    @Test
    void testPostUnbalancedJournalEntry_FailsWithValidationException() {
        LedgerAccount cash = ledgerService.getAccountByCode("1010-CASH");
        LedgerAccount savings = ledgerService.getAccountByCode("2010-SAVINGS");

        List<JournalLine> lines = Arrays.asList(
                new JournalLine(cash, EntryType.DEBIT, new BigDecimal("100000.00")),
                new JournalLine(savings, EntryType.CREDIT, new BigDecimal("80000.00")) // Unbalanced by 20,000
        );

        ValidationException ex = assertThrows(ValidationException.class, () -> {
            ledgerService.postJournalEntry("GL-REF-002", "Unbalanced deposit", lines);
        });

        assertTrue(ex.getMessage().contains("Unbalanced journal entry"));
    }

    // Dummy Repositories
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
