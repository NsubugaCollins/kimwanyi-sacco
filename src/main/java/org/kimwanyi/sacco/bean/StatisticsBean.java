package org.kimwanyi.sacco.bean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.kimwanyi.sacco.enums.LoanStatus;
import org.kimwanyi.sacco.enums.TransactionType;
import org.kimwanyi.sacco.enums.UserStatus;
import org.kimwanyi.sacco.repositoryImpl.LoanRepositoryImpl;
import org.kimwanyi.sacco.repositoryImpl.MemberRepositoryImpl;
import org.kimwanyi.sacco.repositoryImpl.SavingsTransactionRepositoryImpl;
import org.kimwanyi.sacco.util.TransactionManager;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Named("statisticsBean")
@RequestScoped
public class StatisticsBean implements Serializable {

    @Inject
    private AuthBean authBean;

    // --- KPI Totals ---
    private long totalMembers;
    private long activeMembers;
    private long pendingMembers;
    private BigDecimal totalSavingsBalance = BigDecimal.ZERO;
    private BigDecimal totalDeposits = BigDecimal.ZERO;
    private BigDecimal totalWithdrawals = BigDecimal.ZERO;
    private long totalDepositCount;
    private long totalWithdrawalCount;
    private long totalTransactionCount;

    // --- Loan KPIs ---
    private long loansPending;
    private long loansApproved;
    private long loansActive;
    private long loansCompleted;
    private long loansDefaulted;
    private long loansRejected;
    private long totalLoans;
    private BigDecimal totalPrincipalDisbursed = BigDecimal.ZERO;
    private BigDecimal principalPending = BigDecimal.ZERO;
    private BigDecimal principalActive = BigDecimal.ZERO;
    private BigDecimal principalCompleted = BigDecimal.ZERO;
    private BigDecimal principalDefaulted = BigDecimal.ZERO;

    // --- Rates ---
    private double approvalRate;
    private double defaultRate;
    private double completionRate;
    private double depositToWithdrawalRatio;

    // --- Chart JSON strings ---
    private String loanStatusChartJson;
    private String loanAmountChartJson;
    private String savingsVolumeChartJson;
    private String monthlyLoanTrendJson;
    private String monthlyDepositTrendJson;
    private String monthLabelsJson;

    private static final int TREND_MONTHS = 6;

    @PostConstruct
    public void init() {
        try {
            TransactionManager.execute(session -> {
                LoanRepositoryImpl loanRepo = new LoanRepositoryImpl();
                SavingsTransactionRepositoryImpl txRepo = new SavingsTransactionRepositoryImpl();
                MemberRepositoryImpl memberRepo = new MemberRepositoryImpl();

                // Member stats
                List<?> allMembers = memberRepo.findAll(session);
                totalMembers = allMembers.size();
                activeMembers = allMembers.stream()
                        .filter(m -> {
                            try {
                                org.kimwanyi.sacco.entity.Member mem = (org.kimwanyi.sacco.entity.Member) m;
                                return UserStatus.ACTIVE.equals(mem.getStatus());
                            } catch (Exception e) { return false; }
                        }).count();
                pendingMembers = totalMembers - activeMembers;

                // Savings stats
                totalSavingsBalance  = txRepo.sumAllSavingsBalance(session);
                totalDeposits        = txRepo.sumByType(session, TransactionType.DEPOSIT);
                totalWithdrawals     = txRepo.sumByType(session, TransactionType.WITHDRAW);
                totalDepositCount    = txRepo.countByType(session, TransactionType.DEPOSIT);
                totalWithdrawalCount = txRepo.countByType(session, TransactionType.WITHDRAW);
                totalTransactionCount = txRepo.countAll(session);

                if (totalWithdrawals.compareTo(BigDecimal.ZERO) > 0) {
                    depositToWithdrawalRatio = totalDeposits.divide(totalWithdrawals, 2, RoundingMode.HALF_UP).doubleValue();
                } else {
                    depositToWithdrawalRatio = totalDeposits.compareTo(BigDecimal.ZERO) > 0 ? 99.0 : 1.0;
                }

                // Loan stats
                loansPending   = loanRepo.countByStatus(session, LoanStatus.PENDING);
                loansApproved  = loanRepo.countByStatus(session, LoanStatus.APPROVED);
                loansActive    = loanRepo.countByStatus(session, LoanStatus.ACTIVE);
                loansCompleted = loanRepo.countByStatus(session, LoanStatus.COMPLETED);
                loansDefaulted = loanRepo.countByStatus(session, LoanStatus.DEFAULTED);
                loansRejected  = loanRepo.countByStatus(session, LoanStatus.REJECTED);
                totalLoans     = loanRepo.countAll(session);

                principalPending   = loanRepo.sumPrincipalByStatus(session, LoanStatus.PENDING);
                principalActive    = loanRepo.sumPrincipalByStatus(session, LoanStatus.ACTIVE);
                principalCompleted = loanRepo.sumPrincipalByStatus(session, LoanStatus.COMPLETED);
                principalDefaulted = loanRepo.sumPrincipalByStatus(session, LoanStatus.DEFAULTED);
                totalPrincipalDisbursed = loanRepo.sumAllPrincipal(session);

                if (totalLoans > 0) {
                    approvalRate   = round2((loansApproved + loansActive + loansCompleted) * 100.0 / totalLoans);
                    defaultRate    = round2(loansDefaulted * 100.0 / totalLoans);
                    completionRate = round2(loansCompleted * 100.0 / totalLoans);
                }

                // Monthly trends
                List<Object[]> loanMonthly    = loanRepo.countByMonth(session, TREND_MONTHS);
                List<Object[]> depositMonthly = txRepo.depositCountByMonth(session, TREND_MONTHS);

                Map<String, Long> loanMap    = buildMonthMap();
                Map<String, Long> depositMap = buildMonthMap();

                for (Object[] row : loanMonthly) {
                    String key = row[0] + "-" + String.format("%02d", ((Number) row[1]).intValue());
                    if (loanMap.containsKey(key)) loanMap.put(key, ((Number) row[2]).longValue());
                }
                for (Object[] row : depositMonthly) {
                    String key = row[0] + "-" + String.format("%02d", ((Number) row[1]).intValue());
                    if (depositMap.containsKey(key)) depositMap.put(key, ((Number) row[2]).longValue());
                }

                monthLabelsJson         = toJsonStringArray(new ArrayList<>(loanMap.keySet()));
                monthlyLoanTrendJson    = toJsonLongArray(new ArrayList<>(loanMap.values()));
                monthlyDepositTrendJson = toJsonLongArray(new ArrayList<>(depositMap.values()));

                loanStatusChartJson = "[" + loansPending + "," + loansApproved + "," + loansActive + ","
                        + loansCompleted + "," + loansDefaulted + "," + loansRejected + "]";

                loanAmountChartJson = "[" +
                        toM(principalPending) + "," + toM(principalActive) + "," +
                        toM(principalCompleted) + "," + toM(principalDefaulted) + "]";

                savingsVolumeChartJson = "[" + toM(totalDeposits) + "," + toM(totalWithdrawals) + "]";

                return null;
            });
        } catch (Exception e) {
            buildFallbackJson();
        }
    }

    private Map<String, Long> buildMonthMap() {
        Map<String, Long> map = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = TREND_MONTHS - 1; i >= 0; i--) {
            LocalDateTime m = now.minusMonths(i);
            String key = m.getYear() + "-" + String.format("%02d", m.getMonthValue());
            map.put(key, 0L);
        }
        return map;
    }

    private void buildFallbackJson() {
        loanStatusChartJson     = "[0,0,0,0,0,0]";
        loanAmountChartJson     = "[0,0,0,0]";
        savingsVolumeChartJson  = "[0,0]";
        monthLabelsJson         = "[]";
        monthlyLoanTrendJson    = "[]";
        monthlyDepositTrendJson = "[]";
    }

    private String toJsonStringArray(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(list.get(i)).append("\"");
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String toJsonLongArray(List<Long> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private double toM(BigDecimal val) {
        if (val == null || val.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return val.divide(new BigDecimal("1000000"), 2, RoundingMode.HALF_UP).doubleValue();
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    // Getters
    public long getTotalMembers()         { return totalMembers; }
    public long getActiveMembers()        { return activeMembers; }
    public long getPendingMembers()       { return pendingMembers; }
    public BigDecimal getTotalSavingsBalance()    { return totalSavingsBalance; }
    public BigDecimal getTotalDeposits()         { return totalDeposits; }
    public BigDecimal getTotalWithdrawals()      { return totalWithdrawals; }
    public long getTotalDepositCount()           { return totalDepositCount; }
    public long getTotalWithdrawalCount()        { return totalWithdrawalCount; }
    public long getTotalTransactionCount()       { return totalTransactionCount; }
    public long getLoansPending()   { return loansPending; }
    public long getLoansApproved()  { return loansApproved; }
    public long getLoansActive()    { return loansActive; }
    public long getLoansCompleted() { return loansCompleted; }
    public long getLoansDefaulted() { return loansDefaulted; }
    public long getLoansRejected()  { return loansRejected; }
    public long getTotalLoans()     { return totalLoans; }
    public BigDecimal getTotalPrincipalDisbursed() { return totalPrincipalDisbursed; }
    public BigDecimal getPrincipalActive()         { return principalActive; }
    public BigDecimal getPrincipalCompleted()      { return principalCompleted; }
    public BigDecimal getPrincipalDefaulted()      { return principalDefaulted; }
    public double getApprovalRate()   { return approvalRate; }
    public double getDefaultRate()    { return defaultRate; }
    public double getCompletionRate() { return completionRate; }
    public double getDepositToWithdrawalRatio() { return depositToWithdrawalRatio; }
    public String getLoanStatusChartJson()     { return loanStatusChartJson; }
    public String getLoanAmountChartJson()     { return loanAmountChartJson; }
    public String getSavingsVolumeChartJson()  { return savingsVolumeChartJson; }
    public String getMonthlyLoanTrendJson()    { return monthlyLoanTrendJson; }
    public String getMonthlyDepositTrendJson() { return monthlyDepositTrendJson; }
    public String getMonthLabelsJson()         { return monthLabelsJson; }
}
