package com.xy.lucky.oss.rpc.impl.file;

import com.xy.lucky.oss.domain.po.OssFilePo;
import com.xy.lucky.oss.service.OssFileService;
import com.xy.lucky.oss.util.OssUtils;
import com.xy.lucky.rpc.api.oss.dto.FileDownloadRangeDto;
import com.xy.lucky.rpc.api.oss.dto.OssFileDto;
import com.xy.lucky.rpc.api.oss.file.FileDubboService;
import com.xy.lucky.rpc.api.oss.vo.FileChunkVo;
import com.xy.lucky.rpc.api.oss.vo.FileUploadProgressVo;
import com.xy.lucky.rpc.api.oss.vo.FileVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 文件服务 Dubbo 实现
 */
@Slf4j
@DubboService
public class FileDubboServiceImpl implements FileDubboService {

    @Resource
    private OssFileService ossFileService;

    @Resource
    private OssUtils ossUtils;

    @Override
    public FileUploadProgressVo getMultipartUploadProgress(String identifier) {
        log.info("Dubbo调用 - 获取分片上传进度: {}", identifier);
        com.xy.lucky.oss.domain.vo.FileUploadProgressVo serviceVo = ossFileService.getMultipartUploadProgress(identifier);
        return convertToRpcVo(serviceVo);
    }

    @Override
    public FileChunkVo initMultiPartUpload(OssFileDto ossFileDto) {
        log.info("Dubbo调用 - 初始化分片上传: {}", ossFileDto.getIdentifier());
        OssFilePo ossFilePo = convertToOssFilePo(ossFileDto);
        com.xy.lucky.oss.domain.vo.FileChunkVo serviceVo = ossFileService.initMultiPartUpload(ossFilePo);
        return convertToRpcVo(serviceVo);
    }

    @Override
    public FileVo mergeMultipartUpload(String identifier) {
        log.info("Dubbo调用 - 合并分片上传: {}", identifier);
        com.xy.lucky.oss.domain.vo.FileVo serviceVo = ossFileService.mergeMultipartUpload(identifier);
        return convertToRpcVo(serviceVo);
    }

    @Override
    public FileVo isExits(String identifier) {
        log.info("Dubbo调用 - 检查文件是否存在: {}", identifier);
        try {
            com.xy.lucky.oss.domain.vo.FileVo serviceVo = ossFileService.isExits(identifier);
            return convertToRpcVo(serviceVo);
        } catch (Exception e) {
            log.warn("文件不存在: {}", identifier);
            return null;
        }
    }

    @Override
    public FileVo uploadFile(String identifier, String fileName, String contentType, byte[] fileBytes) {
        log.info("Dubbo调用 - 上传文件: identifier={}, fileName={}, size={}", identifier, fileName, fileBytes != null ? fileBytes.length : 0);
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("文件字节数组不能为空");
        }
        MultipartFile multipartFile = new ByteArrayMultipartFile(fileName, fileName, contentType, fileBytes);
        com.xy.lucky.oss.domain.vo.FileVo serviceVo = ossFileService.uploadFile(identifier, multipartFile);
        return convertToRpcVo(serviceVo);
    }

    @Override
    public FileDownloadRangeDto downloadFile(String identifier, String range) {
        log.info("Dubbo调用 - 下载文件: identifier={}, range={}", identifier, range);
        ResponseEntity<?> response = ossFileService.downloadFile(identifier, range);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() instanceof FileDownloadRangeDto) {
            return (FileDownloadRangeDto) response.getBody();
        }
        // 如果响应不是预期的类型，尝试从响应头中解析范围信息
        // 这里简化处理，实际可以根据需要实现
        return FileDownloadRangeDto.builder().build();
    }

    @Override
    public FileVo getFileMd5(byte[] fileBytes, String fileName) {
        log.info("Dubbo调用 - 获取文件MD5: fileName={}", fileName);
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("文件字节数组不能为空");
        }
        MultipartFile multipartFile = new ByteArrayMultipartFile(fileName, fileName, null, fileBytes);
        com.xy.lucky.oss.domain.vo.FileVo serviceVo = ossFileService.getFileMd5(multipartFile);
        return convertToRpcVo(serviceVo);
    }

    /**
     * 将 OssFileDto 转换为 OssFilePo
     */
    private OssFilePo convertToOssFilePo(OssFileDto dto) {
        return OssFilePo.builder()
                .uploadId(dto.getUploadId())
                .bucketName(dto.getBucketName())
                .identifier(dto.getIdentifier())
                .fileName(dto.getFileName())
                .fileType(dto.getFileType())
                .objectKey(dto.getObjectKey())
                .contentType(dto.getContentType())
                .fileSize(dto.getFileSize())
                .partSize(dto.getPartSize())
                .partNum(dto.getPartNum())
                .build();
    }

    /**
     * 将 Service 模块的 FileVo 转换为 RPC API 的 FileVo
     */
    private FileVo convertToRpcVo(com.xy.lucky.oss.domain.vo.FileVo serviceVo) {
        if (serviceVo == null) {
            return null;
        }
        FileVo rpcVo = new FileVo();
        BeanUtils.copyProperties(serviceVo, rpcVo);
        return rpcVo;
    }

    /**
     * 将 Service 模块的 FileChunkVo 转换为 RPC API 的 FileChunkVo
     */
    private FileChunkVo convertToRpcVo(com.xy.lucky.oss.domain.vo.FileChunkVo serviceVo) {
        if (serviceVo == null) {
            return null;
        }
        FileChunkVo rpcVo = new FileChunkVo();
        BeanUtils.copyProperties(serviceVo, rpcVo);
        return rpcVo;
    }

    /**
     * 将 Service 模块的 FileUploadProgressVo 转换为 RPC API 的 FileUploadProgressVo
     */
    private FileUploadProgressVo convertToRpcVo(com.xy.lucky.oss.domain.vo.FileUploadProgressVo serviceVo) {
        if (serviceVo == null) {
            return null;
        }
        FileUploadProgressVo rpcVo = new FileUploadProgressVo();
        BeanUtils.copyProperties(serviceVo, rpcVo);
        return rpcVo;
    }

    /**
     * 字节数组 MultipartFile 实现
     */
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
