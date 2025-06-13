package com.xy.server.api.id;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 获取id
 */
@FeignClient(contextId = "id", value = "im-generator", path = "/api/v1/generator")
public interface ImIdGeneratorFeign {

    // 拉取数量
    Integer pullCount = 15;

    // 剩余数量
    Integer residualCount = 5;

    /**
     * 本地缓存池：key 为 type#key，value 为一个线程安全的队列，存储多个 ID
     */
    Map<String, Queue<Object>> cachePool = new ConcurrentHashMap<>();

    /**
     * 调用远程接口获取一个 ID
     */
    @GetMapping("/id")
    Object getId(@RequestParam("type") String type,
                 @RequestParam("key") String key);

    /**
     * 调用远程接口获取一个 ID
     */
    @GetMapping("/ids")
    List<Object> getBatchIds(@RequestParam("type") String type,
                             @RequestParam("key") String key, @RequestParam("count") Integer count);


    /**
     * 通用类型安全的获取 ID 方法，带缓存机制
     *
     * @param type       策略类型
     * @param key        业务 key
     * @param targetType 目标类型
     * @param <T>        泛型类型
     * @return 返回泛型类型的 ID
     */
    default <T> T getId(String type, String key, Class<T> targetType) {

//        String cacheKey = type + "#" + key;
//
//        Queue<Object> queue = cachePool.computeIfAbsent(cacheKey, k -> new LinkedBlockingQueue<>());
//
//        // 如果缓存为空，拉取一批 ID 进行缓存
//        if(queue.isEmpty() && queue.size() <= residualCount){
//
//            List<Object> newIds = getBatchIds(type, key, pullCount);
//
//            queue.addAll(newIds);
//        }
//
//        Object id = queue.poll();

        Object id = getId(type, key);

        return targetType.cast(id);
    }
}
