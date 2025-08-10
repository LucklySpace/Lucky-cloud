package com.xy.core.utils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 可调整容量的任务队列
 * @param <E>
 */
public class ResizableCapacityLinkedBlockingQueue<E> extends LinkedBlockingQueue<E> {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private volatile int capacity;

    public ResizableCapacityLinkedBlockingQueue(int initialCapacity) {
        super(initialCapacity);
        this.capacity = initialCapacity;
    }

    public void setCapacity(int newCapacity) {
        lock.lock();
        try {
            if (newCapacity <= 0) throw new IllegalArgumentException("Capacity must be positive");
            int oldCapacity = this.capacity;
            this.capacity = newCapacity;
            if (newCapacity > oldCapacity && size() > 0) {
                notFull.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        lock.lock();
        try {
            while (size() == capacity) {
                if (!notFull.await(10, TimeUnit.MILLISECONDS)) {
                    return false;
                }
            }
            return super.offer(e);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        lock.lock();
        try {
            while (size() == capacity) {
                notFull.await();
            }
            super.put(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public E take() throws InterruptedException {
        lock.lock();
        try {
            E item = super.take();
            if (size() < capacity) {
                notFull.signal();
            }
            return item;
        } finally {
            lock.unlock();
        }
    }
}