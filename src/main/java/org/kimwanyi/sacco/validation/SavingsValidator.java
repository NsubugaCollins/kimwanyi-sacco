package org.kimwanyi.sacco.validation;

import org.hibernate.Session;
import org.kimwanyi.sacco.dto.savings.DepositRequest;
import org.kimwanyi.sacco.dto.savings.WithdrawalRequest;
import org.kimwanyi.sacco.entity.Member;
import org.kimwanyi.sacco.entity.SavingsAccount;
import org.kimwanyi.sacco.enums.AccountStatus;
import org.kimwanyi.sacco.enums.UserStatus;
import org.kimwanyi.sacco.exception.DuplicateRecordException;
import org.kimwanyi.sacco.exception.InsufficientBalanceException;
import org.kimwanyi.sacco.exception.ValidationException;
import org.kimwanyi.sacco.repository.SavingsTransactionRepository;

import java.math.BigDecimal;

public class SavingsValidator {

    private static final BigDecimal MAX_TRANSACTION_LIMIT = new BigDecimal("1000000000.00"); // 1 Billion limit per tx

    private final SavingsTransactionRepository savingsTransactionRepository;

    public SavingsValidator() {
        this.savingsTransactionRepository = null;
    }

    public SavingsValidator(SavingsTransactionRepository savingsTransactionRepository) {
        this.savingsTransactionRepository = savingsTransactionRepository;
    }

    public void validateDeposit(DepositRequest request) {
        validateDeposit(null, request);
    }

    public void validateDeposit(Session session, DepositRequest request) {
        if (request == null) {
            throw new ValidationException("Deposit request cannot be null.");
        }
        validateAccountIdentifier(request.getAccountId(), request.getAccountNumber());
        validateAmount(request.getAmount(), "Deposit");
        validateReferenceNumber(session, request.getReferenceNumber());
    }

    public void validateWithdrawal(WithdrawalRequest request) {
        validateWithdrawal(null, request);
    }

    public void validateWithdrawal(Session session, WithdrawalRequest request) {
        if (request == null) {
            throw new ValidationException("Withdrawal request cannot be null.");
        }
        validateAccountIdentifier(request.getAccountId(), request.getAccountNumber());
        validateAmount(request.getAmount(), "Withdrawal");
        validateReferenceNumber(session, request.getReferenceNumber());
    }

    public void validateAccountActive(SavingsAccount account) {
        if (account == null) {
            throw new ValidationException("Savings account not found.");
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new ValidationException("Savings account is not active. Current status: " + account.getStatus());
        }
        validateMemberActive(account.getMember());
    }

    public void validateMemberActive(Member member) {
        if (member == null) {
            throw new ValidationException("Associated member not found.");
        }
        if (member.getStatus() != UserStatus.ACTIVE) {
            throw new ValidationException("Account owner (Member) is not active. Current status: " + member.getStatus());
        }
    }

    public void validateSufficientBalance(BigDecimal currentBalance, BigDecimal requestedAmount) {
        if (currentBalance == null) {
            currentBalance = BigDecimal.ZERO;
        }
        if (currentBalance.compareTo(requestedAmount) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Insufficient balance. Current balance is %s, requested withdrawal is %s.",
                            currentBalance.toPlainString(), requestedAmount.toPlainString())
            );
        }
    }

    private void validateAccountIdentifier(Long accountId, String accountNumber) {
        if (accountId == null && (accountNumber == null || accountNumber.isBlank())) {
            throw new ValidationException("Account identifier (ID or account number) is required.");
        }
    }

    private void validateAmount(BigDecimal amount, String operationType) {
        if (amount == null) {
            throw new ValidationException(operationType + " amount is required.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(operationType + " amount must be greater than zero.");
        }
        // Bank-level decimal precision guard (max 2 decimal places)
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new ValidationException(operationType + " amount cannot have more than 2 decimal places.");
        }
        // Bank-level single transaction cap
        if (amount.compareTo(MAX_TRANSACTION_LIMIT) > 0) {
            throw new ValidationException(operationType + " amount exceeds maximum single transaction limit of " + MAX_TRANSACTION_LIMIT.toPlainString());
        }
    }

    private void validateReferenceNumber(Session session, String referenceNumber) {
        if (referenceNumber == null || referenceNumber.isBlank()) {
            throw new ValidationException("Transaction reference number is mandatory for audit and idempotency compliance.");
        }
        if (referenceNumber.trim().length() > 50) {
            throw new ValidationException("Transaction reference number cannot exceed 50 characters.");
        }
        // Bank-level idempotency / duplicate reference protection
        if (savingsTransactionRepository != null && savingsTransactionRepository.existsByReferenceNumber(session, referenceNumber.trim())) {
            throw new DuplicateRecordException("Duplicate transaction reference number: " + referenceNumber + ". Transaction rejected to prevent double charge.");
        }
    }
}
