package org.kimwanyi.sacco.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.kimwanyi.sacco.enums.AccountType;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "ledger_accounts")
public class LedgerAccount extends BaseEntity {

    @NotBlank(message = "Account code is required")
    @Column(name = "account_code", nullable = false, unique = true, length = 30)
    private String accountCode;

    @NotBlank(message = "Account name is required")
    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    @NotNull(message = "Account type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(name = "current_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public LedgerAccount() {}

    public LedgerAccount(String accountCode, String accountName, AccountType accountType) {
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.accountType = accountType;
        this.currentBalance = BigDecimal.ZERO;
        this.active = true;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
