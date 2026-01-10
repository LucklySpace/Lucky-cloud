package com.xy.lucky.spring.boot.context;

/**
 * ApplicationRunner - 应用启动后执行的 Runner 接口
 * <p>
 * 实现此接口的 Bean 会在应用启动完成后被调用
 */
@FunctionalInterface
public interface ApplicationRunner {

    /**
     * 执行启动后任务
     *
     * @param args 应用参数
     * @throws Exception 如果执行失败
     */
    void run(ApplicationArguments args) throws Exception;
}

