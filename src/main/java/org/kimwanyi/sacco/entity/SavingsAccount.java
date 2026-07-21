package org.kimwanyi.sacco.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.kimwanyi.sacco.enums.AccountStatus;
import org.kimwanyi.sacco.enums.TransactionType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "savings_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_savings_account_number",
                        columnNames = "account_number"
                )
        }
)
public class SavingsAccount extends BaseEntity {

    @NotBlank(message = "Account number is required")
    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @OneToMany(mappedBy = "savingsAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SavingsTransaction> transactions = new ArrayList<>();

    public SavingsAccount() {
    }

    /**
     * Derives the current balance dynamically from all recorded financial transactions.
     * DEPOSIT transactions add to the balance; WITHDRAW transactions subtract from it.
     */
    public BigDecimal getBalance() {
        if (transactions == null || transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal balance = BigDecimal.ZERO;
        for (SavingsTransaction tx : transactions) {
            if (tx.getAmount() != null) {
                if (tx.getType() == TransactionType.DEPOSIT) {
                    balance = balance.add(tx.getAmount());
                } else if (tx.getType() == TransactionType.WITHDRAW) {
                    balance = balance.subtract(tx.getAmount());
                }
            }
        }
        return balance;
    }

    public void addTransaction(SavingsTransaction transaction) {
        transactions.add(transaction);
        transaction.setSavingsAccount(this);
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public List<SavingsTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<SavingsTransaction> transactions) {
        this.transactions = transactions;
    }
}
