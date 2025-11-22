package com.xy.lucky.leaf.model;

import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

/**
 * ID段实体类
 * 用于表示一个ID段，包含起始ID、结束ID、步长和当前ID
 */
@Data
public class Segment {

    private final long start;  // 起始ID
    private final long end;    // 结束ID
    private final long step;   // 步长
    private final AtomicLong current = new AtomicLong(); // 当前ID

    /**
     * 构造函数
     *
     * @param start 起始ID
     * @param end   结束ID
     * @param step  步长
     */
    public Segment(long start, long end, long step) {
        this.start = start;
        this.end = end;
        this.step = step;
        this.current.set(start);
    }

    /**
     * 构造函数
     *
     * @param start 起始ID
     * @param end 结束ID
     * @param step 步长
     * @param current 当前ID
     */
    public Segment(long start, long end, long step, long current) {
        this.start = start;
        this.end = end;
        this.step = step;
        this.current.set(current);
    }

    /**
     * 获取下一个ID
     *
     * @return 下一个ID，如果段已用完则返回-1
     */
    public long next() {
        long val = current.getAndIncrement();
        return val <= end ? val : -1;
    }

    /**
     * 获取剩余ID数量
     *
     * @return 剩余ID数量
     */
    public long remaining() {
        return end - current.get();
    }

    /**
     * 检查段是否已完成
     *
     * @return 如果段已完成则返回true，否则返回false
     */
    public boolean isFinished() {
        return current.get() > end;
    }
}