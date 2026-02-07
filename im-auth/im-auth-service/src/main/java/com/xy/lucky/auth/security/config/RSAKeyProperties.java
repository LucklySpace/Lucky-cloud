package com.xy.lucky.auth.security.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.xy.lucky.security.util.RSAUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * RSA 密钥管理器 - 基于 Nacos 配置中心
 * <p>
 * <p>
 * 1. 密钥存储在 Nacos，不写入本地文件
 * 2. 支持版本管理和平滑过渡（保留前一版本用于解密）
 * 3. 通过 Nacos 监听器自动刷新
 */
@Slf4j
@Setter
@ConfigurationProperties(prefix = "security.rsa")
public class RSAKeyProperties {

    private static final int KEY_SIZE = 1024;
    private static final long TIMEOUT_MS = 5000;
    private static final String LOCK_DATA_ID = "rsa-key-refresh.lock";
    private static final long LOCK_TTL_MS = TimeUnit.SECONDS.toMillis(10);
    private final String instanceId = UUID.randomUUID().toString().substring(0, 10);
    /**
     * 定时刷新间隔（毫秒）
     */
    private final String checkInterval = "1800000";
    /**
     * 盐值
     */
    private String secret;
    /**
     * 密钥数据ID
     */
    private String dataId;
    /**
     * 密钥类型 （可选：text/yaml）
     */
    private String keyType = "yaml";
    /**
     * 密钥有效期（小时）
     */
    private Long keyTtl;
    /**
     * 是否启用定时刷新
     */
    private boolean refreshEnabled = false;
    // 运行时状态
    private volatile RSAKeyData current;
    private volatile RSAKeyData previous;
    private ConfigService configService;

    @Value("${spring.cloud.nacos.config.group:DEFAULT_GROUP}")
    private String group = "DEFAULT_GROUP";

    @Autowired
    public RSAKeyProperties(NacosConfigManager nacosConfigManager) {
        this.configService = nacosConfigManager.getConfigService();
    }

    @PostConstruct
    public void init() throws Exception {

        // 加载或生成密钥
        loadOrGenerateKeys();

        // 注册配置监听器
        registerListener();

        log.info("RSA 密钥管理器初始化完成，当前版本: {}", current.getVersion());
    }

    @PreDestroy
    public void destroy() throws NacosException {
        if (configService != null) {
            configService.shutDown();
        }
    }

    /**
     * 加载或生成 RSA 密钥
     */
    private void loadOrGenerateKeys() throws Exception {
        String config = configService.getConfig(dataId, group, TIMEOUT_MS);

        if (config != null && !config.isEmpty()) {
            // 从 Nacos 加载 YAML 配置
            RSAKeyConfig keyConfig = parseYaml(config);
            this.current = parseKeyData(keyConfig.getCurrent());
            if (keyConfig.getPrevious() != null) {
                this.previous = parseKeyData(keyConfig.getPrevious());
            }
            log.info("从 Nacos 加载 RSA 密钥，版本: {}", current.getVersion());
        } else {
            // 生成新密钥并发布
            generateAndPublish();
        }
    }

    /**
     * 生成新密钥并发布到 Nacos
     */
    public void generateAndPublish() throws Exception {
        KeyPair keyPair = RSAUtil.generateKeyPair(KEY_SIZE, secret);
        String version = Instant.now().toString();

        RSAKeyData newData = new RSAKeyData();
        newData.setVersion(version);
        newData.setCreatedAt(System.currentTimeMillis());
        newData.setPublicKeyStr(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        newData.setPrivateKeyStr(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        newData.setPublicKey(keyPair.getPublic());
        newData.setPrivateKey(keyPair.getPrivate());

        // 当前密钥变为前一个版本
        this.previous = this.current;
        this.current = newData;

        // 发布到 Nacos
        publishToNacos();

        log.info("生成并发布新 RSA 密钥，版本: {}", version);
    }

    /**
     * 发布密钥到 Nacos (YAML 格式)
     */
    private void publishToNacos() throws Exception {
        RSAKeyConfig config = new RSAKeyConfig();
        config.setCurrent(toStorageData(current));
        if (previous != null) {
            config.setPrevious(toStorageData(previous));
        }

        String content = toYaml(config);
        configService.publishConfig(dataId, group, content, keyType);
    }

    /**
     * 注册 Nacos 配置监听器
     */
    private void registerListener() throws NacosException {
        configService.addListener(dataId, group, new Listener() {
            private final Executor executor = Executors.newSingleThreadExecutor();

            @Override
            public Executor getExecutor() {
                return executor;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                try {
                    RSAKeyConfig keyConfig = parseYaml(configInfo);
                    // 仅当版本不同时更新
                    if (!keyConfig.getCurrent().getVersion().equals(current.getVersion())) {
                        previous = current;
                        current = parseKeyData(keyConfig.getCurrent());
                        log.info("RSA 密钥已刷新，新版本: {}", current.getVersion());
                    }
                } catch (Exception e) {
                    log.error("刷新 RSA 密钥失败", e);
                }
            }
        });
    }

    // ============== 定时刷新 ==============

    /**
     * 定时检查并刷新 RSA 密钥
     * 默认每小时检查一次，通过 Nacos 分布式锁确保只有一个实例执行刷新
     */
    @Scheduled(fixedDelayString = checkInterval)
    public void scheduledRefresh() {
        if (!refreshEnabled || current == null) {
            return;
        }

        long timeMillis = System.currentTimeMillis();

        // 检查密钥是否过期
        long keyAge = timeMillis - current.getCreatedAt();

        // 密钥有效期（小时转毫秒）
        long keyTtlMs = TimeUnit.HOURS.toMillis(keyTtl);

        // 判断密钥是否过期
        if (keyAge < keyTtlMs) {
            log.debug("RSA 密钥未过期，剩余有效期: {} 小时",
                    TimeUnit.MILLISECONDS.toHours(keyTtlMs - keyAge));
            return;
        }

        log.info("RSA 密钥已过期，尝试获取锁进行刷新...");

        // 尝试获取分布式锁
        if (tryAcquireLock(timeMillis)) {
            try {
                // 再次检查，避免重复刷新（其他实例可能已刷新）
                String latestConfig = configService.getConfig(dataId, group, TIMEOUT_MS);
                if (latestConfig != null) {
                    RSAKeyConfig keyConfig = parseYaml(latestConfig);
                    long latestAge = timeMillis - keyConfig.getCurrent().getCreatedAt();
                    if (latestAge < keyTtlMs) {
                        log.info("其他实例已刷新密钥，跳过");
                        return;
                    }
                }
                // 执行刷新
                generateAndPublish();
                log.info("定时刷新 RSA 密钥成功，实例: {}", instanceId);
            } catch (Exception e) {
                log.error("定时刷新 RSA 密钥失败", e);
            } finally {
                releaseLock();
            }
        } else {
            log.debug("未能获取刷新锁，其他实例正在刷新");
        }
    }

    /**
     * 尝试获取基于 Nacos 的分布式锁
     */
    private boolean tryAcquireLock(long now) {
        try {
            String lockContent = configService.getConfig(LOCK_DATA_ID, group, TIMEOUT_MS);

            if (lockContent != null && !lockContent.isEmpty()) {
                Yaml yaml = new Yaml();
                Map<String, Object> lockInfo = yaml.load(lockContent);
                long expiresAt = ((Number) lockInfo.get("expiresAt")).longValue();

                if (now < expiresAt) {
                    // 锁未过期，获取失败
                    return false;
                }

                // 锁已过期，先删除过期锁
                log.info("检测到过期锁，持有者: {}, 过期时间: {}, 正在清除...",
                        lockInfo.get("holder"), expiresAt);
                configService.removeConfig(LOCK_DATA_ID, group);
                // 短暂等待确保删除生效
                Thread.sleep(50);
            }

            // 尝试写入新锁
            Map<String, Object> newLock = new LinkedHashMap<>();
            newLock.put("holder", instanceId);
            newLock.put("acquiredAt", now);
            newLock.put("expiresAt", now + LOCK_TTL_MS);

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            String lockYaml = new Yaml(options).dump(newLock);

            boolean published = configService.publishConfig(LOCK_DATA_ID, group, lockYaml, keyType);
            if (!published) {
                return false;
            }

            // 短暂等待后验证是否是自己持有锁（乐观锁验证）
            Thread.sleep(100);
            String verifyContent = configService.getConfig(LOCK_DATA_ID, group, TIMEOUT_MS);
            if (verifyContent != null) {
                Map<String, Object> verifyLock = new Yaml().load(verifyContent);
                String holder = (String) verifyLock.get("holder");
                if (instanceId.equals(holder)) {
                    log.info("成功获取分布式锁，实例: {}", instanceId);
                    return true;
                }
                log.debug("锁被其他实例抢占: {}", holder);
            }
            return false;
        } catch (Exception e) {
            log.warn("获取分布式锁异常", e);
            return false;
        }
    }

    /**
     * 释放分布式锁
     */
    private void releaseLock() {
        try {
            // 验证是否是自己持有的锁，防止误删其他实例的锁
            String lockContent = configService.getConfig(LOCK_DATA_ID, group, TIMEOUT_MS);
            if (lockContent != null && !lockContent.isEmpty()) {
                Map<String, Object> lockInfo = new Yaml().load(lockContent);
                String holder = (String) lockInfo.get("holder");
                if (!instanceId.equals(holder)) {
                    log.warn("锁已被其他实例持有: {}, 跳过释放", holder);
                    return;
                }
            }
            configService.removeConfig(LOCK_DATA_ID, group);
            log.info("分布式锁已释放，实例: {}", instanceId);
        } catch (NacosException e) {
            log.warn("释放分布式锁失败", e);
        }
    }

    /**
     * 获取前一版本私钥（用于平滑过渡解密）
     */
    public PrivateKey getPreviousPrivateKey() {
        return previous != null ? previous.getPrivateKey() : null;
    }

    public String getPublicKeyStr() {
        return current.getPublicKeyStr();
    }

    public String getPrivateKeyStr() {
        return current.getPrivateKeyStr();
    }

    public PublicKey getPublicKey() {
        return current.getPublicKey();
    }

    public PrivateKey getPrivateKey() {
        return current.getPrivateKey();
    }

    public String getVersion() {
        return current.getVersion();
    }

    /**
     * 解析 YAML 配置
     */
    @SuppressWarnings("unchecked")
    private RSAKeyConfig parseYaml(String yamlContent) {
        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(yamlContent);

        RSAKeyConfig config = new RSAKeyConfig();
        if (map.containsKey("current")) {
            config.setCurrent(mapToStorage((Map<String, Object>) map.get("current")));
        }
        if (map.containsKey("previous")) {
            config.setPrevious(mapToStorage((Map<String, Object>) map.get("previous")));
        }
        return config;
    }

    /**
     * 转换为 YAML 字符串
     */
    private String toYaml(RSAKeyConfig config) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("current", storageToMap(config.getCurrent()));
        if (config.getPrevious() != null) {
            map.put("previous", storageToMap(config.getPrevious()));
        }

        return new Yaml(options).dump(map);
    }

    private RSAKeyStorage mapToStorage(Map<String, Object> map) {
        RSAKeyStorage storage = new RSAKeyStorage();
        storage.setVersion((String) map.get("version"));
        storage.setCreatedAt(((Number) map.get("createdAt")).longValue());
        storage.setPublicKey((String) map.get("publicKey"));
        storage.setPrivateKey((String) map.get("privateKey"));
        return storage;
    }

    private Map<String, Object> storageToMap(RSAKeyStorage storage) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("version", storage.getVersion());
        map.put("createdAt", storage.getCreatedAt());
        map.put("publicKey", storage.getPublicKey());
        map.put("privateKey", storage.getPrivateKey());
        return map;
    }

    // ============== 数据转换 ==============

    private RSAKeyData parseKeyData(RSAKeyStorage storage) throws Exception {
        RSAKeyData data = new RSAKeyData();
        data.setVersion(storage.getVersion());
        data.setCreatedAt(storage.getCreatedAt());
        data.setPublicKeyStr(storage.getPublicKey());
        data.setPrivateKeyStr(storage.getPrivateKey());
        data.setPublicKey(RSAUtil.getPublicKey(storage.getPublicKey().getBytes()));
        data.setPrivateKey(RSAUtil.getPrivateKey(storage.getPrivateKey().getBytes()));
        return data;
    }

    private RSAKeyStorage toStorageData(RSAKeyData data) {
        RSAKeyStorage storage = new RSAKeyStorage();
        storage.setVersion(data.getVersion());
        storage.setCreatedAt(data.getCreatedAt());
        storage.setPublicKey(data.getPublicKeyStr());
        storage.setPrivateKey(data.getPrivateKeyStr());
        return storage;
    }

    // ============== 内部类 ==============

    @Data
    public static class RSAKeyData {
        private String version;
        private long createdAt;
        private String publicKeyStr;
        private String privateKeyStr;
        private transient PublicKey publicKey;
        private transient PrivateKey privateKey;
    }

    @Data
    public static class RSAKeyConfig {
        private RSAKeyStorage current;
        private RSAKeyStorage previous;
    }

    @Data
    public static class RSAKeyStorage {
        private String version;
        private long createdAt;
        private String publicKey;
        private String privateKey;
    }
}