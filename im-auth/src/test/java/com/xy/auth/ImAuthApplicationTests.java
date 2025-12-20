package com.xy.auth;

import com.xy.lucky.auth.ImAuthApplication;
import com.xy.lucky.crypto.core.crypto.core.CryptoExecutor;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ImAuthApplication.class)
class ImAuthApplicationTests {

    @Resource
    private CryptoExecutor cryptoExecutor;

    /**
     * 用于测试生成 UID 和 Token 的工具方法
     */
    @Test
    void generateUidAndTokenToCsv() throws Exception {

//        String json = "{\"principal:\"100001\"," +
//                "\"credentials:\"HuEgryBoIv0NO9XgnE8TNaNq0YnLivjKhRFbSEQQ35pLxURK9k8uwSa17d8zmDGRvofLj1QucHe9tmgYnAwcno9x6b3rJ1fzjhjuD+5jqGX+zlfRMMDlV5Wz4+I0CNeXDNyanP2LPSrW3Hv6O/tUwp4sBu2Na+j3WC3LZU6dRdt7G47mR3v5DjXvXavjkZad7EoTVNNZcRCAMsgEr7JzlekUgck8d0VLXiIQ9vpAVmoE3AysB98QygMh6818NaaCGo1M9SMV73M0VlusEbPRqrB31jqoFPHcJKr1ygHAmNxoY6FZfyBrQ4Dahi2rACZyMJaJ2y6xepoKNFy+vIgezg==\",\n" +
//                "\"authType:\"form\"\"}";
//
//        String encrypt = cryptoExecutor.encrypt(json, CryptoMode.RSA);
//
//        System.out.println(encrypt);

    }
}
