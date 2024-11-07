package com.xy.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.server.domain.po.ImGroupPo;

import java.util.List;

/**
 * @author dense
 * @description 针对表【im_group】的数据库操作Service
 * @createDate 2024-03-17 01:34:00
 */
public interface ImGroupService extends IService<ImGroupPo> {


    List<String> selectNinePeople(String groupId);
}
