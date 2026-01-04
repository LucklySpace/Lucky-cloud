package com.xy.lucky.server.config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class ServerRuntimeHintsConfig implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("logback-plus.xml")
                .registerPattern("redisson-cluster.yml")
                .registerPattern("redisson-single.yml");

    }
}
