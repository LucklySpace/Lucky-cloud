package com.xy.lucky.connect.pressure;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public record PressureTestConfig(
        String url,
        Protocol protocol,
        AuthMode authMode,
        String instanceTag,
        int shardTotal,
        int shardIndex,
        String token,
        List<String> tokenPool,
        String deviceType,
        int connections,
        int connectRate,
        int durationSeconds,
        int heartbeatIntervalMs,
        int messageIntervalMs,
        int messageCode,
        int registerCode,
        boolean registerOnConnect,
        int connectTimeoutMs,
        int maxFramePayloadLength,
        int printIntervalSeconds,
        boolean autoReconnect,
        int reconnectDelayMs,
        int workerThreads
) {

    enum Protocol {
        JSON, PROTO
    }

    enum AuthMode {
        URL, HEADER, COOKIE
    }

    static PressureTestConfig fromArgs(String[] args) {
        Map<String, String> argMap = parseArgMap(args);
        if (argMap.containsKey("help") || argMap.containsKey("h")) {
            printHelpAndExit();
        }

        String url = argMap.getOrDefault("url", "ws://127.0.0.1:19000/im");
        Protocol protocol = parseProtocol(argMap.getOrDefault("protocol", "proto"));
        AuthMode authMode = parseAuthMode(argMap.getOrDefault("authMode", "url"));
        String token = trimToNull(argMap.get("token"));
        List<String> tokenPool = loadTokenPool(argMap.get("tokenFile"));
        if (token == null && tokenPool.isEmpty()) {
            throw new IllegalArgumentException("必须提供 --token 或 --tokenFile");
        }
        int shardTotal = parsePositiveInt(argMap.getOrDefault("shardTotal", "1"), "shardTotal");
        int shardIndex = parseNonNegativeInt(argMap.getOrDefault("shardIndex", "0"), "shardIndex");
        if (shardIndex >= shardTotal) {
            throw new IllegalArgumentException("shardIndex 必须小于 shardTotal");
        }

        return new PressureTestConfig(
                url,
                protocol,
                authMode,
                argMap.getOrDefault("instanceTag", "pressure"),
                shardTotal,
                shardIndex,
                token,
                tokenPool,
                argMap.getOrDefault("deviceType", "WEB"),
                parsePositiveInt(argMap.getOrDefault("connections", "100"), "connections"),
                parsePositiveInt(argMap.getOrDefault("connectRate", "50"), "connectRate"),
                parsePositiveInt(argMap.getOrDefault("durationSeconds", "120"), "durationSeconds"),
                parsePositiveInt(argMap.getOrDefault("heartbeatIntervalMs", "25000"), "heartbeatIntervalMs"),
                parseNonNegativeInt(argMap.getOrDefault("messageIntervalMs", "0"), "messageIntervalMs"),
                parsePositiveInt(argMap.getOrDefault("messageCode", "1000"), "messageCode"),
                parsePositiveInt(argMap.getOrDefault("registerCode", "200"), "registerCode"),
                Boolean.parseBoolean(argMap.getOrDefault("registerOnConnect", "true")),
                parsePositiveInt(argMap.getOrDefault("connectTimeoutMs", "8000"), "connectTimeoutMs"),
                parsePositiveInt(argMap.getOrDefault("maxFramePayloadLength", "655360"), "maxFramePayloadLength"),
                parsePositiveInt(argMap.getOrDefault("printIntervalSeconds", "5"), "printIntervalSeconds"),
                Boolean.parseBoolean(argMap.getOrDefault("autoReconnect", "true")),
                parsePositiveInt(argMap.getOrDefault("reconnectDelayMs", "3000"), "reconnectDelayMs"),
                parsePositiveInt(argMap.getOrDefault("workerThreads", "4"), "workerThreads")
        );
    }

    String tokenFor(int index) {
        if (!tokenPool.isEmpty()) {
            return tokenPool.get(Math.floorMod(index, tokenPool.size()));
        }
        return token;
    }

    int globalClientIndex(int localIndex) {
        return localIndex * shardTotal + shardIndex;
    }

    private static Map<String, String> parseArgMap(String[] args) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String arg : args) {
            if (!arg.startsWith("--")) {
                continue;
            }
            String body = arg.substring(2);
            int idx = body.indexOf('=');
            if (idx < 0) {
                map.put(body, "true");
            } else {
                String key = body.substring(0, idx).trim();
                String value = body.substring(idx + 1).trim();
                map.put(key, value);
            }
        }
        return map;
    }

    private static List<String> loadTokenPool(String tokenFile) {
        String file = trimToNull(tokenFile);
        if (file == null) {
            return List.of();
        }
        try {
            return Files.readAllLines(Path.of(file)).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new IllegalArgumentException("读取 token 文件失败: " + file + ", " + e.getMessage(), e);
        }
    }

    private static Protocol parseProtocol(String raw) {
        String text = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        return switch (text) {
            case "JSON" -> Protocol.JSON;
            case "PROTO", "PROTOBUF" -> Protocol.PROTO;
            default -> throw new IllegalArgumentException("不支持的 protocol: " + raw);
        };
    }

    private static AuthMode parseAuthMode(String raw) {
        String text = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        return switch (text) {
            case "URL" -> AuthMode.URL;
            case "HEADER" -> AuthMode.HEADER;
            case "COOKIE" -> AuthMode.COOKIE;
            default -> throw new IllegalArgumentException("不支持的 authMode: " + raw);
        };
    }

    private static int parsePositiveInt(String value, String key) {
        int result = Integer.parseInt(value);
        if (result <= 0) {
            throw new IllegalArgumentException(key + " 必须大于 0");
        }
        return result;
    }

    private static int parseNonNegativeInt(String value, String key) {
        int result = Integer.parseInt(value);
        if (result < 0) {
            throw new IllegalArgumentException(key + " 不能小于 0");
        }
        return result;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private static void printHelpAndExit() {
        String help = """
                用法:
                  java -jar im-connect-pressure.jar --url=ws://127.0.0.1:19000/im --protocol=proto --token=xxx

                参数:
                  --url=ws://host:port/path
                  --protocol=proto|json
                  --authMode=url|header|cookie
                  --instanceTag=pressure
                  --shardTotal=1
                  --shardIndex=0
                  --token=JWT
                  --tokenFile=/abs/path/to/tokens.txt
                  --deviceType=WEB
                  --connections=100
                  --connectRate=50
                  --durationSeconds=120
                  --heartbeatIntervalMs=25000
                  --messageIntervalMs=0
                  --messageCode=1000
                  --registerCode=200
                  --registerOnConnect=true
                  --connectTimeoutMs=8000
                  --maxFramePayloadLength=655360
                  --printIntervalSeconds=5
                  --autoReconnect=true
                  --reconnectDelayMs=3000
                  --workerThreads=4
                """;
        System.out.println(help);
        System.exit(0);
    }
}
