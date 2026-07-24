package org.kimwanyi.sacco.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventPublisher {

    private static final EventPublisher INSTANCE = new EventPublisher();

    @SuppressWarnings("rawtypes")
    private final Map<Class<? extends DomainEvent>, List<EventListener>> listeners = new ConcurrentHashMap<>();

    private EventPublisher() {}

    public static EventPublisher getInstance() {
        return INSTANCE;
    }

    public <T extends DomainEvent> void registerListener(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> void publish(T event) {
        if (event == null) return;
        List<EventListener> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (EventListener listener : eventListeners) {
                listener.onEvent(event);
            }
        }
    }

    public void clearListeners() {
        listeners.clear();
    }
}
