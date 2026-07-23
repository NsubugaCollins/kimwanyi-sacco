package org.kimwanyi.sacco.bean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import org.kimwanyi.sacco.entity.LedgerAccount;
import org.kimwanyi.sacco.enums.AccountType;
import org.kimwanyi.sacco.repositoryImpl.JournalEntryRepositoryImpl;
import org.kimwanyi.sacco.repositoryImpl.LedgerAccountRepositoryImpl;
import org.kimwanyi.sacco.service.LedgerService;
import org.kimwanyi.sacco.serviceImpl.LedgerServiceImpl;
import org.kimwanyi.sacco.util.TransactionManager;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Named("ledgerBean")
@RequestScoped
public class LedgerBean implements Serializable {

    private LedgerService ledgerService;
    private LedgerAccountRepositoryImpl accountRepo;
    
    private String newAccountCode;
    private String newAccountName;
    private AccountType newAccountType = AccountType.ASSET;
    private List<LedgerAccount> accounts = Collections.emptyList();

    private String message;
    private String errorMessage;

    @PostConstruct
    public void init() {
        try {
            this.accountRepo = new LedgerAccountRepositoryImpl();
            JournalEntryRepositoryImpl journalRepo = new JournalEntryRepositoryImpl();
            this.ledgerService = new LedgerServiceImpl(accountRepo, journalRepo);
            loadAccounts();
        } catch (Exception e) {
            // View init
        }
    }

    public void loadAccounts() {
        if (accountRepo != null) {
            try {
                this.accounts = TransactionManager.execute(session -> accountRepo.findAll(session));
            } catch (Exception e) {
                // Graceful fallback
            }
        }
    }

    public String createAccount() {
        try {
            if (ledgerService != null) {
                ledgerService.createAccount(newAccountCode, newAccountName, newAccountType);
                this.message = "Ledger Account " + newAccountCode + " created successfully.";
                this.newAccountCode = "";
                this.newAccountName = "";
                loadAccounts();
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    public String getNewAccountCode() { return newAccountCode; }
    public void setNewAccountCode(String newAccountCode) { this.newAccountCode = newAccountCode; }
    public String getNewAccountName() { return newAccountName; }
    public void setNewAccountName(String newAccountName) { this.newAccountName = newAccountName; }
    public AccountType getNewAccountType() { return newAccountType; }
    public void setNewAccountType(AccountType newAccountType) { this.newAccountType = newAccountType; }
    public List<LedgerAccount> getAccounts() { return accounts; }
    public String getMessage() { return message; }
    public String getErrorMessage() { return errorMessage; }
}
