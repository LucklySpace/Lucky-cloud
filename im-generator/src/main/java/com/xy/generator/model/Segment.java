package com.xy.generator.model;

import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

@Data
public class Segment {
    private final long start;
    private final long end;
    private final long step;
    private final AtomicLong current = new AtomicLong();

    public Segment(long start, long end, long step) {
        this.start = start;
        this.end = end;
        this.step = step;
        this.current.set(start);
    }

    public Segment(long start, long end, long step, long current) {
        this.start = start;
        this.end = end;
        this.step = step;
        this.current.set(current);
    }

    public long next() {
        long val = current.getAndIncrement();
        return val <= end ? val : -1;
    }

    public long remaining() {
        return end - current.get();
    }

    public boolean isFinished() {
        return current.get() > end;
    }
}