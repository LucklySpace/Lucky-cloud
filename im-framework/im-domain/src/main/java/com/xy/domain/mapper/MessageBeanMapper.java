package com.xy.domain.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.core.model.IMGroupMessage;
import com.xy.core.model.IMSingleMessage;
import com.xy.core.model.IMVideoMessage;
import com.xy.core.model.IMessage;
import com.xy.domain.po.ImGroupMessagePo;
import com.xy.domain.po.ImSingleMessagePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.Map;

/**
 * 消息相关实体映射
 * 处理IMessage及其子类与数据库PO类之间的转换
 */
@Mapper
public interface MessageBeanMapper {

    MessageBeanMapper INSTANCE = Mappers.getMapper(MessageBeanMapper.class);

    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * IMSingleMessage -> ImSingleMessagePo
     */
    @Mappings({
            @Mapping(source = "fromId", target = "fromId"),
            @Mapping(source = "messageId", target = "messageId"),
            @Mapping(source = "messageContentType", target = "messageContentType"),
            @Mapping(source = "messageTime", target = "messageTime"),
            @Mapping(source = "messageBody", target = "messageBody", qualifiedByName = "messageBodyToObject"),
            @Mapping(source = "extra", target = "extra", qualifiedByName = "mapToObject"),
            @Mapping(source = "replyMessage", target = "replyMessage", qualifiedByName = "replyMessageToObject"),
            @Mapping(source = "toId", target = "toId"),
            @Mapping(source = "readStatus", target = "readStatus"),
            @Mapping(source = "sequence", target = "sequence"),
            @Mapping(target = "delFlag", ignore = true),
            @Mapping(target = "messageRandom", ignore = true),
            @Mapping(target = "createTime", ignore = true),
            @Mapping(target = "updateTime", ignore = true),
            @Mapping(target = "version", ignore = true)
    })
    ImSingleMessagePo toImSingleMessagePo(IMSingleMessage imSingleMessage);

    /**
     * ImSingleMessagePo -> IMSingleMessage
     */
    @Mappings({
            @Mapping(source = "fromId", target = "fromId"),
            @Mapping(source = "messageId", target = "messageId"),
            @Mapping(source = "messageContentType", target = "messageContentType"),
            @Mapping(source = "messageTime", target = "messageTime"),
            @Mapping(source = "messageBody", target = "messageBody", qualifiedByName = "objectToMessageBody"),
            @Mapping(source = "extra", target = "extra", qualifiedByName = "objectToMap"),
            @Mapping(source = "replyMessage", target = "replyMessage", qualifiedByName = "objectToReplyMessage"),
            @Mapping(source = "toId", target = "toId"),
            @Mapping(target = "messageTempId", ignore = true),
            @Mapping(target = "readStatus", ignore = true),
            @Mapping(target = "sequence", ignore = true),
            @Mapping(target = "mentionedUserIds", ignore = true),
            @Mapping(target = "mentionAll", ignore = true),
            @Mapping(target = "messageType", constant = "1003") // SINGLE_MESSAGE
    })
    IMSingleMessage toIMSingleMessage(ImSingleMessagePo imSingleMessagePo);

    /**
     * IMGroupMessage -> ImGroupMessagePo
     */
    @Mappings({
            @Mapping(source = "fromId", target = "fromId"),
            @Mapping(source = "messageId", target = "messageId"),
            @Mapping(source = "messageContentType", target = "messageContentType"),
            @Mapping(source = "messageTime", target = "messageTime"),
            @Mapping(source = "messageBody", target = "messageBody", qualifiedByName = "messageBodyToObject"),
            @Mapping(source = "extra", target = "extra", qualifiedByName = "mapToObject"),
            @Mapping(source = "replyMessage", target = "replyMessage", qualifiedByName = "replyMessageToObject"),
            @Mapping(source = "groupId", target = "groupId"),
            @Mapping(source = "sequence", target = "sequence"),
            @Mapping(target = "readStatus", ignore = true),
            @Mapping(target = "delFlag", ignore = true),
            @Mapping(target = "messageRandom", ignore = true),
            @Mapping(target = "createTime", ignore = true),
            @Mapping(target = "updateTime", ignore = true),
            @Mapping(target = "version", ignore = true)
    })
    ImGroupMessagePo toImGroupMessagePo(IMGroupMessage imGroupMessage);

    /**
     * ImGroupMessagePo -> IMGroupMessage
     */
    @Mappings({
            @Mapping(source = "fromId", target = "fromId"),
            @Mapping(source = "messageId", target = "messageId"),
            @Mapping(source = "messageContentType", target = "messageContentType"),
            @Mapping(source = "messageTime", target = "messageTime"),
            @Mapping(source = "messageBody", target = "messageBody", qualifiedByName = "objectToMessageBody"),
            @Mapping(source = "extra", target = "extra", qualifiedByName = "objectToMap"),
            @Mapping(source = "replyMessage", target = "replyMessage", qualifiedByName = "objectToReplyMessage"),
            @Mapping(source = "groupId", target = "groupId"),
            @Mapping(target = "messageTempId", ignore = true),
            @Mapping(target = "readStatus", ignore = true),
            @Mapping(target = "sequence", ignore = true),
            @Mapping(target = "mentionedUserIds", ignore = true),
            @Mapping(target = "mentionAll", ignore = true),
            @Mapping(target = "toList", ignore = true),
            @Mapping(target = "messageType", constant = "1004") // GROUP_MESSAGE
    })
    IMGroupMessage toIMGroupMessage(ImGroupMessagePo imGroupMessagePo);

    /**
     * IMVideoMessage -> ImSingleMessagePo
     * 视频消息本质上是单聊消息的一种特殊类型
     */
    @Mappings({
            @Mapping(source = "fromId", target = "fromId"),
            @Mapping(source = "toId", target = "toId"),
            @Mapping(target = "messageId", ignore = true),
            @Mapping(target = "messageContentType", constant = "1005"), // VIDEO_MESSAGE
            @Mapping(target = "messageTime", expression = "java(System.currentTimeMillis())"),
            @Mapping(target = "messageBody", ignore = true),
            @Mapping(target = "extra", ignore = true),
            @Mapping(target = "replyMessage", ignore = true),
            @Mapping(target = "readStatus", ignore = true),
            @Mapping(target = "delFlag", ignore = true),
            @Mapping(target = "sequence", ignore = true),
            @Mapping(target = "messageRandom", ignore = true),
            @Mapping(target = "createTime", ignore = true),
            @Mapping(target = "updateTime", ignore = true),
            @Mapping(target = "version", ignore = true)
    })
    ImSingleMessagePo toImSingleMessagePo(IMVideoMessage imVideoMessage);


    /**
     * 将Map转换为JSON字符串
     */
    @Named("mapToString")
    default String mapToString(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 将JSON字符串转换为Map
     */
    @Named("stringToMap")
    default Map<String, Object> stringToMap(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(str, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 将Object转换为MessageBody
     */
    @Named("objectToMessageBody")
    default IMessage.MessageBody objectToMessageBody(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            // 先将对象转换为JSON，再转换为MessageBody
            String json = OBJECT_MAPPER.writeValueAsString(obj);
            return OBJECT_MAPPER.readValue(json, IMessage.MessageBody.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 将MessageBody转换为Object
     */
    @Named("messageBodyToObject")
    default Object messageBodyToObject(IMessage.MessageBody messageBody) {
        if (messageBody == null) {
            return null;
        }
        try {
            // 先将MessageBody转换为JSON，再转换为Object
            String json = OBJECT_MAPPER.writeValueAsString(messageBody);
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Named("replyMessageToObject")
    default Object replyMessageToObject(IMessage.ReplyMessageInfo replyMessage) {
        if (replyMessage == null) {
            return null;
        }
        try {
            String json = OBJECT_MAPPER.writeValueAsString(replyMessage);
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Named("objectToReplyMessage")
    default IMessage.ReplyMessageInfo objectToReplyMessage(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            String json = OBJECT_MAPPER.writeValueAsString(obj);
            return OBJECT_MAPPER.readValue(json, IMessage.ReplyMessageInfo.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Named("mapToObject")
    default Object mapToObject(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsString(map), Object.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Named("objectToMap")
    default Map<String, Object> objectToMap(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(OBJECT_MAPPER.writeValueAsString(obj), new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}