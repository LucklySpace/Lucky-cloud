package com.xy.lucky.lbs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.lucky.lbs.domain.po.CountyPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CountyMapper extends BaseMapper<CountyPo> {

    @Select("SELECT * FROM im_lbs_china_county ASC LIMIT 1")
    CountyPo findNearest(@Param("lng") Double lng, @Param("lat") Double lat);
}
