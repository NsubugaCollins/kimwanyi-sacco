package org.kimwanyi.sacco.util;

import org.kimwanyi.sacco.enums.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FinancialConstants {

    public static final Currency DEFAULT_CURRENCY = Currency.UGX;
    public static final int DECIMAL_SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    public static final BigDecimal DEFAULT_MONTHLY_INTEREST_RATE = new BigDecimal("0.05"); // 5% per month
    public static final int MAX_LOAN_SAVINGS_MULTIPLIER = 3;
    public static final BigDecimal MIN_LOAN_AMOUNT = new BigDecimal("10000.00");
    public static final BigDecimal MAX_LOAN_AMOUNT = new BigDecimal("500000000.00");

    private FinancialConstants() {}
}
