package org.kimwanyi.sacco.security;

import org.mindrot.jbcrypt.BCrypt;

public class BCryptPasswordEncoder implements PasswordEncoder{
    private static final int COST = 12;

    public String encode(String rawPassword){
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(COST));
    }

    public boolean matches(String rawPassword, String encodedPassword){
        if(rawPassword == null || encodedPassword == null){
            return false;
        }

        return  BCrypt.checkpw(rawPassword, encodedPassword);
    }
}
