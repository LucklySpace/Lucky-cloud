package com.xy.lucky.spring.boot.context;

import com.xy.lucky.spring.annotations.core.*;
import com.xy.lucky.spring.annotations.event.EventListener;
import com.xy.lucky.spring.boot.annotation.ConfigurationProperties;
import com.xy.lucky.spring.boot.annotation.SpringBootApplication;
import com.xy.lucky.spring.boot.env.ConfigurableEnvironment;
import com.xy.lucky.spring.boot.env.Environment;
import com.xy.lucky.spring.boot.env.StandardEnvironment;
import com.xy.lucky.spring.core.BeanDefinition;
import com.xy.lucky.spring.core.DisposableBean;
import com.xy.lucky.spring.core.InitializingBean;
import com.xy.lucky.spring.event.ApplicationEventBus;
import com.xy.lucky.spring.event.ApplicationEventPublisher;
import com.xy.lucky.spring.exception.CyclicDependencyException;
import com.xy.lucky.spring.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.Introspector;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * DefaultApplicationContext - 默认应用上下文实现
 * <p>
 * 实现了 Bean 工厂、依赖注入等核心功能
 */
public class DefaultApplicationContext implements ConfigurableApplicationContext {

    private static final Logger log = LoggerFactory.getLogger(DefaultApplicationContext.class);

    private static final String PROTOTYPE = "prototype";
    private static final String SINGLETON = "singleton";

    private final String id;
    private final long startupDate;
    // Bean 定义和实例缓存
    private final Map<String, BeanDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<String, Object> singletons = new ConcurrentHashMap<>();
    private final Map<String, ObjectFactory<?>> earlySingletonFactories = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> creationLocks = new ConcurrentHashMap<>();
    private final ThreadLocal<Set<String>> inCreation = ThreadLocal.withInitial(HashSet::new);
    // 事件发布
    private final ApplicationEventBus applicationEventBus = new ApplicationEventBus();
    // 扫描到的类
    private final Set<Class<?>> scannedClasses = ConcurrentHashMap.newKeySet();
    // 缓存
    private final ConcurrentHashMap<Class<?>, ConstructorPlan> constructorCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, InjectionPoint[]> injectionPointCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, Optional<MethodHandle>> postConstructCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, List<String>> typeIndex = new ConcurrentHashMap<>();
    private Set<Class<?>> primarySources;
    private String[] args;
    private ConfigurableEnvironment environment;
    // 线程池
    private ExecutorService runnerPool;

    // 状态
    private volatile boolean active = false;
    private volatile boolean closed = false;

    public DefaultApplicationContext() {
        this.id = UUID.randomUUID().toString();
        this.startupDate = System.currentTimeMillis();
    }

    // ================== ConfigurableApplicationContext 接口实现 ==================

    private static String normalizeAutowiredName(Autowired autowired) {
        String name = autowired.name().trim();
        if (!name.isEmpty()) return name;
        return autowired.value().trim();
    }

    @Override
    public void setPrimarySources(Set<Class<?>> primarySources) {
        this.primarySources = primarySources;
    }

    @Override
    public void setArgs(String[] args) {
        this.args = args;
    }

    @Override
    public void refresh() {
        if (this.closed) {
            throw new IllegalStateException("ApplicationContext has been closed");
        }

        // 初始化线程池
        this.runnerPool = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                r -> new Thread(r, "AppRunner")
        );

        // 确保环境已设置
        if (this.environment == null) {
            this.environment = new StandardEnvironment();
        }

        // 扫描并注册 Bean 定义
        scanAndRegister();

        // 注册内部 Bean
        registerInternalBeans();

        // 实例化配置类
        instantiateConfigClasses();

        // 预实例化单例
        initSingletons();

        // 注册事件监听器
        registerEventListeners();

        this.active = true;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.active = false;

        // 关闭线程池
        if (runnerPool != null) {
            runnerPool.shutdown();
            try {
                if (!runnerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    runnerPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                runnerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 调用销毁回调
        for (Map.Entry<String, Object> e : singletons.entrySet()) {
            try {
                Object bean = e.getValue();
                if (bean instanceof DisposableBean) {
                    ((DisposableBean) bean).destroy();
                }
                invokePreDestroy(bean);
            } catch (Exception ex) {
                log.warn("Failed to destroy bean: " + e.getKey(), ex);
            }
        }

        // 清理资源
        applicationEventBus.close();
        definitions.clear();
        singletons.clear();
        earlySingletonFactories.clear();
        creationLocks.clear();
        constructorCache.clear();
        injectionPointCache.clear();
        postConstructCache.clear();
        typeIndex.clear();
    }

    @Override
    public void callRunners(String[] args) {
        ApplicationArguments appArgs = new ApplicationArguments(args != null ? args : this.args);

        // 执行 ApplicationRunner
        definitions.values().stream()
                .filter(def -> ApplicationRunner.class.isAssignableFrom(def.getType()))
                .map(def -> (ApplicationRunner) getBean(def.getName()))
                .forEach(r -> runnerPool.submit(() -> safeRun(r, appArgs)));

        // 执行 CommandLineRunner
        definitions.values().stream()
                .filter(def -> CommandLineRunner.class.isAssignableFrom(def.getType()))
                .map(def -> (CommandLineRunner) getBean(def.getName()))
                .forEach(r -> runnerPool.submit(() -> safeRunCommandLine(r, args)));
    }

    @Override
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close, "AppContext-ShutdownHook"));
    }

    // ================== ApplicationContext 接口实现 ==================

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getApplicationName() {
        return primarySources != null && !primarySources.isEmpty()
                ? primarySources.iterator().next().getSimpleName()
                : "Application";
    }

    @Override
    public String getDisplayName() {
        return getApplicationName();
    }

    @Override
    public long getStartupDate() {
        return this.startupDate;
    }

    @Override
    public ApplicationContext getParent() {
        return null;
    }

    @Override
    public Environment getEnvironment() {
        return this.environment;
    }

    @Override
    public void setEnvironment(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public boolean containsBean(String name) {
        return definitions.containsKey(name) || singletons.containsKey(name);
    }

    @Override
    public boolean isSingleton(String name) {
        BeanDefinition def = definitions.get(name);
        return def != null && SINGLETON.equals(def.getScope());
    }

    @Override
    public boolean isPrototype(String name) {
        BeanDefinition def = definitions.get(name);
        return def != null && PROTOTYPE.equals(def.getScope());
    }

    @Override
    public Class<?> getType(String name) {
        BeanDefinition def = definitions.get(name);
        return def != null ? def.getType() : null;
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return definitions.keySet().toArray(new String[0]);
    }

    // ================== BeanFactory 接口实现 ==================

    @Override
    public int getBeanDefinitionCount() {
        return definitions.size();
    }

    @Override
    public Object getBean(String name) {
        if (closed) throw new IllegalStateException("ApplicationContext has been closed");
        BeanDefinition def = definitions.get(name);
        if (def == null) throw new NoSuchBeanDefinitionException(name);
        if (SINGLETON.equals(def.getScope())) {
            return getSingleton(name, def);
        } else {
            return createWithHandling(name, def);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name, Class<T> requiredType) {
        Object bean = getBean(name);
        if (!requiredType.isInstance(bean)) {
            throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
        }
        return (T) bean;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        List<String> names = typeIndex.get(requiredType);
        if (names == null || names.isEmpty()) {
            // Fallback: 全表扫描
            List<Map.Entry<String, BeanDefinition>> matches = definitions.entrySet().stream()
                    .filter(e -> requiredType.isAssignableFrom(e.getValue().getType()))
                    .toList();
            if (matches.isEmpty()) throw new NoSuchBeanDefinitionException(requiredType);
            if (matches.size() > 1) throw new NoUniqueBeanDefinitionException(requiredType,
                    matches.stream().map(Map.Entry::getKey).toList());
            return (T) getBean(matches.get(0).getKey());
        }
        if (names.size() > 1) {
            throw new NoUniqueBeanDefinitionException(requiredType, names);
        }
        return (T) getBean(names.get(0));
    }

    @Override
    public Object getBean(String name, Object... args) {
        // 简化实现：忽略 args，直接返回
        return getBean(name);
    }

    @Override
    public <T> T getBean(Class<T> requiredType, Object... args) {
        return getBean(requiredType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getBeansOfType(Class<T> type) {
        Map<String, T> result = new LinkedHashMap<>();
        for (Map.Entry<String, BeanDefinition> entry : definitions.entrySet()) {
            if (type.isAssignableFrom(entry.getValue().getType())) {
                result.put(entry.getKey(), (T) getBean(entry.getKey()));
            }
        }
        return result;
    }

    // ================== 内部方法 ==================

    @Override
    public boolean containsBeanDefinition(String name) {
        return definitions.containsKey(name);
    }

    /**
     * 扫描并注册 Bean 定义
     */
    private void scanAndRegister() {
        if (primarySources == null || primarySources.isEmpty()) {
            log.warn("No primary sources to scan");
            return;
        }

        try {
            Class<?> mainClass = primarySources.iterator().next();

            // 确定扫描包路径
            String basePkg = mainClass.getPackageName();
            SpringBootApplication bootApp = mainClass.getAnnotation(SpringBootApplication.class);
            if (bootApp != null && !bootApp.scanBasePackages().isEmpty()) {
                basePkg = bootApp.scanBasePackages();
            }
            ComponentScan componentScan = mainClass.getAnnotation(ComponentScan.class);
            if (componentScan != null && !componentScan.value().isEmpty()) {
                basePkg = componentScan.value();
            }

            Set<Class<?>> classes = ClassUtils.scan(basePkg);
            scannedClasses.clear();
            scannedClasses.addAll(classes);

            // 1. 优先注册 @Configuration
            for (Class<?> cls : classes) {
                if (cls.isAnnotationPresent(Configuration.class)) {
                    registerConfigClassAndBeans(cls);
                }
            }

            // 2. 注册普通组件
            for (Class<?> cls : classes) {
                if (!cls.isAnnotationPresent(Configuration.class)) {
                    registerComponentDefinition(cls);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to scan and register beans", e);
        }
    }

    private void registerConfigClassAndBeans(Class<?> cfgClass) {
        String cfgName = deriveBeanName(cfgClass);
        if (!definitions.containsKey(cfgName)) {
            BeanDefinition cfgDef = new BeanDefinition()
                    .setType(cfgClass)
                    .setName(cfgName)
                    .setFullName(cfgClass.getName())
                    .setLazy(false)
                    .setScope(SINGLETON);
            definitions.put(cfgName, cfgDef);
            indexBeanType(cfgClass, cfgName);
        }

        for (Method m : cfgClass.getDeclaredMethods()) {
            if (!m.isAnnotationPresent(Bean.class)) continue;
            String name = Optional.of(m.getAnnotation(Bean.class).value())
                    .filter(v -> !v.isEmpty())
                    .orElse(m.getName());
            if (definitions.containsKey(name)) continue;

            FactoryMethodPlan plan = createFactoryMethodPlan(cfgClass, m);
            BeanDefinition def = new BeanDefinition()
                    .setType(m.getReturnType())
                    .setName(name)
                    .setFullName(m.getReturnType().getName())
                    .setLazy(m.isAnnotationPresent(Lazy.class))
                    .setScope(determineScope(m))
                    .setFactoryMethodHandle(plan.handle)
                    .setFactoryMethodParamTypes(plan.paramTypes)
                    .setFactoryMethodStatic(plan.isStatic)
                    .setFactoryBeanClass(cfgClass);
            definitions.put(name, def);
            indexBeanType(def.getType(), name);
        }
    }

    private void registerComponentDefinition(Class<?> cls) {
        if (!cls.isAnnotationPresent(Component.class) && !cls.isAnnotationPresent(Service.class)) return;
        String name = deriveBeanName(cls);
        if (definitions.containsKey(name)) return;

        BeanDefinition def = new BeanDefinition()
                .setType(cls)
                .setName(name)
                .setFullName(cls.getName())
                .setLazy(cls.isAnnotationPresent(Lazy.class))
                .setScope(determineScope(cls));
        definitions.put(name, def);
        indexBeanType(def.getType(), name);
    }

    private void registerInternalBeans() {
        registerSingleton("applicationContext", this);
        registerSingleton("applicationEventBus", applicationEventBus);
        registerSingleton("environment", environment);
        registerSingleton("taskExecutor", runnerPool);
    }

    private void registerSingleton(String name, Object instance) {
        BeanDefinition def = new BeanDefinition()
                .setType(instance.getClass())
                .setName(name)
                .setFullName(instance.getClass().getName())
                .setLazy(false)
                .setScope(SINGLETON);
        definitions.put(name, def);
        singletons.put(name, instance);
        indexBeanType(instance.getClass(), name);
    }

    private void instantiateConfigClasses() {
        List<String> cfgNames = definitions.entrySet().stream()
                .filter(e -> e.getValue().getType().isAnnotationPresent(Configuration.class))
                .map(Map.Entry::getKey)
                .toList();

        for (String cfgName : cfgNames) {
            try {
                getBean(cfgName);
            } catch (Exception e) {
                log.warn("Failed to instantiate configuration class: " + cfgName, e);
            }
        }
    }

    private void initSingletons() {
        for (Map.Entry<String, BeanDefinition> entry : definitions.entrySet()) {
            String name = entry.getKey();
            BeanDefinition def = entry.getValue();
            if (singletons.containsKey(name)) continue;
            if (SINGLETON.equals(def.getScope()) && !def.isLazy()) {
                try {
                    getBean(name);
                } catch (Exception e) {
                    log.warn("Failed to pre-instantiate singleton bean: " + name, e);
                }
            }
        }
    }

    private void registerEventListeners() {
        for (Map.Entry<String, BeanDefinition> e : definitions.entrySet()) {
            BeanDefinition def = e.getValue();
            if (Arrays.stream(def.getType().getDeclaredMethods())
                    .anyMatch(m -> m.isAnnotationPresent(EventListener.class))) {
                try {
                    Object bean = getBean(e.getKey());
                    applicationEventBus.registerListener(bean);
                } catch (Exception ex) {
                    log.warn("Failed to register event listener: " + e.getKey(), ex);
                }
            }
        }
    }

    private Object getSingleton(String name, BeanDefinition def) {
        Object singleton = singletons.get(name);
        if (singleton != null) return singleton;

        Object earlyRef = getEarlyReferenceIfPossible(name);
        if (earlyRef != null) return earlyRef;

        Object lock = creationLocks.computeIfAbsent(name, k -> new Object());
        synchronized (lock) {
            try {
                singleton = singletons.get(name);
                if (singleton != null) return singleton;

                Object created = createWithHandling(name, def);
                if (created != null) {
                    singletons.put(name, created);
                }
                return created;
            } finally {
                creationLocks.remove(name);
            }
        }
    }

    private Object createWithHandling(String name, BeanDefinition def) {
        try {
            return createBean(name, def);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create bean: " + name, e);
        }
    }

    private Object createBean(String name, BeanDefinition def) throws Exception {
        Set<String> creating = inCreation.get();
        boolean isProto = PROTOTYPE.equals(def.getScope());

        if (creating.contains(name)) {
            if (isProto) {
                throw new CyclicDependencyException("Prototype cyclic dependency: " + name);
            } else {
                Object early = getEarlyReferenceIfPossible(name);
                if (early != null) return early;
                throw new CyclicDependencyException("Singleton cyclic dependency without early reference: " + name);
            }
        }

        creating.add(name);
        boolean success = false;
        Object bean = null;
        try {
            bean = instantiateBean(def);
            exposeEarlySingletonIfNeeded(name, def, bean);
            injectFields(bean);
            invokeInitMethods(bean);

            if (SINGLETON.equals(def.getScope())) {
                singletons.put(name, bean);
                earlySingletonFactories.remove(name);
            }

            success = true;
            return bean;
        } finally {
            creating.remove(name);
            if (!success) {
                earlySingletonFactories.remove(name);
            }
        }
    }

    private Object instantiateBean(BeanDefinition def) throws Exception {
        if (def.hasFactoryMethod()) {
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

    private Object instantiate(Class<?> cls) throws Exception {
        ConstructorPlan plan = findConstructorForClass(cls);
        Object[] args = resolveConstructorArgs(plan.paramTypes);
        try {
            return args.length == 0 ? plan.ctor.invoke() : plan.ctor.invokeWithArguments(args);
        } catch (Throwable t) {
            if (t instanceof Exception e) throw e;
            throw new RuntimeException("Failed to instantiate: " + cls.getName(), t);
        }
    }

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
                    .orElseThrow(() -> new NoSuchBeanDefinitionException(key.getName(), "No suitable constructor found"));
        });
    }

    private ConstructorPlan createConstructorPlan(Class<?> cls, Class<?>[] paramTypes) {
        try {
            MethodHandle ctor = MethodHandles.lookup().findConstructor(cls, MethodType.methodType(void.class, paramTypes));
            return new ConstructorPlan(ctor, paramTypes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve constructor: " + cls.getName(), e);
        }
    }

    private Object[] resolveConstructorArgs(Class<?>[] paramTypes) {
        if (paramTypes == null || paramTypes.length == 0) return new Object[0];
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            args[i] = getBean(paramTypes[i]);
        }
        return args;
    }

    private Object[] resolveMethodArgs(Class<?>[] paramTypes) {
        if (paramTypes == null || paramTypes.length == 0) return new Object[0];
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            args[i] = getBean(paramTypes[i]);
        }
        return args;
    }

    private Object invokeFactoryMethod(BeanDefinition def, Object factoryBean, Object[] args) throws Exception {
        MethodHandle h = def.getFactoryMethodHandle();
        if (!def.isFactoryMethodStatic()) {
            if (factoryBean == null) throw new IllegalStateException("Factory bean is null: " + def.getName());
            h = h.bindTo(factoryBean);
        }
        try {
            return args.length == 0 ? h.invoke() : h.invokeWithArguments(args);
        } catch (Throwable t) {
            if (t instanceof Exception e) throw e;
            throw new RuntimeException("Failed to invoke factory method: " + def.getName(), t);
        }
    }

    private void exposeEarlySingletonIfNeeded(String name, BeanDefinition def, Object bean) {
        if (SINGLETON.equals(def.getScope())) {
            final Object rawBean = bean;
            earlySingletonFactories.put(name, () -> rawBean);
        }
    }

    private void injectFields(Object bean) throws Exception {
        Class<?> userClass = ClassUtils.getUserClass(bean);

        // 绑定 @ConfigurationProperties
        bindConfigurationProperties(bean, userClass);

        // 处理 @Autowired 和 @Value 注入
        InjectionPoint[] points = getInjectionPointsForClass(userClass);
        for (InjectionPoint p : points) {
            switch (p.kind) {
                case AUTOWIRED -> {
                    Object dep = resolveAutowiredDependency(p);
                    if (dep == null && p.required) {
                        throw new NoSuchBeanDefinitionException(p.type, "Required field: " + p.fieldName);
                    }
                    setField(bean, p.type, p.varHandle, dep);
                }
                case VALUE -> {
                    try {
                        String resolved = environment.resolvePlaceholders(p.valueExpression);
                        Object val = convertValue(resolved, p.type);
                        setField(bean, p.type, p.varHandle, val);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to inject @Value field: " + p.fieldName, e);
                    }
                }
            }
        }
    }

    /**
     * 绑定 @ConfigurationProperties 配置
     */
    private void bindConfigurationProperties(Object bean, Class<?> userClass) {
        if (!userClass.isAnnotationPresent(ConfigurationProperties.class)) {
            return;
        }
        try {
            ConfigurationPropertiesBinder binder = new ConfigurationPropertiesBinder(environment);
            binder.bind(bean);
            log.debug("Bound configuration properties for: {}", userClass.getName());
        } catch (Exception e) {
            log.warn("Failed to bind configuration properties for {}: {}", userClass.getName(), e.getMessage());
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
                        : InjectionPoint.value(null, f.getName(), f.getType(), value.value()));
            }

            if (list.isEmpty()) return new InjectionPoint[0];

            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandles.Lookup privateLookup;
            try {
                privateLookup = MethodHandles.privateLookupIn(key, lookup);
            } catch (Exception e) {
                throw new IllegalStateException("Cannot create privateLookup: " + key.getName(), e);
            }

            for (int i = 0; i < list.size(); i++) {
                InjectionPoint p = list.get(i);
                try {
                    VarHandle vh = privateLookup.findVarHandle(key, p.fieldName, p.type);
                    list.set(i, p.withVarHandle(vh));
                } catch (Exception e) {
                    throw new IllegalStateException("Cannot resolve field handle: " + key.getName() + "." + p.fieldName, e);
                }
            }
            return list.toArray(new InjectionPoint[0]);
        });
    }

    private Object resolveAutowiredDependency(InjectionPoint p) {
        Object dep = null;
        if (p.beanName != null && !p.beanName.isEmpty()) {
            dep = getBeanOrNullByName(p.beanName, p.type);
        }
        if (dep == null && p.required) {
            try {
                dep = getBean(p.type);
            } catch (NoSuchBeanDefinitionException ex) {
                dep = getBeanOrNullByName(p.fieldName, p.type);
            }
        }
        return dep;
    }

    private Object getBeanOrNullByName(String name, Class<?> expectedType) {
        BeanDefinition bd = definitions.get(name);
        if (bd != null && expectedType.isAssignableFrom(bd.getType())) {
            return getBean(name);
        }
        return null;
    }

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
                throw new RuntimeException("Failed to invoke @PostConstruct: " + bean.getClass().getName(), t);
            }
        }
    }

    private Optional<MethodHandle> getPostConstructHandleForClass(Class<?> cls) {
        return postConstructCache.computeIfAbsent(cls, key -> {
            for (Method m : key.getDeclaredMethods()) {
                if (!m.isAnnotationPresent(PostConstruct.class)) continue;
                if (m.getParameterCount() > 0) {
                    throw new IllegalStateException("@PostConstruct method cannot have parameters: " + m.getName());
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
                    throw new IllegalStateException("Failed to resolve @PostConstruct handle: " + key.getName() + "." + m.getName(), e);
                }
            }
            return Optional.empty();
        });
    }

    private void invokePreDestroy(Object bean) throws Exception {
        Class<?> userClass = ClassUtils.getUserClass(bean);
        for (Method m : userClass.getDeclaredMethods()) {
            if (!m.isAnnotationPresent(PreDestroy.class)) continue;
            if (m.getParameterCount() > 0) {
                throw new IllegalStateException("@PreDestroy method cannot have parameters: " + userClass.getName() + "." + m.getName());
            }
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MethodHandles.Lookup privateLookup = MethodHandles.privateLookupIn(userClass, lookup);
                MethodType type = MethodType.methodType(m.getReturnType());
                MethodHandle h = java.lang.reflect.Modifier.isStatic(m.getModifiers())
                        ? privateLookup.findStatic(userClass, m.getName(), type)
                        : privateLookup.findVirtual(userClass, m.getName(), type).bindTo(bean);
                h.invoke();
                return;
            } catch (Throwable t) {
                if (t instanceof Exception e) throw e;
                throw new RuntimeException("Failed to invoke @PreDestroy: " + userClass.getName() + "." + m.getName(), t);
            }
        }
    }

    private Object getEarlyReferenceIfPossible(String name) {
        ObjectFactory<?> factory = earlySingletonFactories.get(name);
        if (factory != null) {
            try {
                return factory.getObject();
            } catch (Exception e) {
                log.warn("Failed to get early reference: {}", name, e);
            }
        }
        return null;
    }

    private void indexBeanType(Class<?> type, String beanName) {
        typeIndex.compute(type, (k, v) -> {
            if (v == null) return new ArrayList<>(List.of(beanName));
            if (!v.contains(beanName)) v.add(beanName);
            return v;
        });
        for (Class<?> itf : type.getInterfaces()) {
            typeIndex.compute(itf, (k, v) -> {
                if (v == null) return new ArrayList<>(List.of(beanName));
                if (!v.contains(beanName)) v.add(beanName);
                return v;
            });
        }
        Class<?> sup = type.getSuperclass();
        if (sup != null && sup != Object.class) {
            typeIndex.compute(sup, (k, v) -> {
                if (v == null) return new ArrayList<>(List.of(beanName));
                if (!v.contains(beanName)) v.add(beanName);
                return v;
            });
        }
    }

    private String deriveBeanName(Class<?> cls) {
        String val = Optional.ofNullable(cls.getAnnotation(Component.class)).map(Component::value)
                .filter(v -> !v.isEmpty())
                .orElse(Optional.ofNullable(cls.getAnnotation(Service.class)).map(Service::value).orElse(""));

        if (val.isEmpty()) {
            val = Optional.ofNullable(cls.getAnnotation(Configuration.class)).map(Configuration::value).orElse("");
        }

        return val.isEmpty() ? Introspector.decapitalize(cls.getSimpleName()) : val;
    }

    private String determineScope(Class<?> cls) {
        return Optional.ofNullable(cls.getAnnotation(Scope.class))
                .map(Scope::value)
                .filter(PROTOTYPE::equals)
                .orElse(SINGLETON);
    }

    private String determineScope(Method factoryMethod) {
        Scope scope = factoryMethod.getAnnotation(Scope.class);
        if (scope != null && PROTOTYPE.equals(scope.value())) return PROTOTYPE;
        return SINGLETON;
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
            throw new IllegalStateException("Failed to resolve factory method: " + declaringClass.getName() + "." + m.getName(), e);
        }
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

    private Object convertValue(String value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType == String.class) return value;
        if (targetType == Integer.class || targetType == int.class) return Integer.parseInt(value);
        if (targetType == Long.class || targetType == long.class) return Long.parseLong(value);
        if (targetType == Boolean.class || targetType == boolean.class) return Boolean.parseBoolean(value);
        if (targetType == Double.class || targetType == double.class) return Double.parseDouble(value);
        if (targetType == Float.class || targetType == float.class) return Float.parseFloat(value);
        return value;
    }

    private void safeRun(ApplicationRunner runner, ApplicationArguments args) {
        try {
            runner.run(args);
        } catch (Exception e) {
            log.error("ApplicationRunner execution failed", e);
        }
    }

    private void safeRunCommandLine(CommandLineRunner runner, String[] args) {
        try {
            runner.run(args);
        } catch (Exception e) {
            log.error("CommandLineRunner execution failed", e);
        }
    }

    /**
     * 获取事件发布器
     */
    public ApplicationEventPublisher getEventPublisher() {
        return applicationEventBus;
    }

    /**
     * 获取已扫描的类集合
     */
    public Set<Class<?>> getScannedClasses() {
        return Collections.unmodifiableSet(scannedClasses);
    }

    // ================== 内部类 ==================

    private enum InjectionKind {AUTOWIRED, VALUE}

    @FunctionalInterface
    private interface ObjectFactory<T> {
        T getObject();
    }

    private record ConstructorPlan(MethodHandle ctor, Class<?>[] paramTypes) {
    }

    private record FactoryMethodPlan(MethodHandle handle, Class<?>[] paramTypes, boolean isStatic) {
    }

    private static final class InjectionPoint {
        private final InjectionKind kind;
        private final VarHandle varHandle;
        private final String fieldName;
        private final Class<?> type;
        private final String beanName;
        private final boolean required;
        private final String valueExpression;

        private InjectionPoint(InjectionKind kind, VarHandle varHandle, String fieldName, Class<?> type,
                               String beanName, boolean required, String valueExpression) {
            this.kind = kind;
            this.varHandle = varHandle;
            this.fieldName = fieldName;
            this.type = type;
            this.beanName = beanName;
            this.required = required;
            this.valueExpression = valueExpression;
        }

        static InjectionPoint autowired(VarHandle varHandle, String fieldName, Class<?> type, String beanName, boolean required) {
            return new InjectionPoint(InjectionKind.AUTOWIRED, varHandle, fieldName, type, beanName, required, null);
        }

        static InjectionPoint value(VarHandle varHandle, String fieldName, Class<?> type, String valueExpression) {
            return new InjectionPoint(InjectionKind.VALUE, varHandle, fieldName, type, null, true, valueExpression);
        }

        InjectionPoint withVarHandle(VarHandle varHandle) {
            return new InjectionPoint(this.kind, varHandle, this.fieldName, this.type, this.beanName, this.required, this.valueExpression);
        }
    }
}
