package com.xy.lucky.job.core;

import com.xy.lucky.job.annotation.LuckyJob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务注册中心
 * 负责扫描 @LuckyJob 注解的方法并注册到本地容器
 *
 * @author lucky
 */
@Slf4j
public class JobRegistry implements ApplicationContextAware, InitializingBean, DisposableBean {

    /**
     * 任务处理器缓存
     * key: jobName
     * value: JobHandler
     */
    private static final Map<String, JobHandler> JOB_HANDLER_MAP = new ConcurrentHashMap<>();
    private ApplicationContext applicationContext;

    public static JobHandler getJobHandler(String jobName) {
        return JOB_HANDLER_MAP.get(jobName);
    }

    public static Map<String, JobHandler> getJobHandlerMap() {
        return JOB_HANDLER_MAP;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initJobHandler();
    }

    private void initJobHandler() {
        String[] beanNames = applicationContext.getBeanNamesForType(Object.class);
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Map<Method, LuckyJob> annotatedMethods = null;
            try {
                // 获取当前Bean的所有方法
                Method[] methods = ReflectionUtils.getAllDeclaredMethods(bean.getClass());
                for (Method method : methods) {
                    LuckyJob luckyJob = AnnotationUtils.findAnnotation(method, LuckyJob.class);
                    if (luckyJob != null) {
                        String jobName = luckyJob.value();
                        if (jobName == null || jobName.trim().length() == 0) {
                            jobName = method.getName();
                        }

                        if (JOB_HANDLER_MAP.containsKey(jobName)) {
                            throw new RuntimeException("lucky-job jobName[" + jobName + "] conflict.");
                        }

                        // 允许访问私有方法
                        ReflectionUtils.makeAccessible(method);

                        JobHandler handler = new JobHandler(bean, method, luckyJob);
                        JOB_HANDLER_MAP.put(jobName, handler);
                        log.info(">>>>>> lucky-job register jobName success: {}", jobName);
                    }
                }
            } catch (Throwable ex) {
                // ignore
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        JOB_HANDLER_MAP.clear();
    }

    /**
     * 内部类：任务处理器封装
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JobHandler {
        private Object target;
        private Method method;
        private LuckyJob annotation;

        public Object execute(Object... args) throws Exception {
            return method.invoke(target, args);
        }
    }
}
