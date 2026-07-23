package org.kimwanyi.sacco.service;

import org.kimwanyi.sacco.dto.savings.DepositRequest;
import org.kimwanyi.sacco.dto.savings.SavingsResponse;
import org.kimwanyi.sacco.dto.savings.WithdrawalRequest;

import java.math.BigDecimal;
import java.util.List;

public interface SavingsService {
    SavingsResponse createAccount(Long memberId, String accountNumber);
    SavingsResponse deposit(DepositRequest request);
    SavingsResponse withdraw(WithdrawalRequest request);
    BigDecimal getBalance(Long accountId);
    SavingsResponse getAccountDetails(Long accountId);
    SavingsResponse getAccountByAccountNumber(String accountNumber);
    SavingsResponse approveDeposit(Long transactionId, Long cashierUserId);
    SavingsResponse rejectDeposit(Long transactionId, Long cashierUserId, String reason);
    List<SavingsResponse.TransactionDto> getPendingDeposits();
}
