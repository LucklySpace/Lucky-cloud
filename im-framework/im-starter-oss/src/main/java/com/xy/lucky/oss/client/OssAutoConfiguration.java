package com.xy.lucky.oss.client;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * OSS 自动配置
 * 负责注册统一的对象存储操作入口 OssTemplate。
 * - 读取 oss.providers，按提供者构建 AmazonS3 客户端与对应实现
 * - 通过 DelegatingOssTemplate 支持默认提供者与按桶名后缀进行路由
 * - 仅在容器中不存在 OssTemplate Bean 时生效
 * 使用方式：引入依赖并在配置文件填写 oss.* 属性后，可直接注入使用
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(OssProperties.class)
public class OssAutoConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean(OssTemplate.class)
    public OssTemplate ossTemplate(OssProperties ossProperties) {
        if (ossProperties.getProviders() == null || ossProperties.getProviders().isEmpty()) {
            throw new IllegalArgumentException("OSS 配置不完整：请配置 oss.providers，并至少包含一个提供者");
        }
        if (ossProperties.getDefaultProvider() == null || ossProperties.getDefaultProvider().isEmpty()) {
            throw new IllegalArgumentException("OSS 配置不完整：请配置 oss.defaultProvider");
        }
        Map<String, OssTemplate> templates = new LinkedHashMap<>();
        for (Map.Entry<String, OssProperties.Provider> e : ossProperties.getProviders().entrySet()) {
            String name = e.getKey();
            OssProperties.Provider p = e.getValue();
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            clientConfiguration.setMaxConnections(Objects.requireNonNullElse(ossProperties.getMaxConnections(), 100));
            clientConfiguration.setConnectionTimeout((int) Objects.requireNonNullElse(ossProperties.getConnectionTimeout(), java.time.Duration.ofSeconds(60)).toMillis());
            clientConfiguration.setSocketTimeout((int) Objects.requireNonNullElse(ossProperties.getSocketTimeout(), java.time.Duration.ofSeconds(60)).toMillis());
            AWSCredentials awsCredentials = new BasicAWSCredentials(p.getAccessKey(), p.getSecretKey());
            AWSCredentialsProvider awsCredentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
            AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(p.getEndpoint(), p.getRegion());
            AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                    .withEndpointConfiguration(endpointConfiguration)
                    .withClientConfiguration(clientConfiguration)
                    .withCredentials(awsCredentialsProvider)
                    .disableChunkedEncoding()
                    .withPathStyleAccessEnabled(Objects.requireNonNullElse(p.getPathStyleAccess(), false))
                    .build();
            OssProperties per = new OssProperties();
            per.setEndpoint(p.getEndpoint());
            per.setRegion(p.getRegion());
            per.setAccessKey(p.getAccessKey());
            per.setSecretKey(p.getSecretKey());
            per.setPathStyleAccess(Objects.requireNonNullElse(p.getPathStyleAccess(), false));
            per.setPresignedUrlExpiry(Objects.requireNonNullElse(p.getPresignedUrlExpiry(), Objects.requireNonNullElse(ossProperties.getPresignedUrlExpiry(), 7 * 24 * 60 * 60)));
            templates.put(name, new OssTemplateImpl(s3, per));
        }
        String def = ossProperties.getDefaultProvider();
        Map<String, String> route = ossProperties.getBucketProviderByCode();
        for (Map.Entry<String, OssProperties.Provider> e : ossProperties.getProviders().entrySet()) {
            String id = e.getKey();
            String pn = e.getValue() == null ? null : e.getValue().getName();
            log.info("OSS provider {} ({}) initialized", id, pn);
        }
        if (route != null && !route.isEmpty()) {
            log.info("OSS bucket routing: {}", route);
        }
        log.info("OSS default provider: {}", def);
        return new DelegatingOssTemplate(templates, def, route);
    }

    /**
     * 代理实现：根据提供者名称或桶名后缀自动选择具体实现
     * - 默认提供者兜底
     * - 支持按 bucketName 最后一个 '-' 后的 code 路由到指定提供者
     */
    public static class DelegatingOssTemplate implements OssTemplate {
        private final Map<String, OssTemplate> templates;
        private final String defaultProvider;
        private final Map<String, String> bucketProviderByCode;

        public DelegatingOssTemplate(Map<String, OssTemplate> templates, String defaultProvider, Map<String, String> bucketProviderByCode) {
            this.templates = templates;
            this.defaultProvider = defaultProvider;
            this.bucketProviderByCode = bucketProviderByCode == null ? new LinkedHashMap<>() : bucketProviderByCode;
        }

        @Override
        public OssTemplate select(String providerName) {
            if (providerName == null) {
                return selectByBucket(null);
            }
            OssTemplate t = templates.get(providerName);
            if (t == null && !templates.isEmpty()) {
                t = templates.values().iterator().next();
            }
            return t;
        }

        private OssTemplate selectByBucket(String bucketName) {
            String code = null;
            int i = bucketName == null ? -1 : bucketName.lastIndexOf('-');
            if (i >= 0 && i + 1 < bucketName.length()) {
                code = bucketName.substring(i + 1);
            }
            String provider = code == null ? null : bucketProviderByCode.get(code);
            if (provider == null) {
                provider = defaultProvider;
            }
            OssTemplate t = provider == null ? null : templates.get(provider);
            if (t == null && !templates.isEmpty()) {
                t = templates.values().iterator().next();
            }
            return t;
        }

        @Override
        public void createBucket(String bucketName) {
            selectByBucket(bucketName).createBucket(bucketName);
        }

        @Override
        public boolean bucketExists(String bucketName) {
            return selectByBucket(bucketName).bucketExists(bucketName);
        }

        @Override
        public java.util.List<com.amazonaws.services.s3.model.Bucket> listBuckets() {
            String provider = defaultProvider;
            OssTemplate t = provider == null ? null : templates.get(provider);
            if (t == null && !templates.isEmpty()) {
                t = templates.values().iterator().next();
            }
            return t.listBuckets();
        }

        @Override
        public void deleteBucket(String bucketName) {
            selectByBucket(bucketName).deleteBucket(bucketName);
        }

        @Override
        public boolean setBucketPublic(String bucketName) {
            return selectByBucket(bucketName).setBucketPublic(bucketName);
        }

        @Override
        public void putObject(String bucketName, String objectName, java.io.InputStream stream, String contentType) throws Exception {
            selectByBucket(bucketName).putObject(bucketName, objectName, stream, contentType);
        }

        @Override
        public void putObject(String bucketName, String objectName, java.io.InputStream stream) throws Exception {
            selectByBucket(bucketName).putObject(bucketName, objectName, stream);
        }

        @Override
        public com.amazonaws.services.s3.model.S3Object getObject(String bucketName, String objectName) {
            return selectByBucket(bucketName).getObject(bucketName, objectName);
        }

        @Override
        public void deleteObject(String bucketName, String objectName) throws Exception {
            selectByBucket(bucketName).deleteObject(bucketName, objectName);
        }

        @Override
        public int deleteObjects(String bucketName, java.util.List<String> objectNames) {
            return selectByBucket(bucketName).deleteObjects(bucketName, objectNames);
        }

        @Override
        public void copyObject(String sourceBucket, String sourceObjectName, String targetBucket, String targetObjectName) throws Exception {
            selectByBucket(targetBucket).copyObject(sourceBucket, sourceObjectName, targetBucket, targetObjectName);
        }

        @Override
        public java.util.List<com.amazonaws.services.s3.model.S3ObjectSummary> listObjects(String bucketName, String prefix, boolean recursive) {
            return selectByBucket(bucketName).listObjects(bucketName, prefix, recursive);
        }

        @Override
        public boolean doesObjectExist(String bucketName, String objectName) {
            return selectByBucket(bucketName).doesObjectExist(bucketName, objectName);
        }

        @Override
        public String getPresignedUrl(String bucketName, String objectName, int expires) {
            return selectByBucket(bucketName).getPresignedUrl(bucketName, objectName, expires);
        }

        @Override
        public String getPresignedPutUrl(String bucketName, String objectName, int expires) {
            return selectByBucket(bucketName).getPresignedPutUrl(bucketName, objectName, expires);
        }

        @Override
        public String getPublicUrl(String bucketName, String objectName) {
            return selectByBucket(bucketName).getPublicUrl(bucketName, objectName);
        }

        @Override
        public com.amazonaws.services.s3.model.ObjectMetadata getObjectMetadata(String bucketName, String objectName) {
            return selectByBucket(bucketName).getObjectMetadata(bucketName, objectName);
        }
    }
}
