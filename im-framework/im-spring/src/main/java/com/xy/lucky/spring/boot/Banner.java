package com.xy.lucky.spring.boot;

import com.xy.lucky.spring.boot.env.Environment;

import java.io.PrintStream;

/**
 * Banner 接口 - 应用启动时的 Banner 打印
 */
@FunctionalInterface
public interface Banner {

    /**
     * 打印 Banner
     *
     * @param environment 环境
     * @param sourceClass 启动类
     * @param out         输出流
     */
    void printBanner(Environment environment, Class<?> sourceClass, PrintStream out);

    /**
     * Banner 打印模式
     */
    enum Mode {
        /**
         * 关闭 Banner
         */
        OFF,
        /**
         * 输出到控制台
         */
        CONSOLE,
        /**
         * 输出到日志
         */
        LOG
    }
}

