package org.kimwanyi.sacco.util;

import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;

public class AccountNumberGenerator {

    private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() % 100000);

    public static String generateSavingsAccountNumber() {
        long seq = SEQUENCE.incrementAndGet();
        return String.format("SA-%d-%05d", Year.now().getValue(), seq % 100000);
    }

    public static String generateLoanAccountNumber() {
        long seq = SEQUENCE.incrementAndGet();
        return String.format("LN-%d-%05d", Year.now().getValue(), seq % 100000);
    }
}
