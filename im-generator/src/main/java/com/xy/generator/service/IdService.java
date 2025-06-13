package com.xy.generator.service;

import com.xy.generator.model.IdMetaInfo;
import com.xy.generator.repository.IdMetaInfoRepository;
import jakarta.annotation.Resource;
import org.hibernate.service.spi.ServiceException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class IdService {

    @Resource
    private RedisTemplate<String, Long> redisTemplate;

    @Resource
    private IdMetaInfoRepository idMetaInfoRepo;

    /**
     * 获取用户ID（号段模式+Redis原子操作）
     */
    public String generateUserId() {
        String key = "id:segment:user";
        Long increment = redisTemplate.opsForValue().increment(key, 1);

        if (increment == null) {
            throw new ServiceException("Redis ID生成失败");
        }

        // 号段用尽时从数据库加载新号段
        if (increment % 1000 == 0) { // 假设步长1000
            loadNewSegment(key);
        }

        return "UID" + System.currentTimeMillis() / 1000 + String.format("%06d", increment % 1000);
    }

    /**
     * 生成消息ID（Redis原子递增）
     */
    public String generateMsgId(int machineId) {
        String key = "id:msg:" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long seq = redisTemplate.opsForValue().increment(key);
        return String.format("MSG%02d%tM%S%04d", machineId, System.currentTimeMillis(), seq % 10000);
    }

    /**
     * 加载新号段到Redis
     */
    @Transactional
    public void loadNewSegment(String id) {
        IdMetaInfo meta = idMetaInfoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("无效ID类型"));

        long newMaxId = meta.getMaxId() + meta.getStep();
        redisTemplate.opsForValue().set("id:segment:" + id.toLowerCase(), newMaxId);
        meta.setMaxId(newMaxId);
        idMetaInfoRepo.save(meta);
    }
}