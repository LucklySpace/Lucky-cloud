package com.xy.lucky.oss.rpc.impl.media;

import com.xy.lucky.oss.service.OssFileImageService;
import com.xy.lucky.rpc.api.oss.media.MediaDubboService;
import com.xy.lucky.rpc.api.oss.vo.FileVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 媒体服务 Dubbo 实现
 */
@Slf4j
@DubboService
public class MediaDubboServiceImpl implements MediaDubboService {

    @Resource
    private OssFileImageService ossFileImageService;

    @Override
    public FileVo uploadImage(String fileName, String contentType, byte[] fileBytes) {
        log.info("Dubbo调用 - 上传图片: fileName={}, size={}", fileName, fileBytes != null ? fileBytes.length : 0);
        MultipartFile multipartFile = buildMultipartFile(fileName, contentType, fileBytes);
        String identifier = DigestUtils.md5Hex(fileBytes);
        com.xy.lucky.oss.domain.vo.ImageVo serviceVo = ossFileImageService.uploadImage(identifier, multipartFile);
        return convertToRpcVo(serviceVo);
    }

    @Override
    public FileVo uploadAvatar(String fileName, String contentType, byte[] fileBytes) {
        log.info("Dubbo调用 - 上传头像: fileName={}, size={}", fileName, fileBytes != null ? fileBytes.length : 0);
        MultipartFile multipartFile = buildMultipartFile(fileName, contentType, fileBytes);
        String identifier = DigestUtils.md5Hex(fileBytes);
        com.xy.lucky.oss.domain.vo.ImageVo serviceVo = ossFileImageService.uploadAvatar(identifier, multipartFile);
        return convertToRpcVo(serviceVo);
    }

    @Override
    public FileVo uploadAudio(String fileName, String contentType, byte[] fileBytes) {
        log.info("Dubbo调用 - 上传音频: fileName={}, size={}", fileName, fileBytes != null ? fileBytes.length : 0);
        MultipartFile multipartFile = buildMultipartFile(fileName, contentType, fileBytes);
        String identifier = DigestUtils.md5Hex(fileBytes);
        com.xy.lucky.oss.domain.vo.FileVo serviceVo = ossFileImageService.uploadAudio(identifier, multipartFile);
        return convertToRpcVo(serviceVo);
    }

    private FileVo convertToRpcVo(com.xy.lucky.oss.domain.vo.FileVo serviceVo) {
        if (serviceVo == null) {
            return null;
        }
        return FileVo.builder()
                .identifier(serviceVo.getKey())
                .name(serviceVo.getName())
                .size(serviceVo.getSize())
                .type(serviceVo.getType())
                .path(serviceVo.getPath())
                .build();
    }

    private FileVo convertToRpcVo(com.xy.lucky.oss.domain.vo.ImageVo serviceVo) {
        if (serviceVo == null) {
            return null;
        }
        return FileVo.builder()
                .identifier(serviceVo.getKey())
                .name(serviceVo.getName())
                .size(serviceVo.getSize())
                .type(serviceVo.getType())
                .path(serviceVo.getPath())
                .thumbnailPath(serviceVo.getThumbPath())
                .build();
    }

    private MultipartFile buildMultipartFile(String fileName, String contentType, byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("文件字节数组不能为空");
        }
        return new ByteArrayMultipartFile(fileName, fileName, contentType, fileBytes);
    }

    private static class ByteArrayMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content != null ? content.length : 0;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws java.io.IOException, IllegalStateException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
