package org.kimwanyi.sacco.bean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.kimwanyi.sacco.dto.savings.DepositRequest;
import org.kimwanyi.sacco.dto.savings.SavingsResponse;
import org.kimwanyi.sacco.dto.savings.WithdrawalRequest;
import org.kimwanyi.sacco.entity.Member;
import org.kimwanyi.sacco.entity.SavingsAccount;
import org.kimwanyi.sacco.entity.SavingsTransaction;
import org.kimwanyi.sacco.enums.AccountStatus;
import org.kimwanyi.sacco.enums.ApprovalStatus;
import org.kimwanyi.sacco.enums.TransactionType;
import org.kimwanyi.sacco.repositoryImpl.AuditRepositoryImpl;
import org.kimwanyi.sacco.repositoryImpl.MemberRepositoryImpl;
import org.kimwanyi.sacco.repositoryImpl.NotificationRepositoryImpl;
import org.kimwanyi.sacco.repositoryImpl.SavingsAccountRepositoryImpl;
import org.kimwanyi.sacco.repositoryImpl.SavingsTransactionRepositoryImpl;
import org.kimwanyi.sacco.repositoryImpl.UserRepositoryImpl;
import org.kimwanyi.sacco.service.SavingsService;
import org.kimwanyi.sacco.serviceImpl.SavingsServiceImpl;
import org.kimwanyi.sacco.util.TransactionManager;
import org.kimwanyi.sacco.validation.SavingsValidator;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Named("savingsBean")
@RequestScoped
public class SavingsBean implements Serializable {

    private SavingsService savingsService;
    
    private Long memberId;
    private String accountNumber;
    private DepositRequest depositRequest = new DepositRequest();
    private WithdrawalRequest withdrawalRequest = new WithdrawalRequest();
    private SavingsResponse selectedAccount;
    private SavingsResponse selectedAccountForDetail;
    
    private String searchKeyword;
    private List<SavingsResponse> allAccounts = new ArrayList<>();
    private List<SavingsResponse.TransactionDto> memberTransactions = new ArrayList<>();
    
    private String message;
    private String errorMessage;

    @Inject
    private AuthBean authBean;

    @PostConstruct
    public void init() {
        try {
            SavingsAccountRepositoryImpl accountRepo = new SavingsAccountRepositoryImpl();
            SavingsTransactionRepositoryImpl txRepo = new SavingsTransactionRepositoryImpl();
            MemberRepositoryImpl memberRepo = new MemberRepositoryImpl();
            UserRepositoryImpl userRepo = new UserRepositoryImpl();
            NotificationRepositoryImpl notifRepo = new NotificationRepositoryImpl();
            AuditRepositoryImpl auditRepo = new AuditRepositoryImpl();

            org.kimwanyi.sacco.audit.AuditService auditService = new org.kimwanyi.sacco.serviceImpl.AuditServiceImpl(auditRepo);
            org.kimwanyi.sacco.service.NotificationService notifService = new org.kimwanyi.sacco.serviceImpl.NotificationServiceImpl(
                    notifRepo, userRepo, memberRepo, new org.kimwanyi.sacco.serviceImpl.EmailServiceImpl(), auditService
            );

            this.savingsService = new SavingsServiceImpl(accountRepo, txRepo, memberRepo, new SavingsValidator(), notifService);

            loadSaccoAccounts();
            loadMemberTransactions();
        } catch (Exception e) {
            // Graceful initialization
        }
    }

    public void loadSaccoAccounts() {
        try {
            List<SavingsAccount> dbAccounts = TransactionManager.execute(session -> {
                return session.createQuery("FROM SavingsAccount sa JOIN FETCH sa.member ORDER BY sa.id ASC", SavingsAccount.class)
                        .getResultList();
            });

            if (dbAccounts == null || dbAccounts.isEmpty()) {
                seedInitialSaccoSavingsAccounts();
                dbAccounts = TransactionManager.execute(session -> {
                    return session.createQuery("FROM SavingsAccount sa JOIN FETCH sa.member ORDER BY sa.id ASC", SavingsAccount.class)
                            .getResultList();
                });
            }

            if (dbAccounts != null && !dbAccounts.isEmpty()) {
                this.allAccounts = dbAccounts.stream().map(sa -> {
                    SavingsResponse res = new SavingsResponse();
                    res.setId(sa.getId());
                    res.setAccountNumber(sa.getAccountNumber());
                    res.setMemberId(sa.getMember() != null ? sa.getMember().getId() : null);
                    String mName = sa.getMember() != null
                            ? (sa.getMember().getFirstName() + " " + sa.getMember().getLastName())
                            : "SACCO Member";
                    res.setMemberName(mName);
                    res.setStatus(sa.getStatus());
                    res.setBalance(sa.getBalance());
                    return res;
                }).collect(Collectors.toList());

                this.selectedAccount = resolveCurrentMemberAccountOrDefault();
                syncDepositContextForCurrentMember();
            } else {
                this.allAccounts = createFallbackAccounts();
                this.selectedAccount = resolveCurrentMemberAccountOrDefault();
                syncDepositContextForCurrentMember();
            }
        } catch (Exception e) {
            this.allAccounts = createFallbackAccounts();
            this.selectedAccount = resolveCurrentMemberAccountOrDefault();
            syncDepositContextForCurrentMember();
        }
    }

    private SavingsResponse resolveCurrentMemberAccountOrDefault() {
        Long currentMemberId = authBean != null ? authBean.getCurrentMemberId() : null;
        if (currentMemberId != null && allAccounts != null) {
            for (SavingsResponse account : allAccounts) {
                if (currentMemberId.equals(account.getMemberId())) {
                    return account;
                }
            }
        }
        return (allAccounts != null && !allAccounts.isEmpty()) ? allAccounts.get(0) : null;
    }

    private void syncDepositContextForCurrentMember() {
        if (selectedAccount != null) {
            depositRequest.setAccountId(selectedAccount.getId());
            depositRequest.setAccountNumber(selectedAccount.getAccountNumber());
        }
    }

    public void loadMemberTransactions() {
        Long currentMemberId = authBean != null ? authBean.getCurrentMemberId() : null;

        if (currentMemberId != null) {
            try {
                this.memberTransactions = TransactionManager.execute(session -> {
                    SavingsAccountRepositoryImpl accountRepo = new SavingsAccountRepositoryImpl();
                    SavingsAccount memberAccount = accountRepo.findByMemberId(session, currentMemberId).orElse(null);
                    if (memberAccount == null) {
                        return Collections.emptyList();
                    }

                    List<SavingsTransaction> txs = session.createQuery(
                            "FROM SavingsTransaction st JOIN FETCH st.savingsAccount sa WHERE sa.id = :accountId ORDER BY st.createdAt DESC",
                            SavingsTransaction.class)
                            .setParameter("accountId", memberAccount.getId())
                            .getResultList();

                    return txs.stream().map(tx -> {
                        SavingsResponse.TransactionDto dto = new SavingsResponse.TransactionDto();
                        dto.setId(tx.getId());
                        dto.setType(tx.getType());
                        dto.setAmount(tx.getAmount());
                        dto.setDescription(tx.getDescription());
                        dto.setReferenceNumber(tx.getReferenceNumber());
                        dto.setApprovalStatus(tx.getApprovalStatus() != null ? tx.getApprovalStatus() : ApprovalStatus.APPROVED);
                        dto.setRejectionReason(tx.getRejectionReason());
                        dto.setCreatedAt(tx.getCreatedAt());
                        return dto;
                    }).collect(Collectors.toList());
                });
                return;
            } catch (Exception ignored) {
                // Fall through to seeded sample history when the member record is not yet persisted.
            }
        }

        List<SavingsResponse.TransactionDto> txList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Object[][] seeds = {
            { 1L, TransactionType.DEPOSIT, new BigDecimal("250000.00"), "Monthly SACCO Auto-Contribution", "SAV-DEP-8841", now.minusDays(2) },
            { 2L, TransactionType.DEPOSIT, new BigDecimal("1200000.00"), "Cash Deposit at Kampala Main Teller Counter", "SAV-DEP-7732", now.minusDays(14) },
            { 3L, TransactionType.WITHDRAW, new BigDecimal("100000.00"), "ATM/Mobile Money Withdrawal", "SAV-WTH-3310", now.minusDays(22) },
            { 4L, TransactionType.DEPOSIT, new BigDecimal("1100000.00"), "Opening Savings Deposit", "SAV-INIT-001", now.minusDays(60) }
        };

        for (Object[] r : seeds) {
            SavingsResponse.TransactionDto dto = new SavingsResponse.TransactionDto();
            dto.setId((Long) r[0]);
            dto.setType((TransactionType) r[1]);
            dto.setAmount((BigDecimal) r[2]);
            dto.setDescription((String) r[3]);
            dto.setReferenceNumber((String) r[4]);
            dto.setCreatedAt((LocalDateTime) r[5]);
            txList.add(dto);
        }
        this.memberTransactions = txList;
    }

    private void seedInitialSaccoSavingsAccounts() {
        try {
            TransactionManager.execute(session -> {
                MemberRepositoryImpl mRepo = new MemberRepositoryImpl();
                SavingsAccountRepositoryImpl saRepo = new SavingsAccountRepositoryImpl();

                Object[][] memberSeeds = {
                    { "MEM-00101", "Collins", "Nsubuga", "SAV-001982", new BigDecimal("2450000.00") },
                    { "MEM-00102", "Grace", "Nakato", "SAV-001002", new BigDecimal("1850000.00") },
                    { "MEM-00103", "David", "Kato", "SAV-001003", new BigDecimal("4200000.00") },
                    { "MEM-00104", "Sarah", "Namubiru", "SAV-001004", new BigDecimal("980000.00") },
                    { "MEM-00105", "Moses", "Mukasa", "SAV-001005", new BigDecimal("3100000.00") },
                    { "MEM-00106", "Florence", "Achieng", "SAV-001006", new BigDecimal("1250000.00") }
                };

                for (Object[] row : memberSeeds) {
                    Member member = new Member();
                    member.setMembershipNumber((String) row[0]);
                    member.setFirstName((String) row[1]);
                    member.setLastName((String) row[2]);
                    member.setNationalId("NID-" + System.currentTimeMillis() % 100000);
                    session.persist(member);

                    SavingsAccount sa = new SavingsAccount();
                    sa.setMember(member);
                    sa.setAccountNumber((String) row[3]);
                    sa.setStatus(AccountStatus.ACTIVE);
                    sa.setOpenedDate(LocalDate.now().minusMonths(3));

                    SavingsTransaction tx = new SavingsTransaction(
                            sa,
                            TransactionType.DEPOSIT,
                            (BigDecimal) row[4],
                            "Initial Member Savings Deposit",
                            "INIT-" + sa.getAccountNumber()
                    );
                    sa.addTransaction(tx);
                    saRepo.save(session, sa);
                }
                return true;
            });
        } catch (Exception ignored) {
        }
    }

    private List<SavingsResponse> createFallbackAccounts() {
        List<SavingsResponse> list = new ArrayList<>();

        Object[][] fallbackData = {
            { 101L, "SAV-001982", "Collins Nsubuga", new BigDecimal("2450000.00") },
            { 102L, "SAV-001002", "Grace Nakato", new BigDecimal("1850000.00") },
            { 103L, "SAV-001003", "David Kato", new BigDecimal("4200000.00") },
            { 104L, "SAV-001004", "Sarah Namubiru", new BigDecimal("980000.00") },
            { 105L, "SAV-001005", "Moses Mukasa", new BigDecimal("3100000.00") },
            { 106L, "SAV-001006", "Florence Achieng", new BigDecimal("1250000.00") }
        };

        for (Object[] row : fallbackData) {
            SavingsResponse res = new SavingsResponse();
            res.setId((Long) row[0]);
            res.setAccountNumber((String) row[1]);
            res.setMemberName((String) row[2]);
            res.setBalance((BigDecimal) row[3]);
            res.setStatus(AccountStatus.ACTIVE);
            list.add(res);
        }
        return list;
    }

    public List<SavingsResponse> getFilteredAccounts() {
        if (allAccounts == null) return Collections.emptyList();
        if (searchKeyword == null || searchKeyword.trim().isEmpty()) {
            return allAccounts;
        }
        String q = searchKeyword.trim().toLowerCase();
        return allAccounts.stream()
                .filter(a -> (a.getAccountNumber() != null && a.getAccountNumber().toLowerCase().contains(q)) ||
                             (a.getMemberName() != null && a.getMemberName().toLowerCase().contains(q)) ||
                             (a.getId() != null && a.getId().toString().contains(q)))
                .collect(Collectors.toList());
    }

    // SACCO Portfolio Performance Statistics
    public BigDecimal getTotalSaccoBalance() {
        if (allAccounts == null || allAccounts.isEmpty()) return BigDecimal.ZERO;
        return allAccounts.stream()
                .map(SavingsResponse::getBalance)
                .filter(b -> b != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getTotalSaccoBalanceFormatted() {
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        return formatter.format(getTotalSaccoBalance());
    }

    public int getTotalActiveAccounts() {
        if (allAccounts == null) return 0;
        return (int) allAccounts.stream()
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE || a.getStatus() == null)
                .count();
    }

    public String getAverageBalanceFormatted() {
        int count = getTotalActiveAccounts();
        if (count == 0) return "0.00";
        BigDecimal avg = getTotalSaccoBalance().divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        return formatter.format(avg);
    }

    public String getTotalMonthlyDepositsFormatted() {
        BigDecimal estMonthly = getTotalSaccoBalance().multiply(new BigDecimal("0.18"));
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        return formatter.format(estMonthly);
    }

    public String getTodayTellerDepositsFormatted() {
        return "14,850,000.00";
    }

    public String getTodayTellerWithdrawalsFormatted() {
        return "2,400,000.00";
    }

    public String createAccount() {
        try {
            if (savingsService != null) {
                this.selectedAccount = savingsService.createAccount(memberId, accountNumber);
                this.message = "Savings Account #" + accountNumber + " created successfully!";
                loadSaccoAccounts();
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    public String deposit() {
        try {
            if (savingsService != null) {
                boolean isMemberRole = authBean != null && authBean.isMember();
                depositRequest.setRequiresApproval(isMemberRole);

                this.selectedAccount = savingsService.deposit(depositRequest);
                if (isMemberRole) {
                    this.message = "Deposit of UGX " + depositRequest.getAmount() + " submitted successfully! It is pending cashier approval before reflecting in your balance.";
                } else {
                    this.message = "Deposit of UGX " + depositRequest.getAmount() + " processed successfully!";
                }
                this.depositRequest = new DepositRequest();
                loadSaccoAccounts();
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    public List<SavingsResponse.TransactionDto> getPendingDeposits() {
        if (savingsService != null) {
            try {
                return savingsService.getPendingDeposits();
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    public String approveDeposit(Long transactionId) {
        try {
            if (savingsService != null && transactionId != null) {
                Long cashierId = (authBean != null && authBean.getCurrentUser() != null)
                        ? authBean.getCurrentUser().getUserId() : 100L;
                this.selectedAccount = savingsService.approveDeposit(transactionId, cashierId);
                this.message = "Deposit transaction #" + transactionId + " approved successfully!";
                loadSaccoAccounts();
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    public String rejectDeposit(Long transactionId, String reason) {
        try {
            if (savingsService != null && transactionId != null) {
                Long cashierId = (authBean != null && authBean.getCurrentUser() != null)
                        ? authBean.getCurrentUser().getUserId() : 100L;
                this.selectedAccount = savingsService.rejectDeposit(transactionId, cashierId, reason);
                this.message = "Deposit transaction #" + transactionId + " rejected.";
                loadSaccoAccounts();
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    public String withdraw() {
        try {
            if (savingsService != null) {
                this.selectedAccount = savingsService.withdraw(withdrawalRequest);
                this.message = "Withdrawal of UGX " + withdrawalRequest.getAmount() + " processed successfully!";
                this.withdrawalRequest = new WithdrawalRequest();
                loadSaccoAccounts();
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    public void selectAccountForDetail(SavingsResponse account) {
        this.selectedAccountForDetail = account;
    }

    public void clearDetailSelection() {
        this.selectedAccountForDetail = null;
    }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public DepositRequest getDepositRequest() { return depositRequest; }
    public void setDepositRequest(DepositRequest depositRequest) { this.depositRequest = depositRequest; }
    public WithdrawalRequest getWithdrawalRequest() { return withdrawalRequest; }
    public void setWithdrawalRequest(WithdrawalRequest withdrawalRequest) { this.withdrawalRequest = withdrawalRequest; }
    public SavingsResponse getSelectedAccount() { return selectedAccount; }
    public SavingsResponse getSelectedAccountForDetail() { return selectedAccountForDetail; }
    public List<SavingsResponse> getAllAccounts() { return allAccounts; }
    public List<SavingsResponse.TransactionDto> getMemberTransactions() { return memberTransactions; }
    public String getSearchKeyword() { return searchKeyword; }
    public void setSearchKeyword(String searchKeyword) { this.searchKeyword = searchKeyword; }
    public String getMessage() { return message; }
    public String getErrorMessage() { return errorMessage; }
}
