package com.xy.lucky.spring.boot.env;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * YamlPropertySource - YAML 配置文件属性源
 */
public class YamlPropertySource extends MapPropertySource {

    public YamlPropertySource(String name, String resourcePath) {
        super(name, loadYaml(resourcePath));
    }

    public YamlPropertySource(String name, InputStream inputStream) {
        super(name, loadYaml(inputStream));
    }

    private static Map<String, Object> loadYaml(String resourcePath) {
        try (InputStream in = YamlPropertySource.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                return new LinkedHashMap<>();
            }
            return loadYaml(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load YAML: " + resourcePath, e);
        }
    }

    private static Map<String, Object> loadYaml(InputStream in) {
        try {
            Map<String, Object> yamlData = new Yaml().load(in);
            if (yamlData == null) {
                return new LinkedHashMap<>();
            }
            Map<String, Object> flatMap = new LinkedHashMap<>();
            flatten("", yamlData, flatMap);
            return flatMap;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse YAML", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void flatten(String prefix, Map<String, Object> map, Map<String, Object> result) {
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
}

