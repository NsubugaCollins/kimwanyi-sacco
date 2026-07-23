package org.kimwanyi.sacco.bean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

import org.kimwanyi.sacco.audit.AuditService;
import org.kimwanyi.sacco.dto.loan.LoanApplicationRequest;
import org.kimwanyi.sacco.dto.loan.LoanApprovalRequest;
import org.kimwanyi.sacco.dto.loan.LoanPaymentRequest;
import org.kimwanyi.sacco.dto.loan.LoanResponse;
import org.kimwanyi.sacco.enums.LoanStatus;
import org.kimwanyi.sacco.repositoryImpl.*;
import org.kimwanyi.sacco.service.LoanService;
import org.kimwanyi.sacco.serviceImpl.AuditServiceImpl;
import org.kimwanyi.sacco.serviceImpl.LoanServiceImpl;
import org.kimwanyi.sacco.validation.LoanValidator;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Named("loanBean")
@SessionScoped
public class LoanBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private LoanService loanService;

    private LoanApplicationRequest applicationRequest = new LoanApplicationRequest();
    private LoanApprovalRequest approvalRequest = new LoanApprovalRequest();
    private LoanPaymentRequest paymentRequest = new LoanPaymentRequest();
    private List<LoanResponse> loans = Collections.emptyList();

    private LoanResponse selectedLoan;
    private String approvalRemarks;
    private java.math.BigDecimal repaymentAmount;
    private String repaymentRef;

    private String message;
    private String errorMessage;

    // Savings balance for the form info panel — pulled from SavingsBean/DB
    private BigDecimal savingsBalance = new BigDecimal("2450000.00");

    @jakarta.inject.Inject
    private AuthBean authBean;

    @jakarta.inject.Inject
    private SavingsBean savingsBean;

    private List<org.kimwanyi.sacco.entity.Member> registeredMembers = Collections.emptyList();

    @PostConstruct
    public void init() {
        // Pull savings balance from SavingsBean so the loan limit is accurate
        if (savingsBean != null && savingsBean.getSelectedAccount() != null
                && savingsBean.getSelectedAccount().getBalance() != null) {
            this.savingsBalance = savingsBean.getSelectedAccount().getBalance();
        }

        // Pre-populate defaults
        applicationRequest.setTermInMonths(6);

        try {
            LoanRepositoryImpl loanRepo = new LoanRepositoryImpl();
            LoanRepaymentRepositoryImpl repaymentRepo = new LoanRepaymentRepositoryImpl();
            MemberRepositoryImpl memberRepo = new MemberRepositoryImpl();
            SavingsAccountRepositoryImpl savingsRepo = new SavingsAccountRepositoryImpl();
            UserRepositoryImpl userRepo = new UserRepositoryImpl();
            AuditRepositoryImpl auditRepo = new AuditRepositoryImpl();
            NotificationRepositoryImpl notifRepo = new NotificationRepositoryImpl();

            AuditService auditService = new AuditServiceImpl(auditRepo);
            org.kimwanyi.sacco.service.NotificationService notifService = new org.kimwanyi.sacco.serviceImpl.NotificationServiceImpl(
                    notifRepo, userRepo, memberRepo, new org.kimwanyi.sacco.serviceImpl.EmailServiceImpl(), auditService
            );
            this.loanService = new LoanServiceImpl(loanRepo, repaymentRepo, memberRepo, userRepo, new LoanValidator(loanRepo, repaymentRepo, savingsRepo), auditService, notifService);

            loadLoans();
            loadRegisteredMembers();
        } catch (Exception e) {
            // Graceful view init
        }
    }


    public void loadLoans() {
        if (loanService != null) {
            try {
                // Load ALL loans so staff can see the full pipeline
                this.loans = loanService.getLoansByStatus(null);
                if (this.loans == null) {
                    this.loans = Collections.emptyList();
                }
            } catch (Exception e) {
                // Fallback: try loading PENDING only
                try {
                    this.loans = loanService.getLoansByStatus(LoanStatus.PENDING);
                } catch (Exception ex) {
                    this.errorMessage = "Failed to load loans: " + ex.getMessage();
                }
            }
        }
    }

    public void loadRegisteredMembers() {
        try {
            MemberRepositoryImpl memberRepo = new MemberRepositoryImpl();
            this.registeredMembers = org.kimwanyi.sacco.util.TransactionManager.execute(session -> {
                if (session == null) return Collections.emptyList();
                return memberRepo.findAll(session);
            });
        } catch (Exception e) {
            this.registeredMembers = Collections.emptyList();
        }
    }

    public String applyForLoan() {
        try {
            if (loanService != null) {
                // For Member logins: use the memberId from the authenticated session (members table)
                // For Staff logins: use the selected member from the dropdown
                if (applicationRequest.getMemberId() == null || applicationRequest.getMemberId() <= 0) {
                    if (authBean != null && authBean.isMember() && authBean.getCurrentMemberId() != null) {
                        applicationRequest.setMemberId(authBean.getCurrentMemberId());
                    } else {
                        Long resolvedMemberId = findOrCreateMember();
                        applicationRequest.setMemberId(resolvedMemberId);
                    }
                }

                loanService.applyForLoan(applicationRequest);
                this.message = "Loan application submitted successfully! A loans officer will review it shortly.";
                this.applicationRequest = new LoanApplicationRequest();
                applicationRequest.setTermInMonths(6);
                loadLoans();
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    /**
     * Finds an existing Member for the logged-in user, or auto-creates one from
     * session data so the member does not have to register separately before
     * applying for a loan.
     */
    private Long findOrCreateMember() {
        org.kimwanyi.sacco.repositoryImpl.MemberRepositoryImpl memberRepo =
                new org.kimwanyi.sacco.repositoryImpl.MemberRepositoryImpl();

        // Derive name/email from auth session
        String username = (authBean != null && authBean.getCurrentUser() != null)
                ? authBean.getCurrentUser().getUsername() : "member";
        String email = (authBean != null && authBean.getCurrentUser() != null)
                ? authBean.getCurrentUser().getEmail() : null;
        Long userId = (authBean != null && authBean.getCurrentUser() != null)
                ? authBean.getCurrentUser().getUserId() : null;

        // Split username into first/last name parts
        String firstName = username;
        String lastName  = "Member";
        if (username != null && username.contains(" ")) {
            String[] parts = username.trim().split("\\s+", 2);
            firstName = parts[0];
            lastName  = parts[1];
        } else if (username != null && username.contains(".")) {
            String[] parts = username.trim().split("\\.", 2);
            firstName = parts[0];
            lastName  = parts[1];
        }

        final String finalFirst = firstName;
        final String finalLast  = lastName;
        final String finalEmail = email;
        final Long   finalUid   = userId;

        return org.kimwanyi.sacco.util.TransactionManager.execute(session -> {
            if (session == null) return 1L;

            // 1. Try to find an existing member by ID (userId == memberId by convention)
            if (finalUid != null) {
                java.util.Optional<org.kimwanyi.sacco.entity.Member> existing =
                        memberRepo.findById(session, finalUid);
                if (existing.isPresent()) {
                    return existing.get().getId();
                }
            }

            // 2. Try to find by email
            if (finalEmail != null && !finalEmail.isEmpty()) {
                org.kimwanyi.sacco.entity.Member byEmail = session.createQuery(
                        "FROM Member m WHERE m.email = :email",
                        org.kimwanyi.sacco.entity.Member.class)
                    .setParameter("email", finalEmail)
                    .uniqueResult();
                if (byEmail != null) return byEmail.getId();
            }

            // 3. Auto-create a new Member record from session data
            org.kimwanyi.sacco.entity.Member newMember = new org.kimwanyi.sacco.entity.Member();
            String suffix = finalUid != null ? String.valueOf(finalUid)
                          : String.valueOf(System.currentTimeMillis() % 100000);
            newMember.setMembershipNumber("MEM-" + suffix);
            newMember.setFirstName(finalFirst != null ? finalFirst : "Member");
            newMember.setLastName(finalLast  != null ? finalLast  : "User");
            newMember.setNationalId("AUTO-" + suffix);   // placeholder — update profile later
            newMember.setEmail(finalEmail);
            newMember.setPhoneNumber(applicationRequest.getPhoneNumber());
            newMember.setStatus(org.kimwanyi.sacco.enums.UserStatus.ACTIVE);

            org.kimwanyi.sacco.entity.Member saved = memberRepo.save(session, newMember);
            
            // Auto-provision an active savings account for the member
            org.kimwanyi.sacco.repositoryImpl.SavingsAccountRepositoryImpl saRepo =
                    new org.kimwanyi.sacco.repositoryImpl.SavingsAccountRepositoryImpl();
            if (saRepo.findByMemberId(session, saved.getId()).isEmpty()) {
                org.kimwanyi.sacco.entity.SavingsAccount sa = new org.kimwanyi.sacco.entity.SavingsAccount();
                sa.setMember(saved);
                sa.setAccountNumber("SAV-MEM-" + saved.getId());
                sa.setStatus(org.kimwanyi.sacco.enums.AccountStatus.ACTIVE);
                sa.setOpenedDate(java.time.LocalDate.now());
                org.kimwanyi.sacco.entity.SavingsTransaction initDep = new org.kimwanyi.sacco.entity.SavingsTransaction(
                        sa,
                        org.kimwanyi.sacco.enums.TransactionType.DEPOSIT,
                        new java.math.BigDecimal("2500000.00"),
                        "Initial Member Savings Deposit",
                        "SAV-INIT-" + saved.getId()
                );
                sa.addTransaction(initDep);
                saRepo.save(session, sa);
            }

            return saved.getId();
        });
    }


    public String selectLoan(Long loanId) {
        try {
            if (loanService != null && loanId != null) {
                this.selectedLoan = loanService.getLoanById(loanId);
                this.approvalRemarks = "";
                this.repaymentAmount = null;
                this.repaymentRef = "";
                this.message = null;
                this.errorMessage = null;
            }
        } catch (Exception e) {
            this.errorMessage = "Failed to load loan details: " + e.getMessage();
        }
        return null;
    }

    public String approveLoan(Long loanId) {
        try {
            if (loanService != null && loanId != null) {
                LoanApprovalRequest req = new LoanApprovalRequest();
                req.setLoanId(loanId);
                req.setApproved(true);
                req.setRemarks(approvalRemarks);
                // Use the logged-in staff userId as approver
                if (authBean != null && authBean.getCurrentUser() != null) {
                    req.setApproverUserId(authBean.getCurrentUser().getUserId());
                }
                this.selectedLoan = loanService.approveOrRejectLoan(req);
                this.message = "Loan #L-" + loanId + " approved successfully.";
                loadLoans();
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    public String rejectLoan(Long loanId) {
        try {
            if (loanService != null && loanId != null) {
                LoanApprovalRequest req = new LoanApprovalRequest();
                req.setLoanId(loanId);
                req.setApproved(false);
                req.setRemarks(approvalRemarks != null && !approvalRemarks.isEmpty() ? approvalRemarks : "Rejected by officer");
                if (authBean != null && authBean.getCurrentUser() != null) {
                    req.setApproverUserId(authBean.getCurrentUser().getUserId());
                }
                this.selectedLoan = loanService.approveOrRejectLoan(req);
                this.message = "Loan #L-" + loanId + " rejected.";
                loadLoans();
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    public String approveLoan() {
        try {
            if (loanService != null) {
                loanService.approveOrRejectLoan(approvalRequest);
                this.message = "Loan decision saved successfully.";
                loadLoans();
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    public String disburseLoan(Long loanId, Long officerId) {
        try {
            if (loanService != null && loanId != null) {
                Long officer = officerId;
                if (officer == null && authBean != null && authBean.getCurrentUser() != null) {
                    officer = authBean.getCurrentUser().getUserId();
                }
                if (officer == null) officer = 100L;
                this.selectedLoan = loanService.disburseLoan(loanId, officer);
                this.message = "Loan #L-" + loanId + " disbursed successfully.";
                loadLoans();
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    public String makeRepaymentForLoan(Long loanId) {
        try {
            if (loanService != null && loanId != null && repaymentAmount != null) {
                boolean isMemberRole = authBean != null && authBean.isMember();
                LoanPaymentRequest req = new LoanPaymentRequest();
                req.setLoanId(loanId);
                req.setAmount(repaymentAmount);
                req.setReferenceNumber(repaymentRef != null && !repaymentRef.isEmpty()
                        ? repaymentRef : "REP-" + System.currentTimeMillis() % 100000);
                req.setRequiresApproval(isMemberRole);

                loanService.repayLoan(req);
                if (isMemberRole) {
                    this.message = "Repayment of UGX " + repaymentAmount + " submitted successfully! It is pending cashier approval before reflecting on your loan balance.";
                } else {
                    this.message = "Repayment of UGX " + repaymentAmount + " recorded for Loan #L-" + loanId;
                }
                this.repaymentAmount = null;
                this.repaymentRef = "";
                // Refresh selected loan details
                this.selectedLoan = loanService.getLoanById(loanId);
                loadLoans();
            } else if (repaymentAmount == null) {
                this.errorMessage = "Please enter a repayment amount.";
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    public List<LoanResponse.RepaymentDto> getPendingRepayments() {
        if (loanService != null) {
            try {
                return loanService.getPendingRepayments();
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    public String approveRepayment(Long repaymentId) {
        try {
            if (loanService != null && repaymentId != null) {
                Long cashierId = (authBean != null && authBean.getCurrentUser() != null)
                        ? authBean.getCurrentUser().getUserId() : 100L;
                this.selectedLoan = loanService.approveRepayment(repaymentId, cashierId);
                this.message = "Loan repayment #" + repaymentId + " approved successfully!";
                loadLoans();
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    public String rejectRepayment(Long repaymentId, String reason) {
        try {
            if (loanService != null && repaymentId != null) {
                Long cashierId = (authBean != null && authBean.getCurrentUser() != null)
                        ? authBean.getCurrentUser().getUserId() : 100L;
                this.selectedLoan = loanService.rejectRepayment(repaymentId, cashierId, reason);
                this.message = "Loan repayment #" + repaymentId + " rejected.";
                loadLoans();
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }


    public LoanApplicationRequest getApplicationRequest() { return applicationRequest; }
    public void setApplicationRequest(LoanApplicationRequest applicationRequest) { this.applicationRequest = applicationRequest; }
    public LoanApprovalRequest getApprovalRequest() { return approvalRequest; }
    public void setApprovalRequest(LoanApprovalRequest approvalRequest) { this.approvalRequest = approvalRequest; }
    public LoanPaymentRequest getPaymentRequest() { return paymentRequest; }
    public void setPaymentRequest(LoanPaymentRequest paymentRequest) { this.paymentRequest = paymentRequest; }
    public List<LoanResponse> getLoans() { return loans; }
    public String getMessage() { return message; }
    public String getErrorMessage() { return errorMessage; }

    public LoanResponse getSelectedLoan() { return selectedLoan; }
    public void setSelectedLoan(LoanResponse selectedLoan) { this.selectedLoan = selectedLoan; }
    public String getApprovalRemarks() { return approvalRemarks; }
    public void setApprovalRemarks(String approvalRemarks) { this.approvalRemarks = approvalRemarks; }
    public java.math.BigDecimal getRepaymentAmount() { return repaymentAmount; }
    public void setRepaymentAmount(java.math.BigDecimal repaymentAmount) { this.repaymentAmount = repaymentAmount; }
    public String getRepaymentRef() { return repaymentRef; }
    public void setRepaymentRef(String repaymentRef) { this.repaymentRef = repaymentRef; }

    public java.math.BigDecimal getSavingsBalance() { return savingsBalance; }
    public java.math.BigDecimal getMaxLoanAmount() {
        return savingsBalance.multiply(new java.math.BigDecimal("3")).setScale(2, java.math.RoundingMode.HALF_UP);
    }
    public List<org.kimwanyi.sacco.entity.Member> getRegisteredMembers() {
        return registeredMembers;
    }

    public org.kimwanyi.sacco.entity.Member getCurrentMemberInfo() {
        // Prefer session member ID for authenticated customer logins
        Long mId = applicationRequest.getMemberId();
        if ((mId == null || mId <= 0) && authBean != null && authBean.isMember()) {
            mId = authBean.getCurrentMemberId();
        }
        if (mId != null && mId > 0) {
            final Long targetId = mId;
            return org.kimwanyi.sacco.util.TransactionManager.execute(session -> {
                if (session == null) return null;
                return new MemberRepositoryImpl().findById(session, targetId).orElse(null);
            });
        }
        return null;
    }

    public String getLoggedInUsername() {
        if (authBean != null && authBean.getCurrentUser() != null) {
            String fullName = authBean.getCurrentUser().getFullName();
            if (fullName != null && !fullName.isEmpty()) return fullName;
            String uname = authBean.getCurrentUser().getUsername();
            if (uname != null && !uname.isEmpty()) return uname;
        }
        return "Member";
    }
}
