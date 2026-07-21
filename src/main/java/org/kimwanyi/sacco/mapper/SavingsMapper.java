package org.kimwanyi.sacco.mapper;

import org.kimwanyi.sacco.dto.savings.SavingsResponse;
import org.kimwanyi.sacco.entity.SavingsAccount;

import java.util.List;
import java.util.stream.Collectors;

public class SavingsMapper {

    public static SavingsResponse toResponse(SavingsAccount account) {
        if (account == null) {
            return null;
        }

        SavingsResponse response = new SavingsResponse();
        response.setId(account.getId());
        response.setAccountNumber(account.getAccountNumber());
        response.setBalance(account.getBalance());
        response.setStatus(account.getStatus());

        if (account.getMember() != null) {
            response.setMemberId(account.getMember().getId());
            String firstName = account.getMember().getFirstName() != null ? account.getMember().getFirstName() : "";
            String lastName = account.getMember().getLastName() != null ? account.getMember().getLastName() : "";
            response.setMemberName((firstName + " " + lastName).trim());
        }

        if (account.getTransactions() != null) {
            List<SavingsResponse.TransactionDto> transactionDtos = account.getTransactions().stream().map(tx -> {
                SavingsResponse.TransactionDto dto = new SavingsResponse.TransactionDto();
                dto.setId(tx.getId());
                dto.setType(tx.getType());
                dto.setAmount(tx.getAmount());
                dto.setDescription(tx.getDescription());
                dto.setReferenceNumber(tx.getReferenceNumber());
                dto.setCreatedAt(tx.getCreatedAt());
                return dto;
            }).collect(Collectors.toList());
            response.setRecentTransactions(transactionDtos);
        }

        return response;
    }
}
