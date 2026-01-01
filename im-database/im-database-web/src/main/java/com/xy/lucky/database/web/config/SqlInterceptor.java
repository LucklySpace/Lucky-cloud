package com.xy.lucky.database.web.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.regex.Matcher;


/**
 * sql 拦截器，用于打印sql日志
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update", args = {
                MappedStatement.class, Object.class})
})
@Slf4j
public class SqlInterceptor implements Interceptor {

    private static String generateSql(Invocation invocation) {
        MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs().length > 1 ? invocation.getArgs()[1] : null;
        BoundSql boundSql = statement.getBoundSql(parameter);

        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        for (ParameterMapping param : boundSql.getParameterMappings()) {
            Object value = boundSql.getAdditionalParameter(param.getProperty());
            sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(value)));
        }
        return sql;
    }

    private static String getParameterValue(Object object) {
        return object instanceof String ? "'" + object + "'" : String.valueOf(object);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object proceed = invocation.proceed();
        long endTime = System.currentTimeMillis();

        String sql = generateSql(invocation);
        log.info("\n 执行SQL耗时：{}ms \n 执行SQL：{}", endTime - startTime, sql);
        return proceed;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
}
