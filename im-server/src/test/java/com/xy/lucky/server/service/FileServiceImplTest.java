package com.xy.lucky.server.service;

import com.xy.lucky.general.exception.BusinessException;
import com.xy.lucky.server.service.impl.FileServiceImpl;
import com.xy.lucky.server.utils.MinioUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * FileServiceImpl 单元测试类
 * 
 * 测试文件服务的核心功能，包括文件上传、文件类型识别、MinIO 交互等操作。
 * 使用 Mockito 模拟 MinioUtil 依赖。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("文件服务测试")
class FileServiceImplTest {

    @Mock
    private MinioUtil minioUtil;

    @InjectMocks
    private FileServiceImpl fileService;

    /**
     * 每个测试方法执行前的初始化操作
     * 设置 MinIO 相关配置
     */
    @BeforeEach
    void setUp() {
        // 使用反射设置 @Value 注入的字段
        ReflectionTestUtils.setField(fileService, "minIOServer", "http://localhost:9000");
        ReflectionTestUtils.setField(fileService, "bucketName", "test-bucket");
    }

    // ==================== getFileType 静态方法测试 ====================

    @Nested
    @DisplayName("getFileType 方法测试")
    class GetFileTypeMethodTests {

        @Test
        @DisplayName("当文件名为jpg格式时_getFileType方法应返回image")
        void getFileType_WithJpgFile_ShouldReturnImage() {
            // 执行测试
            String result = FileServiceImpl.getFileType("photo.jpg");
            
            // 验证结果
            assertEquals("image", result);
        }

        @Test
        @DisplayName("当文件名为png格式时_getFileType方法应返回image")
        void getFileType_WithPngFile_ShouldReturnImage() {
            // 执行测试
            String result = FileServiceImpl.getFileType("screenshot.png");
            
            // 验证结果
            assertEquals("image", result);
        }

        @Test
        @DisplayName("当文件名为mp3格式时_getFileType方法应返回audio")
        void getFileType_WithMp3File_ShouldReturnAudio() {
            // 执行测试
            String result = FileServiceImpl.getFileType("song.mp3");
            
            // 验证结果
            assertEquals("audio", result);
        }

        @Test
        @DisplayName("当文件名为mp4格式时_getFileType方法应返回video")
        void getFileType_WithMp4File_ShouldReturnVideo() {
            // 执行测试
            String result = FileServiceImpl.getFileType("video.mp4");
            
            // 验证结果
            assertEquals("video", result);
        }

        @Test
        @DisplayName("当文件名为pdf格式时_getFileType方法应返回document")
        void getFileType_WithPdfFile_ShouldReturnDocument() {
            // 执行测试
            String result = FileServiceImpl.getFileType("report.pdf");
            
            // 验证结果
            assertEquals("document", result);
        }

        @Test
        @DisplayName("当文件名为未知格式时_getFileType方法应返回file")
        void getFileType_WithUnknownExtension_ShouldReturnFile() {
            // 执行测试
            String result = FileServiceImpl.getFileType("data.xyz");
            
            // 验证结果
            assertEquals("file", result);
        }

        @Test
        @DisplayName("当文件名没有扩展名时_getFileType方法应返回file")
        void getFileType_WithNoExtension_ShouldReturnFile() {
            // 执行测试
            String result = FileServiceImpl.getFileType("filename");
            
            // 验证结果
            assertEquals("file", result);
        }
    }

    // ==================== uploadFile(FilePart) 方法测试 ====================

    @Nested
    @DisplayName("uploadFile(FilePart) 方法测试")
    class UploadFilePartMethodTests {

        @Test
        @DisplayName("当上传有效图片文件时_uploadFile方法应返回包含正确路径的FileVo")
        void uploadFile_WithValidImageFile_ShouldReturnFileVoWithCorrectPath() {
            // 准备测试数据
            String fileName = "test.jpg";
            byte[] fileContent = "test content".getBytes(StandardCharsets.UTF_8);
            
            FilePart filePart = mock(FilePart.class);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            
            DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(fileContent);
            
            when(filePart.filename()).thenReturn(fileName);
            when(filePart.headers()).thenReturn(headers);
            when(filePart.content()).thenReturn(Flux.just(dataBuffer));
            
            // 模拟 MinIO 上传
            given(minioUtil.upload(anyString(), anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                    .willReturn(Mono.just("uploaded_test.jpg"));
            
            // 执行测试并验证
            StepVerifier.create(fileService.uploadFile(filePart))
                    .expectNextMatches(fileVo -> 
                        fileVo.getName().equals(fileName) &&
                        fileVo.getPath().contains("image") &&
                        fileVo.getPath().contains("uploaded_test.jpg"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("当MinIO上传失败时_uploadFile方法应抛出业务异常")
        void uploadFile_WhenMinioUploadFails_ShouldThrowBusinessException() {
            // 准备测试数据
            String fileName = "test.jpg";
            byte[] fileContent = "test content".getBytes(StandardCharsets.UTF_8);
            
            FilePart filePart = mock(FilePart.class);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            
            DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(fileContent);
            
            when(filePart.filename()).thenReturn(fileName);
            when(filePart.headers()).thenReturn(headers);
            when(filePart.content()).thenReturn(Flux.just(dataBuffer));
            
            // 模拟 MinIO 上传返回空
            given(minioUtil.upload(anyString(), anyString(), anyString(), any(InputStream.class), anyLong(), anyString()))
                    .willReturn(Mono.just(""));
            
            // 执行测试并验证
            StepVerifier.create(fileService.uploadFile(filePart))
                    .expectError(BusinessException.class)
                    .verify();
        }
    }

    // ==================== generUrl 方法测试 ====================

    @Nested
    @DisplayName("generUrl 方法测试")
    class GenerUrlMethodTests {

        @Test
        @DisplayName("当生成图片URL时_generUrl方法应返回正确格式的URL")
        void generUrl_WithImageType_ShouldReturnCorrectUrl() {
            // 执行测试
            String result = fileService.generUrl("image", "photo.jpg");
            
            // 验证结果
            assertEquals("http://localhost:9000/test-bucket/image/photo.jpg", result);
        }

        @Test
        @DisplayName("当生成视频URL时_generUrl方法应返回正确格式的URL")
        void generUrl_WithVideoType_ShouldReturnCorrectUrl() {
            // 执行测试
            String result = fileService.generUrl("video", "movie.mp4");
            
            // 验证结果
            assertEquals("http://localhost:9000/test-bucket/video/movie.mp4", result);
        }

        @Test
        @DisplayName("当生成文档URL时_generUrl方法应返回正确格式的URL")
        void generUrl_WithDocumentType_ShouldReturnCorrectUrl() {
            // 执行测试
            String result = fileService.generUrl("document", "report.pdf");
            
            // 验证结果
            assertEquals("http://localhost:9000/test-bucket/document/report.pdf", result);
        }
    }
}

