package com.xy.lucky.message.message.offline;

import java.util.List;

/**
 * 离线消息服务，负责离线存储与补发。
 */
public interface OfflineMessageService {

    /**
     * 存储离线消息。
     *
     * @param userId  用户 ID
     * @param record  离线消息记录
     */
    void store(String userId, OfflineMessageRecord record);

    /**
     * 拉取并移除离线消息。
     *
     * @param userId 用户 ID
     * @param max    最大拉取数量
     * @return 离线消息列表
     */
    List<OfflineMessageRecord> pull(String userId, int max);
}
