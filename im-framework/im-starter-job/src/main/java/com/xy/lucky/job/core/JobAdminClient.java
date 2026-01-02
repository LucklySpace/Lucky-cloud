package com.xy.lucky.job.core;

import com.xy.lucky.job.config.LuckyJobProperties;
import com.xy.lucky.job.dto.RegistryParam;
import com.xy.lucky.utils.ip.IPAddressUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 任务调度中心客户端
 * 负责向调度中心注册执行器和任务
 *
 * @author lucky
 */
@Slf4j
public class JobAdminClient implements InitializingBean, DisposableBean {

    private final LuckyJobProperties properties;
    private final Environment environment;
    private final RestTemplate restTemplate = new RestTemplate();
    private Thread registryThread;
    private volatile boolean toStop = false;

    public JobAdminClient(LuckyJobProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!properties.isEnabled()) {
            return;
        }

        // 启动注册线程
        registryThread = new Thread(() -> {
            while (!toStop) {
                try {
                    RegistryParam registryParam = getRegistryParam();
                    if (registryParam != null) {
                        doRegister(registryParam);
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        log.error(">>>>>> lucky-job registry error: {}", e.getMessage());
                    }
                }

                try {
                    if (!toStop) {
                        TimeUnit.SECONDS.sleep(30);
                    }
                } catch (InterruptedException e) {
                    if (!toStop) {
                        log.warn(">>>>>> lucky-job registry thread interrupted");
                    }
                }
            }
        });
        registryThread.setDaemon(true);
        registryThread.setName("lucky-job-registry-thread");
        registryThread.start();
    }

    @Override
    public void destroy() throws Exception {
        toStop = true;
        if (registryThread != null) {
            registryThread.interrupt();
            try {
                registryThread.join();
            } catch (InterruptedException e) {
                log.error(">>>>>> lucky-job registry thread destroy error", e);
            }
        }
        // TODO: 可以添加注销逻辑
    }

    private RegistryParam getRegistryParam() {
        String appName = properties.getAppName();
        if (!StringUtils.hasText(appName)) {
            appName = environment.getProperty("spring.application.name");
        }
        if (!StringUtils.hasText(appName)) {
            log.warn(">>>>>> lucky-job registry skip: appName is empty");
            return null;
        }

        String ip = properties.getIp();
        if (!StringUtils.hasText(ip)) {
            ip = IPAddressUtil.getPrimaryIpv4AddressOrEmpty();
        }

        Integer port = properties.getPort();
        if (port == null || port <= 0) {
            String portStr = environment.getProperty("server.port");
            if (StringUtils.hasText(portStr)) {
                port = Integer.valueOf(portStr);
            } else {
                port = 8080; // default
            }
        }

        String address = ip + ":" + port;

        RegistryParam param = new RegistryParam();
        param.setAppName(appName);
        param.setAddress(address);

        List<RegistryParam.JobInfo> jobInfos = new ArrayList<>();
        JobRegistry.getJobHandlerMap().forEach((name, handler) -> {
            RegistryParam.JobInfo info = new RegistryParam.JobInfo();
            info.setName(name);
            info.setDescription(handler.getAnnotation().description());
            info.setInitParams(handler.getAnnotation().initParams());
            jobInfos.add(info);
        });
        param.setJobs(jobInfos);

        return param;
    }

    private void doRegister(RegistryParam param) {
        String adminAddresses = properties.getAdminAddresses();
        if (!StringUtils.hasText(adminAddresses)) {
            log.warn(">>>>>> lucky-job registry skip: adminAddresses is empty");
            return;
        }

        String[] addresses = adminAddresses.split(",");
        for (String addr : addresses) {
            try {
                String url = addr.trim();
                if (!url.startsWith("http")) {
                    url = "http://" + url;
                }
                if (url.endsWith("/")) {
                    url = url.substring(0, url.length() - 1);
                }

                String apiUrl = url + "/api/registry/register";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (StringUtils.hasText(properties.getAccessToken())) {
                    headers.set("Lucky-Access-Token", properties.getAccessToken());
                }

                HttpEntity<RegistryParam> request = new HttpEntity<>(param, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.debug(">>>>>> lucky-job registry success: {}", apiUrl);
                    // 只要有一个注册成功即可
                    break;
                } else {
                    log.warn(">>>>>> lucky-job registry failed: {}, status: {}", apiUrl, response.getStatusCode());
                }
            } catch (Exception e) {
                log.warn(">>>>>> lucky-job registry error: {} {}", addr, e.getMessage());
            }
        }
    }
}
