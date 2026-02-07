package com.xy.lucky.chat.config;

/**
 * id 生成器常量
 */
public interface IdGeneratorConstant {

    /**
     * redis 号段   转换类型 long
     */
    String redis = "redis";

    /**
     * 雪花id    转换类型 long
     */
    String snowflake = "snowflake";

    /**
     * 雪花id增强版  转换类型 long
     */
    String uid = "uid";


    /**
     * uuid  转换类型 string
     */
    String uuid = "uuid";


    /**
     * 私聊消息id
     */
    String private_message_id = "private:message:id";

    /**
     * 群聊消息id
     */
    String group_message_id = "group:message:id";

    /**
     * 群邀请id
     */
    String group_invite_id = "group:invite:id";

    /**
     * 会话
     */
    String chat_id = "chat:id";
}

