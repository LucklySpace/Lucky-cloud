package com.xy.lucky.message.message.outbox;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class OutboxStateMachine {

    private final Map<OutboxStatus, Map<OutboxEvent, OutboxStatus>> transitions = new EnumMap<>(OutboxStatus.class);

    public OutboxStateMachine() {
        put(OutboxStatus.PENDING, OutboxEvent.BROKER_ACK, OutboxStatus.SENT);
        put(OutboxStatus.PENDING, OutboxEvent.BROKER_NACK, OutboxStatus.PENDING);
        put(OutboxStatus.PENDING, OutboxEvent.RETRY_EXHAUSTED, OutboxStatus.DLX);
        put(OutboxStatus.SENT, OutboxEvent.CLIENT_ACK, OutboxStatus.DELIVERED);
        put(OutboxStatus.SENT, OutboxEvent.BROKER_NACK, OutboxStatus.PENDING);
        put(OutboxStatus.SENT, OutboxEvent.RETRY_EXHAUSTED, OutboxStatus.DLX);
    }

    public OutboxStatus transit(OutboxStatus current, OutboxEvent event) {
        if (current == null) {
            return event == OutboxEvent.CREATE ? OutboxStatus.PENDING : OutboxStatus.FAILED;
        }
        return transitions.getOrDefault(current, Map.of()).getOrDefault(event, OutboxStatus.FAILED);
    }

    private void put(OutboxStatus from, OutboxEvent event, OutboxStatus to) {
        transitions.computeIfAbsent(from, ignored -> new EnumMap<>(OutboxEvent.class)).put(event, to);
    }
}
