package com.xy.lucky.file.domain;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import net.coobird.thumbnailator.geometry.Positions;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OssFileMediaInfo {

    /**
     * 宽 高
     */
    private Integer width, height;

    /**
     * 水印地址
     */
    private String watermarkPath = "C:/Users/dense/Desktop/截图/通话请求窗口.jpg";

    /**
     * 水印位置
     */
    private Positions watermarkPosition = Positions.BOTTOM_RIGHT;
    /**
     * 透明度
     */
    private Float opacity = 0.5f;
    /**
     * 放大倍数
     */
    private Double scale = 0.5;

    /**
     * 比例
     */
    private Double ratio = 0.3;

    /**
     * 格式
     */
    private String format = "png";

}
