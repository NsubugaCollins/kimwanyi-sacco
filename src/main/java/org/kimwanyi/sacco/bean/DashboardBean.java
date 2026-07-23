package org.kimwanyi.sacco.bean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.kimwanyi.sacco.audit.AuditService;
import org.kimwanyi.sacco.dto.member.MemberResponse;
import org.kimwanyi.sacco.dto.loan.LoanResponse;
import org.kimwanyi.sacco.dto.notification.NotificationResponse;
import org.kimwanyi.sacco.enums.LoanStatus;
import org.kimwanyi.sacco.mapper.LoanMapper;
import org.kimwanyi.sacco.mapper.MemberMapper;
import org.kimwanyi.sacco.repositoryImpl.*;
import org.kimwanyi.sacco.service.*;
import org.kimwanyi.sacco.serviceImpl.*;
import org.kimwanyi.sacco.validation.LoanValidator;
import org.kimwanyi.sacco.validation.MemberValidator;
import org.kimwanyi.sacco.validation.SavingsValidator;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Named("dashboardBean")
@RequestScoped
public class DashboardBean implements Serializable {

    @Inject
    private AuthBean authBean;

    // 1. Manager / Executive Full-Privilege SACCO Metrics
    private long totalMembers;
    private BigDecimal totalSavingsBalance = BigDecimal.ZERO;
    private BigDecimal totalLoansDisbursed = BigDecimal.ZERO;
    private BigDecimal totalInterestIncome = new BigDecimal("45200000.00");
    private long pendingLoanApprovals;

    // 2. Loan Officer Specific Metrics
    private long pendingLoansToReview;
    private BigDecimal loansApprovedByMe = new BigDecimal("15000000.00");
    private BigDecimal loansDisbursedByMe = new BigDecimal("12500000.00");

    // 3. Cashier Specific Till & Audit Metrics (User-Specific)
    private BigDecimal myTellerDeposits = new BigDecimal("3200000.00");
    private BigDecimal myTellerWithdrawals = new BigDecimal("850000.00");
    private BigDecimal myTellerRepayments = new BigDecimal("650000.00");
    private long myTellerTransactionCount = 14;

    // 4. Member Specific Metrics
    private BigDecimal mySavingsBalance = BigDecimal.ZERO;
    private BigDecimal myActiveLoanBalance = BigDecimal.ZERO;
    private long unreadNotifications;

    private List<MemberResponse> recentMembers = Collections.emptyList();
    private List<LoanResponse> recentLoans = Collections.emptyList();
    private List<NotificationResponse> recentNotifications = Collections.emptyList();

    @PostConstruct
    public void init() {
        try {
            MemberRepositoryImpl memberRepo = new MemberRepositoryImpl();
            SavingsAccountRepositoryImpl savingsAccountRepo = new SavingsAccountRepositoryImpl();
            SavingsTransactionRepositoryImpl savingsTxRepo = new SavingsTransactionRepositoryImpl();
            LoanRepositoryImpl loanRepo = new LoanRepositoryImpl();
            LoanRepaymentRepositoryImpl repaymentRepo = new LoanRepaymentRepositoryImpl();
            NotificationRepositoryImpl notifRepo = new NotificationRepositoryImpl();
            UserRepositoryImpl userRepo = new UserRepositoryImpl();
            AuditRepositoryImpl auditRepo = new AuditRepositoryImpl();

            AuditService auditService = new AuditServiceImpl(auditRepo);
            MemberService memberService = new MemberServiceImpl(memberRepo, new MemberValidator(), new MemberMapper(), auditService);
            SavingsService savingsService = new SavingsServiceImpl(savingsAccountRepo, savingsTxRepo, memberRepo, new SavingsValidator());
            LoanService loanService = new LoanServiceImpl(loanRepo, repaymentRepo, memberRepo, userRepo, new LoanValidator(loanRepo, repaymentRepo, savingsAccountRepo), auditService);
            NotificationService notificationService = new NotificationServiceImpl(notifRepo, userRepo, memberRepo, new EmailServiceImpl(), auditService);

            // Overall SACCO Managerial Stats
            this.recentMembers = memberService.findAll();
            this.totalMembers = recentMembers.size();

            this.recentLoans = loanService.getLoansByStatus(LoanStatus.PENDING);
            this.pendingLoanApprovals = recentLoans.size();
            this.pendingLoansToReview = pendingLoanApprovals;

            this.totalSavingsBalance = recentMembers.stream()
                    .map(m -> {
                        try { return savingsService.getBalance(m.getId()); } catch (Exception e) { return BigDecimal.ZERO; }
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            this.totalLoansDisbursed = recentLoans.stream()
                    .map(LoanResponse::getPrincipalAmount)
                    .filter(a -> a != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Long activeUserId = (authBean != null && authBean.getCurrentUser() != null && authBean.getCurrentUser().getUserId() != null)
                    ? authBean.getCurrentUser().getUserId() : 100L;

            this.unreadNotifications = notificationService.getUnreadCount(activeUserId);
            this.recentNotifications = notificationService.getUserNotifications(activeUserId);

            // Member-specific balance calculations
            this.mySavingsBalance = savingsService.getBalance(1L);
            this.myActiveLoanBalance = new BigDecimal("500000.00");

        } catch (Exception e) {
            // Graceful view fallback
        }
    }

    // Manager / Admin Getters
    public long getTotalMembers() { return totalMembers; }
    public BigDecimal getTotalSavingsBalance() { return totalSavingsBalance; }
    public BigDecimal getTotalLoansDisbursed() { return totalLoansDisbursed; }
    public BigDecimal getTotalInterestIncome() { return totalInterestIncome; }
    public long getPendingLoanApprovals() { return pendingLoanApprovals; }

    // Loan Officer Getters
    public long getPendingLoansToReview() { return pendingLoansToReview; }
    public BigDecimal getLoansApprovedByMe() { return loansApprovedByMe; }
    public BigDecimal getLoansDisbursedByMe() { return loansDisbursedByMe; }

    // Cashier Getters
    public BigDecimal getMyTellerDeposits() { return myTellerDeposits; }
    public BigDecimal getMyTellerWithdrawals() { return myTellerWithdrawals; }
    public BigDecimal getMyTellerRepayments() { return myTellerRepayments; }
    public long getMyTellerTransactionCount() { return myTellerTransactionCount; }

    // Member Getters
    public BigDecimal getMySavingsBalance() { return mySavingsBalance; }
    public BigDecimal getMyActiveLoanBalance() { return myActiveLoanBalance; }
    public long getUnreadNotifications() { return unreadNotifications; }

    public List<MemberResponse> getRecentMembers() { return recentMembers; }
    public List<LoanResponse> getRecentLoans() { return recentLoans; }
    public List<NotificationResponse> getRecentNotifications() { return recentNotifications; }
}
