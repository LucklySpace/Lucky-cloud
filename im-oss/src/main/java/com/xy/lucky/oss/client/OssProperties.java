package com.xy.lucky.oss.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 对象存储服务统一配置属性
 * <p>
 * 支持所有 S3 协议兼容的对象存储服务：
 * <ul>
 *   <li>阿里云 OSS (Alibaba Cloud OSS)</li>
 *   <li>腾讯云 COS (Tencent Cloud COS)</li>
 *   <li>七牛云对象存储 (Qiniu Cloud)</li>
 *   <li>MinIO</li>
 *   <li>AWS S3</li>
 *   <li>其他 S3 兼容服务</li>
 * </ul>
 * <p>
 * 使用说明：
 * <pre>
 * oss:
 *   endpoint: https://oss-cn-hangzhou.aliyuncs.com
 *   region: oss-cn-hangzhou
 *   access-key: your-access-key
 *   secret-key: your-secret-key
 *   path-style-access: false  # 阿里云等需要设置为 false
 * </pre>
 *
 * @author Lucky Team
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    /**
     * 对象存储服务的访问端点
     * <p>
     * 示例：
     * <ul>
     *   <li>阿里云 OSS: https://oss-cn-hangzhou.aliyuncs.com</li>
     *   <li>腾讯云 COS: https://cos.ap-guangzhou.myqcloud.com</li>
     *   <li>MinIO: http://localhost:9000</li>
     *   <li>AWS S3: https://s3.amazonaws.com</li>
     * </ul>
     */
    private String endpoint;

    /**
     * 区域
     * <p>
     * 部分云服务商需要指定区域，如：
     * <ul>
     *   <li>阿里云: oss-cn-hangzhou</li>
     *   <li>AWS: us-east-1</li>
     *   <li>MinIO: 可不填</li>
     * </ul>
     */
    private String region;

    /**
     * 访问路径风格
     * <p>
     * true: path-style 模式，URL 格式: http://endpoint/bucketname
     * <br>
     * false: virtual-hosted-style 模式，URL 格式: http://bucketname.endpoint
     * <p>
     * 说明：
     * <ul>
     *   <li>MinIO、自建 S3 服务: 使用 true (path-style)</li>
     *   <li>阿里云 OSS、腾讯云 COS、AWS S3: 使用 false (virtual-hosted-style)</li>
     * </ul>
     */
    private Boolean pathStyleAccess = false;

    /**
     * 访问密钥 ID (Access Key ID)
     */
    private String accessKey;

    /**
     * 访问密钥 Secret (Secret Access Key)
     */
    private String secretKey;

    /**
     * 最大连接数，默认 100
     */
    private Integer maxConnections = 100;

    /**
     * 连接超时时间，默认 60 秒
     */
    private Duration connectionTimeout = Duration.ofSeconds(60);

    /**
     * Socket 超时时间，默认 60 秒
     */
    private Duration socketTimeout = Duration.ofSeconds(60);

    /**
     * 连接池空闲连接数，默认 10
     */
    private Integer connectionPoolMaxIdle = 10;

    /**
     * 连接池保持存活时间，默认 5 分钟
     */
    private Duration connectionPoolKeepAlive = Duration.ofMinutes(5);

    /**
     * 预签名 URL 默认过期时间，默认 7 天（单位：秒）
     */
    private Integer presignedUrlExpiry = 7 * 24 * 60 * 60;

    /**
     * 默认存储桶名称
     */
    private String bucketName;

    /**
     * 是否创建缩略图
     */
    private Boolean createThumbnail = Boolean.FALSE;

    /**
     * 是否添加水印
     */
    private Boolean createWatermark = Boolean.FALSE;

    /**
     * 是否压缩
     */
    private Boolean compress = Boolean.FALSE;

    /**
     * 是否检查文件
     */
    private Boolean checkFile = Boolean.FALSE;

    /**
     * 是否计算音视频时长
     */
    private Boolean calculateDuration = Boolean.TRUE;

    /**
     * 多对象存储提供者
     */
    private Map<String, Provider> providers = new LinkedHashMap<>();

    /**
     * 默认提供者
     */
    private String defaultProvider;

    /**
     * 根据业务类型路由到提供者
     */
    private Map<String, String> bucketProviderByCode = new LinkedHashMap<>();

    /**
     * 验证配置是否完整
     *
     * @return 配置是否有效
     */
    public boolean isValid() {
        return providers != null && !providers.isEmpty() && defaultProvider != null && !defaultProvider.isEmpty();
    }

    /**
     * 获取访问密钥，去除空格
     */
    public String getAccessKey() {
        return accessKey == null ? null : accessKey.trim();
    }

    /**
     * 获取密钥 Secret，去除空格
     */
    public String getSecretKey() {
        return secretKey == null ? null : secretKey.trim();
    }

    /**
     * 获取端点，去除空格
     */
    public String getEndpoint() {
        return endpoint == null ? null : endpoint.trim();
    }

    /**
     * 获取区域，去除空格
     */
    public String getRegion() {
        return region == null ? null : region.trim();
    }

    @Data
    public static class Provider {
        private String name;
        private String endpoint;
        private String region;
        private String accessKey;
        private String secretKey;
        private Boolean pathStyleAccess = false;
        private Integer presignedUrlExpiry;
    }
}
