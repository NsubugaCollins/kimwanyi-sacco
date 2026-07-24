package org.kimwanyi.sacco.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "journal_entries")
public class JournalEntry extends BaseEntity {

    @NotBlank(message = "Reference number is required")
    @Column(name = "reference_number", nullable = false, length = 50, unique = true)
    private String referenceNumber;

    @Column(length = 255)
    private String description;

    @Column(name = "entry_date", nullable = false)
    private LocalDateTime entryDate;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<JournalLine> lines = new ArrayList<>();

    public JournalEntry() {
        this.entryDate = LocalDateTime.now();
    }

    public JournalEntry(String referenceNumber, String description) {
        this.referenceNumber = referenceNumber;
        this.description = description;
        this.entryDate = LocalDateTime.now();
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDateTime entryDate) {
        this.entryDate = entryDate;
    }

    public List<JournalLine> getLines() {
        return lines;
    }

    public void setLines(List<JournalLine> lines) {
        this.lines = lines;
    }

    public void addLine(JournalLine line) {
        if (line != null) {
            line.setJournalEntry(this);
            this.lines.add(line);
        }
    }
}
