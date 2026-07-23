package org.kimwanyi.sacco.service;

import org.kimwanyi.sacco.dto.loan.LoanApplicationRequest;
import org.kimwanyi.sacco.dto.loan.LoanApprovalRequest;
import org.kimwanyi.sacco.dto.loan.LoanPaymentRequest;
import org.kimwanyi.sacco.dto.loan.LoanResponse;
import org.kimwanyi.sacco.enums.LoanStatus;

import java.util.List;

public interface LoanService {
    LoanResponse applyForLoan(LoanApplicationRequest request);
    LoanResponse approveOrRejectLoan(LoanApprovalRequest request);
    LoanResponse disburseLoan(Long loanId, Long officerUserId);
    LoanResponse repayLoan(LoanPaymentRequest request);
    LoanResponse approveRepayment(Long repaymentId, Long cashierUserId);
    LoanResponse rejectRepayment(Long repaymentId, Long cashierUserId, String reason);
    List<LoanResponse.RepaymentDto> getPendingRepayments();
    LoanResponse getLoanById(Long loanId);
    List<LoanResponse> getLoansByMember(Long memberId);
    List<LoanResponse> getLoansByStatus(LoanStatus status);
}
