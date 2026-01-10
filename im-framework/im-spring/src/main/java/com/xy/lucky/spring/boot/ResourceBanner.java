package com.xy.lucky.spring.boot;

import com.xy.lucky.spring.boot.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * ResourceBanner - 从资源文件加载 Banner
 * <p>
 * 支持从 classpath 或文件系统加载自定义 banner 文件
 * <p>
 * 配置示例：
 * <pre>
 * spring:
 *   banner:
 *     location: classpath:banner.txt
 *     charset: UTF-8
 * </pre>
 */
public class ResourceBanner implements Banner {

    private static final Logger log = LoggerFactory.getLogger(ResourceBanner.class);

    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String FILE_PREFIX = "file:";

    private final String location;
    private final Charset charset;

    /**
     * 构造函数
     *
     * @param location banner 文件位置 (如 classpath:banner.txt 或 file:/path/to/banner.txt)
     */
    public ResourceBanner(String location) {
        this(location, StandardCharsets.UTF_8);
    }

    /**
     * 构造函数
     *
     * @param location banner 文件位置
     * @param charset  字符编码
     */
    public ResourceBanner(String location, Charset charset) {
        this.location = location;
        this.charset = charset != null ? charset : StandardCharsets.UTF_8;
    }

    /**
     * 从配置创建 ResourceBanner
     *
     * @param environment 环境配置
     * @return ResourceBanner 实例，如果未配置则返回 null
     */
    public static ResourceBanner fromEnvironment(Environment environment) {
        if (environment == null) {
            return null;
        }

        String location = environment.getProperty("spring.banner.location");
        if (location == null || location.isEmpty()) {
            return null;
        }

        String charsetName = environment.getProperty("spring.banner.charset", "UTF-8");
        Charset charset;
        try {
            charset = Charset.forName(charsetName);
        } catch (Exception e) {
            charset = StandardCharsets.UTF_8;
        }

        return new ResourceBanner(location, charset);
    }

    @Override
    public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
        try {
            String bannerContent = loadBannerContent();
            if (bannerContent != null && !bannerContent.isEmpty()) {
                // 解析占位符
                bannerContent = resolvePlaceholders(bannerContent, environment, sourceClass);
                out.println(bannerContent);
            }
        } catch (Exception e) {
            log.warn("Failed to load banner from {}: {}", location, e.getMessage());
            // 失败时使用默认 Banner
            new DefaultBanner().printBanner(environment, sourceClass, out);
        }
    }

    /**
     * 加载 Banner 内容
     */
    private String loadBannerContent() throws IOException {
        InputStream inputStream = getInputStream();
        if (inputStream == null) {
            log.warn("Banner resource not found: {}", location);
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
            return content.toString();
        }
    }

    /**
     * 获取输入流
     */
    private InputStream getInputStream() throws IOException {
        if (location.startsWith(CLASSPATH_PREFIX)) {
            // classpath 资源
            String resourcePath = location.substring(CLASSPATH_PREFIX.length());
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = ResourceBanner.class.getClassLoader();
            }
            URL url = classLoader.getResource(resourcePath);
            if (url != null) {
                return url.openStream();
            }
            // 尝试不带前导斜杠
            if (resourcePath.startsWith("/")) {
                url = classLoader.getResource(resourcePath.substring(1));
                if (url != null) {
                    return url.openStream();
                }
            }
            return null;
        } else if (location.startsWith(FILE_PREFIX)) {
            // 文件系统资源
            String filePath = location.substring(FILE_PREFIX.length());
            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                return new FileInputStream(file);
            }
            return null;
        } else {
            // 默认作为 classpath 资源处理
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = ResourceBanner.class.getClassLoader();
            }
            URL url = classLoader.getResource(location);
            if (url != null) {
                return url.openStream();
            }
            // 尝试作为文件路径
            File file = new File(location);
            if (file.exists() && file.isFile()) {
                return new FileInputStream(file);
            }
            return null;
        }
    }

    /**
     * 解析占位符
     * <p>
     * 支持的占位符：
     * <ul>
     *   <li>${application.name} - 应用名称</li>
     *   <li>${application.version} - 应用版本</li>
     *   <li>${spring.profiles.active} - 激活的 Profile</li>
     *   <li>${任意配置key} - 从 Environment 获取</li>
     * </ul>
     */
    private String resolvePlaceholders(String content, Environment environment, Class<?> sourceClass) {
        if (content == null || !content.contains("${")) {
            return content;
        }

        String result = content;

        // 内置变量
        if (sourceClass != null) {
            result = result.replace("${application.name}", sourceClass.getSimpleName());
            result = result.replace("${application.title}", sourceClass.getSimpleName());
        }

        // 使用 Environment 解析其他占位符
        if (environment != null) {
            result = environment.resolvePlaceholders(result);
        }

        return result;
    }
}

