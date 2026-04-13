package com.xy.lucky.rpc.api.database.offline;

import com.xy.lucky.domain.po.IMOfflineMessagePo;

import java.util.List;

public interface IMOfflineMessageDubboService {

    Boolean create(IMOfflineMessagePo offlineMessagePo);

    Boolean createBatch(List<IMOfflineMessagePo> list);

    List<IMOfflineMessagePo> pullAndRemoveByUserId(String userId, Integer limit, Long nowTimestamp);
}
