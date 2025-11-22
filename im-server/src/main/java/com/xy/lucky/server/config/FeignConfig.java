package com.xy.lucky.server.config;


/// **
// * feign 配置
// */
//@Configuration
//public class FeignConfig {
//
//    @Bean
//    public okhttp3.OkHttpClient okHttpClient() {
//        return new okhttp3.OkHttpClient.Builder()
//                .connectTimeout(Duration.ofMillis(300))
//                .readTimeout(Duration.ofSeconds(5))
//                .writeTimeout(Duration.ofSeconds(5))
//                .connectionPool(new ConnectionPool(100, 5, TimeUnit.MINUTES))
//                .retryOnConnectionFailure(true)
//                .build();
//    }
//
//    @Bean
//    public Retryer retryer() {
//        return new Retryer.Default(100, TimeUnit.SECONDS.toMillis(1), 2);
//    }
//
//}
