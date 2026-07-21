package org.kimwanyi.sacco.event;

import java.time.LocalDateTime;

public interface DomainEvent {
    String getEventId();
    LocalDateTime getOccurredOn();
}
