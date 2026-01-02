package com.xy.lucky.quartz.domain.context;

public interface TaskLogger {
    /**
     * Update progress and message
     *
     * @param percent 0-100
     * @param msg     message
     */
    void log(int percent, String msg);

    /**
     * Update just message
     */
    void log(String msg);
}
