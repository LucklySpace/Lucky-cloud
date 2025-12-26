package com.xy.lucky.logging.config;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.xy.lucky.logging.disruptor.*;
import com.xy.lucky.logging.mapper.LogRecordConverter;
import com.xy.lucky.logging.repository.LogRepository;
import com.xy.lucky.logging.service.LogAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Disruptor配置类
 * 配置高性能的Disruptor队列用于日志处理
 */
@Configuration
@RequiredArgsConstructor
public class DisruptorConfig {

    private final LogRepository logRepository;
    private final LogAnalysisService analysisService;
    private final LogRecordConverter converter;
    private final SimpMessagingTemplate messagingTemplate;
    @Value("${logging.pipeline.ring-buffer-size:65536}")
    private int ringBufferSize;

    /**
     * 创建并启动Disruptor实例
     *
     * @return Disruptor<LogEvent>
     */
    @Bean(destroyMethod = "shutdown")
    public Disruptor<LogEvent> logDisruptor() {
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        Disruptor<LogEvent> disruptor = new Disruptor<>(
                new LogEventFactory(),
                ringBufferSize,
                threadFactory,
                com.lmax.disruptor.dsl.ProducerType.MULTI,
                new BlockingWaitStrategy()
        );
        LogStoreHandler storeHandler = new LogStoreHandler(logRepository, converter);
        LogAnalysisHandler analysisHandler = new LogAnalysisHandler(analysisService);
        LogBroadcastHandler broadcastHandler = new LogBroadcastHandler(messagingTemplate);
        disruptor.handleEventsWith(storeHandler, analysisHandler, broadcastHandler);
        disruptor.start();
        return disruptor;
    }
}
