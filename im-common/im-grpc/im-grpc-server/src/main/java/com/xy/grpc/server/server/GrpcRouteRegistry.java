package com.xy.grpc.server.server;



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
 * 延迟在 ContextRefreshedEvent 时扫描并注册 GrpcRoute，
 * 避免在容器很早期调用 getBeansWithAnnotation 导致提前创建其它 bean（循环依赖）。
 */
@Component
public class GrpcRouteRegistry implements ApplicationContextAware, SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(GrpcRouteRegistry.class);

    private final Map<String, GrpcRouteHandler> routes = new ConcurrentHashMap<>();
    private ApplicationContext applicationContext;
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
            } catch (Exception e) {
                log.error("GrpcRouteRegistry: failed to register routes", e);
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

    private void registerRoutes() {
        if (applicationContext == null) {
            log.warn("ApplicationContext is null, skip registering routes");
            return;
        }

        // 此处使用 getBeansWithAnnotation 是安全的：在 ContextRefreshedEvent 阶段，所有单例 bean 应当已完成创建
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Component.class);
        if (beans == null || beans.isEmpty()) {
            log.info("No component beans found to scan for @GrpcRoute");
            return;
        }

        int routeCount = 0;
        for (Object bean : beans.values()) {
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
                    routeCount++;
                }
            }
        }

        log.info("总共注册了 {} 个gRPC路由", routeCount);
    }

    private String normalize(String p) {
        if (p == null || p.trim().isEmpty()) {
            log.warn("检测到空路径，使用默认根路径");
            return "/";
        }
        return p.startsWith("/") ? p : "/" + p.trim();
    }

    public GrpcRouteHandler find(String path) {
        String normalizedPath = normalize(path);
        GrpcRouteHandler handler = routes.get(normalizedPath);
        if (handler == null) {
            log.debug("未找到路径对应的处理器: {}", normalizedPath);
        }
        return handler;
    }

    /**
     * 获取当前注册的所有路由数量
     *
     * @return 路由数量
     */
    public int getRouteCount() {
        return routes.size();
    }
}