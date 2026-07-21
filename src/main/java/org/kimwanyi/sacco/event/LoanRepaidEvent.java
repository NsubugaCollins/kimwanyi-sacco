package org.kimwanyi.sacco.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class LoanRepaidEvent implements DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final Long loanId;
    private final Long memberId;
    private final BigDecimal amountPaid;
    private final BigDecimal principalPortion;
    private final BigDecimal interestPortion;
    private final String referenceNumber;

    public LoanRepaidEvent(Long loanId, Long memberId, BigDecimal amountPaid, BigDecimal principalPortion, BigDecimal interestPortion, String referenceNumber) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.loanId = loanId;
        this.memberId = memberId;
        this.amountPaid = amountPaid;
        this.principalPortion = principalPortion;
        this.interestPortion = interestPortion;
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

    public Long getLoanId() {
        return loanId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public BigDecimal getPrincipalPortion() {
        return principalPortion;
    }

    public BigDecimal getInterestPortion() {
        return interestPortion;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }
}
