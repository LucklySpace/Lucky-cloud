package com.xy.connect.config;

/**
 * nacos配置
 */
public class IMNacosConfig {

    private NacosConfig nacosConfig;

    public NacosConfig getNacosConfig() {
        return nacosConfig;
    }

    public void setNacosConfig(NacosConfig nacosConfig) {
        this.nacosConfig = nacosConfig;
    }

    public static class NacosConfig {

        private String name;

        private String address;

        private Integer port;

        private String username;

        private String password;

        private String version;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}