package com.xy.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.domain.po.IMOutboxPo;

import java.util.List;

public interface IMOutboxService extends IService<IMOutboxPo> {
    Boolean updateStatus(Long id, String status, Integer attempts);

    Boolean markAsFailed(Long id, String lastError, Integer attempts);

    List<IMOutboxPo> listByStatus(String status, Integer limit);
}
