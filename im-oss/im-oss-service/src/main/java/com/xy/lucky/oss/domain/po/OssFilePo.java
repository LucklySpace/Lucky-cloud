package com.xy.lucky.oss.domain.po;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "文件")
@Entity
@Table(name = "im_oss_file")
public class OssFilePo {

    @Schema(description = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Schema(description = "分片上传的uploadId")
    @Column(name = "upload_id", length = 128)
    private String uploadId;

    @Schema(description = "桶名称")
    @Column(name = "bucket_name", length = 128)
    private String bucketName;

    @Schema(description = "文件唯一标识（md5）")
    @Column(name = "identifier", length = 64, unique = true, nullable = false)
    @NotBlank
    private String identifier;

    @Schema(description = "文件名")
    @Column(name = "file_name", length = 512)
    @NotBlank
    private String fileName;

    @Schema(description = "文件类型")
    @Column(name = "file_type", length = 128)
    private String fileType;

    @Schema(description = "文件的key")
    @Column(name = "object_key", length = 512)
    private String objectKey;

    @Schema(description = "文件类型")
    @Column(name = "content_type", length = 128)
    private String contentType;

    @Schema(description = "文件大小")
    @Column(name = "file_size")
    private Long fileSize;

    @Schema(description = "每个分片大小")
    @Column(name = "part_size")
    private Long partSize;

    @Schema(description = "分片数量")
    @Column(name = "part_num")
    @NotNull
    @Min(1)
    private Integer partNum;

    @Schema(description = "是否已完成上传")
    @Column(name = "is_finish")
    @ColumnDefault("0")
    private Integer isFinish;

    @Schema(description = "文件地址")
    @Column(name = "path", length = 1024)
    private String path;

    @Schema(description = "创建时间（插入时自动填充）")
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

}
