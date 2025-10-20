//package com.xy.server.api.grpc.id;
//
//import com.xy.core.model.IMetaId;
//import com.xy.grpc.client.annotation.GrpcCall;
//import com.xy.grpc.client.annotation.GrpcClient;
//
/// **
// * gRPC 客户端接口，用于生成各种类型的ID
// * 通过 gRPC 协议与 im-generator 服务进行通信
// */
//@GrpcClient(value = "im-generator")
//public interface ImIdGeneratorGrpcClient {
//
//    /**
//     * 调用远程接口获取一个 ID
//     *
//     * @return 生成的ID
//     */
//    @GrpcCall("/generator/id")
//    IMetaId getId(String type, String key);
//
/// /    /**
/// /     * 调用远程接口批量获取 ID
/// /     *
/// /     * @param params 参数数组
/// /     *               params[0] - 策略类型
/// /     *               params[1] - 业务 key
/// /     *               params[2] - 获取数量
/// /     * @return ID列表
/// /     */
/// /    @GrpcCall("/generator/ids")
/// /    List<IMetaId> getBatchIds(Object[] params);
//
//    /**
//     * 通用类型安全的获取 ID 方法，带缓存机制
//     *
//     * @param type       策略类型
//     * @param key        业务 key
//     * @param targetType 目标类型
//     * @param <T>        泛型类型
//     * @return 返回泛型类型的 ID
//     */
/// /    default <T> T getId(String type, String key, Class<T> targetType) {
/// /        IMetaId iMetaId = getId(new Object[]{type, key});
/// /        Object metaId = iMetaId.getMetaId();
/// /        return targetType.cast(metaId);
/// /    }
//}
//
