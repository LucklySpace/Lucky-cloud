package com.xy.lucky.spring;

import com.xy.lucky.spring.context.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * XSpringApplication - 应用启动器（单例容器管理）
 * <p>
 * 说明：
 * - 负责创建并持有全局 ApplicationContext 实例（线程安全）。
 * - 提供 run(...) 入口，返回已创建的 ApplicationContext。
 * - 提供 set/get/close 等辅助方法。
 * <p>
 * 设计原则：
 * - 使用 CAS 保证并发场景下只会初始化一次（双检查 + compareAndSet）。
 * - 如果构造失败，会清理并抛出异常，避免半初始化状态残留。
 * - 提供可选的覆盖接口 setContextIfAbsent，和显式 closeContext。
 */
public final class XSpringApplication {

    private static final Logger log = LoggerFactory.getLogger(XSpringApplication.class);

    /**
     * 使用 AtomicReference 保存全局 context，保证并发下的可见性与 CAS 操作。
     */
    private static final AtomicReference<ApplicationContext<?>> CONTEXT = new AtomicReference<>();

    private XSpringApplication() {
        // 工具类，禁止实例化
    }

    /**
     * 获取当前全局 ApplicationContext。
     *
     * @throws IllegalStateException 如果 context 尚未初始化
     */
    @SuppressWarnings("unchecked")
    public static <T> ApplicationContext<T> getContext() {
        ApplicationContext<?> ctx = CONTEXT.get();
        if (ctx == null) {
            throw new IllegalStateException("ApplicationContext 尚未初始化，请先调用 XSpringApplication.run(...) 或 setContext(...)");
        }
        return (ApplicationContext<T>) ctx;
    }

    /**
     * 直接设置 context（如果已有则抛出异常）
     *
     * @throws IllegalStateException 如果已有 context
     */
    public static void setContext(ApplicationContext<?> ctx) {
        Objects.requireNonNull(ctx, "ctx 不能为 null");
        if (!CONTEXT.compareAndSet(null, ctx)) {
            throw new IllegalStateException("ApplicationContext 已存在，拒绝重复设置（如需覆盖请使用 setContextForce）");
        }
        log.info("ApplicationContext 已设置: {}", ctx.getClass().getName());
        registerShutdownHookIfNeeded(ctx);
    }

    /**
     * 获取当前 context 的 Optional 包装（如果未初始化，不抛异常）
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<ApplicationContext<T>> getContextOptional() {
        return Optional.ofNullable((ApplicationContext<T>) CONTEXT.get());
    }

    /**
     * 判断容器是否已启动
     */
    public static boolean isRunning() {
        return CONTEXT.get() != null;
    }

    /**
     * 线程安全地设置全局 ApplicationContext（仅在 absent 时设置成功）
     *
     * @param ctx ApplicationContext 实例，非 null
     * @return 如果设置成功返回 true；如果已有 context 存在则返回 false（不覆盖）
     */
    public static boolean setContextIfAbsent(ApplicationContext<?> ctx) {
        Objects.requireNonNull(ctx, "ctx 不能为 null");
        boolean ok = CONTEXT.compareAndSet(null, ctx);
        if (ok) {
            log.info("ApplicationContext 已设置（CAS 成功）: {}", ctx.getClass().getName());
            registerShutdownHookIfNeeded(ctx);
        } else {
            log.warn("setContextIfAbsent: 已存在 ApplicationContext，设置被忽略");
        }
        return ok;
    }

    /**
     * 强制设置全局 ApplicationContext（会覆盖已有的）
     * 使用此方法时请小心，默认情况下不建议覆盖已有 context。
     *
     * @param ctx 新的 ApplicationContext（非 null）
     */
    public static void setContextForce(ApplicationContext<?> ctx) {
        Objects.requireNonNull(ctx, "ctx 不能为 null");
        ApplicationContext<?> previous = CONTEXT.getAndSet(ctx);
        if (previous != null) {
            // 发现已有旧的 context，尝试优雅关闭（最好由调用者决定是否关闭）
            try {
                previous.close();
                log.info("已关闭被覆盖的旧 ApplicationContext");
            } catch (Exception e) {
                log.warn("覆盖旧 ApplicationContext 时关闭失败", e);
            }
        }
        log.info("ApplicationContext 被强制设置为: {}", ctx.getClass().getName());
        registerShutdownHookIfNeeded(ctx);
    }

    /**
     * 启动并返回 ApplicationContext 实例（线程安全、可重入）
     * <p>
     * 使用方式：
     * ApplicationContext<MyApp> ctx = XSpringApplication.run(MyApp.class, args);
     * <p>
     * 行为：
     * - 如果已有 context 存在，直接返回已有实例（不会重复创建）。
     * - 否则创建新的 ApplicationContext 并尝试设置为全局 context（CAS）。
     * - 若在并发情况下其它线程已经创建并设置了 context，则会关闭当前创建的临时实例并返回已存在的实例。
     *
     * @param startupClass 启动类（含 @SpringApplication 注解）
     * @param args         启动参数
     * @param <T>          启动类类型（用于返回泛型）
     * @return 已存在或新创建的 ApplicationContext
     */
    @SuppressWarnings("unchecked")
    public static <T> ApplicationContext<T> run(Class<T> startupClass, String[] args) {
        Objects.requireNonNull(startupClass, "startupClass 不能为 null");

        // 如果已存在，直接返回（避免重复创建）
        ApplicationContext<?> existing = CONTEXT.get();
        if (existing != null) {
            log.warn("ApplicationContext 已存在，run() 返回现有实例: {}", existing.getClass().getName());
            return (ApplicationContext<T>) existing;
        }

        // 尝试创建新的 context（可能在并发下被其他线程替换）
        ApplicationContext<T> created = null;
        try {
            log.info("初始化 ApplicationContext: {}", startupClass.getName());
            created = new ApplicationContext<>(startupClass, args);
            // 尝试用 CAS 设置为全局 context
            if (CONTEXT.compareAndSet(null, created)) {
                log.info("ApplicationContext 初始化并设置为全局实例: {}", created.getClass().getName());
                registerShutdownHookIfNeeded(created);
                return created;
            } else {
                // 说明其他线程已经设置了 context：关闭当前创建的实例并返回现有实例
                log.warn("并发检测到已有 ApplicationContext，释放本次创建的实例并返回已有实例");
                try {
                    created.close();
                } catch (Exception ex) {
                    log.warn("关闭多余 ApplicationContext 实例时出错", ex);
                }
                return (ApplicationContext<T>) CONTEXT.get();
            }
        } catch (RuntimeException | Error e) {
            // 构造期间出现错误：尝试清理并抛出异常（避免半初始化残留）
            log.error("ApplicationContext 创建失败: {}", e.getMessage(), e);
            if (created != null) {
                try {
                    created.close();
                } catch (Exception ex) {
                    log.warn("创建失败后关闭 ApplicationContext 时出错", ex);
                }
            }
            throw e;
        } catch (Exception e) {
            // 若 ApplicationContext 构造器声明了受检异常，则包装为 RuntimeException
            log.error("ApplicationContext 创建失败 (checked): {}", e.getMessage(), e);
            if (created != null) {
                try {
                    created.close();
                } catch (Exception ex) {
                    log.warn("创建失败后关闭 ApplicationContext 时出错", ex);
                }
            }
            throw new RuntimeException("ApplicationContext 创建失败", e);
        }
    }

    /**
     * 通过 supplier 延迟创建并设置 ApplicationContext（适合自定义构造逻辑）
     * <p>
     * 如果已有 context 存在则返回已有实例；否则调用 supplier 创建并尝试设置（CAS）。
     */
    @SuppressWarnings("unchecked")
    public static <T> ApplicationContext<T> runWithSupplier(Supplier<ApplicationContext<T>> supplier) {
        Objects.requireNonNull(supplier, "supplier 不能为 null");
        ApplicationContext<?> existing = CONTEXT.get();
        if (existing != null) {
            log.warn("ApplicationContext 已存在，runWithSupplier 返回现有实例: {}", existing.getClass().getName());
            return (ApplicationContext<T>) existing;
        }

        ApplicationContext<T> created = null;
        try {
            created = supplier.get();
            if (created == null) throw new IllegalStateException("supplier 返回了 null 的 ApplicationContext");
            if (CONTEXT.compareAndSet(null, created)) {
                registerShutdownHookIfNeeded(created);
                return created;
            } else {
                // 其它线程已设置
                try {
                    created.close();
                } catch (Exception ex) {
                    log.warn("关闭多余 ApplicationContext 实例时出错", ex);
                }
                return (ApplicationContext<T>) CONTEXT.get();
            }
        } catch (RuntimeException | Error e) {
            log.error("runWithSupplier 创建 ApplicationContext 失败", e);
            if (created != null) {
                try {
                    created.close();
                } catch (Exception ex) {
                    log.warn("关闭失败", ex);
                }
            }
            throw e;
        }
    }

    /**
     * 关闭并清理全局 ApplicationContext（如果存在）
     * - 通过调用 ApplicationContext.close()
     * - 将全局引用置为 null
     */
    public static void closeContext() {
        ApplicationContext<?> ctx = CONTEXT.getAndSet(null);
        if (ctx == null) {
            log.info("closeContext: 无可关闭的 ApplicationContext");
            return;
        }
        try {
            log.info("关闭全局 ApplicationContext: {}", ctx.getClass().getName());
            ctx.close();
        } catch (Exception e) {
            log.warn("关闭 ApplicationContext 时发生异常", e);
        }
    }

    /**
     * 注册 JVM shutdown hook（仅在尚未注册且应用尚未关闭时添加），
     * 以保证在 JVM 退出时尝试优雅关闭 ApplicationContext。
     */
    private static void registerShutdownHookIfNeeded(ApplicationContext<?> ctx) {
        // 为简洁起见：总是注册一个 hook（重复注册不会有害），但避免频繁注册可以通过标志控制
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                log.info("JVM 退出钩子触发，尝试关闭 ApplicationContext");
                // 使用 getAndSet(null) 确保只关闭一次
                ApplicationContext<?> existing = CONTEXT.getAndSet(null);
                if (existing != null) {
                    existing.close();
                }
            } catch (Throwable t) {
                log.error("Shutdown hook 关闭 ApplicationContext 时发生错误", t);
            }
        }, "XSpringApplication-ShutdownHook"));
    }
}
