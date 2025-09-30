package com.xy.server.api.grpc.outbox;

import com.xy.domain.po.IMOutboxPo;
import com.xy.grpc.client.annotation.GrpcCall;
import com.xy.grpc.client.annotation.GrpcClient;

import java.util.List;

/**
 * gRPC 客户端接口，用于操作消息投递表(IMOutboxPo)
 * 通过 gRPC 协议与 im-database 服务进行通信
 */
@GrpcClient(value = "im-database")
public interface IMOutboxGrpcClient {

    /**
     * 根据ID获取单个消息投递记录
     *
     * @param id 消息投递记录ID
     * @return 消息投递记录对象
     */
    @GrpcCall("/outbox/getById")
    IMOutboxPo getById(Long id);

    /**
     * 保存或更新消息投递记录
     *
     * @param outboxPo 消息投递记录对象
     * @return 是否操作成功
     */
    @GrpcCall("/outbox/saveOrUpdate")
    Boolean saveOrUpdate(IMOutboxPo outboxPo);

    /**
     * 删除消息投递记录
     *
     * @param outboxPo 消息投递记录对象
     * @return 是否删除成功
     */
    @GrpcCall("/outbox/delete")
    Boolean delete(IMOutboxPo outboxPo);

    /**
     * 批量获取指定状态的待发送消息
     *
     * @param status 状态
     * @param limit  限制数量
     * @return 消息列表
     */
    @GrpcCall("/outbox/listByStatus")
    List<IMOutboxPo> listByStatus(String status, Integer limit);

    /**
     * 更新消息状态
     *
     * @param id       消息ID
     * @param status   状态
     * @param attempts 尝试次数
     * @return 是否更新成功
     */
    @GrpcCall("/outbox/updateStatus")
    Boolean updateStatus(Long id, String status, Integer attempts);

    /**
     * 更新消息为发送失败
     *
     * @param id        消息ID
     * @param lastError 错误信息
     * @param attempts  尝试次数
     * @return 是否更新成功
     */
    @GrpcCall("/outbox/markAsFailed")
    Boolean markAsFailed(Long id, String lastError, Integer attempts);
}