package org.kimwanyi.sacco.dto.loan;

import lombok.Data;
import org.kimwanyi.sacco.enums.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LoanResponse {
    private Long id;
    private Long memberId;
    private String memberName;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer termInMonths;
    private BigDecimal totalInterest;
    private BigDecimal totalAmountPayable;
    private BigDecimal remainingBalance;
    private LoanStatus status;
    private String purpose;
    private Long approvedByUserId;
    private LocalDateTime approvedAt;
    private LocalDateTime disbursedAt;
    private String remarks;
    private List<RepaymentDto> recentRepayments;

    @Data
    public static class RepaymentDto {
        private Long id;
        private BigDecimal amountPaid;
        private LocalDateTime paymentDate;
        private String referenceNumber;
        private String remarks;
    }
}
