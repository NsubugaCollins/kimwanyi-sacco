package org.kimwanyi.sacco.finance;

public final class ReceiptNumberGenerator {

    private ReceiptNumberGenerator(){}

    public static String generate(long sequence){

        return String.format(
                "RCT-%d-%08d",
                java.time.Year.now().getValue(),
                sequence
        );

    }

}
