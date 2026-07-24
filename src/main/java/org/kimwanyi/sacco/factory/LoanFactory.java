package org.kimwanyi.sacco.factory;

import org.kimwanyi.sacco.dto.loan.LoanApplicationRequest;
import org.kimwanyi.sacco.entity.Loan;
import org.kimwanyi.sacco.entity.Member;
import org.kimwanyi.sacco.enums.LoanStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class LoanFactory {

    private static final BigDecimal MONTHLY_INTEREST_RATE = new BigDecimal("0.05"); // 5% monthly interest rate

    public static Loan createLoan(Member member, LoanApplicationRequest request) {
        BigDecimal principal = request.getPrincipalAmount();
        int months = request.getTermInMonths();

        // Total Interest = Principal * 5% * termInMonths
        BigDecimal totalInterest = principal
                .multiply(MONTHLY_INTEREST_RATE)
                .multiply(BigDecimal.valueOf(months))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalPayable = principal.add(totalInterest);

        Loan loan = new Loan();
        loan.setMember(member);
        loan.setPrincipalAmount(principal);
        loan.setInterestRate(MONTHLY_INTEREST_RATE);
        loan.setTermInMonths(months);
        loan.setTotalInterest(totalInterest);
        loan.setTotalAmountPayable(totalPayable);
        loan.setRemainingBalance(totalPayable);
        loan.setStatus(LoanStatus.PENDING);
        loan.setPurpose(request.getPurpose() != null ? request.getPurpose().trim() : null);

        return loan;
    }
}
