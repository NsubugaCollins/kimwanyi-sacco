package org.kimwanyi.sacco.service;

import org.kimwanyi.sacco.entity.JournalEntry;
import org.kimwanyi.sacco.entity.JournalLine;
import org.kimwanyi.sacco.entity.LedgerAccount;
import org.kimwanyi.sacco.enums.AccountType;

import java.util.List;

public interface LedgerService {
    LedgerAccount createAccount(String accountCode, String accountName, AccountType accountType);
    LedgerAccount getAccountByCode(String accountCode);
    JournalEntry postJournalEntry(String referenceNumber, String description, List<JournalLine> lines);
}
