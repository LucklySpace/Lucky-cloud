package com.xy.lucky.file.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.xy.lucky.file.domain.OssFileUploadProgress;
import com.xy.lucky.file.domain.po.OssFilePo;
import com.xy.lucky.file.domain.vo.FileChunkVo;
import com.xy.lucky.file.domain.vo.FileVo;
import com.xy.lucky.file.enums.BoolEnum;
import com.xy.lucky.file.enums.StorageBucketEnum;
import com.xy.lucky.file.exception.FileException;
import com.xy.lucky.file.mapper.FileVoMapper;
import com.xy.lucky.file.repository.OssFileRepository;
import com.xy.lucky.file.service.OssFileService;
import com.xy.lucky.file.util.MD5Utils;
import com.xy.lucky.file.util.MinioUtils;
import com.xy.lucky.file.util.RedisUtils;
import com.xy.lucky.utils.id.IdUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * OSS文件服务实现类
 * 处理文件上传、合并、下载等业务逻辑
 */
@Slf4j
@Service
public class OssFileServiceImpl implements OssFileService {

    @Resource
    private MinioUtils minioUtils;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private OssFileRepository ossFileRepository;

    @Resource
    private FileVoMapper fileVoMapper;
    @Resource
    private RestTemplate restTemplate;

    /**
     * 获取文件上传进度
     *
     * @param identifier 文件唯一标识
     * @return 文件上传进度信息
     */
    @Override
    public OssFileUploadProgress getMultipartUploadProgress(String identifier) {
        log.info("查询文件上传进度 - identifier:{}", identifier);

        // 读取上传会话（优先 Redis，回退 DB）
        OssFilePo ossFilePo = getOssFileByIdentifier(identifier);
        OssFileUploadProgress uploadProgress = new OssFileUploadProgress();

        // 新文件：尚未开始上传
        if (ObjectUtils.isEmpty(ossFilePo)) {
            log.info("文件未开始上传 - identifier:{}", identifier);
            uploadProgress.setIsNew(BoolEnum.YES);
            return uploadProgress;
        }

        Integer isFinish = ossFilePo.getIsFinish();
        uploadProgress.setIsFinish(isFinish);

        // 已完成上传：直接返回外网访问地址
        if (isFinish.equals(BoolEnum.YES)) {
            String filePath = minioUtils.getFilePath(ossFilePo.getBucketName(), ossFilePo.getObjectKey());
            log.info("文件已完成上传 - identifier:{}, path:{}", identifier, filePath);
            uploadProgress.setPath(filePath);
            return uploadProgress;
        }

        // 进行分片进度查询
        log.info("获取分片上传进度 - identifier:{}", identifier);
        return minioUtils.getMultipartUploadProgress(ossFilePo, uploadProgress);
    }

    /**
     * 初始化分片上传
     *
     * @param reqOssFilePo 文件信息对象
     * @return 上传初始化结果, 包含uploadId和预签名URL
     */
    @Override
    public FileChunkVo initMultiPartUpload(OssFilePo reqOssFilePo) {
        String identifier = reqOssFilePo.getIdentifier();
        log.info("初始化文件上传 - identifier:{}, fileName:{}", identifier, reqOssFilePo.getFileName());

        OssFilePo existingFile = getOssFileByIdentifier(identifier);
        if (existingFile != null) {
            throw new FileException("文件已在上传中 - identifier:" + identifier);
        }

        FileChunkVo result;

        // 单分片走直传，多分片走分片
        if (reqOssFilePo.getPartNum() == 1) {
            log.info("使用单文件上传 - identifier:{}", identifier);
            result = minioUtils.initUpload(reqOssFilePo);
        } else {
            log.info("使用分片上传 - identifier:{}, partNum:{}", identifier, reqOssFilePo.getPartNum());
            result = minioUtils.initMultiPartUpload(reqOssFilePo);
        }

        // 缓存上传会话（TTL）
        saveOssFileToRedis(reqOssFilePo);

        log.info("上传分片初始化完成 - identifier:{}", identifier);

        return result;
    }

    @Override
    public FileVo mergeMultipartUpload(String identifier) {
        log.info("[分片合并] 开始合并分片，identifier={}", identifier);

        OssFilePo ossFilePo = getOssFileByIdentifier(identifier);
        if (ossFilePo == null) {
            throw new FileException("[分片合并] 文件未发起过分片上传任务，identifier=" + identifier);
        }
        if (BoolEnum.YES.equals(ossFilePo.getIsFinish())) {
            throw new FileException("[分片合并] 文件已完成合并，identifier=" + identifier);
        }

        // 合并到最终对象
        String path = minioUtils.mergeOssFileUpload(ossFilePo);

        // 标记完成并持久化记录
        ossFilePo.setIsFinish(BoolEnum.YES);

        saveOssFileToRedis(ossFilePo);

        tryPersist(ossFilePo, path);

        log.info("[分片合并] 合并完成，identifier={}", identifier);
        return fileVoMapper.toVo(ossFilePo);
    }

    @Override
    public FileVo isExits(String identifier) {
        log.info("[文件存在检查] 开始检查文件是否存在，identifier={}", identifier);

        OssFilePo ossFilePo = getOssFileByIdentifier(identifier);

        if (ossFilePo != null && minioUtils.checkObjectExists(ossFilePo)) {
            String filePath = minioUtils.getFilePath(ossFilePo.getBucketName(), ossFilePo.getObjectKey());
            ossFilePo.setPath(filePath);
            log.info("[文件存在检查] 文件已存在，identifier={}，filePath={}", identifier, filePath);
            return fileVoMapper.toVo(ossFilePo);
        }

        log.warn("[文件存在检查] 文件不存在，identifier={}", identifier);

        throw new FileException("文件不存在");
    }

    @Override
    public FileVo uploadFile(String identifier, MultipartFile file) {

        // 根据文件后缀选择存储桶，避免使用 MIME 类型导致非法桶名
        String originalFilename = file.getOriginalFilename();

        log.info("[文件上传] 开始上传文件 文件名: {}, 文件大小: {}", originalFilename, file.getSize());

        if (file == null || file.isEmpty() || !StringUtils.hasText(identifier)) {
            throw new FileException("文件或文件md5不能为空");
        }

        if (!StringUtils.hasText(originalFilename) && !originalFilename.contains(".")) {
            throw new FileException("文件名格式错误");
        }

        // 校验文件md5
        MD5Utils.checkMD5(identifier, file);

        // 查询是否存在
        OssFilePo ossFilePoByIdentifier = getOssFileByIdentifier(identifier);

        if (Objects.nonNull(ossFilePoByIdentifier)) {
            return fileVoMapper.toVo(ossFilePoByIdentifier);
        }

        // 获取文件后缀
        String suffix = StorageBucketEnum.getSuffix(originalFilename);

        // 获取存储桶
        String bucketName = minioUtils.getOrCreateBucketByFileName(originalFilename);

        // 获取新文件名
        String fileName = IdUtils.randomUUID() + "." + suffix;

        // 生成对象名（使用日期路径+原始文件名）
        String objectName = minioUtils.getObjectName(minioUtils.generatePath(), fileName);

        try {
            log.info("[文件上传] 存储桶：{}，对象名：{}", bucketName, objectName);

            // 上传文件
            minioUtils.uploadFile(bucketName, objectName, file.getInputStream(), file.getContentType());

            // 缓存上传会话（TTL）
            OssFilePo ossFilePo = OssFilePo.builder()
                    .identifier(identifier)
                    .fileName(originalFilename)
                    .fileType(StorageBucketEnum.getBucketCodeByFilename(originalFilename))
                    .bucketName(bucketName)
                    .objectKey(objectName)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .partNum(1)
                    .isFinish(BoolEnum.YES)
                    .path(minioUtils.getFilePath(bucketName, objectName)).build();

            saveOssFileToRedis(ossFilePo);

            tryPersist(ossFilePo, ossFilePo.getPath());

            return fileVoMapper.toVo(ossFilePo);
        } catch (Exception e) {
            log.error("[文件上传] 文件上传失败", e);
            throw new FileException("文件上传失败");
        }
    }

    @Override
    public ResponseEntity<?> downloadFile(String identifier, String range) {
        log.info("[文件下载] 开始下载文件，identifier={}，range={}", identifier, range);

        if (!StringUtils.hasText(identifier)) {
            log.warn("[文件下载] identifier 不能为空");
            return ResponseEntity.badRequest().build();
        }

        // 查询文件是否已经上传
        OssFilePo ossFilePo = getOssFileByIdentifier(identifier);

        if (ossFilePo != null && minioUtils.checkObjectExists(ossFilePo)) {
            log.info("[文件下载] 文件存在，开始下载，identifier={}", identifier);
            return minioUtils.download(ossFilePo, range);
        }

        log.warn("[文件下载] 文件不存在，identifier={}", identifier);
        return ResponseEntity.notFound().build();
    }


    /**
     * 获取文件信息
     *
     * @param identifier 文件md5
     * @return 文件信息
     */
    private OssFilePo getOssFileByIdentifier(String identifier) {
        log.debug("[文件获取] 从 Redis 中获取文件，identifier={}", identifier);

        String objStr = redisUtils.get(identifier);
        if (!StringUtils.hasText(objStr)) {
            log.debug("[文件获取] 文件不存在于 Redis 中，identifier={}", identifier);
            return findFromDb(identifier);
        }

        return JSONObject.parseObject(objStr, OssFilePo.class);
    }

    /**
     * 保存文件信息
     *
     * @param ossFilePo 文件信息
     */
    private void saveOssFileToRedis(OssFilePo ossFilePo) {
        log.debug("[文件保存] 文件信息保存成功，identifier={}", ossFilePo.getIdentifier());
        redisUtils.saveTimeout(ossFilePo.getIdentifier(), JSONObject.toJSONString(ossFilePo), 30, TimeUnit.MINUTES);
    }


    /**
     * 从数据库中获取文件信息
     *
     * @param identifier 文件md5
     * @return 文件信息
     */
    private OssFilePo findFromDb(String identifier) {
        return ossFileRepository.findByIdentifier(identifier)
                .orElse(null);
    }

    /**
     * 尝试持久化文件信息
     *
     * @param ossFilePo 文件信息
     * @param path    文件路径
     */
    private void tryPersist(OssFilePo ossFilePo, String path) {
        try {
            if (path != null) {
                ossFilePo.setPath(path);
            }
            ossFileRepository.findByIdentifier(ossFilePo.getIdentifier())
                    .ifPresentOrElse(existing -> {
                        ossFilePo.setId(existing.getId());
                        ossFileRepository.save(ossFilePo);
                    }, () -> ossFileRepository.save(ossFilePo));
        } catch (Exception ex) {
            log.warn("持久化文件失败 identifier={}", ossFilePo.getIdentifier(), ex);
        }
    }
}
