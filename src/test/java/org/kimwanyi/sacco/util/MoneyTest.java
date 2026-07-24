package org.kimwanyi.sacco.util;

import org.junit.jupiter.api.Test;
import org.kimwanyi.sacco.enums.Currency;
import org.kimwanyi.sacco.exception.ValidationException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class MoneyTest {

    @Test
    void testMoneyAdditionAndSubtraction() {
        Money m1 = new Money(new BigDecimal("10000.00"), Currency.UGX);
        Money m2 = new Money(new BigDecimal("5000.00"), Currency.UGX);

        Money sum = m1.add(m2);
        assertEquals(new BigDecimal("15000.00"), sum.getAmount());

        Money diff = m1.subtract(m2);
        assertEquals(new BigDecimal("5000.00"), diff.getAmount());
    }

    @Test
    void testCurrencyMismatch_ThrowsException() {
        Money ugx = new Money(new BigDecimal("1000.00"), Currency.UGX);
        Money usd = new Money(new BigDecimal("10.00"), Currency.USD);

        assertThrows(ValidationException.class, () -> ugx.add(usd));
    }

    @Test
    void testGenerators() {
        String accountNumber = AccountNumberGenerator.generateSavingsAccountNumber();
        assertTrue(accountNumber.startsWith("SA-"));

        String txnRef = TransactionNumberGenerator.generateTransactionReference();
        assertTrue(txnRef.startsWith("TXN-"));

        String receiptNo = ReceiptNumberGenerator.generateReceiptNumber();
        assertTrue(receiptNo.startsWith("RCP-"));
    }
}
