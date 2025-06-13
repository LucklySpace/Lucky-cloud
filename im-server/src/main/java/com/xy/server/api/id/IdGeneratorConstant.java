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
     * 私聊
     */
    public static final String private_id = "private:id";

    /**
     * 群聊
     */
    public static final String group_id = "group:id";

    /**
     * 会话
     */
    public static final String chat_id = "chat:id";


}
