package org.kimwanyi.sacco.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kimwanyi.sacco.dto.savings.DepositRequest;
import org.kimwanyi.sacco.dto.savings.SavingsResponse;
import org.kimwanyi.sacco.dto.savings.WithdrawalRequest;
import org.kimwanyi.sacco.entity.Member;
import org.kimwanyi.sacco.entity.SavingsAccount;
import org.kimwanyi.sacco.entity.SavingsTransaction;
import org.kimwanyi.sacco.enums.AccountStatus;
import org.kimwanyi.sacco.enums.UserStatus;
import org.kimwanyi.sacco.exception.DuplicateRecordException;
import org.kimwanyi.sacco.exception.InsufficientBalanceException;
import org.kimwanyi.sacco.exception.ValidationException;
import org.kimwanyi.sacco.repository.MemberRepository;
import org.kimwanyi.sacco.repository.SavingsAccountRepository;
import org.kimwanyi.sacco.repository.SavingsTransactionRepository;
import org.kimwanyi.sacco.serviceImpl.SavingsServiceImpl;
import org.kimwanyi.sacco.validation.SavingsValidator;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class SavingsServiceTest {

    private SavingsAccount testAccount;
    private Member testMember;

    private SavingsService savingsService;

    @BeforeEach
    void setUp() {
        testAccount = new SavingsAccount();
        testAccount.setAccountNumber("ACC-10001");
        testAccount.setStatus(AccountStatus.ACTIVE);

        testMember = new Member();
        testMember.setFirstName("John");
        testMember.setLastName("Doe");
        testMember.setStatus(UserStatus.ACTIVE);
        testAccount.setMember(testMember);

        SavingsAccountRepository savingsAccountRepository = new DummySavingsAccountRepository(testAccount);
        SavingsTransactionRepository savingsTransactionRepository = new DummySavingsTransactionRepository();
        MemberRepository memberRepository = new DummyMemberRepository(testMember);
        SavingsValidator savingsValidator = new SavingsValidator(savingsTransactionRepository);

        savingsService = new SavingsServiceImpl(
                savingsAccountRepository,
                savingsTransactionRepository,
                memberRepository,
                savingsValidator
        );
    }

    @Test
    void testBankingDesignBalanceDerivation() {
        // Step 1: DEPOSIT +500,000
        DepositRequest deposit1 = new DepositRequest();
        deposit1.setAccountNumber("ACC-10001");
        deposit1.setAmount(new BigDecimal("500000.00"));
        deposit1.setDescription("Initial Deposit");
        deposit1.setReferenceNumber("REF-001");

        SavingsResponse resp1 = savingsService.deposit(deposit1);
        assertEquals(new BigDecimal("500000.00"), resp1.getBalance());

        // Step 2: WITHDRAW -100,000
        WithdrawalRequest withdraw1 = new WithdrawalRequest();
        withdraw1.setAccountNumber("ACC-10001");
        withdraw1.setAmount(new BigDecimal("100000.00"));
        withdraw1.setDescription("ATM Cash Withdrawal");
        withdraw1.setReferenceNumber("REF-002");

        SavingsResponse resp2 = savingsService.withdraw(withdraw1);
        assertEquals(new BigDecimal("400000.00"), resp2.getBalance());

        // Step 3: DEPOSIT +200,000
        DepositRequest deposit2 = new DepositRequest();
        deposit2.setAccountNumber("ACC-10001");
        deposit2.setAmount(new BigDecimal("200000.00"));
        deposit2.setDescription("Second Deposit");
        deposit2.setReferenceNumber("REF-003");

        SavingsResponse resp3 = savingsService.deposit(deposit2);

        // Verification: Current Balance = 600,000 (derived from transaction evidence)
        assertEquals(new BigDecimal("600000.00"), resp3.getBalance());
        assertEquals(3, resp3.getRecentTransactions().size());
    }

    @Test
    void testInsufficientBalanceWithdrawalFails() {
        DepositRequest deposit = new DepositRequest();
        deposit.setAccountNumber("ACC-10001");
        deposit.setAmount(new BigDecimal("500000.00"));
        deposit.setReferenceNumber("REF-100");
        savingsService.deposit(deposit);

        WithdrawalRequest withdraw = new WithdrawalRequest();
        withdraw.setAccountNumber("ACC-10001");
        withdraw.setAmount(new BigDecimal("600000.00"));
        withdraw.setReferenceNumber("REF-101");

        assertThrows(InsufficientBalanceException.class, () -> {
            savingsService.withdraw(withdraw);
        });
    }

    @Test
    void testBankLevelValidation_MissingReferenceFails() {
        DepositRequest deposit = new DepositRequest();
        deposit.setAccountNumber("ACC-10001");
        deposit.setAmount(new BigDecimal("500000.00"));
        deposit.setReferenceNumber(null); // Missing ref number

        assertThrows(ValidationException.class, () -> {
            savingsService.deposit(deposit);
        });
    }

    @Test
    void testBankLevelValidation_DuplicateReferenceFails() {
        DepositRequest deposit1 = new DepositRequest();
        deposit1.setAccountNumber("ACC-10001");
        deposit1.setAmount(new BigDecimal("500000.00"));
        deposit1.setReferenceNumber("REF-DUP-001");
        savingsService.deposit(deposit1);

        DepositRequest deposit2 = new DepositRequest();
        deposit2.setAccountNumber("ACC-10001");
        deposit2.setAmount(new BigDecimal("100000.00"));
        deposit2.setReferenceNumber("REF-DUP-001"); // Duplicate idempotency key

        assertThrows(DuplicateRecordException.class, () -> {
            savingsService.deposit(deposit2);
        });
    }

    @Test
    void testBankLevelValidation_InvalidDecimalPrecisionFails() {
        DepositRequest deposit = new DepositRequest();
        deposit.setAccountNumber("ACC-10001");
        deposit.setAmount(new BigDecimal("500000.1234")); // Exceeds 2 decimal places
        deposit.setReferenceNumber("REF-DEC-001");

        assertThrows(ValidationException.class, () -> {
            savingsService.deposit(deposit);
        });
    }

    @Test
    void testBankLevelValidation_InactiveMemberFails() {
        testMember.setStatus(UserStatus.INACTIVE);

        DepositRequest deposit = new DepositRequest();
        deposit.setAccountNumber("ACC-10001");
        deposit.setAmount(new BigDecimal("500000.00"));
        deposit.setReferenceNumber("REF-INACTIVE-001");

        assertThrows(ValidationException.class, () -> {
            savingsService.deposit(deposit);
        });
    }

    // Dummy repositories for unit testing business logic in memory
    private static class DummySavingsAccountRepository implements SavingsAccountRepository {
        private final SavingsAccount account;

        public DummySavingsAccountRepository(SavingsAccount account) {
            this.account = account;
        }

        @Override
        public Optional<SavingsAccount> findByAccountNumber(String accountNumber) {
            if (account.getAccountNumber().equals(accountNumber)) {
                return Optional.of(account);
            }
            return Optional.empty();
        }

        @Override
        public Optional<SavingsAccount> findByMemberId(Long memberId) {
            return Optional.of(account);
        }

        @Override
        public boolean existsByAccountNumber(String accountNumber) {
            return account.getAccountNumber().equals(accountNumber);
        }

        @Override
        public SavingsAccount save(SavingsAccount entity) {
            return entity;
        }

        @Override
        public SavingsAccount update(SavingsAccount entity) {
            return entity;
        }

        @Override
        public Optional<SavingsAccount> findById(Long id) {
            return Optional.of(account);
        }

        @Override
        public List<SavingsAccount> findAll() {
            return Collections.singletonList(account);
        }

        @Override
        public void delete(SavingsAccount entity) {}

        @Override
        public long count() { return 1; }

        @Override
        public boolean existsById(Long id) { return true; }
    }

    private static class DummySavingsTransactionRepository implements SavingsTransactionRepository {
        private final List<SavingsTransaction> transactions = new ArrayList<>();

        @Override
        public List<SavingsTransaction> findBySavingsAccountId(Long accountId) {
            return transactions;
        }

        @Override
        public boolean existsByReferenceNumber(String referenceNumber) {
            if (referenceNumber == null) return false;
            return transactions.stream().anyMatch(t -> referenceNumber.equals(t.getReferenceNumber()));
        }

        @Override
        public SavingsTransaction save(SavingsTransaction entity) {
            transactions.add(entity);
            return entity;
        }

        @Override
        public SavingsTransaction update(SavingsTransaction entity) {
            return entity;
        }

        @Override
        public Optional<SavingsTransaction> findById(Long id) {
            return Optional.empty();
        }

        @Override
        public List<SavingsTransaction> findAll() {
            return transactions;
        }

        @Override
        public void delete(SavingsTransaction entity) {}

        @Override
        public long count() { return transactions.size(); }

        @Override
        public boolean existsById(Long id) { return false; }
    }

    private static class DummyMemberRepository implements MemberRepository {
        private final Member member;

        public DummyMemberRepository(Member member) {
            this.member = member;
        }

        @Override
        public boolean existsByNationalId(String nationalId) { return false; }

        @Override
        public boolean existsByMembershipNumber(String membershipNumber) { return false; }

        @Override
        public Member findByMemberNumber(String membershipNumber) { return member; }

        @Override
        public Member findByNationalId(String nationalId) { return member; }

        @Override
        public Member save(Member entity) { return entity; }

        @Override
        public Member update(Member entity) { return entity; }

        @Override
        public Optional<Member> findById(Long id) { return Optional.of(member); }

        @Override
        public List<Member> findAll() { return Collections.singletonList(member); }

        @Override
        public void delete(Member entity) {}

        @Override
        public long count() { return 1; }

        @Override
        public boolean existsById(Long id) { return true; }
    }
}
