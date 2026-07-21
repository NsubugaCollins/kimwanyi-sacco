package org.kimwanyi.sacco.dto.loan;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {
    private Long memberId;
    private BigDecimal principalAmount;
    private Integer termInMonths;
    private String purpose;
}
