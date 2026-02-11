package com.xy.lucky.oss.rpc.impl.media;

import com.xy.lucky.oss.domain.po.OssFileImagePo;
import com.xy.lucky.oss.enums.StorageBucketEnum;
import com.xy.lucky.oss.exception.FileException;
import com.xy.lucky.oss.mapper.FileVoMapper;
import com.xy.lucky.oss.repository.OssFileImageRepository;
import com.xy.lucky.oss.util.OssUtils;
import com.xy.lucky.rpc.api.oss.media.MediaDubboService;
import com.xy.lucky.rpc.api.oss.vo.FileVo;
import com.xy.lucky.utils.id.IdUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 媒体服务 Dubbo 实现
 */
@Slf4j
@DubboService
public class MediaDubboServiceImpl implements MediaDubboService {

    @Resource
    private OssUtils ossUtils;

    @Resource
    private OssFileImageRepository ossFileImageRepository;

    @Resource
    private FileVoMapper fileVoMapper;

    @Override
    public FileVo uploadImage(String fileName, String contentType, byte[] fileBytes) {
        log.info("Dubbo调用 - 上传图片: fileName={}, size={}", fileName, fileBytes != null ? fileBytes.length : 0);

        // 参数校验
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("文件字节数组不能为空");
        }
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            throw new FileException("文件名格式错误");
        }

        // 计算文件 MD5
        String identifier = DigestUtils.md5Hex(fileBytes);
        log.info("计算文件MD5: identifier={}", identifier);

        // 检查文件是否已存在
        OssFileImagePo existingFile = ossFileImageRepository.findByIdentifier(identifier).orElse(null);
        if (existingFile != null) {
            log.info("文件已存在，直接返回: identifier={}", identifier);
            // 如果路径为空，重新生成
            if (!StringUtils.hasText(existingFile.getPath())) {
                String filePath = ossUtils.getFilePath(existingFile.getBucketName(), existingFile.getObjectKey());
                existingFile.setPath(filePath);
                existingFile = ossFileImageRepository.save(existingFile);
            }
            com.xy.lucky.oss.domain.vo.FileVo serviceVo = fileVoMapper.toVo(existingFile);
            return convertToRpcVo(serviceVo);
        }

        // 获取存储桶
        String bucket = ossUtils.getOrCreateBucketByFileName(fileName);

        // 生成文件名和对象键
        String suffix = StorageBucketEnum.getSuffix(fileName);
        String generatedFileName = IdUtils.randomUUID() + "." + suffix;
        String objectName = ossUtils.getObjectName(ossUtils.generatePath(), generatedFileName);

        try {
            // 上传文件到 OSS
            try (InputStream is = new ByteArrayInputStream(fileBytes)) {
                ossUtils.uploadFile(bucket, objectName, is, contentType);
            }

            // 获取文件路径
            String filePath = ossUtils.getFilePath(bucket, objectName);

            // 构建实体对象
            OssFileImagePo doc = OssFileImagePo.builder()
                    .identifier(identifier)
                    .bucketName(bucket)
                    .fileName(fileName)
                    .objectKey(objectName)
                    .fileType(StorageBucketEnum.getBucketCodeByFilename(fileName))
                    .contentType(contentType)
                    .fileSize((long) fileBytes.length)
                    .path(filePath)
                    .build();

            // 保存到数据库
            OssFileImagePo saved = ossFileImageRepository.save(doc);

            // 转换为 RPC VO
            com.xy.lucky.oss.domain.vo.FileVo serviceVo = fileVoMapper.toVo(saved);
            return convertToRpcVo(serviceVo);
        } catch (Exception e) {
            log.error("上传图片失败: fileName={}, identifier={}", fileName, identifier, e);
            throw new FileException("上传图片失败: " + e.getMessage());
        }
    }

    @Override
    public FileVo uploadAvatar(String fileName, String contentType, byte[] fileBytes) {
        log.info("Dubbo调用 - 上传头像: fileName={}, size={}", fileName, fileBytes != null ? fileBytes.length : 0);

        // 参数校验
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("文件字节数组不能为空");
        }
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            throw new FileException("文件名格式错误");
        }

        // 计算文件 MD5
        String identifier = DigestUtils.md5Hex(fileBytes);
        log.info("计算文件MD5: identifier={}", identifier);

        // 检查文件是否已存在
        OssFileImagePo existingFile = ossFileImageRepository.findByIdentifier(identifier).orElse(null);
        if (existingFile != null) {
            log.info("文件已存在，直接返回: identifier={}", identifier);
            // 如果路径为空，重新生成（头像使用公开路径）
            if (!StringUtils.hasText(existingFile.getPath())) {
                String filePath = ossUtils.getPublicFilePath(existingFile.getBucketName(), existingFile.getObjectKey());
                existingFile.setPath(filePath);
                existingFile = ossFileImageRepository.save(existingFile);
            }
            com.xy.lucky.oss.domain.vo.FileVo serviceVo = fileVoMapper.toVo(existingFile);
            return convertToRpcVo(serviceVo);
        }

        // 获取头像存储桶（会自动设置为公开）
        String bucket = ossUtils.getOrCreateBucketByAvatar();

        // 生成文件名和对象键
        String suffix = StorageBucketEnum.getSuffix(fileName);
        String generatedFileName = IdUtils.randomUUID() + "." + suffix;
        String objectName = ossUtils.getObjectName(ossUtils.generatePath(), generatedFileName);

        try {
            // 上传文件到 OSS
            try (InputStream is = new ByteArrayInputStream(fileBytes)) {
                ossUtils.uploadFile(bucket, objectName, is, contentType);
            }

            // 获取公开文件路径（头像需要公开访问）
            String filePath = ossUtils.getPublicFilePath(bucket, objectName);

            // 构建实体对象
            OssFileImagePo doc = OssFileImagePo.builder()
                    .identifier(identifier)
                    .bucketName(bucket)
                    .fileName(fileName)
                    .objectKey(objectName)
                    .fileType(StorageBucketEnum.getBucketCodeByFilename(fileName))
                    .contentType(contentType)
                    .fileSize((long) fileBytes.length)
                    .path(filePath)
                    .build();

            // 保存到数据库
            OssFileImagePo saved = ossFileImageRepository.save(doc);

            // 转换为 RPC VO
            com.xy.lucky.oss.domain.vo.FileVo serviceVo = fileVoMapper.toVo(saved);
            return convertToRpcVo(serviceVo);
        } catch (Exception e) {
            log.error("上传头像失败: fileName={}, identifier={}", fileName, identifier, e);
            throw new FileException("上传头像失败: " + e.getMessage());
        }
    }

    @Override
    public FileVo uploadAudio(String fileName, String contentType, byte[] fileBytes) {
        log.info("Dubbo调用 - 上传音频: fileName={}, size={}", fileName, fileBytes != null ? fileBytes.length : 0);

        // 参数校验
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("文件字节数组不能为空");
        }
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            throw new FileException("文件名格式错误");
        }

        // 计算文件 MD5
        String identifier = DigestUtils.md5Hex(fileBytes);
        log.info("计算文件MD5: identifier={}", identifier);

        // 检查文件是否已存在
        OssFileImagePo existingFile = ossFileImageRepository.findByIdentifier(identifier).orElse(null);
        if (existingFile != null) {
            log.info("文件已存在，直接返回: identifier={}", identifier);
            // 如果路径为空，重新生成
            if (!StringUtils.hasText(existingFile.getPath())) {
                String filePath = ossUtils.getFilePath(existingFile.getBucketName(), existingFile.getObjectKey());
                existingFile.setPath(filePath);
                existingFile = ossFileImageRepository.save(existingFile);
            }
            com.xy.lucky.oss.domain.vo.FileVo serviceVo = fileVoMapper.toVo(existingFile);
            return convertToRpcVo(serviceVo);
        }

        // 获取存储桶
        String bucket = ossUtils.getOrCreateBucketByFileName(fileName);

        // 生成文件名和对象键
        String suffix = StorageBucketEnum.getSuffix(fileName);
        String generatedFileName = IdUtils.randomUUID() + "." + suffix;
        String objectName = ossUtils.getObjectName(ossUtils.generatePath(), generatedFileName);

        try {
            // 上传文件到 OSS
            try (InputStream is = new ByteArrayInputStream(fileBytes)) {
                ossUtils.uploadFile(bucket, objectName, is, contentType);
            }

            // 获取文件路径
            String filePath = ossUtils.getFilePath(bucket, objectName);

            // 构建实体对象
            OssFileImagePo doc = OssFileImagePo.builder()
                    .identifier(identifier)
                    .bucketName(bucket)
                    .fileName(fileName)
                    .objectKey(objectName)
                    .fileType(StorageBucketEnum.getBucketCodeByFilename(fileName))
                    .contentType(contentType)
                    .fileSize((long) fileBytes.length)
                    .path(filePath)
                    .build();

            // 保存到数据库
            OssFileImagePo saved = ossFileImageRepository.save(doc);

            // 转换为 RPC VO
            com.xy.lucky.oss.domain.vo.FileVo serviceVo = fileVoMapper.toVo(saved);
            return convertToRpcVo(serviceVo);
        } catch (Exception e) {
            log.error("上传音频失败: fileName={}, identifier={}", fileName, identifier, e);
            throw new FileException("上传音频失败: " + e.getMessage());
        }
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

}
