package org.kimwanyi.sacco.dto.loan;

import lombok.Data;

@Data
public class LoanApprovalRequest {
    private Long loanId;
    private Long approverUserId;
    private boolean approved;
    private String remarks;
}
