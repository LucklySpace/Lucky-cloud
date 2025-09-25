package com.xy.server.api.id;


/**
 * id 请求 常量
 */
public class IdGeneratorConstant {

    /**
     * redis 号段   转换类型 long
     */
    public static final String redis = "redis";

    /**
     * 雪花id    转换类型 long
     */
    public static final String snowflake = "snowflake";

    /**
     * 雪花id增强版  转换类型 long
     */
    public static final String uid = "uid";


    /**
     * uuid  转换类型 string
     */
    public static final String uuid = "uuid";


    /**
     * 私聊消息id
     */
    public static final String private_message_id = "private:message:id";

    /**
     * 群聊消息id
     */
    public static final String group_message_id = "group:message:id";

    /**
     * 群邀请id
     */
    public static final String group_invite_id = "group:invite:id";

    /**
     * 会话
     */
    public static final String chat_id = "chat:id";


}
