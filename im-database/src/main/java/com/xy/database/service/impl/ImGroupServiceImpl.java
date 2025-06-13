package com.xy.database.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.database.mapper.ImGroupMapper;
import com.xy.database.service.ImGroupService;
import com.xy.domain.po.ImGroupPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_group】的数据库操作Service实现
 */
@Service
public class ImGroupServiceImpl extends ServiceImpl<ImGroupMapper, ImGroupPo>
        implements ImGroupService {

    @Resource
    private ImGroupMapper imGroupMapper;


    @Override
    public List<String> selectNinePeople(String groupId) {
        return imGroupMapper.selectNinePeople(groupId);
    }
}




