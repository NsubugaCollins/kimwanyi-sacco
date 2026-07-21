package org.kimwanyi.sacco.finance;

public final class AccountNumberGenerator {

    private AccountNumberGenerator(){}

    public static String generateSavingsAccountNumber(long sequence){
        return String.format(
                "SAV-%d-%06d",
                java.time.Year.now().getValue(),
                sequence
        );
    }

    public static String generateLoanAccountNumber(long sequence){
        return String.format(
                "LON-%d-%06d",
                java.time.Year.now().getValue(),
                sequence
        );
    }
}
