package org.kimwanyi.sacco.serviceImpl;

import org.kimwanyi.sacco.entity.JournalEntry;
import org.kimwanyi.sacco.entity.JournalLine;
import org.kimwanyi.sacco.entity.LedgerAccount;
import org.kimwanyi.sacco.enums.AccountType;
import org.kimwanyi.sacco.enums.EntryType;
import org.kimwanyi.sacco.exception.DuplicateRecordException;
import org.kimwanyi.sacco.exception.ValidationException;
import org.kimwanyi.sacco.repository.JournalEntryRepository;
import org.kimwanyi.sacco.repository.LedgerAccountRepository;
import org.kimwanyi.sacco.service.LedgerService;
import org.kimwanyi.sacco.util.TransactionManager;

import java.math.BigDecimal;
import java.util.List;

public class LedgerServiceImpl implements LedgerService {

    private final LedgerAccountRepository ledgerAccountRepository;
    private final JournalEntryRepository journalEntryRepository;

    public LedgerServiceImpl(
            LedgerAccountRepository ledgerAccountRepository,
            JournalEntryRepository journalEntryRepository
    ) {
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    @Override
    public LedgerAccount createAccount(String accountCode, String accountName, AccountType accountType) {
        if (accountCode == null || accountCode.isBlank()) {
            throw new ValidationException("Account code cannot be empty.");
        }
        if (accountName == null || accountName.isBlank()) {
            throw new ValidationException("Account name cannot be empty.");
        }
        if (accountType == null) {
            throw new ValidationException("Account type is required.");
        }

        return TransactionManager.execute(session -> {
            if (ledgerAccountRepository.existsByAccountCode(session, accountCode.trim())) {
                throw new DuplicateRecordException("Ledger account already exists with code: " + accountCode);
            }

            LedgerAccount account = new LedgerAccount(accountCode.trim(), accountName.trim(), accountType);
            return ledgerAccountRepository.save(session, account);
        });
    }

    @Override
    public LedgerAccount getAccountByCode(String accountCode) {
        if (accountCode == null || accountCode.isBlank()) {
            throw new ValidationException("Account code cannot be empty.");
        }

        return TransactionManager.execute(session -> {
            return ledgerAccountRepository.findByAccountCode(session, accountCode.trim())
                    .orElseThrow(() -> new ValidationException("Ledger account not found for code: " + accountCode));
        });
    }

    @Override
    public JournalEntry postJournalEntry(String referenceNumber, String description, List<JournalLine> lines) {
        if (referenceNumber == null || referenceNumber.isBlank()) {
            throw new ValidationException("Journal entry reference number is required.");
        }
        if (lines == null || lines.size() < 2) {
            throw new ValidationException("A journal entry must contain at least two lines (at least one debit and one credit).");
        }

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (JournalLine line : lines) {
            if (line.getAccount() == null) {
                throw new ValidationException("Journal line must reference a valid ledger account.");
            }
            if (line.getAmount() == null || line.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Journal line amount must be greater than zero.");
            }
            if (line.getEntryType() == null) {
                throw new ValidationException("Journal line entry type (DEBIT/CREDIT) is required.");
            }

            if (line.getEntryType() == EntryType.DEBIT) {
                totalDebits = totalDebits.add(line.getAmount());
            } else {
                totalCredits = totalCredits.add(line.getAmount());
            }
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new ValidationException(String.format(
                    "Unbalanced journal entry! Total Debits (%s) must equal Total Credits (%s).",
                    totalDebits.toPlainString(), totalCredits.toPlainString()
            ));
        }

        return TransactionManager.execute(session -> {
            if (journalEntryRepository.existsByReferenceNumber(session, referenceNumber.trim())) {
                throw new DuplicateRecordException("Journal entry already exists with reference number: " + referenceNumber);
            }

            JournalEntry entry = new JournalEntry(referenceNumber.trim(), description != null ? description.trim() : null);

            for (JournalLine line : lines) {
                LedgerAccount account = ledgerAccountRepository.findById(session, line.getAccount().getId())
                        .orElseThrow(() -> new ValidationException("Ledger account not found with ID: " + line.getAccount().getId()));

                // Update account balance based on account type
                BigDecimal amount = line.getAmount();
                AccountType type = account.getAccountType();

                if (type == AccountType.ASSET || type == AccountType.EXPENSE) {
                    if (line.getEntryType() == EntryType.DEBIT) {
                        account.setCurrentBalance(account.getCurrentBalance().add(amount));
                    } else {
                        account.setCurrentBalance(account.getCurrentBalance().subtract(amount));
                    }
                } else { // LIABILITY, EQUITY, REVENUE
                    if (line.getEntryType() == EntryType.CREDIT) {
                        account.setCurrentBalance(account.getCurrentBalance().add(amount));
                    } else {
                        account.setCurrentBalance(account.getCurrentBalance().subtract(amount));
                    }
                }

                ledgerAccountRepository.update(session, account);

                JournalLine entryLine = new JournalLine(account, line.getEntryType(), amount);
                entry.addLine(entryLine);
            }

            return journalEntryRepository.save(session, entry);
        });
    }
}
