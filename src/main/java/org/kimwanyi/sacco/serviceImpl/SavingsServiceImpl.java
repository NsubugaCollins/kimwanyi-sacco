package org.kimwanyi.sacco.serviceImpl;

import org.hibernate.Session;
import org.kimwanyi.sacco.dto.savings.DepositRequest;
import org.kimwanyi.sacco.dto.savings.SavingsResponse;
import org.kimwanyi.sacco.dto.savings.WithdrawalRequest;
import org.kimwanyi.sacco.entity.Member;
import org.kimwanyi.sacco.entity.SavingsAccount;
import org.kimwanyi.sacco.entity.SavingsTransaction;
import org.kimwanyi.sacco.enums.AccountStatus;
import org.kimwanyi.sacco.enums.TransactionType;
import org.kimwanyi.sacco.exception.ValidationException;
import org.kimwanyi.sacco.repository.MemberRepository;
import org.kimwanyi.sacco.repository.SavingsAccountRepository;
import org.kimwanyi.sacco.repository.SavingsTransactionRepository;
import org.kimwanyi.sacco.service.SavingsService;
import org.kimwanyi.sacco.util.TransactionManager;
import org.kimwanyi.sacco.validation.SavingsValidator;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class SavingsServiceImpl implements SavingsService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsTransactionRepository savingsTransactionRepository;
    private final MemberRepository memberRepository;
    private final SavingsValidator savingsValidator;

    public SavingsServiceImpl(
            SavingsAccountRepository savingsAccountRepository,
            SavingsTransactionRepository savingsTransactionRepository,
            MemberRepository memberRepository,
            SavingsValidator savingsValidator
    ) {
        this.savingsAccountRepository = savingsAccountRepository;
        this.savingsTransactionRepository = savingsTransactionRepository;
        this.memberRepository = memberRepository;
        this.savingsValidator = savingsValidator;
    }

    @Override
    public SavingsResponse createAccount(Long memberId, String accountNumber) {
        if (memberId == null) {
            throw new ValidationException("Member ID is required to create a savings account.");
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new ValidationException("Account number is required.");
        }

        return TransactionManager.execute(session -> {
            Member member = memberRepository.findById(session, memberId)
                    .orElseThrow(() -> new ValidationException("Member not found with ID: " + memberId));

            if (savingsAccountRepository.existsByAccountNumber(session, accountNumber.trim())) {
                throw new ValidationException("Savings account number already exists: " + accountNumber);
            }

            SavingsAccount account = new SavingsAccount();
            account.setAccountNumber(accountNumber.trim());
            account.setMember(member);
            account.setStatus(AccountStatus.ACTIVE);

            SavingsAccount savedAccount = savingsAccountRepository.save(session, account);
            return toResponse(savedAccount);
        });
    }

    @Override
    public SavingsResponse deposit(DepositRequest request) {
        return TransactionManager.execute(session -> {
            savingsValidator.validateDeposit(session, request);

            SavingsAccount account = findAccountByRequest(session, request.getAccountId(), request.getAccountNumber());
            savingsValidator.validateAccountActive(account);

            SavingsTransaction transaction = new SavingsTransaction(
                    account,
                    TransactionType.DEPOSIT,
                    request.getAmount(),
                    request.getDescription(),
                    request.getReferenceNumber()
            );

            account.addTransaction(transaction);
            savingsTransactionRepository.save(session, transaction);

            return toResponse(account);
        });
    }

    @Override
    public SavingsResponse withdraw(WithdrawalRequest request) {
        return TransactionManager.execute(session -> {
            savingsValidator.validateWithdrawal(session, request);

            SavingsAccount account = findAccountByRequest(session, request.getAccountId(), request.getAccountNumber());
            savingsValidator.validateAccountActive(account);

            BigDecimal currentBalance = account.getBalance();
            savingsValidator.validateSufficientBalance(currentBalance, request.getAmount());

            SavingsTransaction transaction = new SavingsTransaction(
                    account,
                    TransactionType.WITHDRAW,
                    request.getAmount(),
                    request.getDescription(),
                    request.getReferenceNumber()
            );

            account.addTransaction(transaction);
            savingsTransactionRepository.save(session, transaction);

            return toResponse(account);
        });
    }

    @Override
    public BigDecimal getBalance(Long accountId) {
        return TransactionManager.execute(session -> {
            SavingsAccount account = savingsAccountRepository.findById(session, accountId)
                    .orElseThrow(() -> new ValidationException("Savings account not found with ID: " + accountId));
            return account.getBalance();
        });
    }

    @Override
    public SavingsResponse getAccountDetails(Long accountId) {
        return TransactionManager.execute(session -> {
            SavingsAccount account = savingsAccountRepository.findById(session, accountId)
                    .orElseThrow(() -> new ValidationException("Savings account not found with ID: " + accountId));
            return toResponse(account);
        });
    }

    @Override
    public SavingsResponse getAccountByAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new ValidationException("Account number is required.");
        }
        return TransactionManager.execute(session -> {
            SavingsAccount account = savingsAccountRepository.findByAccountNumber(session, accountNumber.trim())
                    .orElseThrow(() -> new ValidationException("Savings account not found: " + accountNumber));
            return toResponse(account);
        });
    }

    private SavingsAccount findAccountByRequest(Session session, Long accountId, String accountNumber) {
        if (accountId != null) {
            return savingsAccountRepository.findById(session, accountId)
                    .orElseThrow(() -> new ValidationException("Savings account not found with ID: " + accountId));
        } else if (accountNumber != null && !accountNumber.isBlank()) {
            return savingsAccountRepository.findByAccountNumber(session, accountNumber.trim())
                    .orElseThrow(() -> new ValidationException("Savings account not found with account number: " + accountNumber));
        }
        throw new ValidationException("Account identifier is required.");
    }

    private SavingsResponse toResponse(SavingsAccount account) {
        SavingsResponse response = new SavingsResponse();
        response.setId(account.getId());
        response.setAccountNumber(account.getAccountNumber());
        if (account.getMember() != null) {
            response.setMemberId(account.getMember().getId());
            String firstName = account.getMember().getFirstName() != null ? account.getMember().getFirstName() : "";
            String lastName = account.getMember().getLastName() != null ? account.getMember().getLastName() : "";
            response.setMemberName((firstName + " " + lastName).trim());
        }
        response.setStatus(account.getStatus());
        response.setBalance(account.getBalance());

        if (account.getTransactions() != null) {
            List<SavingsResponse.TransactionDto> txDtos = account.getTransactions().stream().map(tx -> {
                SavingsResponse.TransactionDto dto = new SavingsResponse.TransactionDto();
                dto.setId(tx.getId());
                dto.setType(tx.getType());
                dto.setAmount(tx.getAmount());
                dto.setDescription(tx.getDescription());
                dto.setReferenceNumber(tx.getReferenceNumber());
                dto.setCreatedAt(tx.getCreatedAt());
                return dto;
            }).collect(Collectors.toList());
            response.setRecentTransactions(txDtos);
        }
        return response;
    }
}
