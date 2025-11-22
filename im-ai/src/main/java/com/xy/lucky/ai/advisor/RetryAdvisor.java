package com.xy.lucky.ai.advisor;

import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.core.Ordered;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import reactor.core.publisher.Flux;


/**
 * RetryAdvisor 为 AI 调用提供重试机制，适用于同步调用，
 * 当调用失败时可按照配置的策略进行重试。
 * 支持最大重试次数和固定间隔重试策略。
 */
public class RetryAdvisor implements CallAroundAdvisor, StreamAroundAdvisor, Ordered {

    /**
     * RetryTemplate 实例，负责执行重试逻辑
     */
    private final RetryTemplate retryTemplate;

    /**
     * 构造函数：初始化并配置重试模板
     * - 最多重试 2 次
     * - 固定间隔 1 秒
     */
    public RetryAdvisor() {
        this.retryTemplate = new RetryTemplate();

        // 配置重试策略：最多 2 次
        SimpleRetryPolicy policy = new SimpleRetryPolicy();
        policy.setMaxAttempts(2);

        // 配置回退策略：每次重试间隔 1 秒 (1000 毫秒)
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(1000);

        // 注入策略到 RetryTemplate
        retryTemplate.setRetryPolicy(policy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
    }

    /**
     * 同步调用增强：在调用链中执行重试逻辑
     *
     * @param advisedRequest 封装的请求对象
     * @param chain          同步调用链，用于执行实际调用
     * @return AI 调用返回的响应对象，若调用失败则根据重试策略重试
     */
    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        // 使用 retryTemplate 执行调用：Lambda 中实际执行调用链
        return retryTemplate.execute(context -> chain.nextAroundCall(advisedRequest));
    }

    /**
     * 流式调用不进行重试，直接透传响应流。
     *
     * @param advisedRequest 封装的请求对象
     * @param chain          流式调用链，用于执行实际调用
     * @return AI 流式调用返回的响应 Flux，不进行重试
     */
    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        return chain.nextAroundStream(advisedRequest);
    }

    /**
     * 返回 Advisor 名称，用于标识。
     *
     * @return Advisor 类名
     */
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 定义 Advisor 执行顺序，数值越小优先级越高。
     *
     * @return 顺序值
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
