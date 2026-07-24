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
        @UniqueConstraint(name = "uk_member_number",    columnNames = "membership_number"),
        @UniqueConstraint(name = "uk_member_national_id", columnNames = "national_id"),
        @UniqueConstraint(name = "uk_member_username",  columnNames = "username"),
        @UniqueConstraint(name = "uk_member_email",     columnNames = "email")
})
public class Member extends BaseEntity {

    @NotBlank(message = "Membership number is required")
    @Column(name = "membership_number", nullable = false, length = 30)
    private String membershipNumber;

    @Column(name = "username", length = 100)
    private String username;

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

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "verification_token", length = 100)
    private String verificationToken;

    @Column(name = "verification_token_expiry")
    private java.time.LocalDateTime verificationTokenExpiry;

    public Member(){

    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getMembershipNumber() {
        return membershipNumber;
    }

    public void setMembershipNumber(String membershipNumber) {
        this.membershipNumber = membershipNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public java.time.LocalDateTime getVerificationTokenExpiry() {
        return verificationTokenExpiry;
    }

    public void setVerificationTokenExpiry(java.time.LocalDateTime verificationTokenExpiry) {
        this.verificationTokenExpiry = verificationTokenExpiry;
    }

    public void deactivate(){
        this.status = UserStatus.INACTIVE;
    }
}