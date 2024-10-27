package com.xy.connect.config;

/**
 * mq配置
 */
public class IMRabbitMQConfig {

    private RabbitMQ rabbitMQ;

    public RabbitMQ getRabbitMQ() {
        return rabbitMQ;
    }

    public void setRabbitMQ(RabbitMQ rabbitMQ) {
        this.rabbitMQ = rabbitMQ;
    }

    @Override
    public String toString() {
        return "RabbitMqConfig{" +
                "rabbitMQ=" + rabbitMQ +
                '}';
    }

    public static class RabbitMQ {

        private String address;

        private int port;

        private String virtual;

        private String username;

        private String password;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getVirtual() {
            return virtual;
        }

        public void setVirtual(String virtual) {
            this.virtual = virtual;
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

        @Override
        public String toString() {
            return "RabbitMQ{" +
                    "address='" + address + '\'' +
                    ", port=" + port +
                    ", username='" + username + '\'' +
                    ", password='" + password + '\'' +
                    '}';
        }
    }
}
