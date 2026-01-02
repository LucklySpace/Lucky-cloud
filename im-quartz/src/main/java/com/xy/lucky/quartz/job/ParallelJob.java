package com.xy.lucky.quartz.job;

import com.xy.lucky.quartz.domain.context.ShardingContext;
import com.xy.lucky.quartz.domain.context.TaskLogger;
import com.xy.lucky.quartz.domain.po.TaskInfoPo;
import com.xy.lucky.quartz.task.ITask;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 并行任务执行器
 */
@Component
public class ParallelJob extends BaseJob implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void executeTask(TaskInfoPo taskInfoPo, ShardingContext context, TaskLogger logger) throws Exception {
        String className = taskInfoPo.getJobClass();
        Object instance;

        // 从 spring context 获取 实例
        if (applicationContext.containsBean(className)) {
            instance = applicationContext.getBean(className);
        } else {
            // Fallback to class instantiation
            try {
                Class<?> clazz = Class.forName(className);
                instance = clazz.getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException e) {
                // Try to find by bean name if class not found, but we checked containsBean above.
                // Maybe className is a short bean name but not exactly matching?
                // For now assume it is class name if not a bean.
                throw new RuntimeException("Job class or bean not found: " + className);
            }
        }

        if (instance instanceof ITask) {
            ((ITask) instance).execute(taskInfoPo.getJobData(), context, logger);
        } else {
            // Legacy support
            try {
                Method method = instance.getClass().getMethod("execute", String.class);
                method.invoke(instance, taskInfoPo.getJobData());
            } catch (NoSuchMethodException e) {
                if (instance instanceof Runnable) {
                    ((Runnable) instance).run();
                } else {
                    throw new RuntimeException("Class " + className + " must implement ITask, Runnable or have execute(String) method");
                }
            }
        }
    }
}
