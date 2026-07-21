package org.kimwanyi.sacco.event;

public interface EventListener<T extends DomainEvent> {
    void onEvent(T event);
}
