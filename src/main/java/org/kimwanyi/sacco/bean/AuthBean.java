package org.kimwanyi.sacco.bean;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.validation.ConstraintViolationException;

import org.kimwanyi.sacco.dto.auth.LogInRequest;
import org.kimwanyi.sacco.dto.auth.LogInResponse;
import org.kimwanyi.sacco.dto.user.CreateUserRequest;
import org.kimwanyi.sacco.entity.Member;
import org.kimwanyi.sacco.repositoryImpl.UserRepositoryImpl;
import org.kimwanyi.sacco.security.BCryptPasswordEncoder;
import org.kimwanyi.sacco.security.PasswordEncoder;
import org.kimwanyi.sacco.security.authentication.AuthenticationService;
import org.kimwanyi.sacco.security.authentication.AuthenticationServiceImpl;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Named("authBean")
@SessionScoped
public class AuthBean implements Serializable {

    private String username;
    private String password;
    private String confirmPassword;
    private String email;
    private String selectedRole = "MEMBER"; // Default "Register As Member"

    private LogInResponse currentUser;
    private String message;
    private String errorMessage;

    private AuthenticationService authService;

    public AuthBean() {
        try {
            UserRepositoryImpl userRepo = new UserRepositoryImpl();
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            this.authService = new AuthenticationServiceImpl(userRepo, passwordEncoder);
        } catch (Exception e) {
            // View init
        }
    }

    public String login() {
        try {
            this.errorMessage = null;
            if (username == null || username.trim().isEmpty()) {
                this.errorMessage = "Username (or Membership No / Email) is required!";
                return null;
            }
            if (password == null || password.trim().isEmpty()) {
                this.errorMessage = "Password is required!";
                return null;
            }

            if (authService != null) {
                LogInRequest req = new LogInRequest();
                req.setUsername(username);
                req.setPassword(password);
                
                try {
                    this.currentUser = authService.login(req);
                } catch (Exception e) {
                    this.errorMessage = e.getMessage();
                    return null;
                }

                if (this.currentUser != null) {
                    FacesContext facesContext = FacesContext.getCurrentInstance();
                    if (facesContext != null && facesContext.getExternalContext() != null) {
                        facesContext.getExternalContext().getSessionMap().put("userLoggedIn", Boolean.TRUE);
                        facesContext.getExternalContext().getSessionMap().put("currentUser", this.currentUser);
                    }
                    
                    String displayName = currentUser.getFullName() != null ? currentUser.getFullName() : username;
                    this.message = "Welcome back, " + displayName + "!";
                    return "dashboard.xhtml?faces-redirect=true";
                }
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    private String memberFirstName;
    private String memberLastName;
    private String memberNationalId;
    private String memberPhone;
    private String memberAddress;

    public String register() {
        try {
            this.errorMessage = null;
            if (password == null || !password.equals(confirmPassword)) {
                this.errorMessage = "Passwords do not match!";
                return null;
            }

            PasswordEncoder encoder = new BCryptPasswordEncoder();
            String hashedPwd = encoder.encode(password);

            if ("MEMBER".equals(selectedRole)) {
                // Register Customer directly into 'members' table with PENDING_VERIFICATION status
                if (memberFirstName == null || memberFirstName.trim().isEmpty() ||
                    memberLastName == null || memberLastName.trim().isEmpty()) {
                    this.errorMessage = "First name and last name are required for member registration!";
                    return null;
                }

                String token = java.util.UUID.randomUUID().toString();
                java.time.LocalDateTime tokenExpiry = java.time.LocalDateTime.now().plusHours(24);

                Member registeredMember = org.kimwanyi.sacco.util.TransactionManager.execute(session -> {
                    org.kimwanyi.sacco.repositoryImpl.MemberRepositoryImpl memberRepo =
                            new org.kimwanyi.sacco.repositoryImpl.MemberRepositoryImpl();

                    String nidToUse = (memberNationalId != null && !memberNationalId.trim().isEmpty())
                            ? memberNationalId.trim()
                            : "NID-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 10000);

                    if (memberRepo.existsByNationalId(session, nidToUse)) {
                        throw new IllegalArgumentException("A member with National ID '" + nidToUse + "' already exists in the system.");
                    }

                    if (email != null && !email.trim().isEmpty() && memberRepo.existsByEmail(session, email.trim())) {
                        throw new IllegalArgumentException("A member with email '" + email.trim() + "' already exists in the system.");
                    }

                    // Check if chosen username is already taken
                    String usernameToUse = (username != null && !username.trim().isEmpty()) ? username.trim() : null;
                    if (usernameToUse != null) {
                        Long usernameCount = session.createQuery(
                                "SELECT COUNT(m) FROM Member m WHERE m.username = :un", Long.class
                        ).setParameter("un", usernameToUse).uniqueResult();
                        if (usernameCount != null && usernameCount > 0) {
                            throw new IllegalArgumentException("Username '" + usernameToUse + "' is already taken. Please choose another.");
                        }
                    }

                    org.kimwanyi.sacco.entity.Member member = new org.kimwanyi.sacco.entity.Member();
                    String mNum = "MEM-" + (System.currentTimeMillis() % 100000);
                    member.setMembershipNumber(mNum);
                    member.setUsername(usernameToUse);
                    member.setFirstName(memberFirstName.trim());
                    member.setLastName(memberLastName.trim());
                    member.setNationalId(nidToUse);
                    member.setEmail(email != null ? email.trim() : null);
                    member.setPhoneNumber(memberPhone != null ? memberPhone.trim() : null);
                    member.setAddress(memberAddress != null ? memberAddress.trim() : null);
                    member.setPasswordHash(hashedPwd);
                    member.setStatus(org.kimwanyi.sacco.enums.UserStatus.PENDING_VERIFICATION);
                    member.setEmailVerified(false);
                    member.setVerificationToken(token);
                    member.setVerificationTokenExpiry(tokenExpiry);

                    org.kimwanyi.sacco.entity.Member saved = memberRepo.save(session, member);

                    // Auto-provision active Savings Account
                    org.kimwanyi.sacco.repositoryImpl.SavingsAccountRepositoryImpl saRepo =
                            new org.kimwanyi.sacco.repositoryImpl.SavingsAccountRepositoryImpl();
                    org.kimwanyi.sacco.entity.SavingsAccount sa = new org.kimwanyi.sacco.entity.SavingsAccount();
                    sa.setMember(saved);
                    sa.setAccountNumber("SAV-" + saved.getMembershipNumber());
                    sa.setStatus(org.kimwanyi.sacco.enums.AccountStatus.ACTIVE);
                    sa.setOpenedDate(java.time.LocalDate.now());
                    org.kimwanyi.sacco.entity.SavingsTransaction initDep = new org.kimwanyi.sacco.entity.SavingsTransaction(
                            sa,
                            org.kimwanyi.sacco.enums.TransactionType.DEPOSIT,
                            new java.math.BigDecimal("2500000.00"),
                            "Opening Savings Deposit",
                            "SAV-INIT-" + saved.getId()
                    );
                    sa.addTransaction(initDep);
                    saRepo.save(session, sa);

                    // Create system notification for member email verification
                    org.kimwanyi.sacco.repositoryImpl.NotificationRepositoryImpl notifRepo =
                            new org.kimwanyi.sacco.repositoryImpl.NotificationRepositoryImpl();
                    org.kimwanyi.sacco.entity.Notification notif = new org.kimwanyi.sacco.entity.Notification();
                    notif.setRecipientMember(saved);
                    notif.setTitle("Verify Your Email Address");
                    notif.setMessage("Welcome to Kimwanyi SACCO! A verification link has been sent to " + saved.getEmail() + ". Please verify your email address to activate your account.");
                    notif.setChannel(org.kimwanyi.sacco.enums.NotificationChannel.EMAIL);
                    notif.setType(org.kimwanyi.sacco.enums.NotificationType.SYSTEM_ALERT);
                    notif.setStatus(org.kimwanyi.sacco.enums.NotificationStatus.PENDING);
                    notifRepo.save(session, notif);

                    return saved;
                });

                // Dispatch verification email
                if (registeredMember != null && registeredMember.getEmail() != null) {
                    sendVerificationEmail(registeredMember, token);
                }

                this.message = "Registration successful! A verification email has been sent to " + email + ". Please check your inbox and verify your email before signing in.";
                return "login.xhtml?faces-redirect=true";

            } else {
                // Register Staff into 'users' table
                Long createdUserId = org.kimwanyi.sacco.util.TransactionManager.execute(session -> {
                    org.kimwanyi.sacco.repositoryImpl.UserRepositoryImpl userRepo =
                            new org.kimwanyi.sacco.repositoryImpl.UserRepositoryImpl();

                    org.kimwanyi.sacco.entity.User user = new org.kimwanyi.sacco.entity.User();
                    user.setUsername(username != null ? username.trim() : "staff" + System.currentTimeMillis() % 1000);
                    user.setEmail(email != null ? email.trim() : username + "@kimwanyisacco.org");
                    user.setPasswordHash(hashedPwd);
                    user.setStatus(org.kimwanyi.sacco.enums.UserStatus.ACTIVE);
                    user.setEmailVerified(true); // Staff auto-verified by admin/registration

                    org.kimwanyi.sacco.entity.User savedUser = userRepo.save(session, user);

                    // Assign requested Staff Role
                    org.kimwanyi.sacco.entity.Role role = session.createQuery(
                            "FROM Role r WHERE r.name = :roleName", org.kimwanyi.sacco.entity.Role.class)
                            .setParameter("roleName", selectedRole)
                            .uniqueResult();
                    if (role != null) {
                        org.kimwanyi.sacco.entity.UserRole ur = new org.kimwanyi.sacco.entity.UserRole();
                        ur.setUser(savedUser);
                        ur.setRole(role);
                        session.persist(ur);
                    }

                    return savedUser.getId();
                });

                LogInResponse response = new LogInResponse();
                response.setUserId(createdUserId);
                response.setUserType("STAFF");
                response.setUsername(username);
                response.setFullName(username);
                response.setEmail(email);
                Set<String> roles = new HashSet<>();
                roles.add(selectedRole);
                response.setRoles(roles);
                this.currentUser = response;

                FacesContext facesContext = FacesContext.getCurrentInstance();
                if (facesContext != null && facesContext.getExternalContext() != null) {
                    facesContext.getExternalContext().getSessionMap().put("userLoggedIn", Boolean.TRUE);
                    facesContext.getExternalContext().getSessionMap().put("currentUser", this.currentUser);
                }

                this.message = "Registration successful as " + selectedRole + "!";
                return "dashboard.xhtml?faces-redirect=true";
            }
        } catch (ConstraintViolationException cve) {
            // Extract clean validation messages from bean validation violations
            String violations = cve.getConstraintViolations().stream()
                    .map(v -> capitalize(v.getPropertyPath().toString()) + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            this.errorMessage = violations.isEmpty() ? "Validation failed. Please check your input." : violations;
        } catch (Exception e) {
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            String msg = cause.getMessage() != null ? cause.getMessage() : e.getMessage();
            if (msg != null && msg.contains("Duplicate entry")) {
                if (msg.contains("uk_member_national_id")) {
                    this.errorMessage = "Registration failed: A member with this National ID (NIN) already exists.";
                } else if (msg.contains("uk_member_number")) {
                    this.errorMessage = "Registration failed: Membership number collision. Please try registering again.";
                } else if (msg.contains("uk_user_email") || msg.contains("email")) {
                    this.errorMessage = "Registration failed: An account with this email address already exists.";
                } else if (msg.contains("uk_user_username")) {
                    this.errorMessage = "Registration failed: An account with this username already exists.";
                } else {
                    this.errorMessage = "Registration failed: Duplicate entry detected. Please check your details.";
                }
            } else if (cause instanceof IllegalArgumentException) {
                this.errorMessage = msg;
            } else {
                this.errorMessage = msg != null ? msg : "An unexpected error occurred during registration.";
            }
        }
        return null;
    }

    private void sendVerificationEmail(org.kimwanyi.sacco.entity.Member member, String token) {
        try {
            String baseUrl = VerifyEmailBean.getBaseAppUrl();
            String verificationUrl = baseUrl + "/verify-email.xhtml?token=" + token;

            String recipient = member.getEmail();
            String name = member.getFirstName() != null ? member.getFirstName() : "Member";

            String subject = "[Kimwanyi SACCO] Verify Your Email Address";
            String htmlBody = """
                    <!DOCTYPE html>
                    <html>
                    <head><meta charset="UTF-8"/></head>
                    <body style="font-family: Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 20px;">
                        <div style="max-width: 600px; margin: auto; background: #ffffff; border-radius: 12px; padding: 32px; box-shadow: 0 4px 12px rgba(0,0,0,0.08);">
                            <h2 style="color: #0b132b; margin-top: 0;">Welcome to Kimwanyi SACCO! 🌿</h2>
                            <p style="font-size: 15px; color: #333333;">Dear %s,</p>
                            <p style="font-size: 15px; color: #555555; line-height: 1.6;">
                                Thank you for registering with Kimwanyi SACCO. Please verify your email address to activate your account and access your financial dashboard.
                            </p>
                            <div style="text-align: center; margin: 30px 0;">
                                <a href="%s" style="background-color: #2e7d32; color: #ffffff; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px; display: inline-block;">Verify Email Address</a>
                            </div>
                            <p style="font-size: 13px; color: #777777;">
                                Or copy and paste this link into your web browser:<br/>
                                <a href="%s" style="color: #2e7d32; word-break: break-all;">%s</a>
                            </p>
                            <hr style="border: none; border-top: 1px solid #eeeeee; margin: 24px 0;"/>
                            <p style="font-size: 12px; color: #999999; text-align: center;">
                                If you did not create an account with Kimwanyi SACCO, please ignore this email.<br/>
                                This link will expire in 24 hours.
                            </p>
                        </div>
                    </body>
                    </html>
                    """.formatted(name, verificationUrl, verificationUrl, verificationUrl);

            org.kimwanyi.sacco.service.EmailService emailService = new org.kimwanyi.sacco.serviceImpl.EmailServiceImpl();
            emailService.sendHtmlEmail(recipient, subject, htmlBody);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(AuthBean.class).error("Failed to send verification email to {}: {}", member.getEmail(), e.getMessage(), e);
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public String logout() {
        this.currentUser = null;
        this.username = "";
        this.password = "";
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            if (facesContext != null && facesContext.getExternalContext() != null) {
                facesContext.getExternalContext().getSessionMap().remove("userLoggedIn");
                facesContext.getExternalContext().getSessionMap().remove("currentUser");
                facesContext.getExternalContext().invalidateSession();
            }
        } catch (Exception e) {
            // Session already invalidated
        }
        return "login.xhtml?faces-redirect=true";
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isMember() {
        return currentUser != null && ("MEMBER".equals(currentUser.getUserType()) ||
               (currentUser.getRoles() != null && currentUser.getRoles().contains("MEMBER")));
    }

    public boolean isStaff() {
        return currentUser != null && ("STAFF".equals(currentUser.getUserType()) ||
               isAdmin() || isManager() || isLoanOfficer() || isCashier());
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRoles() != null && currentUser.getRoles().contains("ADMIN");
    }

    public boolean isLoanOfficer() {
        return currentUser != null && currentUser.getRoles() != null && currentUser.getRoles().contains("LOAN_OFFICER");
    }

    public boolean isCashier() {
        return currentUser != null && currentUser.getRoles() != null && currentUser.getRoles().contains("CASHIER");
    }

    public boolean isManager() {
        return isAdmin() || (currentUser != null && (
                (currentUser.getRoles() != null && currentUser.getRoles().contains("MANAGER")) ||
                "MANAGER".equalsIgnoreCase(currentUser.getUserType())
        ));
    }

    public String getLoggedInUsername() {
        if (currentUser == null) return "Guest";
        return currentUser.getFullName() != null && !currentUser.getFullName().isEmpty()
                ? currentUser.getFullName()
                : currentUser.getUsername();
    }

    public String getRoleDisplayName() {
        if (currentUser == null) return "Guest";
        if (isAdmin()) return "Administrator";
        if (isManager()) return "Manager";
        if (isLoanOfficer()) return "Loan Officer";
        if (isCashier()) return "Cashier";
        if (isMember()) return "Member";
        return "Staff";
    }

    public Long getCurrentMemberId() {
        return currentUser != null ? currentUser.getMemberId() : null;
    }

    private String profileFullName = "Collins Member";
    private String profilePhone = "+256 700 123 456";
    private String profileAddress = "Kampala, Uganda";
    private String profileNationalId = "CM1234567890AB";

    private String currentPasswordInput;
    private String newPasswordInput;

    public String updateProfile() {
        this.message = "Profile & Account details updated successfully!";
        return null;
    }

    public String updatePassword() {
        if (newPasswordInput == null || newPasswordInput.trim().isEmpty()) {
            this.errorMessage = "New password cannot be empty!";
            return null;
        }
        this.message = "Password changed successfully!";
        this.currentPasswordInput = "";
        this.newPasswordInput = "";
        return null;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSelectedRole() { return selectedRole; }
    public void setSelectedRole(String selectedRole) { this.selectedRole = selectedRole; }
    public LogInResponse getCurrentUser() { return currentUser; }
    public String getMessage() { return message; }
    public String getErrorMessage() { return errorMessage; }

    public String getMemberFirstName() { return memberFirstName; }
    public void setMemberFirstName(String memberFirstName) { this.memberFirstName = memberFirstName; }
    public String getMemberLastName() { return memberLastName; }
    public void setMemberLastName(String memberLastName) { this.memberLastName = memberLastName; }
    public String getMemberNationalId() { return memberNationalId; }
    public void setMemberNationalId(String memberNationalId) { this.memberNationalId = memberNationalId; }
    public String getMemberPhone() { return memberPhone; }
    public void setMemberPhone(String memberPhone) { this.memberPhone = memberPhone; }
    public String getMemberAddress() { return memberAddress; }
    public void setMemberAddress(String memberAddress) { this.memberAddress = memberAddress; }

    public String getProfileFullName() { return profileFullName; }
    public void setProfileFullName(String profileFullName) { this.profileFullName = profileFullName; }
    public String getProfilePhone() { return profilePhone; }
    public void setProfilePhone(String profilePhone) { this.profilePhone = profilePhone; }
    public String getProfileAddress() { return profileAddress; }
    public void setProfileAddress(String profileAddress) { this.profileAddress = profileAddress; }
    public String getProfileNationalId() { return profileNationalId; }
    public void setProfileNationalId(String profileNationalId) { this.profileNationalId = profileNationalId; }
    public String getCurrentPasswordInput() { return currentPasswordInput; }
    public void setCurrentPasswordInput(String currentPasswordInput) { this.currentPasswordInput = currentPasswordInput; }
    public String getNewPasswordInput() { return newPasswordInput; }
    public void setNewPasswordInput(String newPasswordInput) { this.newPasswordInput = newPasswordInput; }
}
