package com.xy.lucky.address.domain;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * 区域节点，包括国家、省份、城市、地区等信息
 * <p>
 * 数据可见 resources/area.csv 文件
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"parent"})
public class Area {

    /**
     * 编号 - 全球，即根目录
     */
    public static final Integer ID_GLOBAL = 0;
    /**
     * 编号 - 中国
     */
    public static final Integer ID_CHINA = 1;

    /**
     * 编码
     */
    private Integer code;

    /**
     * 名字
     */
    private String name;

    /**
     * 类型
     */
    private Integer type;

    /**
     * 父节点
     */
    @JsonManagedReference
    private Area parent;

    /**
     * 子节点
     */
    @JsonBackReference
    private List<Area> children;

}
