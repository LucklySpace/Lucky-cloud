package com.xy.database.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.xy.database.utils.DateTimeUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
//@MapperScan("com.xy.databse.mapper")
public class MybatisPlusConfig implements MetaObjectHandler {

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

