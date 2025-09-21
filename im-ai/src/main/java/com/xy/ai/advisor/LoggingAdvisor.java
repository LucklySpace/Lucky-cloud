package com.xy.ai.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.MessageAggregator;
import reactor.core.publisher.Flux;

/**
 * 自定义日志Advisor
 *
 * @author ricejson
 */
public class LoggingAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAdvisor.class);

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        // 调用前记录请求日志
        logger.info("before req:{}", advisedRequest);
        // 调用执行链
        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);
        // 调用后记录响应日志
        logger.info("after resp:{}", advisedResponse);
        return advisedResponse;
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        // 调用前记录请求日志
        logger.info("before req:{}", advisedRequest);
        // 调用执行链
        Flux<AdvisedResponse> flux = chain.nextAroundStream(advisedRequest);
        return new MessageAggregator().aggregateAdvisedResponse(flux, (advisedResponse) -> {
            // 调用后记录响应日志
            logger.info("after resp:{}", advisedResponse);
        });
    }
}


//package com.xy.ai.advisor;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.ai.chat.client.advisor.api.*;
//import org.springframework.core.Ordered;
//import reactor.core.publisher.Flux;
//
//
/// **
// * LoggingAdvisor 提供对 AI 调用的前置和后置日志记录功能，
// * 支持同步调用（Call）和流式调用（Stream）。
// */
//public class LoggingAdvisor implements CallAroundAdvisor, StreamAroundAdvisor, Ordered {
//
//    private static final Logger logger = LoggerFactory.getLogger(LoggingAdvisor.class);
//
//    /**
//     * 为同步调用添加日志：调用前后打印请求和响应对象信息。
//     *
//     * @param advisedRequest AI调用请求封装对象
//     * @param chain          调用链，用于继续执行后续逻辑
//     * @return AI 调用返回的响应对象
//     */
//    @Override
//    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
//        logger.info("[Call BEFORE] 请求参数：{}", advisedRequest);
//        AdvisedResponse response = chain.nextAroundCall(advisedRequest);
//        logger.info("[Call AFTER] 响应结果：{}", response);
//        return response;
//    }
//
//    /**
//     * 为流式调用添加日志：调用前打印请求，
//     * 并在流中对每个响应元素打印日志。
//     *
//     * @param advisedRequest AI调用请求封装对象
//     * @param chain          流式调用链，用于继续执行后续逻辑
//     * @return 包含日志记录的响应 Flux
//     */
//    @Override
//    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
//        logger.info("[Stream BEFORE] 请求参数：{}", advisedRequest);
//        // 获取原始响应流
//        Flux<AdvisedResponse> responseFlux = chain.nextAroundStream(advisedRequest);
//        // 聚合并在每个响应到达时打印日志
//        return responseFlux.doOnNext(response -> logger.info("[Stream ELEMENT] 响应：{}", response));
//    }
//
//    /**
//     * 返回当前 Advisor 的名称，用于标识。
//     *
//     * @return Advisor 名称
//     */
//    @Override
//    public String getName() {
//        return getClass().getSimpleName();
//    }
//
//    /**
//     * 定义当前 Advisor 的全局执行顺序，数值越小优先级越高。
//     *
//     * @return 顺序值
//     */
//    @Override
//    public int getOrder() {
//        return 0;
//    }
//}