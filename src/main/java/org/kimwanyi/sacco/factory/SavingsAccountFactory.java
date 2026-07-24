package org.kimwanyi.sacco.factory;

import org.kimwanyi.sacco.entity.Member;
import org.kimwanyi.sacco.entity.SavingsAccount;
import org.kimwanyi.sacco.entity.SavingsTransaction;
import org.kimwanyi.sacco.enums.TransactionType;

import java.math.BigDecimal;
import java.util.UUID;

public class SavingsAccountFactory {

    public static SavingsAccount createAccount(Member member) {
        String accountNumber = "SA-" + System.currentTimeMillis();
        SavingsAccount account = new SavingsAccount();
        account.setMember(member);
        account.setAccountNumber(accountNumber);
        return account;
    }

    public static SavingsTransaction createTransaction(
            SavingsAccount account,
            TransactionType type,
            BigDecimal amount,
            String description,
            String referenceNumber
    ) {
        String ref = (referenceNumber != null && !referenceNumber.isBlank())
                ? referenceNumber.trim()
                : "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return new SavingsTransaction(account, type, amount, description, ref);
    }
}
