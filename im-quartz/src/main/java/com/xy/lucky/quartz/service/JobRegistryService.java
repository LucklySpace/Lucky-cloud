package com.xy.lucky.quartz.service;

import com.xy.lucky.quartz.domain.dto.RegistryParam;
import com.xy.lucky.quartz.domain.po.JobRegistry;
import com.xy.lucky.quartz.repository.JobRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobRegistryService {

    private final JobRegistryRepository jobRegistryRepository;
    private final DiscoveryClient discoveryClient;

    @Transactional
    public void register(RegistryParam param) {
        if (param == null || param.getAppName() == null || param.getAddress() == null) {
            return;
        }

        JobRegistry registry = jobRegistryRepository.findByAppNameAndAddress(param.getAppName(), param.getAddress());
        if (registry == null) {
            registry = new JobRegistry();
            registry.setAppName(param.getAppName());
            registry.setAddress(param.getAddress());
        }

        registry.setStatus(1); // Online
        registry.setUpdateTime(LocalDateTime.now());
        jobRegistryRepository.save(registry);

        log.debug("Registry updated: appName={}, address={}", param.getAppName(), param.getAddress());
    }

    @Transactional
    public void remove(RegistryParam param) {
        if (param == null || param.getAppName() == null || param.getAddress() == null) {
            return;
        }

        JobRegistry registry = jobRegistryRepository.findByAppNameAndAddress(param.getAppName(), param.getAddress());
        if (registry != null) {
            registry.setStatus(0); // Offline
            jobRegistryRepository.save(registry);
        }
    }

    /**
     * Get available address (Simple Random Load Balance)
     */
    public String getAvailableAddress(String appName) {
        List<JobRegistry> list = jobRegistryRepository.findByAppNameAndStatus(appName, 1);
        if (list == null || list.isEmpty()) {
            return null;
        }

        // Filter dead nodes if necessary (though query already does status=1)
        // Check timeout strictly? Maybe 
        // Here we trust the status which is updated by background thread

        if (list.size() == 1) {
            return list.get(0).getAddress();
        }

        return list.get(ThreadLocalRandom.current().nextInt(list.size())).getAddress();
    }

    public List<String> getAllAvailableAddresses(String appName) {
        List<JobRegistry> list = jobRegistryRepository.findByAppNameAndStatus(appName, 1);
        if (list == null) return Collections.emptyList();

        List<String> addresses = new ArrayList<>();
        for (JobRegistry r : list) {
            addresses.add(r.getAddress());
        }
        return addresses;
    }

    /**
     * Clean dead registry
     * 30s timeout
     */
    @Scheduled(fixedRate = 10000, initialDelay = 10000)
    @Transactional
    public void registryMonitor() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(45);
        // Mark as offline if no heartbeat for 45s
        int count = jobRegistryRepository.updateDead(threshold);
        if (count > 0) {
            log.info("Registry monitor: marked {} instances as offline", count);
        }

        // Delete very old records (e.g. 1 day)
        // jobRegistryRepository.deleteDead(LocalDateTime.now().minusDays(1));
    }

    /**
     * 获取所有服务
     *
     * @return
     */
    public List<String> getAllServices() {
        return discoveryClient.getServices().stream().filter(serviceName -> serviceName.startsWith("im-")).toList();
    }
}
