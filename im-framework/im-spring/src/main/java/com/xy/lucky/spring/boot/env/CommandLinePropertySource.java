package com.xy.lucky.spring.boot.env;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CommandLinePropertySource - 命令行参数属性源
 * <p>
 * 解析格式：--key=value 或 --key
 */
public class CommandLinePropertySource extends MapPropertySource {

    public static final String COMMAND_LINE_PROPERTY_SOURCE_NAME = "commandLineArgs";

    public CommandLinePropertySource(String[] args) {
        super(COMMAND_LINE_PROPERTY_SOURCE_NAME, parseArgs(args));
    }

    public CommandLinePropertySource(String name, String[] args) {
        super(name, parseArgs(args));
    }

    private static Map<String, Object> parseArgs(String[] args) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (args == null) {
            return map;
        }
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String optionText = arg.substring(2);
                int indexOfEquals = optionText.indexOf('=');
                if (indexOfEquals > 0) {
                    String key = optionText.substring(0, indexOfEquals);
                    String value = optionText.substring(indexOfEquals + 1);
                    map.put(key, value);
                } else {
                    map.put(optionText, "true");
                }
            }
        }
        return map;
    }
}

