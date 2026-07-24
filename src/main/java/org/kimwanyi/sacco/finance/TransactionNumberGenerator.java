package org.kimwanyi.sacco.finance;

public final class TransactionNumberGenerator {

    private TransactionNumberGenerator(){}

    public static String generate(long sequence){

        return String.format(
                "TXN-%d-%08d",
                java.time.Year.now().getValue(),
                sequence
        );

    }

}
