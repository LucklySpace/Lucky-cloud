package com.xy.connect.config;

/**
 * redis配置
 */
public class IMRedisConfig {

    private RedisConfig redis;

    public RedisConfig getRedis() {
        return redis;
    }

    public void setRedis(RedisConfig redis) {
        this.redis = redis;
    }

    public static class RedisConfig {

        private String host;
        private int port;
        private String password;
        private int timeout;
        private int database;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getTimeout() {
            return timeout;
        }

    }

}
