package com.xy.database.config;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.xy.database.utils.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
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
 * MybatisPlus sql 拦截器 输出日志
 * 参考url: https://blog.csdn.net/m0_64289188/article/details/145716521
 */
@Slf4j
public class MybatisPlusSqlLogger implements InnerInterceptor {


    /**
     * 日志打印的核心逻辑
     *
     * @param boundSql  BoundSql实例
     * @param ms        MappedStatement实例
     * @param parameter 参数对象
     */
    private static void logInfo(BoundSql boundSql, MappedStatement ms, Object parameter) {

        // 获取SQL的ID
        String sqlId = ms.getId();

        String time = DateTimeUtils.getFormatDate(new Date(), "");

        try {
            // 获取最终的SQL语句
            String sql = getSql(ms.getConfiguration(), boundSql, sqlId);

            String info = "[SQL_EXEC] time=" + time + " id=" + sqlId + "\n" + "  - [SQL] " + sql + "\n";

            log.info(info);

        } catch (Exception e) {
            // 捕获异常并打印
            log.error("[SQL_LOG_ERROR] time={} id={} + \n", time, sqlId, e);
        }
    }

    /**
     * 封装SQL语句信息，返回SQL节点id和对应的SQL语句
     *
     * @param configuration 配置对象
     * @param boundSql      BoundSql实例
     * @param sqlId         SQL节点ID
     * @return 完整的SQL信息
     */
    public static String getSql(Configuration configuration, BoundSql boundSql, String sqlId) {
        // 返回完整的sql信息
        return showSql(configuration, boundSql);
    }

    /**
     * 替换SQL语句中的占位符？为实际的参数值
     *
     * @param configuration 配置对象
     * @param boundSql      BoundSql实例
     * @return 替换后的SQL语句
     */
    public static String showSql(Configuration configuration, BoundSql boundSql) {
        // 获取参数对象
        Object parameterObject = boundSql.getParameterObject();
        // 获取SQL中所有的参数映射
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();

        // 清除SQL中的多余空格
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");

        // 如果存在参数且参数对象不为空，则替换占位符
        if (!CollectionUtils.isEmpty(parameterMappings) && parameterObject != null) {
            // 获取类型处理器注册器
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(parameterObject)));
            } else {
                // 获取MetaObject，用于获取参数属性
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                for (ParameterMapping parameterMapping : parameterMappings) {
                    String propertyName = parameterMapping.getProperty();
                    if (metaObject.hasGetter(propertyName)) {
                        Object obj = metaObject.getValue(propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        // 动态SQL中的额外参数
                        Object obj = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                    } else {
                        // 如果缺少参数，打印“缺失”
                        sql = sql.replaceFirst("\\?", "缺失");
                    }
                }
            }
        }
        return sql;
    }

    /**
     * 将参数对象转化为对应的SQL表示形式（例如：字符串加引号，日期加引号等）
     *
     * @param obj 参数对象
     * @return 转换后的参数值
     */
    private static String getParameterValue(Object obj) {
        if (obj == null) {
            // 处理null情况
            return "NULL";
        }

        if (obj instanceof String) {
            // 字符串加单引号
            return "'" + obj.toString() + "'";
        } else if (obj instanceof Date) {

            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            // 日期格式化后加单引号
            return "'" + formatter.format(obj) + "'";

        } else {
            return obj.toString();
        }
    }

    /**
     * 查询前的拦截
     *
     * @param executor      Executor实例
     * @param ms            MappedStatement实例
     * @param parameter     查询参数
     * @param rowBounds     分页信息
     * @param resultHandler 结果处理器
     * @param boundSql      BoundSql实例
     * @throws SQLException SQL异常
     */
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        logInfo(boundSql, ms, parameter);
    }

    /**
     * 更新前的拦截
     *
     * @param executor  Executor实例
     * @param ms        MappedStatement实例
     * @param parameter 更新参数
     * @throws SQLException SQL异常
     */
    @Override
    public void beforeUpdate(Executor executor, MappedStatement ms, Object parameter) throws SQLException {
        BoundSql boundSql = ms.getBoundSql(parameter);
        logInfo(boundSql, ms, parameter);
    }
}