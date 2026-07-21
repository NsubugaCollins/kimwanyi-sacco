package org.kimwanyi.sacco.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import org.kimwanyi.sacco.entity.User;
import org.kimwanyi.sacco.security.PasswordPolicyValidator;

import java.util.Set;

public class UserValidatorImpl implements UserValidator{
    private final Validator validator;
    private final PasswordPolicyValidator passwordValidator;

    public UserValidatorImpl(Validator validator, PasswordPolicyValidator passwordValidator) {
        this.validator = validator;
        this.passwordValidator = passwordValidator;
    }

    public void validate(User user){
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        if(!violations.isEmpty()){
            StringBuilder builder = new StringBuilder();
            for(ConstraintViolation<User> violation :violations){
                builder.append(violation.getMessage())
                        .append(System.lineSeparator());
            }
            throw new ValidationException(builder.toString());
        }
    }

    public void validatePassword(String username, String password){
        passwordValidator.validate(username,password);
    }
}
