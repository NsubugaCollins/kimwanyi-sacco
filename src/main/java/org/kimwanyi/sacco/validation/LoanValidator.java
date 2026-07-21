package org.kimwanyi.sacco.validation;

import org.hibernate.Session;
import org.kimwanyi.sacco.dto.loan.LoanApplicationRequest;
import org.kimwanyi.sacco.dto.loan.LoanPaymentRequest;
import org.kimwanyi.sacco.entity.Loan;
import org.kimwanyi.sacco.entity.Member;
import org.kimwanyi.sacco.entity.SavingsAccount;
import org.kimwanyi.sacco.entity.User;
import org.kimwanyi.sacco.enums.LoanStatus;
import org.kimwanyi.sacco.enums.UserStatus;
import org.kimwanyi.sacco.exception.AccessDeniedException;
import org.kimwanyi.sacco.exception.DuplicateRecordException;
import org.kimwanyi.sacco.exception.ValidationException;
import org.kimwanyi.sacco.repository.LoanRepaymentRepository;
import org.kimwanyi.sacco.repository.LoanRepository;
import org.kimwanyi.sacco.repository.SavingsAccountRepository;

import java.math.BigDecimal;

public class LoanValidator {

    private static final BigDecimal MAX_SINGLE_LOAN_LIMIT = new BigDecimal("500000000.00"); // 500 Million
    private static final BigDecimal SAVINGS_LOAN_MULTIPLIER = new BigDecimal("3"); // Maximum 3x savings balance

    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final SavingsAccountRepository savingsAccountRepository;

    public LoanValidator(LoanRepository loanRepository, LoanRepaymentRepository loanRepaymentRepository) {
        this(loanRepository, loanRepaymentRepository, null);
    }

    public LoanValidator(
            LoanRepository loanRepository,
            LoanRepaymentRepository loanRepaymentRepository,
            SavingsAccountRepository savingsAccountRepository
    ) {
        this.loanRepository = loanRepository;
        this.loanRepaymentRepository = loanRepaymentRepository;
        this.savingsAccountRepository = savingsAccountRepository;
    }

    public void validateApplication(Session session, LoanApplicationRequest request, Member member) {
        if (request == null) {
            throw new ValidationException("Loan application request cannot be null.");
        }
        if (member == null) {
            throw new ValidationException("Associated member not found.");
        }
        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new ValidationException("Only active members can apply for loans.");
        }
        if (request.getPrincipalAmount() == null || request.getPrincipalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Loan amount must be greater than zero.");
        }
        if (request.getPrincipalAmount().compareTo(MAX_SINGLE_LOAN_LIMIT) > 0) {
            throw new ValidationException("Loan principal exceeds maximum single loan limit of " + MAX_SINGLE_LOAN_LIMIT.toPlainString());
        }

        // Validate Savings Account and 3x Savings Limit
        if (savingsAccountRepository != null) {
            SavingsAccount savingsAccount = savingsAccountRepository.findByMemberId(session, member.getId())
                    .orElseThrow(() -> new ValidationException("Member must have an active savings account to apply for a loan."));

            BigDecimal savingsBalance = savingsAccount.getBalance();
            BigDecimal maxAllowedLoan = savingsBalance.multiply(SAVINGS_LOAN_MULTIPLIER);

            if (request.getPrincipalAmount().compareTo(maxAllowedLoan) > 0) {
                throw new ValidationException(String.format(
                        "Loan amount requested (%s) exceeds maximum allowed limit of 3x savings balance (%s). Current savings: %s.",
                        request.getPrincipalAmount().toPlainString(),
                        maxAllowedLoan.toPlainString(),
                        savingsBalance.toPlainString()
                ));
            }
        }

        if (request.getTermInMonths() == null || request.getTermInMonths() <= 0) {
            throw new ValidationException("Loan term must be at least 1 month.");
        }
        if (request.getTermInMonths() > 120) {
            throw new ValidationException("Loan term cannot exceed 120 months.");
        }
        if (request.getPurpose() == null || request.getPurpose().isBlank()) {
            throw new ValidationException("Loan purpose/description is required.");
        }

        if (loanRepository != null && loanRepository.existsActiveLoanForMember(session, member.getId())) {
            throw new ValidationException("Member already has an active or pending loan application.");
        }
    }

    public void validateApproval(Session session, Loan loan, User approverUser) {
        if (loan == null) {
            throw new ValidationException("Loan application not found.");
        }
        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new ValidationException("Only PENDING loans can be approved or rejected. Current status: " + loan.getStatus());
        }
        if (approverUser == null) {
            throw new ValidationException("Approver user details required.");
        }
        if (approverUser.getStatus() != UserStatus.ACTIVE) {
            throw new ValidationException("Approver user is not active.");
        }

        // RBAC Check 1: Cashier cannot approve loans
        boolean isCashierOnly = approverUser.getUserRoles().stream()
                .filter(ur -> ur.isActive() && ur.getRole() != null)
                .anyMatch(ur -> "CASHIER".equalsIgnoreCase(ur.getRole().getName()));
        boolean isAdminOrOfficer = approverUser.getUserRoles().stream()
                .filter(ur -> ur.isActive() && ur.getRole() != null)
                .anyMatch(ur -> "ADMIN".equalsIgnoreCase(ur.getRole().getName()) || "LOAN_OFFICER".equalsIgnoreCase(ur.getRole().getName()));

        if (isCashierOnly && !isAdminOrOfficer) {
            throw new AccessDeniedException("Cashiers are not authorized to approve or reject loan applications.");
        }

        // RBAC Check 2: Member cannot approve their own loan
        if (loan.getMember() != null) {
            String memberEmail = loan.getMember().getEmail();
            if ((approverUser.getId() != null && approverUser.getId().equals(loan.getMember().getId()))
                    || (memberEmail != null && memberEmail.equalsIgnoreCase(approverUser.getEmail()))
                    || approverUser.getUsername().equalsIgnoreCase(loan.getMember().getMembershipNumber())) {
                throw new ValidationException("A member cannot approve their own loan application.");
            }
        }
    }

    public void validatePayment(Session session, LoanPaymentRequest request, Loan loan) {
        if (request == null) {
            throw new ValidationException("Loan payment request cannot be null.");
        }
        if (loan == null) {
            throw new ValidationException("Loan not found.");
        }
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new ValidationException("Repayment can only be processed for ACTIVE loans. Current status: " + loan.getStatus());
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Repayment amount must be greater than zero.");
        }
        if (request.getAmount().compareTo(loan.getRemainingBalance()) > 0) {
            throw new ValidationException(String.format("Payment amount %s exceeds remaining loan balance of %s.",
                    request.getAmount().toPlainString(), loan.getRemainingBalance().toPlainString()));
        }
        if (request.getReferenceNumber() == null || request.getReferenceNumber().isBlank()) {
            throw new ValidationException("Repayment reference number is mandatory.");
        }

        if (loanRepaymentRepository != null && loanRepaymentRepository.existsByReferenceNumber(session, request.getReferenceNumber().trim())) {
            throw new DuplicateRecordException("Duplicate repayment reference number: " + request.getReferenceNumber() + ". Transaction rejected to prevent double charge.");
        }
    }
}
