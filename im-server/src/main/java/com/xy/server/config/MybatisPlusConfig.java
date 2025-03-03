package com.xy.server.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.xy.imcore.enums.IMStatus;
import com.xy.server.utils.DateTimeUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.xy.server.mapper")
public class MybatisPlusConfig implements MetaObjectHandler {


    @Override
    public void insertFill(MetaObject metaObject) {
        setFieldValByName("createTime", DateTimeUtils.getUTCDateTime(), metaObject);
        setFieldValByName("updateTime", DateTimeUtils.getUTCDateTime(), metaObject);
        setFieldValByName("delFlag", IMStatus.NO.getCode(), metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        setFieldValByName("updateTime", DateTimeUtils.getUTCDateTime(), metaObject);
    }
}