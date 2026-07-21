package org.kimwanyi.sacco.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class InterestCalculator {

    public static BigDecimal calculateSimpleInterest(BigDecimal principal, BigDecimal monthlyRate, int termInMonths) {
        if (principal == null || monthlyRate == null || termInMonths <= 0) {
            return BigDecimal.ZERO.setScale(FinancialConstants.DECIMAL_SCALE, FinancialConstants.ROUNDING_MODE);
        }
        return principal
                .multiply(monthlyRate)
                .multiply(BigDecimal.valueOf(termInMonths))
                .setScale(FinancialConstants.DECIMAL_SCALE, FinancialConstants.ROUNDING_MODE);
    }

    public static BigDecimal calculateTotalPayable(BigDecimal principal, BigDecimal monthlyRate, int termInMonths) {
        BigDecimal interest = calculateSimpleInterest(principal, monthlyRate, termInMonths);
        return principal.add(interest).setScale(FinancialConstants.DECIMAL_SCALE, FinancialConstants.ROUNDING_MODE);
    }

    public static BigDecimal calculateMonthlyInstallment(BigDecimal totalPayable, int termInMonths) {
        if (totalPayable == null || termInMonths <= 0) {
            return BigDecimal.ZERO.setScale(FinancialConstants.DECIMAL_SCALE, FinancialConstants.ROUNDING_MODE);
        }
        return totalPayable.divide(BigDecimal.valueOf(termInMonths), FinancialConstants.DECIMAL_SCALE, FinancialConstants.ROUNDING_MODE);
    }
}
