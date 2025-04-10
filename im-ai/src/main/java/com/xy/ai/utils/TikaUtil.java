package com.xy.ai.utils;


import lombok.RequiredArgsConstructor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件内容提取工具
 */
@Component
@RequiredArgsConstructor
public class TikaUtil {

    public String extractTextString(MultipartFile file) {
        try {
            // 创建解析器--在不确定文档类型时候可以选择使用AutoDetectParser可以自动检测一个最合适的解析器
            Parser parser = new AutoDetectParser();
            // 用于捕获文档提取的文本内容。-1 参数表示使用无限缓冲区,解析到的内容通过此hander获取
            BodyContentHandler bodyContentHandler = new BodyContentHandler(-1);
            // 元数据对象，它在解析器中传递元数据属性---可以获取文档属性
            Metadata metadata = new Metadata();
            // 带有上下文相关信息的ParseContext实例，用于自定义解析过程。
            ParseContext parseContext = new ParseContext();
            parser.parse(file.getInputStream(), bodyContentHandler, metadata, parseContext);
            // 获取文本
            return bodyContentHandler.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}