package org.kimwanyi.sacco.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class TransactionNumberGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String generateTransactionReference() {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "TXN-" + timestamp + "-" + randomSuffix;
    }
}
