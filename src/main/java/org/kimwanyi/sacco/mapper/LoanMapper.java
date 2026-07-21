package org.kimwanyi.sacco.mapper;

import org.kimwanyi.sacco.dto.loan.LoanResponse;
import org.kimwanyi.sacco.entity.Loan;

import java.util.List;
import java.util.stream.Collectors;

public class LoanMapper {

    public static LoanResponse toResponse(Loan loan) {
        if (loan == null) {
            return null;
        }

        LoanResponse response = new LoanResponse();
        response.setId(loan.getId());

        if (loan.getMember() != null) {
            response.setMemberId(loan.getMember().getId());
            String firstName = loan.getMember().getFirstName() != null ? loan.getMember().getFirstName() : "";
            String lastName = loan.getMember().getLastName() != null ? loan.getMember().getLastName() : "";
            response.setMemberName((firstName + " " + lastName).trim());
        }

        response.setPrincipalAmount(loan.getPrincipalAmount());
        response.setInterestRate(loan.getInterestRate());
        response.setTermInMonths(loan.getTermInMonths());
        response.setTotalInterest(loan.getTotalInterest());
        response.setTotalAmountPayable(loan.getTotalAmountPayable());
        response.setRemainingBalance(loan.getRemainingBalance());
        response.setStatus(loan.getStatus());
        response.setPurpose(loan.getPurpose());
        response.setApprovedByUserId(loan.getApprovedByUserId());
        response.setApprovedAt(loan.getApprovedAt());
        response.setDisbursedAt(loan.getDisbursedAt());
        response.setRemarks(loan.getRemarks());

        if (loan.getRepayments() != null) {
            List<LoanResponse.RepaymentDto> repaymentDtos = loan.getRepayments().stream().map(r -> {
                LoanResponse.RepaymentDto dto = new LoanResponse.RepaymentDto();
                dto.setId(r.getId());
                dto.setAmountPaid(r.getAmountPaid());
                dto.setPaymentDate(r.getPaymentDate());
                dto.setReferenceNumber(r.getReferenceNumber());
                dto.setRemarks(r.getRemarks());
                return dto;
            }).collect(Collectors.toList());
            response.setRecentRepayments(repaymentDtos);
        }

        return response;
    }
}
