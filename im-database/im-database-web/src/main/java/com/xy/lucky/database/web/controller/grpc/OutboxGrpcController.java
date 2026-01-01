//package com.xy.database.controller.grpc;
//
//import com.xy.database.service.IMOutboxService;
//import com.xy.domain.po.IMOutboxPo;
//import com.xy.grpc.server.annotation.GrpcRoute;
//import com.xy.grpc.server.annotation.GrpcRouteMapping;
//import jakarta.annotation.Resource;
//
//import java.util.List;
//
/// **
// * gRPC 服务端控制器，用于处理消息投递表(IMOutboxPo)相关操作
// * 通过 gRPC 协议对外提供服务，供其他微服务调用
// */
//@GrpcRouteMapping("/outbox")
//public class OutboxGrpcController {
//
//    @Resource
//    private IMOutboxService imOutboxService;
//
//    /**
//     * 根据ID获取单个消息投递记录
//     *
//     * @param id 消息投递记录ID
//     * @return 消息投递记录对象
//     */
//    @GrpcRoute("/getById")
//    public IMOutboxPo getById(Long id) {
//        return imOutboxService.getById(id);
//    }
//
//    /**
//     * 保存或更新消息投递记录
//     *
//     * @param outboxPo 消息投递记录对象
//     * @return 是否操作成功
//     */
//    @GrpcRoute("/saveOrUpdate")
//    public Boolean saveOrUpdate(IMOutboxPo outboxPo) {
//        return imOutboxService.saveOrUpdate(outboxPo);
//    }
//
//    /**
//     * 删除消息投递记录
//     *
//     * @param outboxPo 消息投递记录对象
//     * @return 是否删除成功
//     */
//    @GrpcRoute("/delete")
//    public Boolean delete(IMOutboxPo outboxPo) {
//        return imOutboxService.removeById(outboxPo);
//    }
//
//    /**
//     * 批量获取指定状态的待发送消息
//     *
//     * @param params 包含状态和限制数量的参数对象
//     *               params[0] - 状态
//     *               params[1] - 限制数量
//     * @return 消息列表
//     */
//    @GrpcRoute("/listByStatus")
//    public List<IMOutboxPo> listByStatus(Object[] params) {
//        String status = (String) params[0];
//        Integer limit = (Integer) params[1];
//        return imOutboxService.listByStatus(status, limit);
//    }
//
//    /**
//     * 更新消息状态
//     *
//     * @param params 包含消息ID、状态和尝试次数的参数对象
//     *               params[0] - 消息ID
//     *               params[1] - 状态
//     *               params[2] - 尝试次数
//     * @return 是否更新成功
//     */
//    @GrpcRoute("/updateStatus")
//    public Boolean updateStatus(Object[] params) {
//        Long id = (Long) params[0];
//        String status = (String) params[1];
//        Integer attempts = (Integer) params[2];
//        return imOutboxService.updateStatus(id, status, attempts);
//    }
//
//    /**
//     * 更新消息为发送失败
//     *
//     * @param params 包含消息ID、错误信息和尝试次数的参数对象
//     *               params[0] - 消息ID
//     *               params[1] - 错误信息
//     *               params[2] - 尝试次数
//     * @return 是否更新成功
//     */
//    @GrpcRoute("/markAsFailed")
//    public Boolean markAsFailed(Object[] params) {
//        Long id = (Long) params[0];
//        String lastError = (String) params[1];
//        Integer attempts = (Integer) params[2];
//        return imOutboxService.markAsFailed(id, lastError, attempts);
//    }
//}