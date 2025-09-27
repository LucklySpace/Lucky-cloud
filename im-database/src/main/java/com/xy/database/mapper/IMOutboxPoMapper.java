package com.xy.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.domain.po.IMOutboxPo;
import org.apache.ibatis.annotations.Mapper;


/**
 * @description 针对表【im_outbox】的数据库操作Mapper
 */
@Mapper
public interface IMOutboxPoMapper extends BaseMapper<IMOutboxPo> {
}