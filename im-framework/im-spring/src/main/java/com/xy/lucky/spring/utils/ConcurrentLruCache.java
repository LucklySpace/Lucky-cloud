package com.xy.lucky.spring.utils;


import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;


/**
 * 并发安全的 LRU (Least Recently Used) 缓存实现。
 * <p>
 * 使用 ConcurrentHashMap 存储键值对以实现高并发访问。
 * 通过双向链表 (EvictionQueue) 维护访问顺序，当缓存容量超出限制时，按照最久未使用策略淘汰旧条目。
 * 读写操作分别缓冲到各自的队列中，使用锁批量清理以减少锁竞争。
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 */
public final class ConcurrentLruCache<K, V> {
    /**
     * 缓存最大容量
     */
    private final int capacity;
    /**
     * 当前缓存大小计数
     */
    private final AtomicInteger currentSize;
    /**
     * 主存储：从 key 到 Node 的映射
     */
    private final ConcurrentMap<K, Node<K, V>> cache;
    /**
     * 缓存缺失时，用于生成值的函数
     */
    private final Function<K, V> generator;
    /**
     * 读操作缓冲，用于延迟更新访问顺序
     */
    private final ReadOperations<K, V> readOperations;
    /**
     * 写操作缓冲，用于延迟执行增加/删除任务
     */
    private final WriteOperations writeOperations;
    /**
     * 驱逐锁，保护批量清理流程
     */
    private final Lock evictionLock;
    /**
     * 双向链表，维护 LRU 驱逐顺序
     */
    private final EvictionQueue<K, V> evictionQueue;
    /**
     * 缓冲区状态，防止重复触发清理
     */
    private final AtomicReference<DrainStatus> drainStatus;

    /**
     * 构造方法：指定缓存容量和生成函数。
     *
     * @param capacity  最大条目数，必须 >= 0
     * @param generator 当 key 不存在时，用于创建新值的函数
     */
    public ConcurrentLruCache(int capacity, Function<K, V> generator) {
        this(capacity, generator, 16);
    }

    /**
     * 内部构造：支持并发级别配置。
     */
    private ConcurrentLruCache(int capacity, Function<K, V> generator, int concurrencyLevel) {
        this.currentSize = new AtomicInteger();
        this.evictionLock = new ReentrantLock();
        this.evictionQueue = new EvictionQueue<>();
        this.drainStatus = new AtomicReference<>(DrainStatus.IDLE);
        assert capacity >= 0 : "Capacity must be >= 0";
        this.capacity = capacity;
        this.cache = new ConcurrentHashMap<>(16, 0.75F, concurrencyLevel);
        this.generator = generator;
        this.readOperations = new ReadOperations<>(this.evictionQueue);
        this.writeOperations = new WriteOperations();
    }

    /**
     * 获取指定 key 的值，如果不存在则通过生成器创建并缓存。
     * 同时更新访问顺序，用于 LRU 逻辑。
     */
    public V get(K key) {
        if (this.capacity == 0) {
            // 容量为 0 时不缓存，直接生成
            return this.generator.apply(key);
        }
        Node<K, V> node = cache.get(key);
        if (node == null) {
            // 缓存未命中：生成并添加
            V value = generator.apply(key);
            put(key, value);
            return value;
        }
        // 缓存命中：记录访问
        processRead(node);
        return node.getValue();
    }

    /**
     * 插入新的 key-value，如果已存在则自动作为一次读取处理。
     */
    private void put(K key, V value) {
        assert key != null : "key must not be null";
        assert value != null : "value must not be null";
        CacheEntry<V> entry = new CacheEntry<>(value, CacheEntryState.ACTIVE);
        Node<K, V> node = new Node<>(key, entry);
        Node<K, V> existing = cache.putIfAbsent(key, node);
        if (existing == null) {
            // 新增条目：调度写入任务
            processWrite(new AddTask(node));
        } else {
            // 已存在：当作读取
            processRead(existing);
        }
    }

    /**
     * 处理一次读取操作，将节点加入读缓冲并在必要时触发清理。
     */
    private void processRead(Node<K, V> node) {
        boolean bufferNotFull = readOperations.recordRead(node);
        DrainStatus status = drainStatus.get();
        if (status.shouldDrainBuffers(bufferNotFull)) {
            drainOperations();
        }
    }

    /**
     * 调度写入（新增或删除）任务，并触发清理。
     */
    private void processWrite(Runnable task) {
        writeOperations.add(task);
        drainStatus.lazySet(DrainStatus.REQUIRED);
        drainOperations();
    }

    /**
     * 在驱逐锁保护下，同时清空读写缓冲，更新访问顺序并执行写任务。
     */
    private void drainOperations() {
        if (evictionLock.tryLock()) {
            try {
                drainStatus.lazySet(DrainStatus.PROCESSING);
                readOperations.drain();
                writeOperations.drain();
            } finally {
                drainStatus.compareAndSet(DrainStatus.PROCESSING, DrainStatus.IDLE);
                evictionLock.unlock();
            }
        }
    }

    /**
     * 返回缓存容量
     */
    public int capacity() {
        return capacity;
    }

    /**
     * 返回当前缓存大小
     */
    public int size() {
        return cache.size();
    }

    /**
     * 清空缓存：锁住后依次移除所有条目并重置缓冲区。
     */
    public void clear() {
        evictionLock.lock();
        try {
            Node<K, V> node;
            while ((node = evictionQueue.poll()) != null) {
                cache.remove(node.key, node);
                markAsRemoved(node);
            }
            readOperations.clear();
            writeOperations.drainAll();
        } finally {
            evictionLock.unlock();
        }
    }

    /**
     * 移除指定 key，如果存在则调度删除任务。
     */
    public boolean remove(K key) {
        Node<K, V> node = cache.remove(key);
        if (node == null) return false;
        markForRemoval(node);
        processWrite(new RemovalTask(node));
        return true;
    }

    /**
     * 标记节点为待删除状态
     */
    private void markForRemoval(Node<K, V> node) {
        CacheEntry<V> cur;
        CacheEntry<V> pending;
        do {
            cur = node.get();
            if (!cur.isActive()) return;
            pending = new CacheEntry<>(cur.value, CacheEntryState.PENDING_REMOVAL);
        } while (!node.compareAndSet(cur, pending));
    }

    /**
     * 将节点真正标记为已删除，并更新大小计数
     */
    private void markAsRemoved(Node<K, V> node) {
        CacheEntry<V> cur;
        CacheEntry<V> rem;
        do {
            cur = node.get();
            rem = new CacheEntry<>(cur.value, CacheEntryState.REMOVED);
        } while (!node.compareAndSet(cur, rem));
        currentSize.decrementAndGet();
    }

    /**
     * 枚举：缓冲区状态，控制何时触发真正的清理
     */
    private static enum DrainStatus {
        IDLE {
            boolean shouldDrainBuffers(boolean buf) {
                return !buf;
            }
        },
        REQUIRED {
            boolean shouldDrainBuffers(boolean buf) {
                return true;
            }
        },
        PROCESSING {
            boolean shouldDrainBuffers(boolean buf) {
                return false;
            }
        };

        abstract boolean shouldDrainBuffers(boolean bufferNotFull);
    }

    /**
     * 枚举：缓存实体生命周期
     */
    private static enum CacheEntryState {ACTIVE, PENDING_REMOVAL, REMOVED}

    /**
     * 缓存条目：值加状态的不可变记录
     */
    private static record CacheEntry<V>(V value, CacheEntryState state) {
        boolean isActive() {
            return state == CacheEntryState.ACTIVE;
        }
    }

    /**
     * 读操作缓冲：按线程分片存储读事件，减少锁竞争。
     * 定期 drain 时将对应节点移动到 LRU 队尾。
     */
    private static final class ReadOperations<K, V> {
        private static final int BUFFER_COUNT = detectNumberOfBuffers();
        private static final int BUFFERS_MASK = BUFFER_COUNT - 1;
        private static final int BUFFER_SIZE = 128;
        private static final int MAX_DRAIN = 64;

        private final AtomicLongArray recordedCount;
        private final long[] readCount;
        private final AtomicLongArray processedCount;
        private final AtomicReferenceArray<Node<K, V>>[] buffers;
        private final EvictionQueue<K, V> evictionQueue;

        ReadOperations(EvictionQueue<K, V> q) {
            this.evictionQueue = q;
            this.recordedCount = new AtomicLongArray(BUFFER_COUNT);
            this.readCount = new long[BUFFER_COUNT];
            this.processedCount = new AtomicLongArray(BUFFER_COUNT);
            this.buffers = new AtomicReferenceArray[BUFFER_COUNT];
            for (int i = 0; i < BUFFER_COUNT; i++) {
                this.buffers[i] = new AtomicReferenceArray<>(BUFFER_SIZE);
            }
        }

        private static int detectNumberOfBuffers() {
            int procs = Runtime.getRuntime().availableProcessors();
            int pow2 = 1 << (32 - Integer.numberOfLeadingZeros(procs - 1));
            return Math.min(4, pow2);
        }

        private static int getBufferIndex() {
            return (int) Thread.currentThread().getId() & BUFFERS_MASK;
        }

        /**
         * 记录一次读取，返回是否继续缓冲
         */
        boolean recordRead(Node<K, V> node) {
            int idx = getBufferIndex();
            long wcount = recordedCount.get(idx);
            recordedCount.lazySet(idx, wcount + 1);
            buffers[idx].lazySet((int) (wcount & (BUFFER_SIZE - 1)), node);
            long pending = wcount - processedCount.get(idx);
            return pending < MAX_DRAIN;
        }

        /**
         * drain 所有线程缓冲，移动节点至队尾
         */
        void drain() {
            int start = (int) Thread.currentThread().getId();
            for (int i = start; i < start + BUFFER_COUNT; i++) {
                drainBuffer(i & BUFFERS_MASK);
            }
        }

        private void drainBuffer(int idx) {
            long wcount = recordedCount.get(idx);
            for (int i = 0; i < MAX_DRAIN; i++) {
                long rcount = readCount[idx]++;
                int pos = (int) (rcount & (BUFFER_SIZE - 1));
                Node<K, V> node = buffers[idx].get(pos);
                if (node == null) break;
                buffers[idx].lazySet(pos, null);
                evictionQueue.moveToBack(node);
            }
            processedCount.lazySet(idx, wcount);
        }

        void clear() {
            for (var buf : buffers) {
                for (int j = 0; j < BUFFER_SIZE; j++) buf.lazySet(j, null);
            }
        }
    }

    /**
     * 写操作队列：将写任务累积，批量执行以降低开销。
     */
    private static final class WriteOperations {
        private static final int THRESHOLD = 16;
        private final Queue<Runnable> ops = new ConcurrentLinkedQueue<>();

        void add(Runnable r) {
            ops.add(r);
        }

        void drain() {
            for (int i = 0; i < THRESHOLD; i++) {
                Runnable r = ops.poll();
                if (r == null) break;
                r.run();
            }
        }

        void drainAll() {
            Runnable r;
            while ((r = ops.poll()) != null) r.run();
        }
    }

    /**
     * 链表节点：持有 CacheEntry 并在链表中维护 prev/next 指针。
     */
    private static final class Node<K, V> extends AtomicReference<CacheEntry<V>> {
        final K key;
        Node<K, V> prev, next;

        Node(K k, CacheEntry<V> e) {
            super(e);
            this.key = k;
        }

        V getValue() {
            return get().value;
        }
    }

    /**
     * 简单的双向链表，用于按插入/访问顺序驱逐。
     */
    private static final class EvictionQueue<K, V> {
        Node<K, V> first, last;

        Node<K, V> poll() {
            if (first == null) return null;
            Node<K, V> f = first;
            Node<K, V> n = f.next;
            first = n;
            if (n == null) last = null;
            else n.prev = null;
            f.next = null;
            return f;
        }

        void add(Node<K, V> e) {
            if (!contains(e)) linkLast(e);
        }

        private boolean contains(Node<K, V> e) {
            return e.prev != null || e.next != null || e == first;
        }

        private void linkLast(Node<K, V> e) {
            Node<K, V> l = last;
            last = e;
            if (l == null) first = e;
            else {
                l.next = e;
                e.prev = l;
            }
        }

        void moveToBack(Node<K, V> e) {
            if (contains(e) && e != last) {
                unlink(e);
                linkLast(e);
            }
        }

        void remove(Node<K, V> e) {
            if (contains(e)) unlink(e);
        }

        private void unlink(Node<K, V> e) {
            Node<K, V> p = e.prev, n = e.next;
            if (p == null) first = n;
            else p.next = n;
            if (n == null) last = p;
            else n.prev = p;
            e.prev = e.next = null;
        }
    }

    /**
     * 写入任务：新增条目并在超出容量时驱逐最旧条目。
     */
    private final class AddTask implements Runnable {
        private final Node<K, V> node;

        AddTask(Node<K, V> node) {
            this.node = node;
        }

        public void run() {
            currentSize.incrementAndGet();
            if (node.get().isActive()) {
                evictionQueue.add(node);
                evictIfNeeded();
            }
        }

        private void evictIfNeeded() {
            while (currentSize.get() > capacity) {
                Node<K, V> oldest = evictionQueue.poll();
                if (oldest == null) return;
                cache.remove(oldest.key, oldest);
                markAsRemoved(oldest);
            }
        }
    }

    /**
     * 写入任务：从驱逐队列移除指定节点。
     */
    private final class RemovalTask implements Runnable {
        private final Node<K, V> node;

        RemovalTask(Node<K, V> node) {
            this.node = node;
        }

        public void run() {
            evictionQueue.remove(node);
            markAsRemoved(node);
        }
    }
}
