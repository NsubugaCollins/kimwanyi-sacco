package org.kimwanyi.sacco.validation;

import org.kimwanyi.sacco.entity.User;

public interface UserValidator {
    void validate(User user);
    void validatePassword(String username, String password);
}
