package com.xy.spring.context;


import com.xy.spring.annotations.SpringApplication;
import com.xy.spring.annotations.core.*;
import com.xy.spring.annotations.event.EventListener;
import com.xy.spring.aop.BeanPostProcessor;
import com.xy.spring.aop.ProxyBeanPostProcessor;
import com.xy.spring.config.YamlConfigLoader;
import com.xy.spring.core.BeanDefinition;
import com.xy.spring.core.InitializingBean;
import com.xy.spring.core.ProxyType;
import com.xy.spring.event.ApplicationEventBus;
import com.xy.spring.event.ApplicationEventPublisher;
import com.xy.spring.exception.CyclicDependencyException;
import com.xy.spring.exception.NoSuchBeanException;
import com.xy.spring.exception.TooMuchBeanException;
import com.xy.spring.exception.handler.ExceptionHandler;
import com.xy.spring.exception.handler.GlobalExceptionHandler;
import com.xy.spring.factory.BeanFactory;
import com.xy.spring.utils.ClassUtils;
import lombok.extern.slf4j.Slf4j;

import java.beans.Introspector;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * 精简版手写 Spring 容器，支持全局异常处理与循环依赖
 *
 * @param <T> 启动主类类型
 */
@Slf4j
public class ApplicationContext<T> implements BeanFactory {

    private static final String PROTOTYPE = "prototype";
    private static final String SINGLETON = "singleton";

    // 启动类，用于获取基础包路径
    private final Class<T> startupClass;

    // 运行 ApplicationRunner 的线程池
    private final ExecutorService runnerPool;

    // Bean 定义映射
    private final Map<String, BeanDefinition> definitions = new ConcurrentHashMap<>();

    // 完全初始化的单例缓存
    private final Map<String, Object> singletons = new ConcurrentHashMap<>();

    // 早期暴露单例，用于解决循环依赖
    private final Map<String, Object> earlySingletons = new ConcurrentHashMap<>();

    // 后置处理器列表
    private final List<BeanPostProcessor> postProcessors = new ArrayList<>();

    // 当前线程正在创建的 Bean 集合，用于循环依赖检测
    private final ThreadLocal<Set<String>> inCreation = ThreadLocal.withInitial(HashSet::new);

    // 全局异常处理器
    private final ExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    // 全局监听类
    private final ApplicationEventBus applicationEventBus = new ApplicationEventBus();

    /**
     * 构造：校验注解、加载配置、扫描注册、预加载单例、执行 Runner
     */
    public ApplicationContext(Class<T> startupClass, String[] args) {
        this.startupClass = startupClass;
        // 初始化固定大小线程池
        this.runnerPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> new Thread(r, "AppRunner")
        );
        validateAnnotation();           // 校验启动注解
        YamlConfigLoader.load();        // 加载 YAML 配置
        scanAndRegister();              // 扫描并注册 BeanDefinition
        initSingletons();               // 非懒加载单例预实例化
        registerEventListeners();       // 注册监听器
        runRunners(args);               // 异步执行 ApplicationRunner
    }

    // 添加 getEventPublisher 方法：
    public ApplicationEventPublisher getEventPublisher() {
        return applicationEventBus;
    }

    /**
     * 校验启动类是否标注 @XSpringApplication
     */
    private void validateAnnotation() {
        if (!startupClass.isAnnotationPresent(SpringApplication.class)) {
            throw new IllegalArgumentException("启动类缺少 @XSpringApplication 注解");
        }
    }

    /**
     * 扫描基础包并注册组件与配置 Bean
     */
    private void scanAndRegister() {
        try {
            // 获取扫描包路径
            String basePkg = Optional.of(startupClass.getAnnotation(SpringApplication.class).value())
                    .filter(v -> !v.isEmpty())
                    .orElse(startupClass.getPackageName());
            Set<Class<?>> classes = ClassUtils.scan(basePkg);

            // 添加代理后置处理器
            postProcessors.add(new ProxyBeanPostProcessor(classes));

            // 逐类注册
            for (Class<?> cls : classes) {
                registerComponent(cls);   // 注册 @Component/@Service
                registerConfigBean(cls);  // 注册 @Configuration 中的 @Bean
            }
        } catch (Exception e) {
            throw new RuntimeException("扫描注册失败", e);
        }
    }

    /**
     * 注册普通组件
     */
    private void registerComponent(Class<?> cls) throws Exception {
        if (!cls.isAnnotationPresent(Component.class) && !cls.isAnnotationPresent(Service.class)) return;
        String name = deriveBeanName(cls);
        // 构建定义并缓存
        BeanDefinition def = new BeanDefinition()
                .setType(cls)
                .setName(name)
                .setFullName(cls.getName())
                .setLazy(cls.isAnnotationPresent(Lazy.class))
                .setScope(determineScope(cls))
                .setProxyType(
                        Optional.ofNullable(cls.getAnnotation(Component.class))
                                .map(Component::proxy)
                                .orElse(ProxyType.NONE)
                );
        definitions.put(name, def);
        // 如果是后置处理器，则提前实例化并加入列表
        if (BeanPostProcessor.class.isAssignableFrom(cls)) {
            postProcessors.add((BeanPostProcessor) createBean(name, def));
        }
    }

    /**
     * 注册配置类中的 @Bean 方法
     */
    private void registerConfigBean(Class<?> cls) throws Exception {
        if (!cls.isAnnotationPresent(Configuration.class)) return;
        //Object configObj = instantiate(cls);
        for (Method m : cls.getDeclaredMethods()) {
            if (!m.isAnnotationPresent(Bean.class)) continue;
            String name = Optional.of(m.getAnnotation(Bean.class).value())
                    .filter(v -> !v.isEmpty())
                    .orElse(m.getName());
            Class<?> returnType = m.getReturnType();
            // 构建定义并缓存
            BeanDefinition def = new BeanDefinition()
                    .setType(returnType)
                    .setName(name)
                    .setFullName(returnType.getName())
                    .setLazy(returnType.isAnnotationPresent(Lazy.class))
                    .setScope(determineScope(returnType))
                    .setProxyType(ProxyType.NONE);
            definitions.put(name, def);
        }
    }

    /**
     * 导出 Bean 名称，优先注解值，否则驼峰名
     */
    private String deriveBeanName(Class<?> cls) {
        String val = Optional.of(
                Optional.ofNullable(cls.getAnnotation(Component.class))
                        .map(Component::value)
                        .orElse(Optional.ofNullable(cls.getAnnotation(Service.class))
                                .map(Service::value)
                                .orElse(""))
        ).orElse(cls.getName());
        return val.isEmpty() ? Introspector.decapitalize(cls.getSimpleName()) : val;
    }


    /**
     * 确定作用域，默认为 singleton
     */
    private String determineScope(Class<?> cls) {
        return Optional.ofNullable(cls.getAnnotation(Scope.class))
                .map(Scope::value)
                .filter(PROTOTYPE::equals)
                .orElse(SINGLETON);
    }

    /**
     * 预实例化所有非懒加载单例
     */
    private void initSingletons() {
        definitions.forEach((name, def) -> {
            if (SINGLETON.equals(def.getScope()) && !def.isLazy()) {
                getBean(name);
            }
        });
    }


    /**
     * 注册事件监听器方法
     */
    private void registerEventListeners() {
        definitions.forEach((name, def) -> {
            Class<?> cls = def.getType();
            for (Method method : cls.getDeclaredMethods()) {
                if (method.isAnnotationPresent(EventListener.class)) {
                    applicationEventBus.registerListener(getBean(name));
                }
            }
        });
    }


    @Override
    public Object getBean(String name) {
        BeanDefinition def = definitions.get(name);
        if (def == null) return null;
        return SINGLETON.equals(def.getScope())
                ? getSingleton(name, def)
                : createWithHandling(name, def);
    }

    @Override
    public Object getBean(String beanName, Class type) {
        if (type == null) {
            throw new IllegalStateException("bean类型不能为空！");
        }
        BeanDefinition beanDefinition = definitions.get(beanName);
        if (beanDefinition != null && type.equals(beanDefinition.getType())) {
            return getBean(beanDefinition.getType());
        }
        throw new NoSuchBeanException();
    }

    @Override
    public Object getBean(Class type) {
        // 按类型查找 BeanDefinition
        List<Map.Entry<String, BeanDefinition>> matches = definitions.entrySet().stream()
                .filter(e -> type.equals(e.getValue().getType()))
                .toList();
        if (matches.isEmpty()) {
            throw new NoSuchBeanException();
        }
        if (matches.size() > 1) {
            throw new TooMuchBeanException();
        }

        return getBean(matches.getFirst().getKey());
    }

    /**
     * 获取单例，支持早期曝光
     */
    private Object getSingleton(String name, BeanDefinition def) {

        if (earlySingletons.containsKey(name)) {
            return earlySingletons.get(name);
        }

        // 原子创建
        return singletons.computeIfAbsent(name, n -> createWithHandling(n, def));
    }

    /**
     * 包装异常处理的创建过程
     */
    private Object createWithHandling(String name, BeanDefinition def) {
        try {
            return createBean(name, def);
        } catch (Exception e) {
            exceptionHandler.handle(e, "创建 Bean[" + name + "] 失败");
            throw new RuntimeException(e);
        }
    }

    /**
     * 核心创建流程：实例化、早期曝光、后置处理、注入、初始化
     */
    private Object createBean(String name, BeanDefinition def) throws Exception {
        // 循环依赖检测
        if (!inCreation.get().add(name)) {
            throw new CyclicDependencyException("循环依赖: " + name);
        }
        try {
            // 实例化对象
            Object bean = instantiate(def.getType());
            // 早期曝光
            if (SINGLETON.equals(def.getScope())) {
                earlySingletons.put(name, bean);
            }
            // 初始化前后置处理
            bean = postProcessors.stream()
                    .reduce(bean, (b, p) -> p.postProcessBeforeInitialization(b, name, def.getProxyType()), (a, b) -> b);

            // 注入字段
            injectFields(bean);

            // 初始化回调
            invokeInitMethods(bean);

            // 初始化后后置处理
            bean = postProcessors.stream()
                    .reduce(bean, (b, p) -> p.postProcessAfterInitialization(b, name), (a, b) -> b);

            // 移除早期曝光
            earlySingletons.remove(name);

            return bean;

        } finally {

            inCreation.get().remove(name);
        }
    }

    /**
     * 实例化 bean，优先无参构造
     */
    private Object instantiate(Class<?> cls) throws Exception {
        for (Constructor<?> c : cls.getConstructors()) {
            if (c.getParameterCount() == 0) {
                return c.newInstance();
            }
        }
        Constructor<?> c = cls.getConstructors()[0];
        return c.newInstance(new Object[c.getParameterCount()]);
    }

    /**
     * 注入 @Autowired 与 @Value 注解字段
     */
    /**
     * 注入 @Autowired 与 @Value 注解字段
     */
    private void injectFields(Object bean) throws IllegalAccessException {
        // 遍历所有字段，支持按类型或按名称注入
        for (Field f : bean.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (f.isAnnotationPresent(Autowired.class)) {

                Object dep = getBean(f.getName());
                if (dep == null) {
                    dep = getBean(f.getType());
                }
                ;
                // 按类型注入
                if (dep == null) {
                    throw new NoSuchBeanException("无法注入字段 " + f.getName());
                }
                f.set(bean, dep);
            } else if (f.isAnnotationPresent(Value.class)) {
                Object val = YamlConfigLoader.get(f.getAnnotation(Value.class).value(), f);
                f.set(bean, val);
            }
        }
    }


    /**
     * 调用 InitializingBean 与 @PostConstruct 方法
     */
    private void invokeInitMethods(Object bean) throws Exception {
        if (bean instanceof InitializingBean) {
            ((InitializingBean) bean).afterPropertiesSet();
        }
        for (Method m : bean.getClass().getDeclaredMethods()) {
            if (m.isAnnotationPresent(PostConstruct.class)) {
                if (m.getParameterCount() > 0) {
                    throw new IllegalStateException("@PostConstruct 方法不能带参数");
                }
                m.setAccessible(true);
                m.invoke(bean);
                break;
            }
        }
    }

    /**
     * 执行所有 ApplicationRunner 并优雅关闭线程池
     */
    private void runRunners(String[] args) {
        ApplicationArguments appArgs = new ApplicationArguments(args);
        definitions.values().stream()
                .filter(def -> ApplicationRunner.class.isAssignableFrom(def.getType()))
                .map(def -> (ApplicationRunner) getBean(def.getName()))
                .forEach(r -> runnerPool.submit(() -> safeRun(r, appArgs)));
        shutdownPool();
    }

    /**
     * 安全执行 Runner，捕获异常
     */
    private void safeRun(ApplicationRunner runner, ApplicationArguments args) {
        try {
            runner.run(args);
        } catch (Exception e) {
            exceptionHandler.handle(e, "Runner 执行异常");
        }
    }

    /**
     * 优雅关闭线程池
     */
    private void shutdownPool() {
        runnerPool.shutdown();
        try {
            if (!runnerPool.awaitTermination(120, TimeUnit.SECONDS)) {
                runnerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            runnerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

