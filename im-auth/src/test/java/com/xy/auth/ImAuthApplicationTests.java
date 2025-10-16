package com.xy.auth;


import org.junit.jupiter.api.Test;


//@SpringBootTest
class ImAuthApplicationTests {


    /**
     * JWT 过期时间（单位：小时）
     */
    private static final int EXPIRE_HOURS = 24;

    /**
     * CSV 文件输出路径（可以根据需要修改为绝对路径）
     */
    private static final String CSV_PATH = "D:/uid_token_output.csv";

    /**
     * 用于测试生成 UID 和 Token 的工具方法
     */
    @Test
    void generateUidAndTokenToCsv() throws Exception {

    }
}
