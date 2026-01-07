package com.xy.lucky.spring.boot.env;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * StandardEnvironment - 标准环境实现
 * <p>
 * 支持：
 * - 多 Profile（spring.profiles.active）
 * - 多配置文件（application.yml, application-{profile}.yml）
 * - 命令行参数覆盖
 * - 系统属性和环境变量
 * - 占位符解析
 */
public class StandardEnvironment implements ConfigurableEnvironment {

    public static final String ACTIVE_PROFILES_PROPERTY = "spring.profiles.active";
    public static final String DEFAULT_PROFILES_PROPERTY = "spring.profiles.default";
    public static final String DEFAULT_PROFILE = "default";

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private final MutablePropertySources propertySources = new MutablePropertySources();
    private final Set<String> activeProfiles = new LinkedHashSet<>();
    private final Set<String> defaultProfiles = new LinkedHashSet<>(Collections.singletonList(DEFAULT_PROFILE));

    // 内部属性存储（用于直接设置的属性）
    private final Map<String, Object> internalProperties = new LinkedHashMap<>();

    public StandardEnvironment() {
        customizePropertySources();
    }

    /**
     * 初始化属性源
     */
    protected void customizePropertySources() {
        // 1. 系统属性（优先级较高）
        this.propertySources.addLast(new MapPropertySource("systemProperties", getSystemProperties()));

        // 2. 系统环境变量
        this.propertySources.addLast(new MapPropertySource("systemEnvironment", getSystemEnvironment()));

        // 3. 加载默认配置文件
        loadYamlConfig("application.yml", "applicationConfig");

        // 4. 解析激活的 profiles
        resolveActiveProfiles();

        // 5. 加载 profile 特定配置文件
        for (String profile : getActiveProfiles()) {
            loadYamlConfig("application-" + profile + ".yml", "applicationConfig-" + profile);
        }
    }

    /**
     * 加载 YAML 配置文件
     */
    private void loadYamlConfig(String fileName, String sourceName) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (in == null) {
                return; // 配置文件不存在，跳过
            }
            Map<String, Object> yamlData = new Yaml().load(in);
            if (yamlData != null) {
                Map<String, Object> flattenedMap = new LinkedHashMap<>();
                flatten("", yamlData, flattenedMap);
                // Profile 配置优先级更高，添加到前面
                if (sourceName.contains("-")) {
                    this.propertySources.addFirst(new MapPropertySource(sourceName, flattenedMap));
                } else {
                    this.propertySources.addLast(new MapPropertySource(sourceName, flattenedMap));
                }
            }
        } catch (Exception e) {
            // 配置文件加载失败，记录警告但不中断启动
            System.err.println("Warning: Failed to load " + fileName + ": " + e.getMessage());
        }
    }

    /**
     * 扁平化嵌套 Map
     */
    @SuppressWarnings("unchecked")
    private void flatten(String prefix, Map<String, Object> map, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                flatten(key, (Map<String, Object>) value, result);
            } else {
                result.put(key, value);
            }
        }
    }

    /**
     * 解析激活的 profiles
     */
    private void resolveActiveProfiles() {
        String profiles = getProperty(ACTIVE_PROFILES_PROPERTY);
        if (profiles != null && !profiles.trim().isEmpty()) {
            for (String profile : profiles.split(",")) {
                String trimmed = profile.trim();
                if (!trimmed.isEmpty()) {
                    this.activeProfiles.add(trimmed);
                }
            }
        }
    }

    @Override
    public void parseCommandLineArgs(String[] args) {
        if (args == null || args.length == 0) {
            return;
        }
        Map<String, Object> commandLineArgs = new LinkedHashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String optionText = arg.substring(2);
                int indexOfEquals = optionText.indexOf('=');
                if (indexOfEquals > 0) {
                    String key = optionText.substring(0, indexOfEquals);
                    String value = optionText.substring(indexOfEquals + 1);
                    commandLineArgs.put(key, value);
                } else {
                    commandLineArgs.put(optionText, "true");
                }
            }
        }
        if (!commandLineArgs.isEmpty()) {
            // 命令行参数优先级最高
            this.propertySources.addFirst(new MapPropertySource("commandLineArgs", commandLineArgs));

            // 重新解析 active profiles
            if (commandLineArgs.containsKey(ACTIVE_PROFILES_PROPERTY)) {
                this.activeProfiles.clear();
                String profiles = commandLineArgs.get(ACTIVE_PROFILES_PROPERTY).toString();
                for (String profile : profiles.split(",")) {
                    String trimmed = profile.trim();
                    if (!trimmed.isEmpty()) {
                        this.activeProfiles.add(trimmed);
                        // 加载对应的 profile 配置
                        loadYamlConfig("application-" + trimmed + ".yml", "applicationConfig-" + trimmed);
                    }
                }
            }
        }
    }

    // ================== Environment 接口实现 ==================

    @Override
    public String[] getActiveProfiles() {
        return this.activeProfiles.toArray(new String[0]);
    }

    @Override
    public void setActiveProfiles(String... profiles) {
        this.activeProfiles.clear();
        if (profiles != null) {
            for (String profile : profiles) {
                if (profile != null && !profile.trim().isEmpty()) {
                    this.activeProfiles.add(profile.trim());
                }
            }
        }
    }

    @Override
    public String[] getDefaultProfiles() {
        return this.defaultProfiles.toArray(new String[0]);
    }

    // ================== ConfigurableEnvironment 接口实现 ==================

    @Override
    public void setDefaultProfiles(String... profiles) {
        this.defaultProfiles.clear();
        if (profiles != null) {
            for (String profile : profiles) {
                if (profile != null && !profile.trim().isEmpty()) {
                    this.defaultProfiles.add(profile.trim());
                }
            }
        }
    }

    @Override
    public boolean acceptsProfiles(String... profiles) {
        if (profiles == null || profiles.length == 0) {
            return true;
        }
        for (String profile : profiles) {
            if (this.activeProfiles.contains(profile) ||
                    (this.activeProfiles.isEmpty() && this.defaultProfiles.contains(profile))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addActiveProfile(String profile) {
        if (profile != null && !profile.trim().isEmpty()) {
            this.activeProfiles.add(profile.trim());
        }
    }

    @Override
    public MutablePropertySources getPropertySources() {
        return this.propertySources;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSystemProperties() {
        return (Map) System.getProperties();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSystemEnvironment() {
        return (Map) System.getenv();
    }

    @Override
    public void merge(ConfigurableEnvironment parent) {
        for (PropertySource<?> ps : parent.getPropertySources()) {
            if (!this.propertySources.contains(ps.getName())) {
                this.propertySources.addLast(ps);
            }
        }
        for (String profile : parent.getActiveProfiles()) {
            this.activeProfiles.add(profile);
        }
        for (String profile : parent.getDefaultProfiles()) {
            this.defaultProfiles.add(profile);
        }
    }

    @Override
    public void setProperty(String key, String value) {
        this.internalProperties.put(key, value);
        // 更新或添加内部属性源
        MapPropertySource internalSource = (MapPropertySource) this.propertySources.get("internalProperties");
        if (internalSource == null) {
            internalSource = new MapPropertySource("internalProperties", this.internalProperties);
            this.propertySources.addFirst(internalSource);
        }
    }

    // ================== PropertyResolver 接口实现 ==================

    @Override
    public boolean containsProperty(String key) {
        for (PropertySource<?> propertySource : this.propertySources) {
            if (propertySource.containsProperty(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getProperty(String key) {
        return getProperty(key, String.class, null);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, Class<T> targetType) {
        return getProperty(key, targetType, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        Object value = null;
        for (PropertySource<?> propertySource : this.propertySources) {
            value = propertySource.getProperty(key);
            if (value != null) {
                break;
            }
        }
        if (value == null) {
            return defaultValue;
        }
        return (T) convertValue(value, targetType);
    }

    @Override
    public String getRequiredProperty(String key) throws IllegalStateException {
        String value = getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Required property '" + key + "' was not found");
        }
        return value;
    }

    @Override
    public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
        T value = getProperty(key, targetType);
        if (value == null) {
            throw new IllegalStateException("Required property '" + key + "' was not found");
        }
        return value;
    }

    @Override
    public String resolvePlaceholders(String text) {
        return doResolvePlaceholders(text, false);
    }

    @Override
    public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
        return doResolvePlaceholders(text, true);
    }

    /**
     * 解析占位符
     */
    private String doResolvePlaceholders(String text, boolean required) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (!text.contains("${")) {
            String value = getProperty(text);
            return value != null ? value : text;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String key;
            String defaultValue = null;
            int colonIndex = placeholder.indexOf(':');
            if (colonIndex > 0) {
                key = placeholder.substring(0, colonIndex);
                defaultValue = placeholder.substring(colonIndex + 1);
            } else {
                key = placeholder;
            }
            String value = getProperty(key);
            if (value == null) {
                value = defaultValue;
            }
            if (value == null) {
                if (required) {
                    throw new IllegalArgumentException("Could not resolve placeholder '" + key + "' in value \"" + text + "\"");
                }
                value = matcher.group(0); // 保留原占位符
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 类型转换
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        String strValue = value.toString();
        if (targetType == String.class) {
            return strValue;
        } else if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(strValue);
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(strValue);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(strValue);
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(strValue);
        } else if (targetType == Float.class || targetType == float.class) {
            return Float.parseFloat(strValue);
        } else if (targetType == Short.class || targetType == short.class) {
            return Short.parseShort(strValue);
        } else if (targetType == Byte.class || targetType == byte.class) {
            return Byte.parseByte(strValue);
        }
        return value;
    }
}

