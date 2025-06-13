package com.xy.auth;

import cn.hutool.core.date.DateField;
import cn.hutool.core.util.IdUtil;
import com.xy.imcore.utils.JwtUtil;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

//import org.springframework.boot.test.context.SpringBootTest;
//
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

        /**
         * 生成多少条 UID + Token
         */
        Integer COUNT = 50000;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(CSV_PATH).toFile()))) {
            // 写 CSV 头
            writer.write("uid,token");

            writer.newLine();

            Integer startId =  10000;

            for (int i = 0; i < COUNT; i++) {
                // 生成 UID
                String uid = String.valueOf(startId + i);

                // 使用 JwtUtil 工具类创建 Token（你自己的工具类）
                String token = JwtUtil.createToken(uid, EXPIRE_HOURS, DateField.HOUR);

                // 写入 CSV 一行
                writer.write(String.format("%s,%s", uid, token));

                writer.newLine();
            }

            System.out.println("✅ 成功生成 " + COUNT + " 条 UID 与 Token，并写入到：" + CSV_PATH);
        }
    }
}
