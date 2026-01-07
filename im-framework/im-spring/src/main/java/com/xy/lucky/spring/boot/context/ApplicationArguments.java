package com.xy.lucky.spring.boot.context;

import java.util.*;

/**
 * ApplicationArguments - 应用参数封装
 * <p>
 * 解析命令行参数，支持：
 * <ul>
 *   <li>选项参数：--key=value 或 --key</li>
 *   <li>非选项参数：不以 -- 开头的参数</li>
 * </ul>
 */
public class ApplicationArguments {

    private final String[] sourceArgs;
    private final Set<String> optionNames = new LinkedHashSet<>();
    private final Map<String, List<String>> optionValues = new LinkedHashMap<>();
    private final List<String> nonOptionArgs = new ArrayList<>();

    public ApplicationArguments(String[] args) {
        this.sourceArgs = args != null ? args.clone() : new String[0];
        parse(this.sourceArgs);
    }

    private void parse(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String optionText = arg.substring(2);
                int indexOfEquals = optionText.indexOf('=');
                if (indexOfEquals > 0) {
                    String key = optionText.substring(0, indexOfEquals);
                    String value = optionText.substring(indexOfEquals + 1);
                    optionNames.add(key);
                    optionValues.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
                } else {
                    optionNames.add(optionText);
                    optionValues.computeIfAbsent(optionText, k -> new ArrayList<>());
                }
            } else {
                nonOptionArgs.add(arg);
            }
        }
    }

    /**
     * 获取原始参数数组
     */
    public String[] getSourceArgs() {
        return sourceArgs.clone();
    }

    /**
     * 获取所有选项名称
     */
    public Set<String> getOptionNames() {
        return Collections.unmodifiableSet(optionNames);
    }

    /**
     * 判断是否包含指定选项
     */
    public boolean containsOption(String name) {
        return optionNames.contains(name);
    }

    /**
     * 获取指定选项的值列表
     */
    public List<String> getOptionValues(String name) {
        List<String> values = optionValues.get(name);
        return values != null ? Collections.unmodifiableList(values) : Collections.emptyList();
    }

    /**
     * 获取指定选项的首个值
     */
    public String getOptionValue(String name) {
        List<String> values = optionValues.get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    /**
     * 获取非选项参数列表
     */
    public List<String> getNonOptionArgs() {
        return Collections.unmodifiableList(nonOptionArgs);
    }
}

