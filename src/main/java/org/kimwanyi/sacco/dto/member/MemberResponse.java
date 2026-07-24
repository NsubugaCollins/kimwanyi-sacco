package org.kimwanyi.sacco.dto.member;

import lombok.Data;
import org.kimwanyi.sacco.enums.UserStatus;

@Data
public class MemberResponse {

    private Long id;

    private String membershipNumber;

    private String firstName;

    private String lastName;

    private String nationalId;

    private String phoneNumber;

    private String email;

    private UserStatus status;

}