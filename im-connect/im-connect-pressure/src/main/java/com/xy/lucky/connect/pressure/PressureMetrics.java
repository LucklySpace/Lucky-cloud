package com.xy.lucky.connect.pressure;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public class PressureMetrics {
    final LongAdder connectAttempt = new LongAdder();
    final LongAdder connectSuccess = new LongAdder();
    final LongAdder connectFail = new LongAdder();
    final LongAdder reconnectScheduled = new LongAdder();
    final AtomicInteger activeConnections = new AtomicInteger(0);

    final LongAdder registerSent = new LongAdder();
    final LongAdder registerAck = new LongAdder();
    final LongAdder heartbeatSent = new LongAdder();
    final LongAdder heartbeatAck = new LongAdder();
    final LongAdder businessSent = new LongAdder();
    final LongAdder sendFail = new LongAdder();
    final LongAdder received = new LongAdder();

    final LongAdder handshakeLatencyMsTotal = new LongAdder();
    final LongAdder handshakeLatencyCount = new LongAdder();

    String snapshotLine() {
        long hsCount = handshakeLatencyCount.sum();
        long avgHandshake = hsCount == 0 ? 0 : handshakeLatencyMsTotal.sum() / hsCount;
        return "attempt=" + connectAttempt.sum()
                + ", success=" + connectSuccess.sum()
                + ", fail=" + connectFail.sum()
                + ", active=" + activeConnections.get()
                + ", reconnect=" + reconnectScheduled.sum()
                + ", regSent=" + registerSent.sum()
                + ", regAck=" + registerAck.sum()
                + ", hbSent=" + heartbeatSent.sum()
                + ", hbAck=" + heartbeatAck.sum()
                + ", bizSent=" + businessSent.sum()
                + ", recv=" + received.sum()
                + ", sendFail=" + sendFail.sum()
                + ", avgHandshakeMs=" + avgHandshake;
    }
}
