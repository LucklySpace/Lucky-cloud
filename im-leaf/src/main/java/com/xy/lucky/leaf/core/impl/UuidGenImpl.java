package com.xy.lucky.leaf.core.impl;

import com.xy.lucky.core.model.IMetaId;
import com.xy.lucky.leaf.core.IDGen;
import com.xy.lucky.leaf.model.IdRingBuffer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 基于 UUID 的 ID 生成器，实现了 RingBuffer 缓存池
 *
 * 特性：
 * - 使用环形缓冲区预生成 UUID，按需补充，保证性能
 * - 异步填充缓冲区，避免阻塞主线程
 * - 支持动态配置缓冲区大小和填充阈值
 */
@Component("uuidIDGen")
public class UuidGenImpl implements IDGen {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "UUID-Generator");
        t.setDaemon(true);
        return t;
    });

    // 缓存区位数（2 的幂），默认 10 -> 大小 1024
    @Value("${uuid.buffer-size-bits:10}")
    private int bufferSizeBits;

    // 缓存区阈值比例（低于该比例时触发补充），默认 0.2 -> 20%
    @Value("${uuid.padding-factor:0.2}")
    private double paddingFactor;

    private IdRingBuffer<String> ringBuffer;
    private int bufferSize;

    /**
     * Bean 初始化后，构造并预热缓存池
     *
     * @return 初始化是否成功
     */
    @Override
    public boolean init() {
        // 计算实际缓存大小
        this.bufferSize = 1 << bufferSizeBits;
        this.ringBuffer = new IdRingBuffer<>(bufferSize);
        // 预填充全部 UUID
        fillBuffer();
        return true;
    }

    /**
     * 异步获取一个 UUID
     * 每次调用会从缓存中取出一个，并在剩余量不足阈值时补充
     *
     * @param key 不影响生成，仅用于日志或追踪
     * @return Mono 包裹的 UUID 字符串
     */
    @Override
    public Mono<IMetaId> get(String key) {
        return Mono.fromCallable(() -> getId(key));
    }

    /**
     * 同步获取一个 UUID
     * 每次调用会从缓存中取出一个，并在剩余量不足阈值时补充
     *
     * @param key 不影响生成，仅用于日志或追踪
     * @return ID对象
     */
    @Override
    public IMetaId getId(String key) {
        // 检测剩余量
        if (ringBuffer.size() < bufferSize * paddingFactor) {
            // 使用独立线程异步填充缓冲区，避免阻塞当前线程
            executorService.submit(this::fillBuffer);
        }

        String nextId = ringBuffer.take();

        return IMetaId.builder().stringId(nextId).build();
    }

    /**
     * 批量生成 UUID 并填充缓存池直到满
     */
    private void fillBuffer() {
        while (!ringBuffer.isFull()) {
            ringBuffer.put(UUID.randomUUID().toString());
        }
    }
}