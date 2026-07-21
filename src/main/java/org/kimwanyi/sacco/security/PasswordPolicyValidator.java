package org.kimwanyi.sacco.security;

import jakarta.validation.ValidationException;

import java.util.regex.Pattern;

public class PasswordPolicyValidator {
    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/]");

    public void validate(String username, String password){
        if(password == null){
            throw new ValidationException("Password is required.");
        }

        if(password.length()<8){
            throw new ValidationException("Password must contain at least 8 characters.");
        }

        if(password.length()>15){
            throw new ValidationException("Password must not exceed 15 characters");
        }

        if(!UPPER.matcher(password).find()){
            throw new ValidationException("Password must contain an uppercase letter");
        }

        if(!LOWER.matcher(password).find()){
            throw new ValidationException("Password must contain a lowercase letter");
        }

        if (!DIGIT.matcher(password).find()) {
            throw new ValidationException("Password must contain a digit.");
        }

        if (!SPECIAL.matcher(password).find()) {
            throw new ValidationException("Password must contain a special character.");
        }

        if (password.contains(" ")) {
            throw new ValidationException("Password must not contain spaces.");
        }

        if (username != null && password.toLowerCase().contains(username.toLowerCase())) {
            throw new ValidationException(
                    "Password must not contain the username.");
        }
    }

}
