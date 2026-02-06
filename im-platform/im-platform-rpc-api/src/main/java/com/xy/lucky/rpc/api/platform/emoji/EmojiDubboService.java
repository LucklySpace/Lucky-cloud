package com.xy.lucky.rpc.api.platform.emoji;

import com.xy.lucky.rpc.api.platform.dto.EmojiPackDto;
import com.xy.lucky.rpc.api.platform.vo.EmojiPackVo;
import com.xy.lucky.rpc.api.platform.vo.EmojiRespVo;
import com.xy.lucky.rpc.api.platform.vo.EmojiVo;

import java.util.List;

/**
 * 表情服务 Dubbo 接口
 *
 * @author Lucky Platform
 * @since 1.0.0
 */
public interface EmojiDubboService {

    /**
     * 创建或更新表情包
     *
     * @param request 表情包信息
     * @return 表情包信息
     */
    EmojiPackVo upsertPack(EmojiPackDto request);

    /**
     * 列出所有表情包
     *
     * @return 表情包列表
     */
    List<EmojiPackVo> listPacks();

    /**
     * 上传表情图片到指定表情包
     *
     * @param emojiId 表情ID
     * @return 表情信息
     */
    EmojiVo uploadEmoji(String emojiId);

    /**
     * 列出指定表情包中的所有表情
     *
     * @param packId 表情包ID
     * @return 表情列表
     */
    List<EmojiVo> listEmojis(String packId);

    /**
     * 上传表情包封面图
     *
     * @param packId 表情包ID
     * @param url    封面图URL
     * @return 表情包信息
     */
    EmojiPackVo uploadCover(String packId, String url);

    /**
     * 启用/禁用表情包
     *
     * @param packId  表情包ID
     * @param enabled 是否启用
     * @return 表情包信息
     */
    EmojiPackVo togglePack(String packId, boolean enabled);

    /**
     * 查询表情包详情
     *
     * @param packId 表情包ID
     * @return 表情包详情
     */
    EmojiRespVo getPackId(String packId);

    /**
     * 删除表情
     *
     * @param emojiId      表情ID
     * @param removeObject 是否同时删除对象存储
     */
    void deleteEmoji(String emojiId, boolean removeObject);

    /**
     * 生成表情包编码
     *
     * @return 表情包编码
     */
    String generatePackCode();
}
