package com.xy.lucky.spring.context;


import java.util.List;

public class ApplicationArguments {

    private final String[] args;

    public ApplicationArguments(String[] args) {
        this.args = args;
    }

    public String[] getSourceArgs() {
        return args;
    }

    public List<String> getNonOptionArgs() {
        return List.of(args); // 简化：不区分参数类型
    }
}
