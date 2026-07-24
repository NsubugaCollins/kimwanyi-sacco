package org.kimwanyi.sacco.service;

import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kimwanyi.sacco.dto.loan.LoanApplicationRequest;
import org.kimwanyi.sacco.dto.loan.LoanApprovalRequest;
import org.kimwanyi.sacco.dto.loan.LoanPaymentRequest;
import org.kimwanyi.sacco.dto.loan.LoanResponse;
import org.kimwanyi.sacco.entity.*;
import org.kimwanyi.sacco.enums.LoanStatus;
import org.kimwanyi.sacco.enums.UserStatus;
import org.kimwanyi.sacco.exception.AccessDeniedException;
import org.kimwanyi.sacco.exception.ValidationException;
import org.kimwanyi.sacco.repository.LoanRepaymentRepository;
import org.kimwanyi.sacco.repository.LoanRepository;
import org.kimwanyi.sacco.repository.MemberRepository;
import org.kimwanyi.sacco.repository.UserRepository;
import org.kimwanyi.sacco.serviceImpl.LoanServiceImpl;
import org.kimwanyi.sacco.validation.LoanValidator;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class LoanServiceTest {

    private Member testMember;
    private User adminUser;
    private User cashierUser;

    private LoanService loanService;
    private DummyLoanRepository loanRepository;
    private DummyLoanRepaymentRepository loanRepaymentRepository;

    @BeforeEach
    void setUp() {
        testMember = new Member();
        testMember.setId(100L);
        testMember.setFirstName("Jane");
        testMember.setLastName("Kibuuka");
        testMember.setEmail("jane@example.com");
        testMember.setMembershipNumber("MEM-001");
        testMember.setStatus(UserStatus.ACTIVE);

        // Admin User
        adminUser = new User();
        adminUser.setId(200L);
        adminUser.setUsername("admin_john");
        adminUser.setEmail("admin@kimwanyi.org");
        adminUser.setStatus(UserStatus.ACTIVE);
        Role adminRole = new Role("ADMIN");
        adminUser.addRole(adminRole);

        // Cashier User
        cashierUser = new User();
        cashierUser.setId(300L);
        cashierUser.setUsername("cashier_peter");
        cashierUser.setEmail("peter@kimwanyi.org");
        cashierUser.setStatus(UserStatus.ACTIVE);
        Role cashierRole = new Role("CASHIER");
        cashierUser.addRole(cashierRole);

        loanRepository = new DummyLoanRepository();
        loanRepaymentRepository = new DummyLoanRepaymentRepository();
        DummyMemberRepository memberRepository = new DummyMemberRepository(testMember);
        DummyUserRepository userRepository = new DummyUserRepository(Arrays.asList(adminUser, cashierUser));

        // Member savings account balance = 500,000 (Max 3x loan = 1,500,000)
        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount.setMember(testMember);
        savingsAccount.setAccountNumber("SA-1001");
        SavingsTransaction initialDeposit = new SavingsTransaction(savingsAccount, org.kimwanyi.sacco.enums.TransactionType.DEPOSIT, new BigDecimal("500000.00"), "Initial Deposit", "DEP-001");
        savingsAccount.addTransaction(initialDeposit);

        DummySavingsAccountRepository savingsAccountRepository = new DummySavingsAccountRepository(savingsAccount);

        LoanValidator loanValidator = new LoanValidator(loanRepository, loanRepaymentRepository, savingsAccountRepository);

        DummyAuditService auditService = new DummyAuditService();

        loanService = new LoanServiceImpl(
                loanRepository,
                loanRepaymentRepository,
                memberRepository,
                userRepository,
                loanValidator,
                auditService
        );
    }

    @Test
    void testLoanApplication_Calculates5PercentMonthlyInterestCorrectly() {
        // Principal = 1,000,000, Term = 6 months
        // Total Interest = 1,000,000 * 5% * 6 = 300,000
        // Total Amount Payable = 1,300,000
        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setMemberId(100L);
        request.setPrincipalAmount(new BigDecimal("1000000.00"));
        request.setTermInMonths(6);
        request.setPurpose("Agricultural Equipment Loan");

        LoanResponse response = loanService.applyForLoan(request);

        assertNotNull(response.getId());
        assertEquals(new BigDecimal("1000000.00"), response.getPrincipalAmount());
        assertEquals(new BigDecimal("300000.00"), response.getTotalInterest());
        assertEquals(new BigDecimal("1300000.00"), response.getTotalAmountPayable());
        assertEquals(new BigDecimal("1300000.00"), response.getRemainingBalance());
        assertEquals(LoanStatus.PENDING, response.getStatus());
    }

    @Test
    void testLoanApproval_ByAdmin_Succeeds() {
        LoanApplicationRequest appReq = createSampleApplication();
        LoanResponse applied = loanService.applyForLoan(appReq);

        LoanApprovalRequest approvalReq = new LoanApprovalRequest();
        approvalReq.setLoanId(applied.getId());
        approvalReq.setApproverUserId(200L); // Admin ID
        approvalReq.setApproved(true);
        approvalReq.setRemarks("Approved after credit check");

        LoanResponse approved = loanService.approveOrRejectLoan(approvalReq);

        assertEquals(LoanStatus.APPROVED, approved.getStatus());
        assertEquals(200L, approved.getApprovedByUserId());
        assertNotNull(approved.getApprovedAt());
    }

    @Test
    void testLoanApproval_ByCashier_Fails() {
        LoanApplicationRequest appReq = createSampleApplication();
        LoanResponse applied = loanService.applyForLoan(appReq);

        LoanApprovalRequest approvalReq = new LoanApprovalRequest();
        approvalReq.setLoanId(applied.getId());
        approvalReq.setApproverUserId(300L); // Cashier ID
        approvalReq.setApproved(true);

        assertThrows(AccessDeniedException.class, () -> {
            loanService.approveOrRejectLoan(approvalReq);
        });
    }

    @Test
    void testLoanApproval_SelfApproval_Fails() {
        // Create user with same ID/email as member
        User memberUser = new User();
        memberUser.setId(100L); // Same ID as testMember (100L)
        memberUser.setUsername("MEM-001");
        memberUser.setEmail("jane@example.com");
        memberUser.setStatus(UserStatus.ACTIVE);
        memberUser.addRole(new Role("ADMIN"));
        loanRepository.addUser(memberUser);

        LoanApplicationRequest appReq = createSampleApplication();
        LoanResponse applied = loanService.applyForLoan(appReq);

        LoanApprovalRequest approvalReq = new LoanApprovalRequest();
        approvalReq.setLoanId(applied.getId());
        approvalReq.setApproverUserId(100L); // Self approval attempt
        approvalReq.setApproved(true);

        assertThrows(ValidationException.class, () -> {
            loanService.approveOrRejectLoan(approvalReq);
        });
    }

    @Test
    void testLoanDisbursementAndRepaymentLifecycle() {
        // 1. Apply
        LoanApplicationRequest appReq = createSampleApplication();
        LoanResponse applied = loanService.applyForLoan(appReq);

        // 2. Approve
        LoanApprovalRequest approvalReq = new LoanApprovalRequest();
        approvalReq.setLoanId(applied.getId());
        approvalReq.setApproverUserId(200L);
        approvalReq.setApproved(true);
        loanService.approveOrRejectLoan(approvalReq);

        // 3. Disburse
        LoanResponse disbursed = loanService.disburseLoan(applied.getId(), 200L);
        assertEquals(LoanStatus.ACTIVE, disbursed.getStatus());
        assertNotNull(disbursed.getDisbursedAt());

        // 4. Partial Repayment (500,000 out of 1,300,000)
        LoanPaymentRequest pay1 = new LoanPaymentRequest();
        pay1.setLoanId(applied.getId());
        pay1.setAmount(new BigDecimal("500000.00"));
        pay1.setReferenceNumber("PAY-REF-001");

        LoanResponse afterPay1 = loanService.repayLoan(pay1);
        assertEquals(new BigDecimal("800000.00"), afterPay1.getRemainingBalance());
        assertEquals(LoanStatus.ACTIVE, afterPay1.getStatus());

        // 5. Final Repayment (800,000)
        LoanPaymentRequest pay2 = new LoanPaymentRequest();
        pay2.setLoanId(applied.getId());
        pay2.setAmount(new BigDecimal("800000.00"));
        pay2.setReferenceNumber("PAY-REF-002");

        LoanResponse completed = loanService.repayLoan(pay2);
        assertEquals(new BigDecimal("0.00"), completed.getRemainingBalance());
        assertEquals(LoanStatus.COMPLETED, completed.getStatus());
    }

    @Test
    void testLoanApplication_Exceeds3xSavingsBalance_Fails() {
        // Savings = 500,000, Max 3x limit = 1,500,000
        // Request 2,000,000 -> Should fail
        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setMemberId(100L);
        request.setPrincipalAmount(new BigDecimal("2000000.00"));
        request.setTermInMonths(6);
        request.setPurpose("Motorcycle Purchase");

        ValidationException ex = assertThrows(ValidationException.class, () -> {
            loanService.applyForLoan(request);
        });

        assertTrue(ex.getMessage().contains("exceeds maximum allowed limit of 3x savings balance"));
    }

    @Test
    void testLoanApplication_ExistingActiveLoan_Fails() {
        LoanApplicationRequest app1 = createSampleApplication();
        loanService.applyForLoan(app1); // Member now has a PENDING loan

        LoanApplicationRequest app2 = createSampleApplication();
        ValidationException ex = assertThrows(ValidationException.class, () -> {
            loanService.applyForLoan(app2);
        });

        assertTrue(ex.getMessage().contains("already has an active or pending loan application"));
    }

    private LoanApplicationRequest createSampleApplication() {
        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setMemberId(100L);
        request.setPrincipalAmount(new BigDecimal("1000000.00"));
        request.setTermInMonths(6);
        request.setPurpose("Coffee Farming Expansion");
        return request;
    }

    // Dummy Repositories
    private static class DummyLoanRepository implements LoanRepository {
        private final Map<Long, Loan> loans = new HashMap<>();
        private final Map<Long, User> users = new HashMap<>();
        private long idSequence = 1L;

        public void addUser(User user) {
            users.put(user.getId(), user);
        }

        public User getUser(Long id) {
            return users.get(id);
        }

        @Override
        public List<Loan> findByMemberId(Session session, Long memberId) {
            return loans.values().stream().filter(l -> l.getMember() != null && memberId.equals(l.getMember().getId())).toList();
        }

        @Override
        public List<Loan> findByStatus(Session session, LoanStatus status) {
            return loans.values().stream().filter(l -> status == l.getStatus()).toList();
        }

        @Override
        public boolean existsActiveLoanForMember(Session session, Long memberId) {
            return loans.values().stream().anyMatch(l -> l.getMember() != null
                    && memberId.equals(l.getMember().getId())
                    && (l.getStatus() == LoanStatus.ACTIVE || l.getStatus() == LoanStatus.PENDING));
        }

        @Override
        public Loan save(Session session, Loan entity) {
            if (entity.getId() == null) {
                entity.setId(idSequence++);
            }
            loans.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Loan update(Session session, Loan entity) {
            loans.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<Loan> findById(Session session, Long id) {
            return Optional.ofNullable(loans.get(id));
        }

        @Override
        public List<Loan> findAll(Session session) {
            return new ArrayList<>(loans.values());
        }

        @Override
        public void delete(Session session, Loan entity) {
            if (entity != null) loans.remove(entity.getId());
        }

        @Override
        public long count(Session session) {
            return loans.size();
        }

        @Override
        public boolean existsById(Session session, Long id) {
            return loans.containsKey(id);
        }
    }

    private static class DummyLoanRepaymentRepository implements LoanRepaymentRepository {
        private final List<LoanRepayment> repayments = new ArrayList<>();
        private long idSeq = 1L;

        @Override
        public List<LoanRepayment> findByLoanId(Session session, Long loanId) {
            return repayments.stream().filter(r -> r.getLoan() != null && loanId.equals(r.getLoan().getId())).toList();
        }

        @Override
        public boolean existsByReferenceNumber(Session session, String referenceNumber) {
            return repayments.stream().anyMatch(r -> referenceNumber.equals(r.getReferenceNumber()));
        }

        @Override
        public LoanRepayment save(Session session, LoanRepayment entity) {
            if (entity.getId() == null) entity.setId(idSeq++);
            repayments.add(entity);
            return entity;
        }

        @Override
        public LoanRepayment update(Session session, LoanRepayment entity) {
            return entity;
        }

        @Override
        public Optional<LoanRepayment> findById(Session session, Long id) {
            return Optional.empty();
        }

        @Override
        public List<LoanRepayment> findAll(Session session) {
            return repayments;
        }

        @Override
        public void delete(Session session, LoanRepayment entity) {}

        @Override
        public long count(Session session) { return repayments.size(); }

        @Override
        public boolean existsById(Session session, Long id) { return false; }
    }

    private static class DummyMemberRepository implements MemberRepository {
        private final Member member;

        public DummyMemberRepository(Member member) {
            this.member = member;
        }

        @Override
        public boolean existsByNationalId(Session session, String nationalId) { return false; }

        @Override
        public boolean existsByMembershipNumber(Session session, String membershipNumber) { return false; }

        @Override
        public Member findByMemberNumber(Session session, String membershipNumber) { return member; }

        @Override
        public Member findByNationalId(Session session, String nationalId) { return member; }

        @Override
        public Member save(Session session, Member entity) { return entity; }

        @Override
        public Member update(Session session, Member entity) { return entity; }

        @Override
        public Optional<Member> findById(Session session, Long id) { return Optional.ofNullable(member); }

        @Override
        public List<Member> findAll(Session session) { return Collections.singletonList(member); }

        @Override
        public void delete(Session session, Member entity) {}

        @Override
        public long count(Session session) { return 1; }

        @Override
        public boolean existsById(Session session, Long id) { return true; }
    }

    private static class DummyUserRepository implements UserRepository {
        private final Map<Long, User> userMap = new HashMap<>();

        public DummyUserRepository(List<User> users) {
            for (User u : users) {
                userMap.put(u.getId(), u);
            }
        }

        @Override
        public User findByUserName(Session session, String username) {
            return userMap.values().stream().filter(u -> username.equalsIgnoreCase(u.getUsername())).findFirst().orElse(null);
        }

        @Override
        public User findByEmail(Session session, String email) {
            return userMap.values().stream().filter(u -> email.equalsIgnoreCase(u.getEmail())).findFirst().orElse(null);
        }

        @Override
        public boolean existsByUserName(Session session, String username) {
            return userMap.values().stream().anyMatch(u -> username.equalsIgnoreCase(u.getUsername()));
        }

        @Override
        public boolean existsByEmail(Session session, String email) {
            return userMap.values().stream().anyMatch(u -> email.equalsIgnoreCase(u.getEmail()));
        }

        @Override
        public User save(Session session, User entity) {
            userMap.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public User update(Session session, User entity) {
            userMap.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<User> findById(Session session, Long id) {
            return Optional.ofNullable(userMap.get(id));
        }

        @Override
        public List<User> findAll(Session session) {
            return new ArrayList<>(userMap.values());
        }

        @Override
        public void delete(Session session, User entity) {}

        @Override
        public long count(Session session) { return userMap.size(); }

        @Override
        public boolean existsById(Session session, Long id) { return userMap.containsKey(id); }
    }

    private static class DummySavingsAccountRepository implements org.kimwanyi.sacco.repository.SavingsAccountRepository {
        private final SavingsAccount savingsAccount;

        public DummySavingsAccountRepository(SavingsAccount savingsAccount) {
            this.savingsAccount = savingsAccount;
        }

        @Override
        public Optional<SavingsAccount> findByAccountNumber(Session session, String accountNumber) {
            return Optional.ofNullable(savingsAccount);
        }

        @Override
        public Optional<SavingsAccount> findByMemberId(Session session, Long memberId) {
            return Optional.ofNullable(savingsAccount);
        }

        @Override
        public boolean existsByAccountNumber(Session session, String accountNumber) { return true; }

        @Override
        public SavingsAccount save(Session session, SavingsAccount entity) { return entity; }

        @Override
        public SavingsAccount update(Session session, SavingsAccount entity) { return entity; }

        @Override
        public Optional<SavingsAccount> findById(Session session, Long id) { return Optional.ofNullable(savingsAccount); }

        @Override
        public List<SavingsAccount> findAll(Session session) { return Collections.singletonList(savingsAccount); }

        @Override
        public void delete(Session session, SavingsAccount entity) {}

        @Override
        public long count(Session session) { return 1; }

        @Override
        public boolean existsById(Session session, Long id) { return true; }
    }

    private static class DummyAuditService implements org.kimwanyi.sacco.audit.AuditService {
        private final List<String> logs = new ArrayList<>();

        @Override
        public void logSuccess(Long userId, org.kimwanyi.sacco.enums.AuditAction action, String entityName, Long entityId, String description) {
            logs.add(action + ":" + entityName + ":" + entityId + ":" + description);
        }

        @Override
        public void logFailure(Long userId, org.kimwanyi.sacco.enums.AuditAction action, String description) {
            logs.add("FAILURE:" + action + ":" + description);
        }

        public List<String> getLogs() {
            return logs;
        }
    }
}
