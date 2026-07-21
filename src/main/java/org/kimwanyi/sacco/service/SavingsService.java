package org.kimwanyi.sacco.service;

import org.kimwanyi.sacco.dto.savings.DepositRequest;
import org.kimwanyi.sacco.dto.savings.SavingsResponse;
import org.kimwanyi.sacco.dto.savings.WithdrawalRequest;

import java.math.BigDecimal;

public interface SavingsService {
    SavingsResponse createAccount(Long memberId, String accountNumber);
    SavingsResponse deposit(DepositRequest request);
    SavingsResponse withdraw(WithdrawalRequest request);
    BigDecimal getBalance(Long accountId);
    SavingsResponse getAccountDetails(Long accountId);
    SavingsResponse getAccountByAccountNumber(String accountNumber);
}
