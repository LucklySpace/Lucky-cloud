package com.xy.lucky.leaf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ID元信息实体类
 * 用于存储Redis Segment ID生成器的相关元信息
 */
@Data
@Entity
@Table(name = "id_meta_info")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IdMetaInfo {

    /**
     * ID类型标识
     */
    @Id
    private String id;

    /**
     * 当前最大ID
     */
    @Column(name = "max_id", nullable = false)
    private Long maxId;

    /**
     * 号段步长
     */
    @Column(name = "step", nullable = false)
    private Integer step;

    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 乐观锁版本号，JPA自动管理
     */
    @Version
    private Integer version;
}