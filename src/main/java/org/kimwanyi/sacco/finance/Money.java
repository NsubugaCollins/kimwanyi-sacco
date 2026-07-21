package org.kimwanyi.sacco.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    private Money() {
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return zero();
        }

        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static boolean isPositive(BigDecimal amount) {
        return normalize(amount).compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isNegative(BigDecimal amount) {
        return normalize(amount).compareTo(BigDecimal.ZERO) < 0;
    }
}
