package com.xy.lucky.leaf.model;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 简单的环形缓冲区，用于缓存ID
 * 使用Lock保证线程安全，提高并发性能
 *
 * @param <E> 元素类型
 */
public class IdRingBuffer<E> {

    private final Object[] buffer; // 存储元素的数组
    private final int capacity; // 容量必须是 2 的幂
    private final Lock lock = new ReentrantLock();
    private volatile int head = 0; // 读指针
    private volatile int tail = 0; // 写指针
    private volatile int size = 0; // 当前缓存区中元素数量

    /**
     * 构造函数
     *
     * @param capacity 缓冲区容量，必须是2的幂
     */
    public IdRingBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new Object[capacity];
    }

    /**
     * 添加元素到缓冲区尾部
     *
     * @param e 要添加的元素
     * @throws IllegalStateException 当缓冲区已满时抛出
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
     *
     * @return 从缓冲区头部获取的元素
     * @throws IllegalStateException 当缓冲区为空时抛出
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
     * 检查缓冲区是否已满
     *
     * @return 如果缓冲区已满则返回true，否则返回false
     */
    public boolean isFull() {
        return size == capacity;
    }

    /**
     * 检查缓冲区是否为空
     *
     * @return 如果缓冲区为空则返回true，否则返回false
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 获取当前缓冲区中元素的数量
     *
     * @return 当前缓冲区中元素的数量
     */
    public int size() {
        return size;
    }
}