package com.xy.spring.context;


import com.xy.spring.annotations.SpringApplication;
import com.xy.spring.annotations.core.*;
import com.xy.spring.annotations.event.EventListener;
import com.xy.spring.aop.BeanPostProcessor;
import com.xy.spring.aop.ProxyBeanPostProcessor;
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
import com.xy.spring.utils.YamlConfigLoader;
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
public class ApplicationContext<T> implements BeanFactory<T> {

    /**
     * 原型作用域标识
     */
    private static final String PROTOTYPE = "prototype";
    /**
     * 单例作用域标识
     */
    private static final String SINGLETON = "singleton";

    /**
     * 启动主类，用于确定基础包路径和注解校验
     */
    private final Class<T> startupClass;
    /**
     * 用于并行执行 ApplicationRunner 的线程池
     */
    private final ExecutorService runnerPool;
    /**
     * 所有 Bean 的定义信息：name -> BeanDefinition
     */
    private final Map<String, BeanDefinition> definitions = new ConcurrentHashMap<>();
    /**
     * 完全初始化后的单例实例缓存：name -> 实例
     */
    private final Map<String, Object> singletons = new ConcurrentHashMap<>();
    /**
     * 早期曝光的单例实例，用于解决单例循环依赖
     */
    private final Map<String, Object> earlySingletons = new ConcurrentHashMap<>();
    /**
     * 注册的 BeanPostProcessor 列表，用于 Bean 初始化前后增强
     */
    private final List<BeanPostProcessor> postProcessors = new ArrayList<>();
    /**
     * 当前线程正在创建的 Bean 名称集合，用于检测循环依赖
     */
    private final ThreadLocal<Set<String>> inCreation = ThreadLocal.withInitial(HashSet::new);
    /**
     * 全局异常处理器，统一捕获并处理运行时异常
     */
    private final ExceptionHandler exceptionHandler = new GlobalExceptionHandler();
    /**
     * 事件总线，用于发布和订阅应用事件
     */
    private final ApplicationEventBus applicationEventBus = new ApplicationEventBus();

    /**
     * 构造方法：
     * 1. 校验启动类注解
     * 2. 加载 YAML 配置
     * 3. 扫描并注册 BeanDefinition
     * 4. 预初始化非懒加载单例
     * 5. 注册事件监听器
     * 6. 异步执行 ApplicationRunner
     *
     * @param startupClass 启动主类
     * @param args         启动参数
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
        initSingletons();               // 预实例化所有非懒加载单例
        registerEventListeners();       // 注册事件监听器
        runRunners(args);               // 执行 ApplicationRunner
    }

    /**
     * 暴露事件发布器，用于外部手动发布事件
     */
    public ApplicationEventPublisher getEventPublisher() {
        return applicationEventBus;
    }

    /**
     * 校验启动主类是否添加 @SpringApplication 注解
     */
    private void validateAnnotation() {
        if (!startupClass.isAnnotationPresent(SpringApplication.class)) {
            throw new IllegalArgumentException("启动类缺少 @SpringApplication 注解");
        }
    }

    /**
     * 扫描基础包并注册 @Component/@Service 以及 @Configuration 中的 @Bean
     */
    private void scanAndRegister() {
        try {
            // 获取扫描包路径
            String basePkg = Optional.of(startupClass.getAnnotation(SpringApplication.class).value())
                    .filter(v -> !v.isEmpty())
                    .orElse(startupClass.getPackageName());
            Set<Class<?>> classes = ClassUtils.scan(basePkg);

            // 添加 AOP 后置处理器
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
     * 注册普通组件（@Component 或 @Service），并对 BeanPostProcessor 类型提前实例化
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
                .setProxyType(Optional.ofNullable(cls.getAnnotation(Component.class))
                        .map(Component::proxy)
                        .orElse(ProxyType.NONE));
        definitions.put(name, def);

        // 若为后置处理器，立即创建并加入列表
        if (BeanPostProcessor.class.isAssignableFrom(cls)) {
            postProcessors.add((BeanPostProcessor) createBean(name, def));
        }
    }

    /**
     * 注册配置类中的 @Bean 方法，支持工厂方法注入参数
     */
    private void registerConfigBean(Class<?> cls) throws Exception {
        if (!cls.isAnnotationPresent(Configuration.class)) return;
        // 先实例化配置类本身
        Object configObj = instantiate(cls);
        for (Method m : cls.getDeclaredMethods()) {
            if (!m.isAnnotationPresent(Bean.class)) continue;
            String name = Optional.of(m.getAnnotation(Bean.class).value())
                    .filter(v -> !v.isEmpty())
                    .orElse(m.getName());

            // 构建定义并缓存
            BeanDefinition def = new BeanDefinition()
                    .setType(m.getReturnType())
                    .setName(name)
                    .setFullName(m.getReturnType().getName())
                    .setLazy(m.isAnnotationPresent(Lazy.class))
                    .setScope(determineScope(m.getReturnType()))
                    .setFactoryMethod(m)
                    .setFactoryBean(configObj)
                    .setProxyType(ProxyType.NONE);
            definitions.put(name, def);
        }
    }

    /**
     * 根据注解或类名生成 Bean 名称，优先注解 value
     */
    private String deriveBeanName(Class<?> cls) {
        String val = Optional.ofNullable(cls.getAnnotation(Component.class)).map(Component::value)
                .filter(v -> !v.isEmpty())
                .orElse(Optional.ofNullable(cls.getAnnotation(Service.class)).map(Service::value).orElse(""));
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
     * 预实例化所有非懒加载单例 Bean，确保单例提前创建
     */
    private void initSingletons() {
        definitions.forEach((name, def) -> {
            if (SINGLETON.equals(def.getScope()) && !def.isLazy()) {
                getBean(name);
            }
        });
    }


    /**
     * 扫描并注册所有标注了 @EventListener 的方法对应的 Bean 为事件监听器
     */
    private void registerEventListeners() {
        definitions.forEach((name, def) -> {
            if (Arrays.stream(def.getType().getDeclaredMethods())
                    .anyMatch(m -> m.isAnnotationPresent(EventListener.class))) {
                applicationEventBus.registerListener(getBean(name));
            }
        });
    }

    /**
     * 获取 Bean 对外接口，区分单例与原型
     */
    @Override
    public Object getBean(String name) {
        BeanDefinition def = definitions.get(name);
        if (def == null) return null;
        // 单例使用缓存或早期曝光，原型每次创建
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
     * 获取单例实例，支持早期曝光解决循环依赖
     */
    private Object getSingleton(String name, BeanDefinition def) {
        // 先检查早期曝光缓存
        if (earlySingletons.containsKey(name)) {
            return earlySingletons.get(name);
        }
        // 否则原子方式创建并缓存
        return singletons.computeIfAbsent(name, n -> createWithHandling(n, def));
    }

    /**
     * 包装创建逻辑并统一异常处理
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
     * 核心创建流程：检测循环依赖 → 实例化 → 早期曝光 → 前置处理 → 字段注入 → 初始化回调 → 后置处理 → 清理早期曝光
     */
    private Object createBean(String name, BeanDefinition def) throws Exception {
        Set<String> creating = inCreation.get();
        boolean isProto = PROTOTYPE.equals(def.getScope());

        // 循环依赖检测：原型不支持循环依赖，单例通过早期曝光解决
        if (creating.contains(name)) {
            if (isProto) {
                throw new CyclicDependencyException("原型循环依赖: " + name);
            } else {
                return earlySingletons.get(name);
            }
        }
        creating.add(name);
        try {
            // 实例化 Bean：优先工厂方法，其次构造器注入，再无参构造
            Object bean = def.hasFactoryMethod()
                    ? def.getFactoryMethod().invoke(def.getFactoryBean(), resolveMethodArgs(def.getFactoryMethod()))
                    : instantiate(def.getType());

            // 单例 Bean 早期曝光，供依赖注入阶段使用
            if (SINGLETON.equals(def.getScope())) {
                earlySingletons.put(name, bean);
            }

            // 前置处理：BeanPostProcessor.postProcessBeforeInitialization
            bean = postProcessors.stream()
                    .reduce(bean, (b, p) -> p.postProcessBeforeInitialization(b, name, def.getProxyType()), (a, b) -> b);

            // 字段注入
            injectFields(bean);

            // 初始化回调：InitializingBean 和 @PostConstruct
            invokeInitMethods(bean);

            // 后置处理：BeanPostProcessor.postProcessAfterInitialization
            bean = postProcessors.stream()
                    .reduce(bean, (b, p) -> p.postProcessAfterInitialization(b, name), (a, b) -> b);

            // 移除早期曝光缓存
            earlySingletons.remove(name);

            return bean;

        } finally {
            creating.remove(name);
        }
    }

    /**
     * 解析工厂方法参数，支持其他 Bean 注入
     */
    private Object[] resolveMethodArgs(Method m) {
        return Arrays.stream(m.getParameterTypes())
                .map(this::getBean)
                .toArray();
    }

    /**
     * 实例化 Bean：优先带 @Autowired 的构造器
     */
    private Object instantiate(Class<?> cls) throws Exception {
        // 构造器注入优先
        for (Constructor<?> c : cls.getConstructors()) {
            if (c.isAnnotationPresent(Autowired.class)) {
                return c.newInstance(resolveConstructorArgs(c));
            }
        }
        // 无参构造回退
        Constructor<?> noArg = Arrays.stream(cls.getConstructors())
                .filter(c -> c.getParameterCount() == 0)
                .findFirst()
                .orElseThrow(() -> new NoSuchBeanException("缺少可用构造器: " + cls.getName()));
        return noArg.newInstance();
    }

    /**
     * 注入 @Autowired 与 @Value 注解字段
     */
    private Object[] resolveConstructorArgs(Constructor<?> c) {
        return Arrays.stream(c.getParameterTypes())
                .map(this::getBean)
                .toArray();
    }

    /**
     * 注入字段，包括 @Autowired 和 @Value 注解
     */
    private void injectFields(Object bean) throws IllegalAccessException {
        // 遍历所有字段，支持按类型或按名称注入
        for (Field f : bean.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (f.isAnnotationPresent(Autowired.class)) {
                // 按名称或按类型查找依赖
                Object dep = getBean(f.getName());
                if (dep == null) {
                    dep = getBean(f.getType());
                }
                // 按类型注入
                if (dep == null) {
                    throw new NoSuchBeanException("无法注入字段: " + f.getName());
                }
                f.set(bean, dep);
            } else if (f.isAnnotationPresent(Value.class)) {
                Object val = YamlConfigLoader.get(f.getAnnotation(Value.class).value(), f);
                f.set(bean, val);
            }
        }
    }


    /**
     * 调用初始化回调，包括 InitializingBean.afterPropertiesSet 和 @PostConstruct 方法
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
     * 优雅关闭线程池，等待所有任务完成
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

