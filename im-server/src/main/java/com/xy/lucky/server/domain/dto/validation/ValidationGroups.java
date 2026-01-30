package com.xy.lucky.server.domain.dto.validation;

/**
 * 参数校验分组
 * <p>
 * 用于不同场景下的参数校验
 * </p>
 *
 * @author xy
 */
public interface ValidationGroups {

    /**
     * 创建操作
     */
    interface Create {
    }

    /**
     * 更新操作
     */
    interface Update {
    }

    /**
     * 删除操作
     */
    interface Delete {
    }

    /**
     * 查询操作
     */
    interface Query {
    }

    /**
     * 审批操作
     */
    interface Approve {
    }

    // ==================== 群管理相关 ====================

    /**
     * 踢出群成员
     */
    interface KickMember {
    }

    /**
     * 设置/取消管理员
     */
    interface SetAdmin {
    }

    /**
     * 移交群主
     */
    interface TransferOwner {
    }

    /**
     * 设置群加入方式
     */
    interface SetJoinMode {
    }

    /**
     * 禁言/取消禁言成员
     */
    interface MuteMember {
    }

    /**
     * 全员禁言/取消全员禁言
     */
    interface MuteAll {
    }

    /**
     * 解散群组
     */
    interface DismissGroup {
    }
}

