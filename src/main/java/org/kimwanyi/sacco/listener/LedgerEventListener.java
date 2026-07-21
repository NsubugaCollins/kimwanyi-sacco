package org.kimwanyi.sacco.listener;

import org.kimwanyi.sacco.entity.JournalLine;
import org.kimwanyi.sacco.entity.LedgerAccount;
import org.kimwanyi.sacco.enums.AccountType;
import org.kimwanyi.sacco.enums.EntryType;
import org.kimwanyi.sacco.enums.TransactionType;
import org.kimwanyi.sacco.event.*;
import org.kimwanyi.sacco.service.LedgerService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class LedgerEventListener {

    public static final String CASH_ACCOUNT_CODE = "1010-CASH";
    public static final String SAVINGS_ACCOUNT_CODE = "2010-SAVINGS";
    public static final String LOANS_ACCOUNT_CODE = "1020-LOANS";
    public static final String INTEREST_INCOME_ACCOUNT_CODE = "4010-INTEREST";

    private final LedgerService ledgerService;

    public LedgerEventListener(LedgerService ledgerService) {
        this.ledgerService = ledgerService;

        // Register default GL Chart of Accounts if missing
        ensureDefaultAccountsExist();
    }

    public void registerListeners(EventPublisher publisher) {
        if (publisher == null) return;

        publisher.registerListener(SavingsTransactionEvent.class, this::handleSavingsTransaction);
        publisher.registerListener(LoanDisbursedEvent.class, this::handleLoanDisbursed);
        publisher.registerListener(LoanRepaidEvent.class, this::handleLoanRepaid);
    }

    private void ensureDefaultAccountsExist() {
        createAccountIfAbsent(CASH_ACCOUNT_CODE, "Cash on Hand", AccountType.ASSET);
        createAccountIfAbsent(SAVINGS_ACCOUNT_CODE, "Member Savings Deposits", AccountType.LIABILITY);
        createAccountIfAbsent(LOANS_ACCOUNT_CODE, "Loans Receivable", AccountType.ASSET);
        createAccountIfAbsent(INTEREST_INCOME_ACCOUNT_CODE, "Interest Income on Loans", AccountType.REVENUE);
    }

    private void createAccountIfAbsent(String code, String name, AccountType type) {
        try {
            ledgerService.getAccountByCode(code);
        } catch (Exception e) {
            ledgerService.createAccount(code, name, type);
        }
    }

    public void handleSavingsTransaction(SavingsTransactionEvent event) {
        LedgerAccount cashAccount = ledgerService.getAccountByCode(CASH_ACCOUNT_CODE);
        LedgerAccount savingsAccount = ledgerService.getAccountByCode(SAVINGS_ACCOUNT_CODE);

        List<JournalLine> lines = new ArrayList<>();

        if (event.getTransactionType() == TransactionType.DEPOSIT) {
            // DEBIT Cash / CREDIT Savings Deposits
            lines.add(new JournalLine(cashAccount, EntryType.DEBIT, event.getAmount()));
            lines.add(new JournalLine(savingsAccount, EntryType.CREDIT, event.getAmount()));
        } else {
            // DEBIT Savings Deposits / CREDIT Cash
            lines.add(new JournalLine(savingsAccount, EntryType.DEBIT, event.getAmount()));
            lines.add(new JournalLine(cashAccount, EntryType.CREDIT, event.getAmount()));
        }

        String description = "GL entry for savings " + event.getTransactionType().name() + " (Ref: " + event.getReferenceNumber() + ")";
        ledgerService.postJournalEntry("GL-SAV-" + event.getReferenceNumber(), description, lines);
    }

    public void handleLoanDisbursed(LoanDisbursedEvent event) {
        LedgerAccount loansAccount = ledgerService.getAccountByCode(LOANS_ACCOUNT_CODE);
        LedgerAccount cashAccount = ledgerService.getAccountByCode(CASH_ACCOUNT_CODE);

        List<JournalLine> lines = new ArrayList<>();
        // DEBIT Loans Receivable / CREDIT Cash
        lines.add(new JournalLine(loansAccount, EntryType.DEBIT, event.getPrincipalAmount()));
        lines.add(new JournalLine(cashAccount, EntryType.CREDIT, event.getPrincipalAmount()));

        String description = "GL entry for Loan Disbursement (Loan ID: " + event.getLoanId() + ")";
        ledgerService.postJournalEntry("GL-DISB-LOAN-" + event.getLoanId() + "-" + System.currentTimeMillis(), description, lines);
    }

    public void handleLoanRepaid(LoanRepaidEvent event) {
        LedgerAccount cashAccount = ledgerService.getAccountByCode(CASH_ACCOUNT_CODE);
        LedgerAccount loansAccount = ledgerService.getAccountByCode(LOANS_ACCOUNT_CODE);
        LedgerAccount interestAccount = ledgerService.getAccountByCode(INTEREST_INCOME_ACCOUNT_CODE);

        List<JournalLine> lines = new ArrayList<>();
        // DEBIT Cash (Total Payment)
        lines.add(new JournalLine(cashAccount, EntryType.DEBIT, event.getAmountPaid()));

        // CREDIT Loans Receivable (Principal Portion)
        if (event.getPrincipalPortion() != null && event.getPrincipalPortion().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(new JournalLine(loansAccount, EntryType.CREDIT, event.getPrincipalPortion()));
        }

        // CREDIT Interest Income (Interest Portion)
        if (event.getInterestPortion() != null && event.getInterestPortion().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(new JournalLine(interestAccount, EntryType.CREDIT, event.getInterestPortion()));
        }

        // If portions not explicitly split, CREDIT Loans Receivable by total amount paid
        if (lines.size() == 1) {
            lines.add(new JournalLine(loansAccount, EntryType.CREDIT, event.getAmountPaid()));
        }

        String description = "GL entry for Loan Repayment (Ref: " + event.getReferenceNumber() + ")";
        ledgerService.postJournalEntry("GL-REPAY-" + event.getReferenceNumber(), description, lines);
    }
}
