package com.xy.lucky.quartz.task;

import com.xy.lucky.quartz.domain.context.ShardingContext;
import com.xy.lucky.quartz.domain.context.TaskLogger;

public interface ITask {
    /**
     * Execute task with context
     *
     * @param params  Job Data JSON
     * @param context Sharding context
     * @param logger  Logger for progress
     * @throws Exception if failed
     */
    void execute(String params, ShardingContext context, TaskLogger logger) throws Exception;
}
