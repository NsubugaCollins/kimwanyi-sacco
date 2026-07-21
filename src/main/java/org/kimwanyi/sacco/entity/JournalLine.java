package org.kimwanyi.sacco.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.kimwanyi.sacco.enums.EntryType;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "journal_lines")
public class JournalLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ledger_account_id", nullable = false)
    private LedgerAccount account;

    @NotNull(message = "Entry type (DEBIT/CREDIT) is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private EntryType entryType;

    @NotNull(message = "Amount is required")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    public JournalLine() {}

    public JournalLine(LedgerAccount account, EntryType entryType, BigDecimal amount) {
        this.account = account;
        this.entryType = entryType;
        this.amount = amount;
    }

    public JournalEntry getJournalEntry() {
        return journalEntry;
    }

    public void setJournalEntry(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
    }

    public LedgerAccount getAccount() {
        return account;
    }

    public void setAccount(LedgerAccount account) {
        this.account = account;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(EntryType entryType) {
        this.entryType = entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
