package org.kimwanyi.sacco.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import lombok.Data;
import org.kimwanyi.sacco.enums.UserStatus;


import java.time.LocalDate;


@Data
@Entity
@Table(name = "members",
        uniqueConstraints = {
        @UniqueConstraint(
                        name = "uk_member_number",
                        columnNames = "membership_number"
                        ),
                @UniqueConstraint(
                        name = "uk_member_national_id",
                        columnNames = "national_id"
                        )}
)
public class Member extends BaseEntity {

    @NotBlank(message = "Membership number is required")
    @Column(name = "membership_number", nullable = false, length = 30)
    private String membershipNumber;

    @NotBlank(message = "First name is required")
    @Column(nullable = false, length = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Column(nullable = false, length = 50)
    private String lastName;

    @NotBlank(message = "National ID is required")
    @Column(name = "national_id", nullable = false, length = 50)
    private String nationalId;

    @Column(length = 20)
    private String phoneNumber;

    @Email
    @Column(length = 100)
    private String email;

    @Column(length = 150)
    private String address;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    public Member(){

    }

    public String getMembershipNumber() {
        return membershipNumber;
    }

    public void setMembershipNumber(String membershipNumber) {
        this.membershipNumber = membershipNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public void deactivate(){
        this.status = UserStatus.INACTIVE;
    }
}