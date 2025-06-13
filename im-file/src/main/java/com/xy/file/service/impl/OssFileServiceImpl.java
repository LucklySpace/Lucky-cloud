package com.xy.file.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.xy.file.entity.OssFile;
import com.xy.file.entity.OssFileUploadProgress;
import com.xy.file.enums.BoolEnum;
import com.xy.file.service.OssFileService;
import com.xy.file.util.MinioUtils;
import com.xy.file.util.RedisRepo;
import com.xy.file.util.ResponseResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    private RedisRepo redisRepo;

    /**
     * 获取文件上传进度
     *
     * @param identifier 文件唯一标识
     * @return 文件上传进度信息
     */
    @Override
    public ResponseResult getMultipartUploadProgress(String identifier) {
        log.info("查询文件上传进度 - identifier:{}", identifier);

        // 从Redis获取文件信息
        OssFile ossFile = getOssFileByIdentifier(identifier);
        OssFileUploadProgress uploadProgress = new OssFileUploadProgress();

        // 新文件,尚未开始上传
        if (ObjectUtil.isNull(ossFile)) {
            log.info("文件未开始上传 - identifier:{}", identifier);
            uploadProgress.setIsNew(BoolEnum.YES);
            return ResponseResult.success(uploadProgress);
        }

        Integer isFinish = ossFile.getIsFinish();
        uploadProgress.setIsFinish(isFinish);

        // 文件已完成上传
        if (ObjectUtil.equal(isFinish, BoolEnum.YES)) {
            String filePath = minioUtils.getFilePath(ossFile.getBucketName(), ossFile.getFileName());
            log.info("文件已完成上传 - identifier:{}, path:{}", identifier, filePath);
            uploadProgress.setPath(filePath);
            return ResponseResult.success(uploadProgress);
        }

        // 获取分片上传进度
        log.info("获取分片上传进度 - identifier:{}", identifier);
        return minioUtils.getMultipartUploadProgress(ossFile, uploadProgress);
    }

    /**
     * 初始化分片上传
     *
     * @param reqOssFile 文件信息对象
     * @return 上传初始化结果, 包含uploadId和预签名URL
     */
    @Override
    public ResponseResult initMultiPartUpload(OssFile reqOssFile) {
        String identifier = reqOssFile.getIdentifier();
        log.info("初始化文件上传 - identifier:{}, fileName:{}", identifier, reqOssFile.getFileName());

        // 检查文件是否已在上传中
        OssFile existingFile = getOssFileByIdentifier(identifier);
        Assert.isNull(existingFile, "文件已在上传中 - identifier:" + identifier);

        ResponseResult result;

        // 根据分片数选择上传方式
        if (reqOssFile.getPartNum() == 1) {
            log.info("使用单文件上传 - identifier:{}", identifier);
            result = minioUtils.initUpload(reqOssFile);
        } else {
            log.info("使用分片上传 - identifier:{}, partNum:{}", identifier, reqOssFile.getPartNum());
            result = minioUtils.initMultiPartUpload(reqOssFile);
        }

        // 保存文件信息到Redis
        saveOssFile(reqOssFile);
        log.info("上传初始化完成 - identifier:{}", identifier);
        return result;
    }

    @Override
    public ResponseResult mergeMultipartUpload(String identifier) {
        log.info("[分片合并] 开始合并分片，identifier={}", identifier);

        // 校验
        OssFile ossFile = getOssFileByIdentifier(identifier);
        Assert.notNull(ossFile, "[分片合并] 文件未发起过分片上传任务，identifier=" + identifier);
        Assert.isFalse(ObjectUtil.equal(BoolEnum.YES, ossFile.getIsFinish()), "[分片合并] 文件已完成合并，identifier=" + identifier);

        // 合并文件
        ResponseResult responseResult = minioUtils.mergeOssFileUpload(ossFile);

        // 更新数据状态
        ossFile.setIsFinish(BoolEnum.YES);
        saveOssFile(ossFile);

        log.info("[分片合并] 合并完成，identifier={}", identifier);
        return responseResult;
    }

    @Override
    public ResponseResult isExits(String identifier) {
        log.info("[文件存在检查] 开始检查文件是否存在，identifier={}", identifier);

        // 查询文件是否已经上传
        OssFile ossFile = getOssFileByIdentifier(identifier);

        if (minioUtils.checkObjectExists(ossFile)) {
            String filePath = minioUtils.getFilePath(ossFile.getBucketName(), ossFile.getFileName());
            ossFile.setPath(filePath);
            log.info("[文件存在检查] 文件已存在，identifier={}，filePath={}", identifier, filePath);
            return ResponseResult.success(ossFile);
        }

        log.warn("[文件存在检查] 文件不存在，identifier={}", identifier);
        return ResponseResult.error();
    }

    /**
     * 获取文件信息
     *
     * @param identifier
     * @return
     */
    private OssFile getOssFileByIdentifier(String identifier) {
        log.debug("[文件获取] 从 Redis 中获取文件，identifier={}", identifier);

        String objStr = redisRepo.get(identifier);
        if (!StringUtils.hasText(objStr)) {
            log.debug("[文件获取] 文件不存在于 Redis 中，identifier={}", identifier);
            return null;
        }

        return JSONObject.parseObject(objStr, OssFile.class);
    }

    private void saveOssFile(OssFile ossFile) {
        log.debug("[文件保存] 将文件信息保存到 Redis，identifier={}", ossFile.getIdentifier());
        redisRepo.saveTimeout(ossFile.getIdentifier(), JSONObject.toJSONString(ossFile), 30, TimeUnit.MINUTES);
        log.debug("[文件保存] 文件信息保存成功，identifier={}", ossFile.getIdentifier());
    }

    @Override
    public ResponseEntity downloadFile(String identifier, String range) {
        log.info("[文件下载] 开始下载文件，identifier={}，range={}", identifier, range);

        // 查询文件是否已经上传
        OssFile ossFile = getOssFileByIdentifier(identifier);

        if (minioUtils.checkObjectExists(ossFile)) {
            log.info("[文件下载] 文件存在，开始下载，identifier={}", identifier);
            return minioUtils.download(ossFile, range);
        }

        log.warn("[文件下载] 文件不存在，identifier={}", identifier);
        return null;
    }
}


