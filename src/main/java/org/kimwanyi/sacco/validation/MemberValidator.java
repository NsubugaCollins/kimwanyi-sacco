package org.kimwanyi.sacco.validation;

import org.kimwanyi.sacco.entity.Member;
import org.kimwanyi.sacco.exception.ValidationException;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

public class MemberValidator {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(?:\\+256|256|0)7[0-9]{8}$");
    private static final Pattern NATIONAL_ID_PATTERN = Pattern.compile("^[A-Z0-9]{8,20}$");

    public void validate(Member member){
        validateNames(member);
        validateNationalId(member.getNationalId());
        validatePhone(member.getPhoneNumber());
        validateAge(member.getDateOfBirth());
    }

    private void validateNames(Member member){
        if(member.getFirstName() == null || member.getFirstName().trim().isEmpty()){
            throw new ValidationException("First name is required");
        }
        if(member.getLastName() == null || member.getLastName().trim().isEmpty()){
            throw new ValidationException("Last name is required");
        }
    }

    private void validateNationalId(String nationalId){
        if(nationalId == null){
            throw new ValidationException("National ID is required");
        }
        if(!NATIONAL_ID_PATTERN.matcher(nationalId).matches()){
            throw new ValidationException("Invalid National ID format");
        }
    }

    private void validatePhone(String phone){
        if(phone == null){
            return;
        }
        if(!PHONE_PATTERN.matcher(phone).matches()){
            throw new ValidationException("Invalid phone number");
        }
    }

    private void validateAge(LocalDate dob){
        if(dob == null){
            throw new ValidationException("Date of birth is required");
        }
        int age = Period.between(dob, LocalDate.now()).getYears();
        if(age < 18){
            throw new ValidationException("Member must be at least 18 years old");
        }
    }
}
