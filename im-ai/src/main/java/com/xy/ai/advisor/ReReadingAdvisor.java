package com.xy.ai.advisor;

import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;


/**
 * ReReadingAdvisor 在每次调用 AI 之前，重写用户输入，
 * 并将原始用户文本保存在 userParams 中，以便后续使用。
 * 支持同步调用和流式调用。
 */
public class ReReadingAdvisor implements CallAroundAdvisor, StreamAroundAdvisor, Ordered {

    /**
     * 在原始请求基础上构建新的请求：
     * - 将原始用户文本保存到 userParams 的 "re2_input_query" 中
     * - 覆盖 userText 为固定问题
     *
     * @param advisedRequest 原始封装请求对象
     * @return 新的封装请求对象
     */
    private AdvisedRequest before(AdvisedRequest advisedRequest) {
        // 复制原始用户参数，防止修改原对象
        Map<String, Object> advisedUserParams = new HashMap<>(advisedRequest.userParams());
        // 将原始用户文本保存到自定义参数中
        advisedUserParams.put("re2_input_query", advisedRequest.userText());

        // 基于原始请求构建新的请求
        return AdvisedRequest.from(advisedRequest)
                .userText("你的作用是什么，谁创造的你")   // 覆盖用户输入为指定问题
                .userParams(advisedUserParams)            // 更新参数
                .build();
    }

    /**
     * 在同步调用前，先执行 before 方法构造新的请求，然后继续执行调用链。
     *
     * @param advisedRequest 原始封装请求对象
     * @param chain           同步调用链
     * @return AI 同步调用返回的响应对象
     */
    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        return chain.nextAroundCall(this.before(advisedRequest));
    }

    /**
     * 在流式调用前，先执行 before 方法构造新的请求，然后继续执行流式调用链。
     *
     * @param advisedRequest 原始封装请求对象
     * @param chain           流式调用链
     * @return AI 流式调用返回的响应 Flux
     */
    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        return chain.nextAroundStream(this.before(advisedRequest));
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

    /**
     * 返回当前 Advisor 的名称，用于标识。
     *
     * @return Advisor 名称
     */
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
