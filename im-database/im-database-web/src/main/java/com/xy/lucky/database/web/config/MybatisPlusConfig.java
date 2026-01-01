package com.xy.lucky.database.web.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.xy.lucky.database.web.utils.DateTimeUtils;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
//@MapperScan("com.xy.databse.mapper")
public class MybatisPlusConfig implements MetaObjectHandler {

    /**
     * 动态识别 JDBC 数据库类型
     *
     * @return
     */
    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
        Properties props = new Properties();
        props.setProperty("Oracle", "oracle");
        props.setProperty("MySQL", "mysql");
        props.setProperty("PostgreSQL", "postgresql");
        props.setProperty("DB2", "db2");
        props.setProperty("SQL Server", "sqlserver");
        provider.setProperties(props);
        return provider;
    }


    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // ✅ 让 SQL 带参数
        interceptor.addInnerInterceptor(new MybatisPlusSqlLogger());
        return interceptor;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        setFieldValByName("createTime", DateTimeUtils.getUTCDateTime(), metaObject);
        setFieldValByName("updateTime", DateTimeUtils.getUTCDateTime(), metaObject);
        //setFieldValByName("delFlag", IMStatus.NO.getCode(), metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        setFieldValByName("updateTime", DateTimeUtils.getUTCDateTime(), metaObject);
    }

}

