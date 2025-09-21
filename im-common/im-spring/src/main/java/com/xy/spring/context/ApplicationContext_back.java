package com.xy.spring.context;


import com.xy.spring.annotations.SpringApplication;
import com.xy.spring.annotations.core.*;
import com.xy.spring.annotations.event.EventListener;
import com.xy.spring.aop.AsyncBeanPostProcessor;
import com.xy.spring.aop.BeanPostProcessor;
import com.xy.spring.core.BeanDefinition;
import com.xy.spring.core.DisposableBean;
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
import com.xy.spring.utils.YamlConfigLoader;
import lombok.extern.slf4j.Slf4j;

import java.beans.Introspector;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;


/**
 * 精简版手写 Spring 容器，支持全局异常处理与循环依赖 * * @param <T> 启动主类类型
 */
@Slf4j
public class ApplicationContext_back<T> implements BeanFactory<T>, AutoCloseable {

    private static final String PROTOTYPE = "prototype";
    private static final String SINGLETON = "singleton";

    private final Class<T> startupClass;
    private final ExecutorService runnerPool;

    // bean name -> definition
    private final Map<String, BeanDefinition> definitions = new ConcurrentHashMap<>();
    // fully initialized singletons
    private final Map<String, Object> singletons = new ConcurrentHashMap<>();
    // early-exposed raw instances (保留为兼容，但优先使用 factories)
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();
    // early reference factories（关键）：name -> ObjectFactory (可返回代理)
    private final Map<String, ObjectFactory<?>> earlySingletonFactories = new ConcurrentHashMap<>();
    // bean post processors (实例化后的对象增强用)
    private final List<BeanPostProcessor> postProcessors = new CopyOnWriteArrayList<>();
    // creation lock map，用于并发创建控制（每个 bean 一个锁对象）
    private final ConcurrentHashMap<String, Object> creationLocks = new ConcurrentHashMap<>();
    // 当前线程正在创建的 Bean 名称集合，用于循环依赖检测（线程本地）
    private final ThreadLocal<Set<String>> inCreation = ThreadLocal.withInitial(HashSet::new);

    private final ExceptionHandler exceptionHandler = new GlobalExceptionHandler();
    private final ApplicationEventBus applicationEventBus = new ApplicationEventBus();

    // 标记容器是否已关闭
    private volatile boolean closed = false;

    /**
     * 构造函数：只做启动流程（不在构造中等待 ApplicationRunner 完成）
     */
    public ApplicationContext_back(Class<T> startupClass, String[] args) {
        this.startupClass = startupClass;
        // runnerPool 交由 close 时关闭（避免在构造中阻塞）
        this.runnerPool = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                r -> new Thread(r, "AppRunner")
        );

        validateAnnotation();        // 校验启动注解
        YamlConfigLoader.load();     // 加载配置
        scanAndRegister();           // 扫描并注册 BeanDefinition
        instantiateAllBeanPostProcessors(); // 先实例化所有 BPP（重要：BPP 要尽早）
        initSingletons();            // 预实例化所有非懒加载单例（会用到提前注册的 BPP）
        registerEventListeners();    // 注册事件监听器（基于 bean）
        runRunners(args);            // 提交 runners，只提交任务，不等待完成

        // 注册 JVM ShutdownHook，实现自动关闭（"自动 GC"）
        Runtime.getRuntime().addShutdownHook(new Thread(this::close, "AppContext-ShutdownHook"));
    }

    public Executor getTaskExecutor() {
        return runnerPool;
    }

    /**
     * 对外暴露事件发布器
     */
    public ApplicationEventPublisher getEventPublisher() {
        return applicationEventBus;
    }

    /**
     * 验证启动类注解
     */
    private void validateAnnotation() {
        if (!startupClass.isAnnotationPresent(SpringApplication.class)) {
            throw new IllegalArgumentException("启动类缺少 @SpringApplication 注解");
        }
    }

    /**
     * 扫描并注册组件与配置类中的 @Bean
     * 保持原有逻辑，但不在这里实例化 BeanPostProcessor（只记录 BeanDefinition）
     */
    private void scanAndRegister() {
        try {
            String basePkg = Optional.of(startupClass.getAnnotation(SpringApplication.class).value())
                    .filter(v -> !v.isEmpty())
                    .orElse(startupClass.getPackageName());
            Set<Class<?>> classes = ClassUtils.scan(basePkg);

            // 使用并行流注册（替换原 for 循环）
            classes.parallelStream().forEach(cls -> {
                registerComponentDefinition(cls);
                registerConfigBeanDefinition(cls);
            });

            // 如果启用异步支持，注册异步相关的后置处理器
            if (startupClass.isAnnotationPresent(EnableAsync.class)) {
                registerAsyncBeanPostProcessor();
            }

            // 保留对 AOP 代理后置处理器的注册（如果你有 ProxyBeanPostProcessor 的实现）
            // 我们不直接实例化它，而是在 instantiateAllBeanPostProcessors 时统一处理
        } catch (Exception e) {
            throw new RuntimeException("扫描注册失败", e);
        }
    }
//    private void scanAndRegister() {
//        try {
//            String basePkg = Optional.of(startupClass.getAnnotation(SpringApplication.class).value())
//                    .filter(v -> !v.isEmpty())
//                    .orElse(startupClass.getPackageName());
//            Set<Class<?>> classes = ClassUtils.scan(basePkg);
//
//            // 先收集 BPP 的定义；不要在此处立即实例化所有实现类
//            for (Class<?> cls : classes) {
//                registerComponentDefinition(cls);
//                registerConfigBeanDefinition(cls);
//            }
//
//            // 如果启用异步支持，注册异步相关的后置处理器
//            if (startupClass.isAnnotationPresent(EnableAsync.class)) {
//                registerAsyncBeanPostProcessor();
//            }
//
//            // 保留对 AOP 代理后置处理器的注册（如果你有 ProxyBeanPostProcessor 的实现）
//            // 我们不直接实例化它，而是在 instantiateAllBeanPostProcessors 时统一处理
//        } catch (Exception e) {
//            throw new RuntimeException("扫描注册失败", e);
//        }
//    }

    /**
     * 注册异步相关的BeanPostProcessor
     */
    private void registerAsyncBeanPostProcessor() {
        // 注册异步代理创建器
        String name = "asyncBeanPostProcessor";
        BeanDefinition def = new BeanDefinition()
                .setType(AsyncBeanPostProcessor.class)
                .setName(name)
                .setFullName(AsyncBeanPostProcessor.class.getName())
                .setLazy(false)
                .setScope(SINGLETON)
                .setProxyType(ProxyType.NONE);
        definitions.put(name, def);
    }

    /**
     * 将 @Component / @Service 转为 BeanDefinition（不立即实例化）
     */
    private void registerComponentDefinition(Class<?> cls) {
        if (!cls.isAnnotationPresent(Component.class) && !cls.isAnnotationPresent(Service.class)) return;
        String name = deriveBeanName(cls);
        BeanDefinition def = new BeanDefinition()
                .setType(cls)
                .setName(name)
                .setFullName(cls.getName())
                .setLazy(cls.isAnnotationPresent(Lazy.class))
                .setScope(determineScope(cls))
                .setProxyType(Optional.ofNullable(cls.getAnnotation(Component.class))
                        .map(Component::proxy)
                        .orElse(ProxyType.NONE));
        definitions.put(name, def);
    }

    /**
     * 将 @Configuration 中的 @Bean 注册为 BeanDefinition（不立即实例化配置类里的 bean）
     */
    private void registerConfigBeanDefinition(Class<?> cls) {
        if (!cls.isAnnotationPresent(Configuration.class)) return;
        // 配置类本身作为一个普通 bean 定义（如果尚未注册）
        String cfgName = deriveBeanName(cls);
        if (!definitions.containsKey(cfgName)) {
            BeanDefinition cfgDef = new BeanDefinition()
                    .setType(cls)
                    .setName(cfgName)
                    .setFullName(cls.getName())
                    .setLazy(false)
                    .setScope(SINGLETON)
                    .setProxyType(ProxyType.NONE);
            definitions.put(cfgName, cfgDef);
        }

        // 注册其 @Bean 方法为 BeanDefinition（工厂方法模式）
        for (Method m : cls.getDeclaredMethods()) {
            if (!m.isAnnotationPresent(Bean.class)) continue;
            String name = Optional.of(m.getAnnotation(Bean.class).value())
                    .filter(v -> !v.isEmpty())
                    .orElse(m.getName());
            BeanDefinition def = new BeanDefinition()
                    .setType(m.getReturnType())
                    .setName(name)
                    .setFullName(m.getReturnType().getName())
                    .setLazy(m.isAnnotationPresent(Lazy.class))
                    .setScope(determineScope(m.getReturnType()))
                    .setFactoryMethod(m)
                    .setFactoryBeanClass(cls) // 仅记录配置类类型，实际实例化时会创建 config bean 并注入 factoryBean
                    .setProxyType(ProxyType.NONE);
            definitions.put(name, def);
        }
    }

    /**
     * 根据注解或类名生成 Bean 名称
     */
    private String deriveBeanName(Class<?> cls) {
        String val = Optional.ofNullable(cls.getAnnotation(Component.class)).map(Component::value)
                .filter(v -> !v.isEmpty())
                .orElse(Optional.ofNullable(cls.getAnnotation(Service.class)).map(Service::value).orElse(""));
        return val.isEmpty() ? Introspector.decapitalize(cls.getSimpleName()) : val;
    }

    /**
     * 确定作用域
     */
    private String determineScope(Class<?> cls) {
        return Optional.ofNullable(cls.getAnnotation(Scope.class))
                .map(Scope::value)
                .filter(PROTOTYPE::equals)
                .orElse(SINGLETON);
    }

    /**
     * 预实例化 (non-lazy) 单例 Bean（会触发创建逻辑）
     * <p>
     * 注意：实例化过程中可以安全使用早期引用工厂，且我们已在 instantiateAllBeanPostProcessors 提前准备好 BPP。
     */
    private void initSingletons() {
        definitions.forEach((name, def) -> {
            if (SINGLETON.equals(def.getScope()) && !def.isLazy()) {
                getBean(name);
            }
        });
    }

    /**
     * 先实例化所有 BeanPostProcessor 类型的 bean（确保它们在普通 bean 创建前可用）
     */
    private void instantiateAllBeanPostProcessors() {
        // 找到所有实现 BeanPostProcessor 的定义（但不要在扫描阶段就实例化）
        List<String> bppNames = definitions.entrySet().stream()
                .filter(e -> BeanPostProcessor.class.isAssignableFrom(e.getValue().getType()))
                .map(Map.Entry::getKey)
                .toList();

        for (String name : bppNames) {
            try {
                Object obj = getBean(name); // 使用 getBean，会走单例创建逻辑
                if (obj instanceof BeanPostProcessor) {
                    postProcessors.add((BeanPostProcessor) obj);
                }
            } catch (Exception e) {
                log.warn("加载 BeanPostProcessor 失败: " + name, e);
            }
        }
    }

    /**
     * 注册标注了 @EventListener 的 bean 为事件监听器
     */
    private void registerEventListeners() {
        for (Map.Entry<String, BeanDefinition> e : definitions.entrySet()) {
            BeanDefinition def = e.getValue();
            if (Arrays.stream(def.getType().getDeclaredMethods()).anyMatch(m -> m.isAnnotationPresent(EventListener.class))) {
                Object bean = getBean(e.getKey());
                applicationEventBus.registerListener(bean);
            }
        }
    }

    /**
     * 根据名称获取 Bean（入口）
     */
    @Override
    public Object getBean(String name) {
        if (closed) throw new IllegalStateException("ApplicationContext 已关闭");
        BeanDefinition def = definitions.get(name);
        if (def == null) return null;
        if (SINGLETON.equals(def.getScope())) {
            return getSingleton(name, def);
        } else {
            // prototype 每次创建一个新实例
            return createWithHandling(name, def);
        }
    }

    /**
     * getBean by name + type（修复版）
     * 返回 bean 并做类型校验
     */
    @Override
    public Object getBean(String beanName, Class type) {
        if (type == null) throw new IllegalStateException("bean类型不能为空！");
        BeanDefinition bd = definitions.get(beanName);
        if (bd != null && type.isAssignableFrom(bd.getType())) {
            Object bean = getBean(beanName);
            if (bean == null) throw new NoSuchBeanException();
            return bean;
        }
        throw new NoSuchBeanException();
    }

    /**
     * 按类型查找单一 Bean（修复了之前 equals 的问题，支持 isAssignableFrom）
     */
    @Override
    public Object getBean(Class type) {
        List<Map.Entry<String, BeanDefinition>> matches = definitions.entrySet().stream()
                .filter(e -> type.isAssignableFrom(e.getValue().getType()))
                .toList();
        if (matches.isEmpty()) throw new NoSuchBeanException();
        if (matches.size() > 1) throw new TooMuchBeanException();
        return getBean(matches.getFirst().getKey());
    }

    /**
     * 单例获取：优先返回 fully-initialized singletons，其次早期引用（通过 factory），否则创建
     */
    private Object getSingleton(String name, BeanDefinition def) {
        Object singleton = singletons.get(name);
        if (singleton != null) return singleton;

        Object earlyRef = getEarlyReferenceIfPossible(name);
        if (earlyRef != null) return earlyRef;

        // 使用 computeIfAbsent 简化锁创建
        Object lock = creationLocks.computeIfAbsent(name, k -> new Object());
        synchronized (lock) {
            singleton = singletons.get(name); // double-check
            if (singleton != null) return singleton;

            Object created = createWithHandling(name, def);
            if (created != null) {
                singletons.put(name, created); // 无需 putIfAbsent，已在锁内
            }
            return created;
        }
    }

    /**
     * 包装 createBean 的异常处理
     */
    private Object createWithHandling(String name, BeanDefinition def) {
        try {
            return createBean(name, def);
        } catch (Exception e) {
            // 全局异常处理器处理后，抛出运行时异常并保留原始异常为 cause
            exceptionHandler.handle(e, "创建 Bean[" + name + "] 失败");
            throw new RuntimeException("创建 Bean[" + name + "] 失败", e);
        }
    }

    /**
     * 获取早期引用（优先 factory → earlySingletonObjects）
     */
    private Object getEarlyReferenceIfPossible(String name) {
        ObjectFactory<?> factory = earlySingletonFactories.get(name);
        if (factory != null) {
            try {
                Object o = factory.getObject();
                if (o != null) return o;
            } catch (Exception ignored) {
            }
        }
        return earlySingletonObjects.get(name);
    }

    /**
     * 核心创建流程（改进版）
     * - 支持工厂方法、构造器注入与字段注入
     * - 单例在实例化后注册 earlySingletonFactory（由 postProcessors 提供早期代理）
     * - 使用 success 标志确保异常路径清理 early maps
     */
    private Object createBean(String name, BeanDefinition def) throws Exception {
        Set<String> creating = inCreation.get();
        boolean isProto = PROTOTYPE.equals(def.getScope());

        // 循环依赖检测：如果当前线程已经在创建，尝试返回早期引用
        if (creating.contains(name)) {

            if (isProto) {
                throw new CyclicDependencyException("原型循环依赖: " + name + ", 链: " + creating);
            } else {
                Object early = getEarlyReferenceIfPossible(name);
                if (early != null) return early;
                // 如果还没有早期引用，抛出明确异常（避免返回 null 导致隐晦 NPE）
                throw new CyclicDependencyException("单例循环依赖但尚无早期引用: " + name + ", 链: " + creating);
            }
        }

        creating.add(name);
        boolean success = false;
        Object bean = null;
        try {
            // ---------- 实例化阶段 ----------
            bean = instantiateBean(def);

            // ---------- 早期曝光（单例） ----------
            exposeEarlySingletonIfNeeded(name, def, bean);

            // ---------- 前置处理（postProcessBeforeInitialization） ----------
            bean = applyPostProcessBeforeInitialization(bean, name, def);

            // ---------- 字段注入 ----------
            injectFields(bean);

            // ---------- 初始化回调 ----------
            invokeInitMethods(bean);

            // ---------- 后置处理（postProcessAfterInitialization） ----------
            bean = applyPostProcessAfterInitialization(bean, name);

            // ---------- 成功：将最终对象放入 singletons 并清除 early maps ----------
            if (SINGLETON.equals(def.getScope())) {
                singletons.put(name, bean);
                earlySingletonFactories.remove(name);
                earlySingletonObjects.remove(name);
            }

            success = true;
            return bean;
        } finally {
            // 无论成功或失败，都要从 inCreation 中移除
            creating.remove(name);

            // 失败时清理早期缓存，避免残留脏数据影响后续
            if (!success) {
                earlySingletonFactories.remove(name);
                earlySingletonObjects.remove(name);
            }
        }
    }

    private Object instantiateBean(BeanDefinition def) throws Exception {
        if (def.hasFactoryMethod()) {
            // 若是配置类的 @Bean，先确保 factory bean 实例化
            Object factoryBean = null;
            if (def.getFactoryBeanClass() != null) {
                // factoryBean name derive from class
                String cfgName = deriveBeanName(def.getFactoryBeanClass());
                factoryBean = getBean(cfgName);
            } else {
                factoryBean = def.getFactoryBean();
            }
            return def.getFactoryMethod().invoke(factoryBean, resolveMethodArgs(def.getFactoryMethod()));
        } else {
            return instantiate(def.getType());
        }
    }

    private void exposeEarlySingletonIfNeeded(String name, BeanDefinition def, Object bean) {
        if (def.getScope().equals(SINGLETON)) {
            final Object rawBean = bean;
            earlySingletonFactories.put(name, () -> {
                Object early = rawBean;
                for (BeanPostProcessor bpp : postProcessors) {
                    try {
                        early = bpp.getEarlyBeanReference(early, name); // 假设 BPP 有此方法；若无，则 BPP 可实现默认返回自身
                    } catch (NoSuchMethodError | AbstractMethodError ignored) {
                        // 兼容：若 BPP 未实现 getEarlyBeanReference，忽略
                    } catch (Exception ex) {
                        log.warn("getEarlyBeanReference failed for " + name, ex);
                    }
                }
                return early;
            });
            earlySingletonObjects.put(name, bean); // 兼容
        }
    }

    /**
     * 将 postProcessBeforeInitialization 应用到 bean
     */
    private Object applyPostProcessBeforeInitialization(Object bean, String name, BeanDefinition def) {
        Object result = bean;
        for (BeanPostProcessor bpp : postProcessors) {
            try {
                result = bpp.postProcessBeforeInitialization(result, name, def.getProxyType());
            } catch (Exception e) {
                log.warn("postProcessBeforeInitialization 异常: " + name, e);
            }
        }
        return result;
    }

    /**
     * 将 postProcessAfterInitialization 应用到 bean
     */
    private Object applyPostProcessAfterInitialization(Object bean, String name) {
        Object result = bean;
        for (BeanPostProcessor bpp : postProcessors) {
            try {
                result = bpp.postProcessAfterInitialization(result, name);
                // 如果返回了代理对象，则使用代理对象
                if (result != bean) {
                    return result;
                }
            } catch (Exception e) {
                log.warn("postProcessAfterInitialization 异常: " + name, e);
            }
        }
        return result;
    }

    /**
     * 解析工厂方法参数（支持按类型注入）
     */
    private Object[] resolveMethodArgs(Method m) {
        return Arrays.stream(m.getParameterTypes())
                .map(this::getBean)
                .toArray();
    }

    /**
     * 实例化：优先使用带 @Autowired 的构造器
     * 注意：这里只查找 public 构造器。若想支持 private 构造器，请改为 getDeclaredConstructors() 并 setAccessible(true)。
     */
    private Object instantiate(Class<?> cls) throws Exception {
        // 优先使用带 @Autowired 注解的构造器
        for (Constructor<?> c : cls.getConstructors()) {
            if (c.isAnnotationPresent(Autowired.class)) {
                return c.newInstance(resolveConstructorArgs(c));
            }
        }
        // 回退到无参构造器
        try {
            Constructor<?> noArg = Arrays.stream(cls.getConstructors())
                    .filter(c -> c.getParameterCount() == 0)
                    .findFirst()
                    .orElseThrow(() -> new NoSuchBeanException("缺少可用构造器: " + cls.getName()));
            return noArg.newInstance();
        } catch (InvocationTargetException ite) {
            throw ite;
        }
    }

    private Object[] resolveConstructorArgs(Constructor<?> c) {
        return Arrays.stream(c.getParameterTypes())
                .map(this::getBean)
                .toArray();
    }

    /**
     * 字段注入：处理 @Autowired 与 @Value
     * 说明：
     * - 优先按 @Autowired 的类型注入（若有多个实现，抛 TooMuchBeanException）
     * - 当前实现没有完整 @Qualifier 支持（可按需扩展）
     */
    private void injectFields(Object bean) throws IllegalAccessException {
        Class<?> cls = bean.getClass();
        for (Field f : cls.getDeclaredFields()) {
            f.setAccessible(true);
            if (f.isAnnotationPresent(Autowired.class)) {
                Autowired autowired = f.getAnnotation(Autowired.class);
                boolean required = autowired.required();
                String name = autowired.name().trim();
                if (name.isEmpty()) {
                    name = autowired.value().trim(); // value 作为 name 的别名
                }
                Object dep = null;

                // 优先按名称注入（如果指定）
                if (!name.isEmpty()) {
                    dep = getBeanOrNullByName(name, f.getType());
                }

                // 名称未找到，回退到按类型注入
                if (dep == null && required) {
                    try {
                        dep = getBean(f.getType());
                    } catch (NoSuchBeanException | TooMuchBeanException ex) {
                        // 按名称回退注入（字段名）
                        dep = getBeanOrNullByName(f.getName(), f.getType());
                    }
                }

                // required=false 时，dep 可能为 null，不抛异常
                if (dep == null && required) {
                    throw new NoSuchBeanException("无法注入字段: " + f.getName() + " of " + cls.getName() + " (required=true)");
                }

                f.set(bean, dep);
            } else if (f.isAnnotationPresent(Value.class)) {
                Value valAnno = f.getAnnotation(Value.class);
                String expr = valAnno.value(); // 可能是 "redis.host" 或 "${some.key:default}"
                try {
                    Object val = YamlConfigLoader.get(expr, f);
                    f.set(bean, val);
                } catch (Exception e) {
                    throw new RuntimeException("注入 @Value 字段失败: " + f + " expr=" + expr + " -> " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 尝试按 name 获取 bean（如果注册了且类型匹配则返回，否则返回 null）
     */
    private Object getBeanOrNullByName(String name, Class<?> expectedType) {
        BeanDefinition bd = definitions.get(name);
        if (bd != null && expectedType.isAssignableFrom(bd.getType())) {
            return getBean(name);
        }
        return null;
    }

    /**
     * 调用初始化回调（InitializingBean、@PostConstruct）
     */
    private void invokeInitMethods(Object bean) throws Exception {
        if (bean instanceof InitializingBean) {
            ((InitializingBean) bean).afterPropertiesSet();
        }
        for (Method m : bean.getClass().getDeclaredMethods()) {
            if (m.isAnnotationPresent(PostConstruct.class)) {
                if (m.getParameterCount() > 0) {
                    throw new IllegalStateException("@PostConstruct 方法不能有参数: " + m.getName());
                }
                m.setAccessible(true);
                m.invoke(bean);
                break;
            }
        }
    }

    /**
     * 提交所有 ApplicationRunner 任务（非阻塞）
     * 说明：不在构造器中等待它们完成；通过 close() 显示关闭线程池。
     */
    private void runRunners(String[] args) {
        ApplicationArguments appArgs = new ApplicationArguments(args);
        definitions.values().stream()
                .filter(def -> ApplicationRunner.class.isAssignableFrom(def.getType()))
                .map(def -> (ApplicationRunner) getBean(def.getName()))
                .forEach(r -> runnerPool.submit(() -> safeRun(r, appArgs)));
        // 不在此处 shutdown pool — 改为 close() 时统一关闭
    }

    private void safeRun(ApplicationRunner runner, ApplicationArguments args) {
        try {
            runner.run(args);
        } catch (Exception e) {
            exceptionHandler.handle(e, "Runner 执行异常");
        }
    }

    /**
     * 关闭容器：销毁单例 bean、关闭线程池、清理资源
     * - 调用 DisposableBean 或 @PreDestroy（如果你实现了相应契约）
     */
    @Override
    public void close() {
        if (closed) return;
        closed = true;

        // 1) 停止接收新任务，优雅关闭 runnerPool
        runnerPool.shutdown();
        try {
            if (!runnerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                runnerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            runnerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 2) 调用销毁回调（支持 DisposableBean / @PreDestroy）
        for (Map.Entry<String, Object> e : singletons.entrySet()) {
            try {
                Object bean = e.getValue();
                if (bean instanceof DisposableBean) {
                    ((DisposableBean) bean).destroy();
                }
                // @PreDestroy 支持（若实现）
                for (Method m : bean.getClass().getDeclaredMethods()) {
                    if (m.isAnnotationPresent(PreDestroy.class)) {
                        m.setAccessible(true);
                        m.invoke(bean);
                        break;
                    }
                }
            } catch (Exception ex) {
                log.warn("销毁 bean 失败: " + e.getKey(), ex);
            }
        }

        // 3) 清理容器内部缓存
        definitions.clear();
        singletons.clear();
        earlySingletonObjects.clear();
        earlySingletonFactories.clear();
        postProcessors.clear();
        creationLocks.clear();
    }

    /**
     * 简化的 ObjectFactory 接口，用于早期引用工厂（返回可能为代理的对象）
     */
    @FunctionalInterface
    private interface ObjectFactory<T> {
        T getObject();
    }
}
