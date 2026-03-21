package com.xy.lucky.connect.pressure;

public class WebSocketPressureApplication {
    public static void main(String[] args) throws Exception {
        PressureTestConfig config = PressureTestConfig.fromArgs(args);
        WebSocketPressureRunner runner = new WebSocketPressureRunner(config);
        runner.run();
    }
}
