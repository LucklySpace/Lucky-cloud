package com.xy.lucky.spring.context;


import com.xy.lucky.spring.annotations.SpringApplication;
import com.xy.lucky.spring.annotations.aop.Aspect;
import com.xy.lucky.spring.annotations.aop.EnableAop;
import com.xy.lucky.spring.annotations.core.*;
import com.xy.lucky.spring.annotations.event.EventListener;
import com.xy.lucky.spring.aop.AsyncBeanPostProcessor;
import com.xy.lucky.spring.aop.BeanPostProcessor;
import com.xy.lucky.spring.aop.ProxyBeanPostProcessor;
import com.xy.lucky.spring.core.*;
import com.xy.lucky.spring.event.ApplicationEventBus;
import com.xy.lucky.spring.event.ApplicationEventPublisher;
import com.xy.lucky.spring.exception.CyclicDependencyException;
import com.xy.lucky.spring.exception.NoSuchBeanException;
import com.xy.lucky.spring.exception.TooMuchBeanException;
import com.xy.lucky.spring.exception.handler.ExceptionHandler;
import com.xy.lucky.spring.exception.handler.GlobalExceptionHandler;
import com.xy.lucky.spring.factory.BeanFactory;
import com.xy.lucky.spring.utils.ClassUtils;
import lombok.extern.slf4j.Slf4j;

import java.beans.Introspector;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

/**
 * 精简版手写 Spring 容器（优化版）
 * <p>
 * 主要优化点（满足你的要求）：
 * 1) 扫描阶段优先识别并注册所有 @Configuration 及其 @Bean 定义（第一遍）
 * 2) 在后续初始化阶段尽早实例化这些 Configuration 类（使 factory bean 可用）
 * 3) 在配置类实例化后，再实例化 BeanPostProcessor（包括那些由 @Bean 提供的 BPP）
 * 4) 最后再做普通单例的预实例化（initSingletons），避免依赖注入缺失
 * <p>
 * 其它优化：
 * - 反射元数据缓存（构造器、可注入字段、@PostConstruct 方法）
 * - 类型索引 typeIndex（支持按类型查找，且避免重复项）
 * - creationLocks 在创建结束后清理，避免 Map 无限制增长
 *
 * @param <T> 启动主类类型
 */
@Slf4j
public class ApplicationContext<T> implements BeanFactory<T>, AutoCloseable {

    private static final String PROTOTYPE = "prototype";
    private static final String SINGLETON = "singleton";

    private final Class<T> startupClass;
    private final ExecutorService runnerPool;

    // bean name -> definition
    private final Map<String, BeanDefinition> definitions = new ConcurrentHashMap<>();
    // fully initialized singletons
    private final Map<String, Object> singletons = new ConcurrentHashMap<>();
    // early reference factories（name -> ObjectFactory），用于循环依赖早期代理
    private final Map<String, ObjectFactory<?>> earlySingletonFactories = new ConcurrentHashMap<>();
    // bean post processors (实例化后的对象增强用)
    private final List<BeanPostProcessor> postProcessors = new CopyOnWriteArrayList<>();
    // creation lock map，用于并发创建控制（每个 bean 一个锁对象），创建完成后会清理对应锁以避免 map 无限制增长
    private final ConcurrentHashMap<String, Object> creationLocks = new ConcurrentHashMap<>();
    // 当前线程正在创建的 Bean 名称集合，用于循环依赖检测（线程本地）
    private final ThreadLocal<Set<String>> inCreation = ThreadLocal.withInitial(HashSet::new);
    // 异常处理
    private final ExceptionHandler exceptionHandler = new GlobalExceptionHandler();
    // 事件发布
    private final ApplicationEventBus applicationEventBus = new ApplicationEventBus();
    // 扫描到的 Class 集合（用于 AOP 等基于扫描结果的功能）
    private final Set<Class<?>> scannedClasses = ConcurrentHashMap.newKeySet();

    // ------------------ 新增缓存/索引（性能优化点） ------------------
    private final ConcurrentHashMap<Class<?>, ConstructorPlan> constructorCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, InjectionPoint[]> injectionPointCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, Optional<MethodHandle>> postConstructCache = new ConcurrentHashMap<>();
    // 类型索引：类型 -> beanName 列表（加速按类型查找）
    private final ConcurrentHashMap<Class<?>, List<String>> typeIndex = new ConcurrentHashMap<>();
    // 标记容器是否已关闭
    private volatile boolean closed = false;

    // ------------------ 构造与初始化 ------------------

    /**
     * 构造函数：只做启动流程（不在构造中等待 ApplicationRunner 完成）
     */
    public ApplicationContext(Class<T> startupClass, String[] args) {
        this.startupClass = startupClass;
        // runnerPool 交由 close 时关闭（避免在构造中阻塞）
        this.runnerPool = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                r -> new Thread(r, "AppRunner")
        );

        // 1) 校验启动注解
        validateAnnotation();

        // 2) 加载配置
        ApplicationConfigLoader.load();

        // 3) 扫描并注册 BeanDefinition（分多步，以保证 Configuration 先被识别注册）
        scanAndRegister();

        // 4) 注册内部框架 bean（如 ApplicationEventBus、配置加载器）
        registerInternalBeans();

        // 5) **尽早实例化所有 Configuration 类实例**（使工厂 bean 可用，供后续 @Bean/依赖注入使用）
        instantiateConfigClasses();

        // 6) 实例化所有 BeanPostProcessor（这一步会实例化由 @Bean 方法提供的 BPP，因为配置类已可用）
        instantiateAllBeanPostProcessors();

        // 7) 预实例化所有非懒加载单例（普通 bean）
        initSingletons();

        // 8) 注册事件监听器（基于 bean）
        registerEventListeners();

        // 9) 提交 runners，只提交任务，不等待完成
        runRunners(args);

        // 注册 JVM ShutdownHook，实现自动关闭（"自动 GC"）
        Runtime.getRuntime().addShutdownHook(new Thread(this::close, "AppContext-ShutdownHook"));
    }

    public Executor getTaskExecutor() {
        return runnerPool;
    }

    private static String normalizeAutowiredName(Autowired autowired) {
        String name = autowired.name().trim();
        if (!name.isEmpty()) return name;
        return autowired.value().trim();
    }

//    /**
//     * 获取配置加载器
//     */
//    public ApplicationConfigLoader getConfigLoader() {
//        return ApplicationConfigLoader.getConfigLoader();
//    }

    /**
     * 对外暴露事件发布器
     */
    public ApplicationEventPublisher getEventPublisher() {
        return (ApplicationEventPublisher) getBean(ApplicationEventPublisher.class);
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
     * 注册内置 Bean（例如 ApplicationEventBus, configLoader）
     */
    private void registerInternalBeans() {
        // 通过 registerSingleton 把已实例化对象注册到 definitions 与 singletons
        registerSingleton("applicationContext", this);
        registerSingleton("applicationEventBus", applicationEventBus);
        registerSingleton("taskExecutor", runnerPool);
    }

    /**
     * 扫描并注册组件与配置类中的 @Bean
     * <p>
     * 优化说明：
     * - 第一遍优先注册所有 @Configuration（及其 @Bean 方法），并把配置类自身注册为单例定义（但不实例化）
     * - 第二遍再注册所有普通组件（@Component/@Service）—— 若与 @Configuration/@Bean 冲突，保留先注册的定义（避免覆盖 factory bean）
     * - 第三步注册框架性定义（例如 AsyncBeanPostProcessor 的 BeanDefinition）
     * <p>
     * 目的：保证 factory bean 的定义在普通组件之前被注册，随后我们会尽早实例化这些配置类。
     */
    private void scanAndRegister() {
        try {
            String basePkg = Optional.of(startupClass.getAnnotation(SpringApplication.class).value())
                    .filter(v -> !v.isEmpty())
                    .orElse(startupClass.getPackageName());
            Set<Class<?>> classes = ClassUtils.scan(basePkg);
            scannedClasses.clear();
            scannedClasses.addAll(classes);

            // 1) 第一遍：只处理 @Configuration（并注册配置类本身与其 @Bean 方法）
            for (Class<?> cls : classes) {
                if (cls.isAnnotationPresent(Configuration.class)) {
                    registerConfigClassAndBeansFirstPass(cls);
                }
            }

            // 2) 第二遍：处理普通组件（@Component / @Service 等），但是不要覆盖已有定义
            for (Class<?> cls : classes) {
                if (!cls.isAnnotationPresent(Configuration.class)) {
                    registerComponentDefinition(cls);
                }
            }

            // 3) 第三步：注册框架/功能性的 BeanDefinition（例如 Async BPP 等）
            if (startupClass.isAnnotationPresent(EnableAsync.class)) {
                registerAsyncBeanPostProcessor();
            }

            //
            if (startupClass.isAnnotationPresent(EnableAop.class)) {
                registerAspectBeans(classes);
                registerAopBeanPostProcessor();
            }
        } catch (Exception e) {
            throw new RuntimeException("扫描注册失败", e);
        }
    }

    /**
     * 注册异步相关的BeanPostProcessor（只注册定义）
     */
    private void registerAsyncBeanPostProcessor() {
        String name = "asyncBeanPostProcessor";
        BeanDefinition def = new BeanDefinition()
                .setType(AsyncBeanPostProcessor.class)
                .setName(name)
                .setFullName(AsyncBeanPostProcessor.class.getName())
                .setLazy(false)
                .setScope(SINGLETON)
                .setProxyType(ProxyType.NONE);
        definitions.put(name, def);
        indexBeanType(def.getType(), name);
    }

    private void registerAspectBeans(Set<Class<?>> classes) {
        for (Class<?> cls : classes) {
            if (!cls.isAnnotationPresent(Aspect.class)) continue;
            String name = deriveBeanName(cls);
            if (definitions.containsKey(name)) continue;
            BeanDefinition def = new BeanDefinition()
                    .setType(cls)
                    .setName(name)
                    .setFullName(cls.getName())
                    .setLazy(false)
                    .setScope(SINGLETON)
                    .setProxyType(ProxyType.NONE);
            definitions.put(name, def);
            indexBeanType(def.getType(), name);
        }
    }

    /**
     * 注册 AOP 相关的 BeanPostProcessor（只注册定义）
     */
    private void registerAopBeanPostProcessor() {
        String name = "proxyBeanPostProcessor";
        BeanDefinition def = new BeanDefinition()
                .setType(ProxyBeanPostProcessor.class)
                .setName(name)
                .setFullName(ProxyBeanPostProcessor.class.getName())
                .setLazy(false)
                .setScope(SINGLETON)
                .setProxyType(ProxyType.NONE);
        definitions.put(name, def);
        indexBeanType(def.getType(), name);
    }

    /**
     * 手动注册一个已实例化的单例 Bean 到容器（支持依赖注入）
     * <p>
     * 说明：
     * - 该方法会把实例放入 singletons，并在 definitions 中注册对应的 BeanDefinition（如果不存在）
     * - 对已存在的同名 singletons 会抛异常（可根据需求改为覆盖）
     */
    public void registerSingleton(Object instance) {
        if (closed) throw new IllegalStateException("ApplicationContext 已关闭");
        if (instance == null) throw new IllegalArgumentException("实例不能为空");

        String name = deriveBeanName(instance.getClass());
        registerSingleton(name, instance);
    }

    /**
     * 手动注册一个已实例化的单例 Bean 到容器（指定 beanName）
     */
    public void registerSingleton(String name, Object instance) {
        if (closed) throw new IllegalStateException("ApplicationContext 已关闭");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("beanName 不能为空");
        if (instance == null) throw new IllegalArgumentException("实例不能为空");
        name = name.trim();

        // 检查 definitions：如果存在，检查类型匹配
        BeanDefinition existingDef = definitions.get(name);

        // 检查名称冲突：如果 singletons 已存在，抛异常（可改为覆盖，视需求）
        if (singletons.containsKey(name)) {
            throw new IllegalArgumentException("Bean 名称 [" + name + "] 已存在，无法重复注册");
        }

        if (existingDef != null && !existingDef.getType().isInstance(instance)) {
            throw new IllegalArgumentException("实例类型 [" + instance.getClass().getName() + "] 与现有定义不匹配: " + existingDef.getType().getName());
        }

        try {
            // 1. 创建或更新 BeanDefinition
            BeanDefinition def = existingDef != null ? existingDef : new BeanDefinition();
            def.setType(instance.getClass())
                    .setName(name)
                    .setFullName(instance.getClass().getName())
                    .setLazy(false)
                    .setScope(SINGLETON)
                    .setProxyType(ProxyType.NONE);  // 默认无代理，可扩展
            definitions.put(name, def);
            indexBeanType(def.getType(), name);

            // 2. 可选：应用前置处理（如果需要增强）
            Object processed = applyPostProcessBeforeInitialization(instance, name, def);

            // 3. 可选：字段注入（如果实例有 @Autowired 字段，从容器注入依赖）
            injectFields(processed);  // 注意：processed 可能为代理

            // 4. 可选：初始化回调（如果未调用过）
            invokeInitMethods(processed);

            // 5. 后置处理（支持 AOP 代理）
            Object finalInstance = applyPostProcessAfterInitialization(processed, name, def);

            // 6. 放入 singletons（使用最终处理后的实例）
            singletons.put(name, finalInstance);

            log.info("手动注册单例 Bean: {} -> {}", name, instance.getClass().getSimpleName());

        } catch (Exception e) {
            // 回滚：移除可能的脏数据
            definitions.remove(name);
            singletons.remove(name);
            exceptionHandler.handle(e, "手动注册 Bean[" + name + "] 失败");
            throw new RuntimeException("手动注册 Bean[" + name + "] 失败", e);
        }
    }

    /**
     * 优化版的 registerComponentDefinition（保持原语义）
     * 注意：此处不会覆盖已由 @Configuration/@Bean 注册过的同名定义，避免覆盖 factory bean
     */
    private void registerComponentDefinition(Class<?> cls) {
        if (!cls.isAnnotationPresent(Component.class) && !cls.isAnnotationPresent(Service.class)) return;
        String name = deriveBeanName(cls);
        // 如果该名称已经被配置类或 @Bean 方法占用，就不要覆盖它
        if (definitions.containsKey(name)) {
            // 可记录日志以便调试配置冲突
            log.debug("组件 {} 的名称 {} 已由其他定义占用，跳过（避免覆盖 factory bean）", cls.getName(), name);
            return;
        }
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
        indexBeanType(def.getType(), name);
    }

    /**
     * 根据注解或类名生成 Bean 名称
     */
    private String deriveBeanName(Class<?> cls) {
        // 优先检查 Component/Service/Configuration 注解的 value
        String val = Optional.ofNullable(cls.getAnnotation(Component.class)).map(Component::value)
                .filter(v -> !v.isEmpty())
                .orElse(Optional.ofNullable(cls.getAnnotation(Service.class)).map(Service::value).orElse(""));

        if (val.isEmpty()) {
            val = Optional.ofNullable(cls.getAnnotation(Configuration.class)).map(Configuration::value).orElse("");
        }

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
     * 注册配置类及其 @Bean 方法（第一遍，优先执行）
     * <p>
     * 要点：
     * - 将配置类本身注册为 singleton（非懒），以便后续我们能够尽早实例化配置类
     * - 将其 @Bean 方法注册为 BeanDefinition（工厂方法模式），并且把 factoryBeanClass 设置为配置类（cfgClass）
     * - 注意：此阶段只注册定义，不做实例化
     */
    private void registerConfigClassAndBeansFirstPass(Class<?> cfgClass) {
        // 配置类本身的 beanName（例如 myConfig -> myConfig）
        String cfgName = deriveBeanName(cfgClass);
        // 如果已经注册过，不覆盖（避免重复）
        if (!definitions.containsKey(cfgName)) {
            BeanDefinition cfgDef = new BeanDefinition()
                    .setType(cfgClass)
                    .setName(cfgName)
                    .setFullName(cfgClass.getName())
                    .setLazy(false)                 // 强制非懒，保证配置类尽早可用
                    .setScope(SINGLETON)           // 配置类通常为单例
                    .setProxyType(ProxyType.NONE);
            definitions.put(cfgName, cfgDef);
            indexBeanType(cfgClass, cfgName);
        }

        // 注册 @Bean 方法（工厂方法） —— 重要：不要在这里实例化配置类，仅注册定义
        for (Method m : cfgClass.getDeclaredMethods()) {
            if (!m.isAnnotationPresent(Bean.class)) continue;
            String name = Optional.of(m.getAnnotation(Bean.class).value())
                    .filter(v -> !v.isEmpty())
                    .orElse(m.getName());
            // 如果已有同名定义，则跳过或记录冲突（避免覆盖）
            if (definitions.containsKey(name)) {
                log.warn("Bean 名称冲突：配置类 {} 的 @Bean 方法 {} 名称 {} 已存在，跳过注册", cfgClass.getName(), m.getName(), name);
                continue;
            }
            FactoryMethodPlan plan = createFactoryMethodPlan(cfgClass, m);
            BeanDefinition def = new BeanDefinition()
                    .setType(m.getReturnType())
                    .setName(name)
                    .setFullName(m.getReturnType().getName())
                    .setLazy(m.isAnnotationPresent(Lazy.class))     // @Bean 可声明 lazy
                    .setScope(determineScope(m, m.getReturnType()))
                    .setFactoryMethodHandle(plan.handle)
                    .setFactoryMethodParamTypes(plan.paramTypes)
                    .setFactoryMethodStatic(plan.isStatic)
                    .setFactoryBeanClass(cfgClass)                   // 关键：记录 factoryBeanClass 为配置类类型
                    .setProxyType(ProxyType.NONE);
            definitions.put(name, def);
            indexBeanType(def.getType(), name);
        }
    }

    /**
     * 实例化所有配置类（Configuration）—— 尽早执行
     * <p>
     * 目的：
     * - 确保配置类实例先被创建（包括 @Autowired 注入、@PostConstruct）
     * - 使得配置类里的 @Bean 方法在后续实例化阶段可以被安全调用（比如创建其它依赖）
     */
    private void instantiateConfigClasses() {
        // 收集所有被标记为 Configuration 的定义名
        List<String> cfgNames = definitions.entrySet().stream()
                .filter(e -> e.getValue().getType().isAnnotationPresent(Configuration.class))
                .map(Map.Entry::getKey)
                .toList();

        for (String cfgName : cfgNames) {
            try {
                // 使用 getBean 来创建并初始化配置类单例（如果尚未创建）
                getBean(cfgName);
            } catch (Exception e) {
                // 配置类实例化失败通常会导致严重后果，记录并继续尝试其他配置类
                log.warn("配置类实例化失败: " + cfgName, e);
            }
        }
    }

    /**
     * 预实例化 (non-lazy) 单例 Bean（会触发创建逻辑）
     * <p>
     * 逐个串行创建，避免过度并发导致复杂的竞态与循环依赖问题。
     */
    private void initSingletons() {
        // 使用确定的顺序（例如 definitions.keySet 的迭代顺序）逐个创建，避免并行导致复杂问题
        for (Map.Entry<String, BeanDefinition> entry : definitions.entrySet()) {
            String name = entry.getKey();
            BeanDefinition def = entry.getValue();
            // 跳过已经存在于 singletons 的（例如配置类、手动注册的单例）
            if (singletons.containsKey(name)) continue;
            if (SINGLETON.equals(def.getScope()) && !def.isLazy()) {
                try {
                    getBean(name);
                } catch (Exception e) {
                    // 记录但继续创建其他 bean（某些 bean 创建失败不应阻止整个容器启动）
                    log.warn("预实例化单例 bean 失败: " + name, e);
                }
            }
        }
    }

    /**
     * 先实例化所有 BeanPostProcessor 类型的 bean（确保它们在普通 bean 创建前可用）
     * <p>
     * 注意：配置类已实例化（instantiateConfigClasses），因此若有 BPP 是由 @Bean 提供，这里会正确创建并加入到 postProcessors 列表中。
     */
    private void instantiateAllBeanPostProcessors() {
        // 找到所有实现 BeanPostProcessor 的定义（仅收集名称，不在此刻并行实例化）
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
            // 检测类中是否存在 EventListener 注解方法（简易检查）
            if (Arrays.stream(def.getType().getDeclaredMethods()).anyMatch(m -> m.isAnnotationPresent(EventListener.class))) {
                try {
                    Object bean = getBean(e.getKey());
                    applicationEventBus.registerListener(bean);
                } catch (Exception ex) {
                    log.warn("注册事件监听器失败: " + e.getKey(), ex);
                }
            }
        }
    }

    // ------------------ Bean 获取与创建 ------------------

    private String determineScope(Method factoryMethod, Class<?> returnType) {
        Scope scope = factoryMethod.getAnnotation(Scope.class);
        if (scope != null && PROTOTYPE.equals(scope.value())) return PROTOTYPE;
        return SINGLETON;
    }

    /**
     * 根据名称获取 Bean（入口）
     */
    @Override
    public Object getBean(String name) {
        if (closed) throw new IllegalStateException("ApplicationContext 已关闭");
        BeanDefinition def = definitions.get(name);
        if (def == null) throw new NoSuchBeanException("No bean named '" + name + "' is defined");
        if (SINGLETON.equals(def.getScope())) {
            return getSingleton(name, def);
        } else {
            // prototype 每次创建一个新实例
            return createWithHandling(name, def);
        }
    }

    /**
     * getBean by name + type（修复版）
     */
    @Override
    public Object getBean(String beanName, Class type) {
        if (beanName == null || beanName.trim().isEmpty()) throw new IllegalStateException("beanName 不能为空！");
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
            try {
                singleton = singletons.get(name); // double-check
                if (singleton != null) return singleton;

                Object created = createWithHandling(name, def);
                if (created != null) {
                    singletons.put(name, created); // 无需 putIfAbsent，已在锁内
                }
                return created;
            } finally {
                // 创建完成后移除锁，避免 map 无限增长
                creationLocks.remove(name);
            }
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
     * 按类型查找单一 Bean（优化版：使用 typeIndex，fallback 全表扫描）
     */
    @Override
    public Object getBean(Class type) {
        if (type == null) throw new IllegalArgumentException("type 不能为空");
        List<String> names = typeIndex.get(type);
        if (names == null || names.isEmpty()) {
            // fallback: 全表扫描（保持兼容）
            List<Map.Entry<String, BeanDefinition>> matches = definitions.entrySet().stream()
                    .filter(e -> type.isAssignableFrom(e.getValue().getType()))
                    .toList();
            if (matches.isEmpty()) throw new NoSuchBeanException();
            if (matches.size() > 1)
                throw new TooMuchBeanException("匹配到多个 Bean，类型: " + type.getName() + ", 数量: " + matches.size());
            return getBean(matches.getFirst().getKey());
        }
        if (names.size() > 1) {
            throw new TooMuchBeanException("匹配到多个 Bean，类型: " + type.getName() + ", beanNames: " + names);
        }
        return getBean(names.get(0));
    }

    /**
     * 获取早期引用（优先 factory）
     */
    private Object getEarlyReferenceIfPossible(String name) {
        ObjectFactory<?> factory = earlySingletonFactories.get(name);
        if (factory != null) {
            try {
                Object o = factory.getObject();
                if (o != null) return o;
            } catch (Exception e) {
                log.warn("获取早期引用失败: {}", name, e);
            }
        }
        return null;
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
            bean = applyPostProcessAfterInitialization(bean, name, def);

            // ---------- 成功：将最终对象放入 singletons 并清除 early maps ----------
            if (SINGLETON.equals(def.getScope())) {
                singletons.put(name, bean);
                earlySingletonFactories.remove(name);
            }

            success = true;
            return bean;
        } finally {
            // 无论成功或失败，都要从 inCreation 中移除
            creating.remove(name);

            // 失败时清理早期缓存，避免残留脏数据影响后续
            if (!success) {
                earlySingletonFactories.remove(name);
            }
        }
    }

    private Object instantiateBean(BeanDefinition def) throws Exception {
        if (def.hasFactoryMethod()) {
            // 若是配置类的 @Bean，先确保 factory bean 实例化（factoryBeanClass 指向配置类）
            Object factoryBean = null;
            if (!def.isFactoryMethodStatic()) {
                if (def.getFactoryBeanClass() != null) {
                    String cfgName = deriveBeanName(def.getFactoryBeanClass());
                    factoryBean = getBean(cfgName);
                } else {
                    factoryBean = def.getFactoryBean();
                }
            }
            Object[] args = resolveMethodArgs(def.getFactoryMethodParamTypes());
            return invokeFactoryMethod(def, factoryBean, args);
        } else {
            return instantiate(def.getType());
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
     * 早期引用暴露（单例）
     * 仅保留 factories（避免保存 raw bean 到 earlySingletonObjects 导致重复引用）
     */
    private void exposeEarlySingletonIfNeeded(String name, BeanDefinition def, Object bean) {
        if (SINGLETON.equals(def.getScope())) {
            final Object rawBean = bean;
            final ProxyType proxyType = def.getProxyType();
            earlySingletonFactories.put(name, () -> {
                Object early = rawBean;
                for (BeanPostProcessor bpp : postProcessors) {
                    try {
                        early = bpp.getEarlyBeanReference(early, name, proxyType);
                    } catch (Exception ex) {
                        log.warn("getEarlyBeanReference failed for " + name, ex);
                    }
                }
                return early;
            });
        }
    }

    /**
     * 将 postProcessAfterInitialization 应用到 bean
     */
    private Object applyPostProcessAfterInitialization(Object bean, String name, BeanDefinition def) {
        Object result = bean;
        for (BeanPostProcessor bpp : postProcessors) {
            try {
                result = bpp.postProcessAfterInitialization(result, name, def.getProxyType());
                // 如果返回了代理对象，则使用代理对象（并立即返回以减少多次 wrap）
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
    private Object[] resolveMethodArgs(Class<?>[] paramTypes) {
        if (paramTypes == null || paramTypes.length == 0) return new Object[0];
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            args[i] = getBean(paramTypes[i]);
        }
        return args;
    }

    /**
     * 实例化：优先使用带 @Autowired 的构造器
     * 使用 constructorCache 缓存构造器选择的结果以避免重复扫描
     * <p>
     * 注意：这里只查找 public 构造器。若想支持 private 构造器，请改为 getDeclaredConstructors() 并 setAccessible(true)。
     */
    private Object instantiate(Class<?> cls) throws Exception {
        ConstructorPlan plan = findConstructorForClass(cls);
        Object[] args = resolveConstructorArgs(plan.paramTypes);
        try {
            return args.length == 0 ? plan.ctor.invoke() : plan.ctor.invokeWithArguments(args);
        } catch (Throwable t) {
            if (t instanceof Exception e) throw e;
            throw new RuntimeException("实例化失败: " + cls.getName(), t);
        }
    }

    /**
     * 查找并缓存用于实例化的构造器（首选带 @Autowired 的构造器，否则首选无参构造器）
     */
    private ConstructorPlan findConstructorForClass(Class<?> cls) {
        return constructorCache.computeIfAbsent(cls, key -> {
            for (Constructor<?> c : key.getConstructors()) {
                if (c.isAnnotationPresent(Autowired.class)) {
                    return createConstructorPlan(key, c.getParameterTypes());
                }
            }
            return Arrays.stream(key.getConstructors())
                    .filter(c -> c.getParameterCount() == 0)
                    .findFirst()
                    .map(c -> createConstructorPlan(key, c.getParameterTypes()))
                    .orElseThrow(() -> new NoSuchBeanException("缺少可用构造器: " + key.getName()));
        });
    }

    private Object[] resolveConstructorArgs(Class<?>[] paramTypes) {
        if (paramTypes == null || paramTypes.length == 0) return new Object[0];
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            args[i] = getBean(paramTypes[i]);
        }
        return args;
    }

    /**
     * 字段注入：处理 @Autowired 与 @Value
     */
    private void injectFields(Object bean) throws IllegalAccessException {
        Class<?> userClass = ClassUtils.getUserClass(bean);
        InjectionPoint[] points = getInjectionPointsForClass(userClass);
        for (InjectionPoint p : points) {
            switch (p.kind) {
                case AUTOWIRED -> {
                    Object dep = resolveAutowiredDependency(p);
                    if (dep == null && p.required) {
                        throw new NoSuchBeanException("无法注入字段: " + p.fieldName + " of " + userClass.getName() + " (required=true)");
                    }
                    setField(bean, p.type, p.varHandle, dep);
                }
                case VALUE -> {
                    try {
                        Object val = ApplicationConfigLoader.get(p.valueExpression, p.fieldForValue);
                        setField(bean, p.type, p.varHandle, val);
                    } catch (Exception e) {
                        throw new RuntimeException("注入 @Value 字段失败: " + p.fieldName + " expr=" + p.valueExpression + " -> " + e.getMessage(), e);
                    }
                }
            }
        }
    }

    private InjectionPoint[] getInjectionPointsForClass(Class<?> cls) {
        return injectionPointCache.computeIfAbsent(cls, key -> {
            Field[] fields = key.getDeclaredFields();
            if (fields.length == 0) return new InjectionPoint[0];

            List<InjectionPoint> list = new ArrayList<>();
            for (Field f : fields) {
                Autowired autowired = f.getAnnotation(Autowired.class);
                Value value = f.getAnnotation(Value.class);
                if (autowired == null && value == null) continue;
                list.add(autowired != null
                        ? InjectionPoint.autowired(null, f.getName(), f.getType(), normalizeAutowiredName(autowired), autowired.required())
                        : InjectionPoint.value(null, f.getName(), f.getType(), value.value(), f));
            }

            if (list.isEmpty()) return new InjectionPoint[0];

            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandles.Lookup privateLookup;
            try {
                privateLookup = MethodHandles.privateLookupIn(key, lookup);
            } catch (Exception e) {
                throw new IllegalStateException("无法创建 privateLookup: " + key.getName(), e);
            }

            for (int i = 0; i < list.size(); i++) {
                InjectionPoint p = list.get(i);
                try {
                    VarHandle vh = privateLookup.findVarHandle(key, p.fieldName, p.type);
                    list.set(i, p.withVarHandle(vh));
                } catch (Exception e) {
                    throw new IllegalStateException("无法解析字段句柄: " + key.getName() + "." + p.fieldName, e);
                }
            }
            return list.toArray(new InjectionPoint[0]);
        });
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
     * 优化：使用 postConstructCache 缓存 post construct 方法
     */
    private void invokeInitMethods(Object bean) throws Exception {
        if (bean instanceof InitializingBean) {
            ((InitializingBean) bean).afterPropertiesSet();
        }
        Optional<MethodHandle> h = getPostConstructHandleForClass(ClassUtils.getUserClass(bean));
        if (h.isPresent()) {
            try {
                h.get().bindTo(bean).invoke();
            } catch (Throwable t) {
                if (t instanceof Exception e) throw e;
                throw new RuntimeException("调用 @PostConstruct 失败: " + bean.getClass().getName(), t);
            }
        }
    }

    private Optional<MethodHandle> getPostConstructHandleForClass(Class<?> cls) {
        return postConstructCache.computeIfAbsent(cls, key -> {
            for (Method m : key.getDeclaredMethods()) {
                if (!m.isAnnotationPresent(PostConstruct.class)) continue;
                if (m.getParameterCount() > 0) {
                    throw new IllegalStateException("@PostConstruct 方法不能有参数: " + m.getName());
                }
                try {
                    MethodHandles.Lookup lookup = MethodHandles.lookup();
                    MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(key, lookup);
                    MethodType type = MethodType.methodType(m.getReturnType());
                    MethodHandle handle = java.lang.reflect.Modifier.isStatic(m.getModifiers())
                            ? privateLookup.findStatic(key, m.getName(), type)
                            : privateLookup.findVirtual(key, m.getName(), type);
                    return Optional.of(handle);
                } catch (Exception e) {
                    throw new IllegalStateException("解析 @PostConstruct 句柄失败: " + key.getName() + "." + m.getName(), e);
                }
            }
            return Optional.empty();
        });
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
                invokePreDestroy(bean);
            } catch (Exception ex) {
                log.warn("销毁 bean 失败: " + e.getKey(), ex);
            }
        }

        // 3) 清理容器内部缓存
        applicationEventBus.close();
        definitions.clear();
        singletons.clear();
        earlySingletonFactories.clear();
        postProcessors.clear();
        creationLocks.clear();
        constructorCache.clear();
        injectionPointCache.clear();
        postConstructCache.clear();
        typeIndex.clear();
    }

    /**
     * 把某个 bean 的类型加入 typeIndex（同时索引接口与父类）
     * 使用去重逻辑，避免重复项
     */
    private void indexBeanType(Class<?> type, String beanName) {
        // concrete type
        typeIndex.compute(type, (k, v) -> {
            if (v == null) return new ArrayList<>(List.of(beanName));
            if (!v.contains(beanName)) v.add(beanName);
            return v;
        });
        // interfaces
        for (Class<?> itf : type.getInterfaces()) {
            typeIndex.compute(itf, (k, v) -> {
                if (v == null) return new ArrayList<>(List.of(beanName));
                if (!v.contains(beanName)) v.add(beanName);
                return v;
            });
        }
        // superclass
        Class<?> sup = type.getSuperclass();
        if (sup != null && sup != Object.class) {
            typeIndex.compute(sup, (k, v) -> {
                if (v == null) return new ArrayList<>(List.of(beanName));
                if (!v.contains(beanName)) v.add(beanName);
                return v;
            });
        }
    }

    // ------------------ 辅助工具方法 ------------------

    private ConstructorPlan createConstructorPlan(Class<?> cls, Class<?>[] paramTypes) {
        try {
            MethodHandle ctor = MethodHandles.lookup().findConstructor(cls, MethodType.methodType(void.class, paramTypes));
            return new ConstructorPlan(ctor, paramTypes);
        } catch (Exception e) {
            throw new IllegalStateException("解析构造器句柄失败: " + cls.getName(), e);
        }
    }

    private FactoryMethodPlan createFactoryMethodPlan(Class<?> declaringClass, Method m) {
        try {
            boolean isStatic = java.lang.reflect.Modifier.isStatic(m.getModifiers());
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(declaringClass, lookup);
            MethodType type = MethodType.methodType(m.getReturnType(), m.getParameterTypes());
            MethodHandle handle = isStatic
                    ? privateLookup.findStatic(declaringClass, m.getName(), type)
                    : privateLookup.findVirtual(declaringClass, m.getName(), type);
            return new FactoryMethodPlan(handle, m.getParameterTypes(), isStatic);
        } catch (Exception e) {
            throw new IllegalStateException("解析工厂方法句柄失败: " + declaringClass.getName() + "." + m.getName(), e);
        }
    }

    private Object invokeFactoryMethod(BeanDefinition def, Object factoryBean, Object[] args) throws Exception {
        MethodHandle h = def.getFactoryMethodHandle();
        if (!def.isFactoryMethodStatic()) {
            if (factoryBean == null) throw new IllegalStateException("factoryBean 为空: " + def.getName());
            h = h.bindTo(factoryBean);
        }
        try {
            return args.length == 0 ? h.invoke() : h.invokeWithArguments(args);
        } catch (Throwable t) {
            if (t instanceof Exception e) throw e;
            throw new RuntimeException("调用工厂方法失败: " + def.getName(), t);
        }
    }

    private Object resolveAutowiredDependency(InjectionPoint p) {
        Object dep = null;
        if (p.beanName != null && !p.beanName.isEmpty()) {
            dep = getBeanOrNullByName(p.beanName, p.type);
        }
        if (dep == null && p.required) {
            try {
                dep = getBean(p.type);
            } catch (NoSuchBeanException | TooMuchBeanException ex) {
                dep = getBeanOrNullByName(p.fieldName, p.type);
            }
        }
        return dep;
    }

    private void setField(Object bean, Class<?> type, VarHandle handle, Object value) {
        Object v = adaptPrimitiveValue(type, value);
        handle.set(bean, v);
    }

    private Object adaptPrimitiveValue(Class<?> type, Object value) {
        if (value == null) return null;
        if (!type.isPrimitive()) return value;
        if (type == int.class) return ((Number) value).intValue();
        if (type == long.class) return ((Number) value).longValue();
        if (type == boolean.class) return (value instanceof Boolean) ? value : Boolean.parseBoolean(value.toString());
        if (type == short.class) return ((Number) value).shortValue();
        if (type == byte.class) return ((Number) value).byteValue();
        if (type == float.class) return ((Number) value).floatValue();
        if (type == double.class) return ((Number) value).doubleValue();
        if (type == char.class) {
            String s = value.toString();
            return s.isEmpty() ? '\0' : s.charAt(0);
        }
        return value;
    }

    private void invokePreDestroy(Object bean) throws Exception {
        Class<?> userClass = ClassUtils.getUserClass(bean);
        for (Method m : userClass.getDeclaredMethods()) {
            if (!m.isAnnotationPresent(PreDestroy.class)) continue;
            if (m.getParameterCount() > 0) {
                throw new IllegalStateException("@PreDestroy 方法不能有参数: " + userClass.getName() + "." + m.getName());
            }
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(userClass, lookup);
                MethodType type = MethodType.methodType(m.getReturnType());
                MethodHandle h = java.lang.reflect.Modifier.isStatic(m.getModifiers())
                        ? privateLookup.findStatic(userClass, m.getName(), type)
                        : privateLookup.findVirtual(userClass, m.getName(), type).bindTo(bean);
                if (java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                    h.invoke();
                } else {
                    h.invoke();
                }
                return;
            } catch (Throwable t) {
                if (t instanceof Exception e) throw e;
                throw new RuntimeException("调用 @PreDestroy 失败: " + userClass.getName() + "." + m.getName(), t);
            }
        }
    }

    public Set<Class<?>> getScannedClasses() {
        return Collections.unmodifiableSet(scannedClasses);
    }

    private enum InjectionKind {
        AUTOWIRED,
        VALUE
    }

    private record ConstructorPlan(MethodHandle ctor, Class<?>[] paramTypes) {
    }

    private record FactoryMethodPlan(MethodHandle handle, Class<?>[] paramTypes, boolean isStatic) {
    }

    /**
     * 简化的 ObjectFactory 接口，用于早期引用工厂（返回可能为代理的对象）
     */
    @FunctionalInterface
    private interface ObjectFactory<T> {
        T getObject();
    }

    private static final class InjectionPoint {
        private final InjectionKind kind;
        private final VarHandle varHandle;
        private final String fieldName;
        private final Class<?> type;

        private final String beanName;
        private final boolean required;

        private final String valueExpression;
        private final Field fieldForValue;

        private InjectionPoint(InjectionKind kind,
                               VarHandle varHandle,
                               String fieldName,
                               Class<?> type,
                               String beanName,
                               boolean required,
                               String valueExpression,
                               Field fieldForValue) {
            this.kind = kind;
            this.varHandle = varHandle;
            this.fieldName = fieldName;
            this.type = type;
            this.beanName = beanName;
            this.required = required;
            this.valueExpression = valueExpression;
            this.fieldForValue = fieldForValue;
        }

        private static InjectionPoint autowired(VarHandle varHandle, String fieldName, Class<?> type, String beanName, boolean required) {
            return new InjectionPoint(InjectionKind.AUTOWIRED, varHandle, fieldName, type, beanName, required, null, null);
        }

        private static InjectionPoint value(VarHandle varHandle, String fieldName, Class<?> type, String valueExpression, Field fieldForValue) {
            return new InjectionPoint(InjectionKind.VALUE, varHandle, fieldName, type, null, true, valueExpression, fieldForValue);
        }

        private InjectionPoint withVarHandle(VarHandle varHandle) {
            return new InjectionPoint(
                    this.kind,
                    varHandle,
                    this.fieldName,
                    this.type,
                    this.beanName,
                    this.required,
                    this.valueExpression,
                    this.fieldForValue
            );
        }
    }
}
