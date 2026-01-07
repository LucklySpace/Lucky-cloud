package com.xy.lucky.spring.boot;

import com.xy.lucky.spring.boot.context.ConfigurableApplicationContext;
import com.xy.lucky.spring.boot.context.DefaultApplicationContext;
import com.xy.lucky.spring.boot.env.ConfigurableEnvironment;
import com.xy.lucky.spring.boot.env.StandardEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SpringApplication - Spring Boot 风格的应用启动器
 * <p>
 * 用法示例:
 * <pre>
 * &#64;SpringBootApplication
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 * </pre>
 * <p>
 * Banner 配置:
 * <pre>
 * spring:
 *   banner:
 *     location: classpath:banner.txt
 *     charset: UTF-8
 *   main:
 *     banner-mode: console  # off, console, log
 * </pre>
 */
public class SpringApplication {

    private static final Logger log = LoggerFactory.getLogger(SpringApplication.class);

    private static final AtomicReference<ConfigurableApplicationContext> CONTEXT = new AtomicReference<>();

    private final Class<?> primarySource;
    private final Set<Class<?>> primarySources = new LinkedHashSet<>();

    private Banner banner = new DefaultBanner();
    private Banner.Mode bannerMode = Banner.Mode.CONSOLE;
    private boolean logStartupInfo = true;
    private ConfigurableEnvironment environment;

    /**
     * 创建 SpringApplication 实例
     *
     * @param primarySource 主配置类（标注 @SpringBootApplication）
     */
    public SpringApplication(Class<?> primarySource) {
        this.primarySource = primarySource;
        this.primarySources.add(primarySource);
    }

    /**
     * 创建 SpringApplication 实例（多配置源）
     *
     * @param primarySources 主配置类数组
     */
    public SpringApplication(Class<?>... primarySources) {
        this.primarySource = (primarySources != null && primarySources.length > 0) ? primarySources[0] : null;
        if (primarySources != null) {
            this.primarySources.addAll(Arrays.asList(primarySources));
        }
    }

    // ================== 静态快捷方法 ==================

    /**
     * 静态快捷方法，启动应用
     *
     * @param primarySource 主配置类
     * @param args          命令行参数
     * @return ApplicationContext 实例
     */
    public static ConfigurableApplicationContext run(Class<?> primarySource, String... args) {
        return new SpringApplication(primarySource).run(args);
    }

    /**
     * 静态快捷方法，启动多配置源应用
     *
     * @param primarySources 主配置类数组
     * @param args           命令行参数
     * @return ApplicationContext 实例
     */
    public static ConfigurableApplicationContext run(Class<?>[] primarySources, String[] args) {
        return new SpringApplication(primarySources).run(args);
    }

    /**
     * 获取当前全局 ApplicationContext
     *
     * @return ApplicationContext，若未初始化则返回 null
     */
    public static ConfigurableApplicationContext getContext() {
        return CONTEXT.get();
    }

    /**
     * 判断应用是否已启动
     */
    public static boolean isRunning() {
        ConfigurableApplicationContext ctx = CONTEXT.get();
        return ctx != null && ctx.isActive();
    }

    /**
     * 关闭应用
     */
    public static void exit() {
        ConfigurableApplicationContext ctx = CONTEXT.getAndSet(null);
        if (ctx != null) {
            ctx.close();
        }
    }

    // ================== 实例方法 ==================

    /**
     * 启动应用
     *
     * @param args 命令行参数
     * @return ApplicationContext 实例
     */
    public ConfigurableApplicationContext run(String... args) {
        Instant startTime = Instant.now();

        // 检查是否已存在 Context
        ConfigurableApplicationContext existing = CONTEXT.get();
        if (existing != null && existing.isActive()) {
            log.warn("ApplicationContext 已存在且处于活跃状态，返回现有实例");
            return existing;
        }

        ConfigurableApplicationContext context = null;
        try {
            // 1. 准备环境
            ConfigurableEnvironment env = prepareEnvironment(args);

            // 2. 配置 Banner
            configureBanner(env);

            // 3. 打印 Banner
            printBanner(env);

            // 4. 创建 ApplicationContext
            context = createApplicationContext();

            // 5. 准备 Context
            prepareContext(context, env, args);

            // 6. 刷新 Context（核心初始化）
            refreshContext(context);

            // 7. 设置全局引用
            if (!CONTEXT.compareAndSet(null, context)) {
                // 并发情况下其他线程已设置，关闭当前创建的实例
                log.warn("并发检测到已有 ApplicationContext，释放当前实例");
                context.close();
                return CONTEXT.get();
            }

            // 8. 注册 ShutdownHook
            registerShutdownHook(context);

            // 9. 打印启动完成信息
            Duration timeTaken = Duration.between(startTime, Instant.now());
            logStarted(timeTaken);

            // 10. 执行 Runners
            callRunners(context, args);

            return context;
        } catch (Throwable ex) {
            log.error("Application startup failed", ex);
            if (context != null) {
                try {
                    context.close();
                } catch (Exception e) {
                    log.warn("Failed to close context after startup failure", e);
                }
            }
            throw new IllegalStateException("Application startup failed", ex);
        }
    }

    /**
     * 准备环境
     */
    private ConfigurableEnvironment prepareEnvironment(String[] args) {
        if (this.environment == null) {
            this.environment = new StandardEnvironment();
        }
        // 解析命令行参数
        this.environment.parseCommandLineArgs(args);
        return this.environment;
    }

    /**
     * 配置 Banner（从环境配置中读取）
     */
    private void configureBanner(ConfigurableEnvironment env) {
        // 配置 Banner 模式
        String bannerModeStr = env.getProperty("spring.main.banner-mode");
        if (bannerModeStr != null && !bannerModeStr.isEmpty()) {
            try {
                this.bannerMode = Banner.Mode.valueOf(bannerModeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid banner mode: {}, using default CONSOLE", bannerModeStr);
            }
        }

        // 如果配置了 banner location，使用 ResourceBanner
        ResourceBanner resourceBanner = ResourceBanner.fromEnvironment(env);
        if (resourceBanner != null) {
            this.banner = resourceBanner;
            log.debug("Using custom banner from: {}", env.getProperty("spring.banner.location"));
        }
    }

    /**
     * 打印 Banner
     */
    private void printBanner(ConfigurableEnvironment env) {
        if (this.bannerMode == Banner.Mode.OFF) {
            return;
        }
        if (this.banner != null) {
            if (this.bannerMode == Banner.Mode.LOG) {
                this.banner.printBanner(env, this.primarySource, new LogPrintStream(log));
            } else {
                this.banner.printBanner(env, this.primarySource, System.out);
            }
        }
    }

    /**
     * 创建 ApplicationContext
     */
    protected ConfigurableApplicationContext createApplicationContext() {
        return new DefaultApplicationContext();
    }

    /**
     * 准备 Context
     */
    private void prepareContext(ConfigurableApplicationContext context,
                                ConfigurableEnvironment env,
                                String[] args) {
        context.setEnvironment(env);
        context.setPrimarySources(primarySources);
        context.setArgs(args);
    }

    /**
     * 刷新 Context（触发 Bean 扫描和初始化）
     */
    private void refreshContext(ConfigurableApplicationContext context) {
        context.refresh();
    }

    /**
     * 注册 JVM ShutdownHook
     */
    private void registerShutdownHook(ConfigurableApplicationContext context) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                log.info("JVM 退出钩子触发，关闭 ApplicationContext");
                ConfigurableApplicationContext ctx = CONTEXT.getAndSet(null);
                if (ctx != null) {
                    ctx.close();
                }
            } catch (Throwable t) {
                log.error("Shutdown hook 关闭 ApplicationContext 时发生错误", t);
            }
        }, "SpringApplication-ShutdownHook"));
    }

    /**
     * 打印启动完成信息
     */
    private void logStarted(Duration timeTaken) {
        if (logStartupInfo) {
            log.info("Started {} in {} seconds",
                    primarySource != null ? primarySource.getSimpleName() : "Application",
                    String.format("%.3f", timeTaken.toMillis() / 1000.0));
        }
    }

    /**
     * 执行所有 ApplicationRunner 和 CommandLineRunner
     */
    private void callRunners(ConfigurableApplicationContext context, String[] args) {
        context.callRunners(args);
    }

    // ================== Builder 模式配置 ==================

    /**
     * 设置 Banner 模式
     */
    public SpringApplication setBannerMode(Banner.Mode bannerMode) {
        this.bannerMode = bannerMode;
        return this;
    }

    /**
     * 设置自定义 Banner
     */
    public SpringApplication setBanner(Banner banner) {
        this.banner = banner;
        return this;
    }

    /**
     * 设置是否打印启动日志
     */
    public SpringApplication setLogStartupInfo(boolean logStartupInfo) {
        this.logStartupInfo = logStartupInfo;
        return this;
    }

    /**
     * 设置自定义 Environment
     */
    public SpringApplication setEnvironment(ConfigurableEnvironment environment) {
        this.environment = environment;
        return this;
    }

    /**
     * 添加额外的配置源
     */
    public SpringApplication addPrimarySources(Collection<Class<?>> additionalPrimarySources) {
        this.primarySources.addAll(additionalPrimarySources);
        return this;
    }

    // ================== 内部类 ==================

    /**
     * 日志输出流适配器
     */
    private static class LogPrintStream extends java.io.PrintStream {
        private final Logger logger;
        private final StringBuilder buffer = new StringBuilder();

        public LogPrintStream(Logger logger) {
            super(System.out);
            this.logger = logger;
        }

        @Override
        public void println(String x) {
            logger.info(x);
        }

        @Override
        public void print(String s) {
            buffer.append(s);
        }

        @Override
        public void println() {
            if (buffer.length() > 0) {
                logger.info(buffer.toString());
                buffer.setLength(0);
            }
        }
    }
}
