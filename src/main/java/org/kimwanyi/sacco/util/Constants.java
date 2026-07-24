package org.kimwanyi.sacco.util;

import java.math.BigDecimal;

public class Constants {
    private Constants(){

    }

    //saving rules
    public static final BigDecimal MINIMUM_SAVINGS_BALANCE = new BigDecimal("20000.00");
    public static final double  SAVING_INTEREST_RATE = 0.1;

    //loan rules
    public static final double LOAN_INTEREST_RATE = 0.2;

    public static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    public static final int ACCOUNT_LOCK_MINUTES = 5;
    public static final int SESSION_TIMEOUT_MINUTES = 15;
}
