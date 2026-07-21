package org.kimwanyi.sacco.event;

import org.kimwanyi.sacco.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class SavingsTransactionEvent implements DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final Long memberId;
    private final Long savingsAccountId;
    private final TransactionType transactionType;
    private final BigDecimal amount;
    private final String referenceNumber;

    public SavingsTransactionEvent(Long memberId, Long savingsAccountId, TransactionType transactionType, BigDecimal amount, String referenceNumber) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.memberId = memberId;
        this.savingsAccountId = savingsAccountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.referenceNumber = referenceNumber;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getSavingsAccountId() {
        return savingsAccountId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }
}
