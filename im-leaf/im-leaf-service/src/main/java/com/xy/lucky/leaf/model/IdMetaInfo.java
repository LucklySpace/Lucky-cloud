package com.xy.lucky.leaf.model;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "ID元信息实体类")
public class IdMetaInfo {

    /**
     * ID类型标识
     */
    @Id
    @Schema(description = "ID类型标识")
    private String id;

    /**
     * 当前最大ID
     */
    @Schema(description = "当前最大ID")
    @Column(name = "max_id", nullable = false)
    private Long maxId;

    /**
     * 号段步长
     */
    @Schema(description = "号段步长")
    @Column(name = "step", nullable = false)
    private Integer step;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 乐观锁版本号，JPA自动管理
     */
    @Schema(description = "乐观锁版本号，JPA自动管理")
    @Version
    private Integer version;
}