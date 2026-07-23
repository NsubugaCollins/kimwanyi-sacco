package org.kimwanyi.sacco.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import org.kimwanyi.sacco.enums.ApprovalStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "loan_repayments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_loan_repayment_reference",
                        columnNames = "reference_number"
                )
        }
)
public class LoanRepayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(name = "amount_paid", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "reference_number", nullable = false, length = 50)
    private String referenceNumber;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.APPROVED;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    public LoanRepayment() {
        this.paymentDate = LocalDateTime.now();
    }

    public LoanRepayment(Loan loan, BigDecimal amountPaid, String referenceNumber, String remarks) {
        this(loan, amountPaid, referenceNumber, remarks, ApprovalStatus.APPROVED);
    }

    public LoanRepayment(Loan loan, BigDecimal amountPaid, String referenceNumber, String remarks, ApprovalStatus approvalStatus) {
        this.loan = loan;
        this.amountPaid = amountPaid;
        this.referenceNumber = referenceNumber;
        this.remarks = remarks;
        this.paymentDate = LocalDateTime.now();
        this.approvalStatus = approvalStatus != null ? approvalStatus : ApprovalStatus.APPROVED;
    }
}
