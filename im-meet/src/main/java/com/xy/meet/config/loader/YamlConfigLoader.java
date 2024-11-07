package com.xy.meet.config.loader;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

public class YamlConfigLoader {

    public static <T> T loadConfig(String filePath, Class<T> clazz) {
        Yaml yaml = new Yaml();
        try (InputStream in = YamlConfigLoader.class
                .getClassLoader()
                .getResourceAsStream(filePath)) {
            if (in == null) {
                throw new IllegalArgumentException("File not found: " + filePath);
            }
            return yaml.loadAs(in, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

}
