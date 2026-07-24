package org.kimwanyi.sacco.finance;

import java.math.BigDecimal;

public final class FinancialConstants {

    private FinancialConstants() {
    }

    public static final BigDecimal MINIMUM_SAVINGS_BALANCE =
            new BigDecimal("20000.00");

    public static final BigDecimal SAVINGS_INTEREST_RATE =
            new BigDecimal("0.05");

    public static final BigDecimal LOAN_INTEREST_RATE =
            new BigDecimal("0.10");

    public static final int MAXIMUM_LOAN_MULTIPLIER = 3;

    public static final int PASSWORD_EXPIRY_DAYS = 90;

    public static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
}
