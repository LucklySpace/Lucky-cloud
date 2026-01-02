package com.xy.lucky.quartz.job;

import com.xy.lucky.quartz.domain.context.ShardingContext;
import com.xy.lucky.quartz.domain.context.TaskLogger;
import com.xy.lucky.quartz.domain.po.TaskInfoPo;
import com.xy.lucky.quartz.service.JobRegistryService;
import lombok.Data;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;

@Component
@DisallowConcurrentExecution
public class RemoteHttpJob extends BaseJob implements ApplicationContextAware {

    private final RestTemplate restTemplate = new RestTemplate();
    private ApplicationContext applicationContext;
    private JobRegistryService jobRegistryService;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.jobRegistryService = applicationContext.getBean(JobRegistryService.class);
    }

    @Override
    protected void executeTask(TaskInfoPo taskInfoPo, ShardingContext context, TaskLogger logger) throws Exception {
        String appName = taskInfoPo.getAppName();
        String jobHandler = taskInfoPo.getJobHandler();

        if (appName == null || jobHandler == null) {
            throw new RuntimeException("Remote job config missing: appName or jobHandler is null");
        }

        logger.log("开始调度远程任务: " + appName + " / " + jobHandler);

        // 1. Get Address
        String address = jobRegistryService.getAvailableAddress(appName);
        if (address == null) {
            throw new RuntimeException("No available instance for app: " + appName);
        }

        String url = "http://" + address + "/lucky-job/run";
        logger.log("调度地址: " + url);

        // 2. Prepare Request
        TriggerRequest request = new TriggerRequest();
        request.setJobName(jobHandler);
        request.setParams(taskInfoPo.getJobData());
        request.setLogId(0L); // Optional
        request.setTimestamp(System.currentTimeMillis());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<TriggerRequest> entity = new HttpEntity<>(request, headers);

        // 3. Send Request
        try {
            ResponseEntity<TriggerResponse> response = restTemplate.postForEntity(url, entity, TriggerResponse.class);
            TriggerResponse body = response.getBody();

            if (body != null && body.getCode() == 200) {
                logger.log("远程执行成功: " + body.getContent());
            } else {
                String msg = body != null ? body.getMsg() : "Empty response";
                throw new RuntimeException("Remote execution failed: " + msg);
            }
        } catch (Exception e) {
            throw new RuntimeException("Request failed: " + e.getMessage(), e);
        }
    }

    @Data
    static class TriggerRequest implements Serializable {
        private String jobName;
        private String params;
        private Long logId;
        private Long timestamp;
    }

    @Data
    static class TriggerResponse implements Serializable {
        private int code;
        private String msg;
        private Object content;
    }
}
