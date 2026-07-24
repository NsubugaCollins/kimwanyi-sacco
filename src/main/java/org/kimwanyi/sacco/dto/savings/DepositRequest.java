package org.kimwanyi.sacco.dto.savings;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositRequest {
    private String accountNumber;
    private Long accountId;
    private BigDecimal amount;
    private String description;
    private String referenceNumber;
    private boolean requiresApproval = false;
}
