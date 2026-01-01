package com.xy.lucky.database.web.config;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.util.CollectionUtils;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;


/**
 * MyBatis-Plus SQL 日志拦截器
 *
 * <p>在 SQL 执行前拦截，打印完整的可执行 SQL，替换所有占位符。</p>
 * <p>
 * 使用方式：在 MyBatis-Plus 配置中添加此拦截器。
 */
@Slf4j
public class MybatisPlusSqlLogger implements InnerInterceptor {

    /**
     * 在查询执行前拦截，打印 SQL
     */
    @Override
    public void beforeQuery(Executor executor,
                            MappedStatement ms,
                            Object parameter,
                            RowBounds rowBounds,
                            ResultHandler resultHandler,
                            BoundSql boundSql) throws SQLException {
        logSql(boundSql, ms);
    }

    /**
     * 在更新执行前拦截，打印 SQL
     */
    @Override
    public void beforeUpdate(Executor executor,
                             MappedStatement ms,
                             Object parameter) throws SQLException {
        BoundSql boundSql = ms.getBoundSql(parameter);
        logSql(boundSql, ms);
    }

    /**
     * 统一的 SQL 日志打印方法
     *
     * @param boundSql SQL 与参数映射
     * @param ms       MappedStatement 对象
     */
    private void logSql(BoundSql boundSql, MappedStatement ms) {
        try {
            // 获取完整 SQL
            String sql = buildExecutableSql(ms.getConfiguration(), boundSql);
            // 打印时间、ID 与 SQL
            String logMessage = new StringBuilder()
                    .append("[SQL_EXEC] id=")
                    .append(ms.getId())
                    .append("\n")
                    .append(sql)
                    .toString();
            log.info(logMessage);
        } catch (Exception e) {
            log.error("[SQL_LOG_ERROR] id={} error=", ms.getId(), e);
        }
    }

    /**
     * 将 BoundSql 与参数映射转换为可执行的完整 SQL 字符串
     *
     * @param configuration MyBatis 配置
     * @param boundSql      封装后的 SQL 与参数
     * @return 可执行的 SQL 字符串
     */
    private String buildExecutableSql(org.apache.ibatis.session.Configuration configuration,
                                      BoundSql boundSql) {
        // 原始 SQL，规范空白
        String sql = boundSql.getSql().replaceAll("\\s+", " ");
        // 参数映射
        List<ParameterMapping> mappings = boundSql.getParameterMappings();
        Object paramObject = boundSql.getParameterObject();

        if (CollectionUtils.isEmpty(mappings) || paramObject == null) {
            return sql;
        }

        TypeHandlerRegistry registry = configuration.getTypeHandlerRegistry();
        for (ParameterMapping mapping : mappings) {
            Object value = getParameterValue(boundSql, mapping, paramObject, configuration, registry);
            // 按顺序替换第一个占位符
            sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(formatValue(value)));
        }
        return sql;
    }

    /**
     * 获取每个占位符对应的参数值
     */
    private Object getParameterValue(BoundSql boundSql,
                                     ParameterMapping mapping,
                                     Object paramObject,
                                     org.apache.ibatis.session.Configuration configuration,
                                     TypeHandlerRegistry registry) {
        String propName = mapping.getProperty();
        if (registry.hasTypeHandler(paramObject.getClass())) {
            return paramObject;
        }
        MetaObject meta = configuration.newMetaObject(paramObject);
        if (meta.hasGetter(propName)) {
            return meta.getValue(propName);
        } else if (boundSql.hasAdditionalParameter(propName)) {
            return boundSql.getAdditionalParameter(propName);
        }
        return null;
    }

    /**
     * 将参数转为 SQL 字面量形式
     */
    private String formatValue(Object obj) {
        if (obj == null) {
            return "NULL";
        } else if (obj instanceof String) {
            return "'" + obj + "'";
        } else if (obj instanceof Date) {
            DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            return "'" + fmt.format(obj) + "'";
        } else {
            return obj.toString();
        }
    }
}
