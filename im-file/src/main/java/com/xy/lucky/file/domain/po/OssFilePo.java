package com.xy.lucky.file.domain.po;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "im_oss_file")
public class OssFilePo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "主键")
    private Long id;

    @Column(name = "upload_id", length = 128)
    @Schema(description = "上传ID")
    private String uploadId;

    @Column(name = "bucket_name", length = 128)
    @Schema(description = "bucket名称")
    private String bucketName;

    @Column(name = "identifier", length = 64, unique = true, nullable = false)
    @Schema(description = "文件标识")
    private String identifier;

    @Column(name = "file_name", length = 512)
    @Schema(description = "文件名称")
    private String fileName;

    @Column(name = "file_type", length = 128)
    @Schema(description = "文件类型")
    private String fileType;

    @Column(name = "object_key", length = 512)
    @Schema(description = "对象键")
    private String objectKey;

    @Column(name = "content_type", length = 128)
    @Schema(description = "内容类型")
    private String contentType;

    @Column(name = "file_size")
    @Schema(description = "文件大小")
    private Long fileSize;

    @Column(name = "part_size")
    @Schema(description = "分片大小")
    private Long partSize;

    @Column(name = "part_num")
    @Schema(description = "分片数量")
    private Integer partNum;

    @Column(name = "is_finish")
    @ColumnDefault("0")
    @Schema(description = "是否完成")
    private Integer isFinish;

    @Column(name = "path", length = 1024)
    @Schema(description = "文件路径")
    private String path;
}
