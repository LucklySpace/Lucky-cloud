package com.xy.database.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.database.mapper.ImUserDataMapper;
import com.xy.database.service.ImUserDataService;
import com.xy.domain.po.ImUserDataPo;
import org.springframework.stereotype.Service;

@Service
public class ImUserDataServiceImpl extends ServiceImpl<ImUserDataMapper, ImUserDataPo>
        implements ImUserDataService {
}
