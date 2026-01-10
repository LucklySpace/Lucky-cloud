package com.xy.lucky.spring.boot;

import com.xy.lucky.spring.boot.env.Environment;

import java.io.PrintStream;

/**
 * 默认 Banner 实现
 */
public class DefaultBanner implements Banner {

    private static final String[] BANNER = {
            "",
            "  _                _                  ____             _             ",
            " | |    _   _  ___| | ___   _        / ___| _ __  _ __(_)_ __   __ _ ",
            " | |   | | | |/ __| |/ / | | |       \\___ \\| '_ \\| '__| | '_ \\ / _` |",
            " | |___| |_| | (__|   <| |_| |        ___) | |_) | |  | | | | | (_| |",
            " |_____|\\__,_|\\___|_|\\_\\\\__, |       |____/| .__/|_|  |_|_| |_|\\__, |",
            "                       |___/               |_|                |___/ ",
            ""
    };

    private static final String VERSION = "1.0.0";

    @Override
    public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
        for (String line : BANNER) {
            out.println(line);
        }
        out.println(" :: Lucky Spring ::                             (v" + VERSION + ")");
        out.println();
    }
}

