package com.xy.database.utils;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * MyBatis 批量执行器，用于执行批量插入/更新操作。
 * <p>
 * 使用示例：  batchExecutor.batchSave(users, UserMapper.class, UserMapper::insert);
 */
@Slf4j
@Component
public class MybatisBatchExecutor {

    @Resource
    private SqlSessionTemplate sqlSessionTemplate;

    /**
     * 通用批量保存方法
     *
     * @param list       要保存的数据列表
     * @param mapperType MyBatis Mapper 接口类型
     * @param biConsumer lambda 表达式，定义 mapper 和单条数据的执行逻辑
     * @param <M>        Mapper 类型（接口）
     * @param <T>        实体类型
     */
    public <M, T> void batchSave(List<T> list, Class<M> mapperType, BiConsumer<M, T> biConsumer) {
        if (CollectionUtils.isEmpty(list)) {
            log.warn("待保存数据为空，跳过批量操作");
            return;
        }

        SqlSessionFactory sqlSessionFactory = sqlSessionTemplate.getSqlSessionFactory();
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {

            M mapper = session.getMapper(mapperType);

            for (T item : list) {
                biConsumer.accept(mapper, item);
            }

            session.commit();
            session.clearCache();
            log.info("MyBatis 批量执行成功，操作记录数: {}", list.size());

        } catch (Exception e) {
            log.error("MyBatis 批量执行异常，已回滚事务", e);
            throw new RuntimeException("MyBatis 批量操作失败", e);
        }
    }
}
