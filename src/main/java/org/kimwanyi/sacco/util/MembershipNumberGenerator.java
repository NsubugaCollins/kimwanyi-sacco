package org.kimwanyi.sacco.util;

import java.time.Year;

public class MembershipNumberGenerator {
    private MembershipNumberGenerator(){

    }

    public static String generate(long sequence){
        return String.format("KS-%d-%06d", Year.now().getValue(), sequence);
    }
}
