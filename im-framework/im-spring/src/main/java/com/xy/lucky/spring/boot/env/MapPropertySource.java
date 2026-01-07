package com.xy.lucky.spring.boot.env;

import java.util.Map;

/**
 * MapPropertySource - 基于 Map 的属性源实现
 */
public class MapPropertySource extends PropertySource<Map<String, Object>> {

    public MapPropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String name) {
        return this.source.get(name);
    }

    @Override
    public boolean containsProperty(String name) {
        return this.source.containsKey(name);
    }

    /**
     * 获取属性名数组
     */
    public String[] getPropertyNames() {
        return this.source.keySet().toArray(new String[0]);
    }
}

