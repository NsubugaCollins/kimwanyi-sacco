package org.kimwanyi.sacco.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class LoanDisbursedEvent implements DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final Long loanId;
    private final Long memberId;
    private final BigDecimal principalAmount;
    private final Long officerUserId;

    public LoanDisbursedEvent(Long loanId, Long memberId, BigDecimal principalAmount, Long officerUserId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.loanId = loanId;
        this.memberId = memberId;
        this.principalAmount = principalAmount;
        this.officerUserId = officerUserId;
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

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public Long getOfficerUserId() {
        return officerUserId;
    }
}
