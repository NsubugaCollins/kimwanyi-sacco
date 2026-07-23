package org.kimwanyi.sacco.dto.savings;

import lombok.Data;
import org.kimwanyi.sacco.enums.AccountStatus;
import org.kimwanyi.sacco.enums.ApprovalStatus;
import org.kimwanyi.sacco.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SavingsResponse {
    private Long id;
    private String accountNumber;
    private Long memberId;
    private String memberName;
    private AccountStatus status;
    private BigDecimal balance;
    private List<TransactionDto> recentTransactions;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public List<TransactionDto> getRecentTransactions() { return recentTransactions; }
    public void setRecentTransactions(List<TransactionDto> recentTransactions) { this.recentTransactions = recentTransactions; }

    @Data
    public static class TransactionDto {
        private Long id;
        private TransactionType type;
        private BigDecimal amount;
        private String description;
        private String referenceNumber;
        private ApprovalStatus approvalStatus = ApprovalStatus.APPROVED;
        private String rejectionReason;
        private LocalDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public TransactionType getType() { return type; }
        public void setType(TransactionType type) { this.type = type; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getReferenceNumber() { return referenceNumber; }
        public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
        public ApprovalStatus getApprovalStatus() { return approvalStatus; }
        public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }
        public String getRejectionReason() { return rejectionReason; }
        public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
