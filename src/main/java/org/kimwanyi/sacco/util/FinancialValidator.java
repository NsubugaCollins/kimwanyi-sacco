package org.kimwanyi.sacco.util;

import org.kimwanyi.sacco.exception.ValidationException;

import java.math.BigDecimal;

public class FinancialValidator {

    public static void validatePositiveAmount(BigDecimal amount, String fieldName) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException(fieldName + " must be greater than zero.");
        }
    }

    public static void validateNonNegativeAmount(BigDecimal amount, String fieldName) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(fieldName + " cannot be negative.");
        }
    }

    public static void validateLoanAmountWithinLimits(BigDecimal requestedPrincipal, BigDecimal savingsBalance) {
        validatePositiveAmount(requestedPrincipal, "Loan principal amount");

        if (requestedPrincipal.compareTo(FinancialConstants.MIN_LOAN_AMOUNT) < 0) {
            throw new ValidationException(String.format("Minimum loan amount is %s %s.",
                    FinancialConstants.DEFAULT_CURRENCY.getSymbol(), FinancialConstants.MIN_LOAN_AMOUNT.toPlainString()));
        }

        if (requestedPrincipal.compareTo(FinancialConstants.MAX_LOAN_AMOUNT) > 0) {
            throw new ValidationException(String.format("Maximum loan limit exceeded (%s %s).",
                    FinancialConstants.DEFAULT_CURRENCY.getSymbol(), FinancialConstants.MAX_LOAN_AMOUNT.toPlainString()));
        }

        BigDecimal maxAllowed = (savingsBalance != null ? savingsBalance : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(FinancialConstants.MAX_LOAN_SAVINGS_MULTIPLIER));

        if (requestedPrincipal.compareTo(maxAllowed) > 0) {
            throw new ValidationException(String.format(
                    "Requested loan amount (%s) exceeds maximum limit of 3x savings balance (%s).",
                    requestedPrincipal.toPlainString(), maxAllowed.toPlainString()
            ));
        }
    }
}
