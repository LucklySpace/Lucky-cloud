package com.xy.lucky.job.core;

import com.xy.lucky.job.dto.TriggerRequest;
import com.xy.lucky.job.dto.TriggerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;

/**
 * 任务执行控制器
 * 接收调度中心的调度请求
 *
 * @author lucky
 */
@RestController
@RequestMapping("/lucky-job")
@Slf4j
public class JobExecutorController {

    @PostMapping("/run")
    public TriggerResponse run(@RequestBody TriggerRequest request) {
        if (request == null || request.getJobName() == null) {
            return TriggerResponse.fail("Invalid request");
        }

        JobRegistry.JobHandler jobHandler = JobRegistry.getJobHandler(request.getJobName());
        if (jobHandler == null) {
            return TriggerResponse.fail("Job not found: " + request.getJobName());
        }

        try {
            log.info(">>>>>> lucky-job trigger job: {}, params: {}", request.getJobName(), request.getParams());

            Method method = jobHandler.getMethod();
            Object target = jobHandler.getTarget();

            // 参数适配
            // 简单支持：无参 或 1个String参数
            Class<?>[] paramTypes = method.getParameterTypes();
            Object result;
            if (paramTypes.length == 0) {
                result = method.invoke(target);
            } else if (paramTypes.length == 1 && paramTypes[0] == String.class) {
                result = method.invoke(target, request.getParams());
            } else {
                return TriggerResponse.fail("Job method signature not supported. Only support no-args or single String arg.");
            }

            log.info(">>>>>> lucky-job execute success");
            return TriggerResponse.success(result);
        } catch (Throwable e) {
            log.error(">>>>>> lucky-job execute error", e);
            return TriggerResponse.fail("Execute error: " + e.getMessage());
        }
    }

    @PostMapping("/beat")
    public TriggerResponse beat() {
        return TriggerResponse.success();
    }
}
