package org.kimwanyi.sacco.dto.member;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateMemberRequest {

    private String firstName;

    private String lastName;

    private String nationalId;

    private String phoneNumber;

    private String email;

    private String address;

    private LocalDate dateOfBirth;

}