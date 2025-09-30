package com.xy.generator.model;


import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 简易无锁环形缓冲区
 *
 * @param <E> 元素类型
 */

/**
 * 简单的环形缓冲区，用于缓存 UID
 * 使用 Lock 保证线程安全，提高并发性能
 */
public class IdRingBuffer<E> {
    private final Object[] buffer; // 存储元素的数组
    private final int capacity; // 容量必须是 2 的幂
    private final Lock lock = new ReentrantLock();
    private volatile int head = 0; // 读指针
    private volatile int tail = 0; // 写指针
    private volatile int size = 0; // 当前缓存区中元素数量

    public IdRingBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new Object[capacity];
    }

    /**
     * 添加元素到缓冲区尾部
     */
    public void put(E e) {
        lock.lock();
        try {
            if (isFull()) {
                throw new IllegalStateException("RingBuffer is full");
            }
            buffer[tail] = e;
            tail = (tail + 1) & (capacity - 1);
            size++;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 从缓冲区头部获取元素
     */
    @SuppressWarnings("unchecked")
    public E take() {
        lock.lock();
        try {
            if (isEmpty()) {
                throw new IllegalStateException("RingBuffer is empty");
            }
            E e = (E) buffer[head];
            buffer[head] = null;
            head = (head + 1) & (capacity - 1);
            size--;
            return e;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 是否已满
     */
    public boolean isFull() {
        return size == capacity;
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 当前缓存区已使用元素个数
     */
    public int size() {
        return size;
    }
}