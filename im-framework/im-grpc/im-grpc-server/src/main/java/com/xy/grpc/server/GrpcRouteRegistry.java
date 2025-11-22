package com.xy.grpc.server;


import com.xy.grpc.server.annotation.GrpcRoute;
import com.xy.grpc.server.annotation.GrpcRouteMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Delay scanning and registering GrpcRoute at ContextRefreshedEvent,
 * avoiding early calls to getBeansWithAnnotation that may prematurely create other beans (circular dependencies).
 * <p>
 * 延迟在 ContextRefreshedEvent 时扫描并注册 GrpcRoute，
 * 避免在容器很早期调用 getBeansWithAnnotation 导致提前创建其它 bean（循环依赖）。
 */
@Component
public class GrpcRouteRegistry implements ApplicationContextAware, SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(GrpcRouteRegistry.class);

    // Route mappings / 路由映射
    private final Map<String, GrpcRouteHandler> routes = new ConcurrentHashMap<>();
    // Application context / 应用上下文
    private ApplicationContext applicationContext;
    // Running status / 运行状态
    private volatile boolean running = false;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void start() {
        if (!running) {
            try {
                registerRoutes();
                running = true;
                log.info("GrpcRouteRegistry: route registration completed. total routes = {}", routes.size());
                // gRPC路由注册表：路由注册完成
            } catch (Exception e) {
                log.error("GrpcRouteRegistry: failed to register routes", e);
                // gRPC路由注册表：路由注册失败
            }
        }
    }

    @Override
    public void stop() {
        running = false;
        routes.clear();
        log.info("GrpcRouteRegistry stopped and routes cleared");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Register routes from annotated beans
     * 从带注解的Bean中注册路由
     */
    private void registerRoutes() {
        if (applicationContext == null) {
            log.warn("ApplicationContext is null, skip registering routes");
            // ApplicationContext为空，跳过路由注册
            return;
        }

        // Using getBeansWithAnnotation here is safe: at ContextRefreshedEvent stage,
        // all singleton beans should have been created
        // 此处使用 getBeansWithAnnotation 是安全的：在 ContextRefreshedEvent 阶段，所有单例 bean 应当已完成创建
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);
        if (beans == null || beans.isEmpty()) {
            log.info("No component beans found to scan for @GrpcRoute");
            // 未找到需要扫描@GrpcRoute的组件Bean
            return;
        }

        int routeCount = 0;
        for (Object bean : beans.values()) {
            // Check @GrpcRouteMapping annotation on class
            // 检查类上的 @GrpcRouteMapping 注解
            GrpcRouteMapping classMapping = bean.getClass().getAnnotation(GrpcRouteMapping.class);
            String classPath = "";
            if (classMapping != null) {
                classPath = normalize(classMapping.value());
            }

            Method[] methods = bean.getClass().getMethods();
            for (Method m : methods) {
                GrpcRoute ann = m.getAnnotation(GrpcRoute.class);
                if (ann != null) {
                    String path = normalize(ann.value());
                    // If class has @GrpcRouteMapping, combine paths
                    // 如果类上有 @GrpcRouteMapping，则组合路径
                    if (!classPath.isEmpty()) {
                        if (path.equals("/")) {
                            path = classPath;
                        } else {
                            path = classPath + (path.startsWith("/") ? path : "/" + path);
                        }
                    }
                    routes.put(path, new GrpcRouteHandler(bean, m));
                    log.debug("Registered grpc route: {} -> {}#{}", path, bean.getClass().getName(), m.getName());
                    // 已注册gRPC路由
                    routeCount++;
                }
            }
        }

        log.info(" Total registered  {}  gRPC routes", routeCount);
    }

    /**
     * Normalize path string
     * 规范化路径字符串
     *
     * @param p Path string / 路径字符串
     * @return Normalized path / 规范化路径
     */
    private String normalize(String p) {
        if (p == null || p.trim().isEmpty()) {
            log.warn("检测到空路径，使用默认根路径");
            // Detected empty path, using default root path
            return "/";
        }
        return p.startsWith("/") ? p : "/" + p.trim();
    }

    /**
     * Find route handler by path
     * 根据路径查找路由处理器
     *
     * @param path Route path / 路由路径
     * @return Route handler or null if not found / 路由处理器，如果未找到则返回null
     */
    public GrpcRouteHandler find(String path) {
        String normalizedPath = normalize(path);
        GrpcRouteHandler handler = routes.get(normalizedPath);
        if (handler == null) {
            log.debug(" No handler found for path : {}", normalizedPath);
            //未找到路径对应的处理器
        }
        return handler;
    }

    /**
     * Get the total number of currently registered routes
     * 获取当前注册的所有路由数量
     *
     * @return Number of routes / 路由数量
     */
    public int getRouteCount() {
        return routes.size();
    }
}