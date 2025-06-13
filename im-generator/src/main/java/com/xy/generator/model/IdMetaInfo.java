package com.xy.generator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "id_meta_info")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IdMetaInfo {

    // ID类型枚举
    @Id
//    @Enumerated(EnumType.STRING)
    private String id;

    // 当前最大ID
    @Column(name = "max_id", nullable = false)
    private Long maxId;

    // 号段步长
    @Column(name = "step", nullable = false)
    private Integer step;


    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 乐观锁版本号，JPA自动管理
     */
    @Version
    private Integer version;
}

