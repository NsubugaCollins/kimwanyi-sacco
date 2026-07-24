package org.kimwanyi.sacco.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.kimwanyi.sacco.enums.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "loans")
public class Loan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate = new BigDecimal("0.05"); // 5% per month

    @Column(name = "term_in_months", nullable = false)
    private Integer termInMonths;

    @Column(name = "total_interest", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalInterest;

    @Column(name = "total_amount_payable", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmountPayable;

    @Column(name = "remaining_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.PENDING;

    @Column(name = "purpose", length = 500)
    private String purpose;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "disbursed_at")
    private LocalDateTime disbursedAt;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LoanRepayment> repayments = new ArrayList<>();

    public Loan() {
    }

    public void addRepayment(LoanRepayment repayment) {
        repayments.add(repayment);
        repayment.setLoan(this);
    }
}
