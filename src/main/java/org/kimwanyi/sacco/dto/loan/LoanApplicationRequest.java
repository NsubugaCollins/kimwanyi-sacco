package org.kimwanyi.sacco.dto.loan;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {
    private Long memberId;
    private BigDecimal principalAmount;
    private Integer termInMonths;
    private String purpose;

    // Extended fields for the rich application form
    private String loanType    = "PERSONAL";   // PERSONAL, BUSINESS, EMERGENCY, AGRICULTURE
    private String paymentMode = "MPESA";       // MPESA, MTN, AIRTEL, BANK_TRANSFER, CASH
    private String accountNumber;
    private String phoneNumber;
}

