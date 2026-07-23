package org.kimwanyi.sacco.dto.loan;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanPaymentRequest {
    private Long loanId;
    private BigDecimal amount;
    private String referenceNumber;
    private String remarks;
    private boolean requiresApproval = false;
}
