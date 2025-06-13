/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80033 (8.0.33)
 Source Host           : localhost:3306
 Source Schema         : im-core

 Target Server Type    : MySQL
 Target Server Version : 80033 (8.0.33)
 File Encoding         : 65001

 Date: 27/05/2025 20:19:06
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for id_meta_info
-- ----------------------------
DROP TABLE IF EXISTS `id_meta_info`;
CREATE TABLE `id_meta_info`  (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `max_id` bigint NULL DEFAULT NULL,
  `step` int NULL DEFAULT NULL,
  `update_time` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  `version` int NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of id_meta_info
-- ----------------------------
INSERT INTO `id_meta_info` VALUES ('im', 106090, 10, '2025-05-22 23:13:59', 8248);
INSERT INTO `id_meta_info` VALUES ('session_id', 13000, 1000, '2025-05-21 18:06:15', 2);

-- ----------------------------
-- Table structure for im_chat
-- ----------------------------
DROP TABLE IF EXISTS `im_chat`;
CREATE TABLE `im_chat`  (
  `chat_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '聊天ID',
  `chat_type` int NOT NULL COMMENT '聊天类型：0单聊，1群聊，2机器人，3公众号',
  `owner_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '会话拥有者用户ID',
  `to_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '对方用户ID或群组ID',
  `is_mute` tinyint NOT NULL COMMENT '是否免打扰（1免打扰）',
  `is_top` tinyint NOT NULL COMMENT '是否置顶（1置顶）',
  `sequence` bigint NULL DEFAULT NULL COMMENT '消息序列号',
  `read_sequence` bigint NULL DEFAULT NULL COMMENT '已读消息序列',
  `create_time` bigint NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint NULL DEFAULT NULL COMMENT '更新时间',
  `del_flag` tinyint NULL DEFAULT 0 COMMENT '删除标识（0正常，1删除）',
  `version` int UNSIGNED NULL DEFAULT NULL COMMENT '版本信息',
  PRIMARY KEY (`chat_id`) USING BTREE,
  INDEX `idx_chat_owner_to`(`owner_id` ASC, `to_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of im_chat
-- ----------------------------
INSERT INTO `im_chat` VALUES ('ac5a1b15-b5bb-4729-8d6b-6e966e43b8eb', 1003, '100001', '100005', 0, 0, 1748274346889, NULL, 1743516755731, 1748274347223, 0, NULL);
INSERT INTO `im_chat` VALUES ('ad0276b6-e181-422e-8d3e-f97965d78f89', 1003, '100001', '100004', 0, 0, 1745685760971, NULL, 1745651528536, 1745685761140, 0, NULL);
INSERT INTO `im_chat` VALUES ('df8b89f0-7bcc-45e4-a950-09cb005694ce', 1003, '100004', '100001', 0, 0, 1745685760971, NULL, 1745651528507, 1745685761126, 0, NULL);
INSERT INTO `im_chat` VALUES ('ff37b253-6a87-4e83-8028-0297ceedb9da', 1003, '100005', '100001', 0, 0, 1748274346889, NULL, 1743516755721, 1748274347210, 0, NULL);

-- ----------------------------
-- Table structure for im_friendship
-- ----------------------------
DROP TABLE IF EXISTS `im_friendship`;
CREATE TABLE `im_friendship`  (
  `owner_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `to_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '好友用户ID',
  `remark` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `status` int NULL DEFAULT NULL COMMENT '状态（1正常，2删除）',
  `black` int NULL DEFAULT NULL COMMENT '黑名单状态（1正常，2拉黑）',
  `create_time` bigint NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint NULL DEFAULT NULL COMMENT '更新时间',
  `sequence` bigint NULL DEFAULT NULL COMMENT '序列号',
  `black_sequence` bigint NULL DEFAULT NULL COMMENT '黑名单序列号',
  `add_source` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '好友来源',
  `extra` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '扩展字段',
  `version` int UNSIGNED NULL DEFAULT NULL COMMENT '版本信息',
  PRIMARY KEY (`owner_id`, `to_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of im_friendship
-- ----------------------------
INSERT INTO `im_friendship` VALUES ('100001', '100002', NULL, 1, 1, 1740994514376, NULL, 1740994514369, NULL, NULL, NULL, NULL);
INSERT INTO `im_friendship` VALUES ('100002', '100001', NULL, 1, 1, 1740994514934, NULL, 1740994514932, NULL, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for im_friendship_group
-- ----------------------------
DROP TABLE IF EXISTS `im_friendship_group`;
CREATE TABLE `im_friendship_group`  (
  `from_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `group_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分组ID',
  `group_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分组名称',
  `sequence` bigint NULL DEFAULT NULL COMMENT '序列号',
  `create_time` bigint NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint NULL DEFAULT NULL COMMENT '更新时间',
  `del_flag` tinyint NULL DEFAULT 0 COMMENT '删除标识',
  `version` int UNSIGNED NULL DEFAULT NULL COMMENT '版本信息',
  PRIMARY KEY (`group_id`) USING BTREE,
  UNIQUE INDEX `uniq_from_group`(`from_id` ASC, `group_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of im_friendship_group
-- ----------------------------

-- ----------------------------
-- Table structure for im_friendship_group_member
-- ----------------------------
DROP TABLE IF EXISTS `im_friendship_group_member`;
CREATE TABLE `im_friendship_group_member`  (
  `group_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `to_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '好友用户ID',
  `create_time` bigint NULL DEFAULT NULL COMMENT '添加时间',
  `del_flag` tinyint NULL DEFAULT 0 COMMENT '删除标识',
  `version` int UNSIGNED NULL DEFAULT NULL COMMENT '版本信息',
  PRIMARY KEY (`group_id`, `to_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of im_friendship_group_member
-- ----------------------------

-- ----------------------------
-- Table structure for im_friendship_request
-- ----------------------------
DROP TABLE IF EXISTS `im_friendship_request`;
CREATE TABLE `im_friendship_request`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '请求ID',
  `from_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '请求发起者',
  `to_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '请求接收者',
  `remark` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `read_status` int NULL DEFAULT NULL COMMENT '是否已读（1已读）',
  `add_source` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '好友来源',
  `message` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '好友验证信息',
  `approve_status` int NULL DEFAULT NULL COMMENT '审批状态（1同意，2拒绝）',
  `create_time` bigint NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint NULL DEFAULT NULL COMMENT '更新时间',
  `sequence` bigint NULL DEFAULT NULL COMMENT '序列号',
  `del_flag` tinyint NULL DEFAULT 0 COMMENT '删除标识（0正常，1删除）',
  `version` int UNSIGNED NULL DEFAULT NULL COMMENT '版本信息',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of im_friendship_request
-- ----------------------------
INSERT INTO `im_friendship_request` VALUES ('56056acf-41af-4a0a-8112-e27a9d24593f', '100001', '100004', NULL, 0, NULL, NULL, 0, 1740982583800, NULL, NULL, 0, NULL);
INSERT INTO `im_friendship_request` VALUES ('b4ff63c5-e2ca-4f6d-8934-2075477de81b', '100002', '100001', NULL, 1, NULL, NULL, 1, 1740990586802, 1740994517484, NULL, 0, NULL);
INSERT INTO `im_friendship_request` VALUES ('dd2051bd-98fb-42fa-addc-765ffd6a8f38', '100001', '100005', NULL, 0, NULL, NULL, 0, 1740562677504, NULL, NULL, 0, NULL);
INSERT INTO `im_friendship_request` VALUES ('fc5e3683-46e2-43f2-9ab4-70378e9ec161', '100001', 'b4ff63c5-e2ca-4f6d-8934-2075477de81b', NULL, 0, NULL, NULL, 0, 1740993820110, NULL, NULL, 0, NULL);

-- ----------------------------
-- Table structure for im_group
-- ----------------------------
DROP TABLE IF EXISTS `im_group`;
CREATE TABLE `im_group`  (
  `group_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '群组ID',
  `owner_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '群主用户ID',
  `group_type` int NOT NULL COMMENT '群类型（1私有群，2公开群）',
  `group_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '群名称',
  `mute` tinyint NULL DEFAULT NULL COMMENT '是否全员禁言（0不禁言，1禁言）',
  `apply_join_type` int NOT NULL COMMENT '申请加群方式（0禁止申请，1需要审批，2允许自由加入）',
  `avatar` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '群头像',
  `max_member_count` int NULL DEFAULT NULL COMMENT '最大成员数',
  `introduction` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '群简介',
  `notification` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '群公告',
  `status` int NULL DEFAULT NULL COMMENT '群状态（0正常，1解散）',
  `sequence` bigint NULL DEFAULT NULL COMMENT '消息序列号',
  `create_time` bigint NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint NULL DEFAULT NULL COMMENT '更新时间',
  `extra` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '扩展字段',
  `version` int UNSIGNED NULL DEFAULT NULL COMMENT '版本信息',
  PRIMARY KEY (`group_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of im_group
-- ----------------------------

-- ----------------------------
-- Table structure for im_group_member
-- ----------------------------
DROP TABLE IF EXISTS `im_group_member`;
CREATE TABLE `im_group_member`  (
  `group_member_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '群成员ID',
  `group_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '群组ID',
  `member_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '成员用户ID',
  `role` int NOT NULL COMMENT '群成员角色（0普通成员，1管理员，2群主）',
  `speak_date` bigint NULL DEFAULT NULL COMMENT '最后发言时间',
  `mute` tinyint NOT NULL COMMENT '是否禁言（0不禁言，1禁言）',
  `alias` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '群昵称',
  `join_time` bigint NULL DEFAULT NULL COMMENT '加入时间',
  `leave_time` bigint NULL DEFAULT NULL COMMENT '离开时间',
  `join_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '加入类型',
  `extra` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '扩展字段',
  `del_flag` tinyint NULL DEFAULT 0 COMMENT '删除标识（0正常，1删除）',
  `create_time` bigint NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint NULL DEFAULT NULL COMMENT '更新时间',
  `version` int UNSIGNED NULL DEFAULT NULL COMMENT '版本信息',
  PRIMARY KEY (`group_member_id`) USING BTREE,
  INDEX `idx_group_id`(`group_id` ASC) USING BTREE,
  INDEX `idx_member_id`(`member_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of im_group_member
-- ----------------------------

-- ----------------------------
-- Table structure for im_group_message
-- ----------------------------
DROP TABLE IF EXISTS `im_group_message`;
CREATE TABLE `im_group_message`  (
  `message_id` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息ID',
  `group_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '群组ID',
  `from_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '发送者用户ID',
  `message_body` json NULL COMMENT '消息内容',
  `message_time` bigint NULL DEFAULT NULL COMMENT '发送时间',
  `message_content_type` int NULL DEFAULT NULL COMMENT '消息类型',
  `extra` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '扩展字段',
  `del_flag` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '删除标识',
  `sequence` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '消息序列',
  `message_random` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '随机标识',
  `create_time` bigint NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint NULL DEFAULT NULL COMMENT '更新时间',
  `version` int UNSIGNED NULL DEFAULT NULL COMMENT '版本信息',
  PRIMARY KEY (`message_id`) USING BTREE,
  INDEX `idx_group_msg_group`(`group_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of im_group_message
-- ----------------------------

-- ----------------------------
-- Table structure for im_group_message_status
-- ----------------------------
DROP TABLE IF EXISTS `im_group_message_status`;
CREATE TABLE `im_group_message_status`  (
  `group_id` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '群组ID',
  `message_id` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息ID',
  `to_id` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '接收者用户ID',
  `read_status` int NULL DEFAULT NULL COMMENT '阅读状态（1已读）',
  `create_time` bigint NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint NULL DEFAULT NULL COMMENT '更新时间',
  `version` int UNSIGNED NULL DEFAULT NULL COMMENT '版本信息',
  PRIMARY KEY (`group_id`, `message_id`, `to_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of im_group_message_status
-- ----------------------------

-- ----------------------------
-- Table structure for im_private_message
-- ----------------------------
DROP TABLE IF EXISTS `im_private_message`;
CREATE TABLE `im_private_message`  (
  `message_id` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息ID',
  `from_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '发送者用户ID',
  `to_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '接收者用户ID',
  `message_body` json NULL COMMENT '消息内容',
  `message_time` bigint NULL DEFAULT NULL COMMENT '发送时间',
  `message_content_type` int NULL DEFAULT NULL COMMENT '消息类型',
  `read_status` int NULL DEFAULT NULL COMMENT '阅读状态（1已读）',
  `extra` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '扩展字段',
  `del_flag` int NULL DEFAULT NULL COMMENT '删除标识',
  `sequence` bigint NULL DEFAULT NULL COMMENT '消息序列',
  `message_random` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '随机标识',
  `create_time` bigint NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint NULL DEFAULT NULL COMMENT '更新时间',
  `version` int UNSIGNED NULL DEFAULT NULL COMMENT '版本信息',
  PRIMARY KEY (`message_id`) USING BTREE,
  INDEX `idx_private_from`(`from_id` ASC) USING BTREE,
  INDEX `idx_private_to`(`to_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of im_private_message
-- ----------------------------
INSERT INTO `im_private_message` VALUES ('1906969524805640192', '100002', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1743491939156, 1, 0, NULL, NULL, NULL, NULL, 1743491945792, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1906970731435597824', '100002', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1743492226834, 1, 0, NULL, NULL, NULL, NULL, 1743492226834, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1906974846098153472', '100001', '100002', '{\"message\": \"😂\"}', 1743493207846, 1, 0, NULL, NULL, NULL, NULL, 1743493207848, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1906975077405630464', '100002', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1743493262994, 1, 0, NULL, NULL, NULL, NULL, 1743493262996, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1906975177917931520', '100003', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1743493286958, 1, 0, NULL, NULL, NULL, NULL, 1743493286959, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1906975223707148288', '100001', '100003', '{\"message\": \"😂\"}', 1743493297875, 1, 0, NULL, NULL, NULL, NULL, 1743493297878, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1906975943583932416', '100003', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1743493469507, 1, 0, NULL, NULL, NULL, NULL, 1743493469508, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1906976412007997440', '100004', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1743493581188, 1, 0, NULL, NULL, NULL, NULL, 1743493581188, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1906977355302772736', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1743493806087, 1, 0, NULL, NULL, NULL, NULL, 1743493806088, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1907073612960624640', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1743516755705, 1, 0, NULL, NULL, NULL, NULL, 1743516755722, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1907073954632822784', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1743516837162, 1, 0, NULL, NULL, NULL, NULL, 1743516837166, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1911057040533995520', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1744466478845, 1, 0, NULL, NULL, NULL, NULL, 1744466482715, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1911057091893248000', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1744466491088, 1, 0, NULL, NULL, NULL, NULL, 1744466491089, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1911057136088629248', '100001', '100005', '{\"message\": \"😅\"}', 1744466501625, 1, 0, NULL, NULL, NULL, NULL, 1744466501627, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1911057185870823424', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1744466513494, 1, 0, NULL, NULL, NULL, NULL, 1744466513494, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1911057257618587648', '100001', '100005', '{\"message\": \"笑死了\"}', 1744466530600, 1, 0, NULL, NULL, NULL, NULL, 1744466530601, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1911069592622055424', '100005', '100001', '{\"message\": \"不必理会\"}', 1744469471494, 1, 0, NULL, NULL, NULL, NULL, 1744469471499, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1911074656841482240', '100005', '100001', '{\"message\": \"不必理会\"}', 1744470678898, 1, 0, NULL, NULL, NULL, NULL, 1744470678900, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1911081473881427968', '100005', '100001', '{\"message\": \"不必理会\"}', 1744472304211, 1, 0, NULL, NULL, NULL, NULL, 1744472304285, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1911083007830675456', '100005', '100001', '{\"message\": \"不必理会\"}', 1744472669929, 1, 0, NULL, NULL, NULL, NULL, 1744472669931, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1911084153169268736', '100005', '100001', '{\"message\": \"不必理会\"}', 1744472942999, 1, 0, NULL, NULL, NULL, NULL, 1744472943000, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1911084426826633216', '100005', '100001', '{\"message\": \"不必理会\"}', 1744473008244, 1, 0, NULL, NULL, NULL, NULL, 1744473008244, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1911084868663005184', '100005', '100001', '{\"message\": \"不必理会\"}', 1744473113586, 1, 0, NULL, NULL, NULL, NULL, 1744473113587, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916026802972241920', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1745651363240, 1, 0, NULL, NULL, NULL, NULL, 1745651369541, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916027164550606848', '100005', '100001', '{\"message\": \"疯狂星期四  v我50!\"}', 1745651448749, 1, 0, NULL, NULL, NULL, NULL, 1745651448756, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916027498996019200', '100004', '100001', '{\"message\": \"疯狂星期四  v我50!\"}', 1745651528487, 1, 0, NULL, NULL, NULL, NULL, 1745651528491, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916035714513481728', '100004', '100001', '{\"message\": \"疯狂星期四  v我50!\"}', 1745653487222, 1, 0, NULL, NULL, NULL, NULL, 1745653493986, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916121845921820672', '100004', '100001', '{\"message\": \"疯狂星期四  v我50! ok?\"}', 1745674022552, 1, 0, NULL, NULL, NULL, NULL, 1745674030083, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916124931570610176', '100004', '100001', '{\"message\": \"疯狂星期四  v我50! ok?\"}', 1745674758223, 1, 0, NULL, NULL, NULL, NULL, 1745674763682, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916130754258612224', '100004', '100001', '{\"message\": \"疯狂星期四  v我50! ok?\"}', 1745676146460, 1, 0, NULL, NULL, NULL, NULL, 1745676152674, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916132131252809728', '100004', '100001', '{\"message\": \"疯狂星期四  v我50! ok?\"}', 1745676474761, 1, 0, NULL, NULL, NULL, NULL, 1745676474940, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916133569756475392', '100004', '100001', '{\"message\": \"疯狂星期四  v我50! ok?\"}', 1745676817727, 1, 0, NULL, NULL, NULL, NULL, 1745676817949, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916136889137049600', '100004', '100001', '{\"message\": \"疯狂星期四  v我50! ok?\"}', 1745677609129, 1, 0, NULL, NULL, NULL, NULL, 1745677663100, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916139092669181952', '100004', '100001', '{\"message\": \"疯狂星期四  v我50! ok?\"}', 1745678134492, 1, 0, NULL, NULL, NULL, NULL, 1745678134673, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916139753284644864', '100004', '100001', '{\"message\": \"疯狂星期四  v我50! ok?\"}', 1745678291995, 1, 0, NULL, NULL, NULL, NULL, 1745678292169, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916141885819793408', '100004', '100001', '{\"message\": \"疯狂星期四  v我50! ok?\"}', 1745678800431, 1, 0, NULL, NULL, NULL, NULL, 1745678800826, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916142404793610240', '100004', '100001', '{\"message\": \"疯狂星期四  v我50! ok?\"}', 1745678924164, 1, 0, NULL, NULL, NULL, NULL, 1745678924343, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916143156006039552', '100004', '100001', '{\"message\": \"疯狂星期四  v我50! ok?\"}', 1745679103267, 1, 0, NULL, NULL, NULL, NULL, 1745679103496, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916143341373235200', '100004', '100001', '{\"message\": \"疯狂星期四  v我50! ok?\"}', 1745679147465, 1, 0, NULL, NULL, NULL, NULL, 1745679147603, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916143678809300992', '100004', '100001', '{\"message\": \"疯狂星期四  v我50! ok?\"}', 1745679227916, 1, 0, NULL, NULL, NULL, NULL, 1745679228066, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916170677900607488', '100004', '100001', '{\"message\": \"疯狂星期四  v我50! ok?\"}', 1745685665001, 1, 0, NULL, NULL, NULL, NULL, 1745685665324, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916171080427929600', '100004', '100001', '{\"message\": \"疯狂星期四  v我50! ok?\"}', 1745685760971, 1, 0, NULL, NULL, NULL, NULL, 1745685761099, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916434458551533568', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1745748555208, 1, 0, NULL, NULL, NULL, NULL, 1745748555492, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916438276534190080', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1745749465484, 1, 0, NULL, NULL, NULL, NULL, 1745749465665, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916440324373757952', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1745749953727, 1, 0, NULL, NULL, NULL, NULL, 1745749953738, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916440504007409664', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1745749996555, 1, 0, NULL, NULL, NULL, NULL, 1745750421391, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1916454843229683712', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1745753415292, 1, 0, NULL, NULL, NULL, NULL, 1745753963638, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1919756607500701696', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1746540617242, 1, 0, NULL, NULL, NULL, NULL, 1746540617764, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1919757078365851648', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1746540729501, 1, 0, NULL, NULL, NULL, NULL, 1746540729513, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1919758756896841728', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1746541129697, 1, 0, NULL, NULL, NULL, NULL, 1746541138027, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1919758827587641344', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1746541146548, 1, 0, NULL, NULL, NULL, NULL, 1746541151101, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1919943960575975424', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1746585285696, 1, 0, NULL, NULL, NULL, NULL, 1746585294304, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1919944627210276864', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1746585444633, 1, 0, NULL, NULL, NULL, NULL, 1746585444756, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1919999416807821312', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1746598507493, 1, 0, NULL, NULL, NULL, NULL, 1746598507702, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1920125427310436352', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1746628550738, 1, 0, NULL, NULL, NULL, NULL, 1746628554289, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1920479237438545920', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1746712905647, 1, 0, NULL, NULL, NULL, NULL, 1746712905743, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1920479271773118464', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1746712913827, 1, 0, NULL, NULL, NULL, NULL, 1746712913838, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1920663185636544512', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1746756762311, 1, 0, NULL, NULL, NULL, NULL, 1746756762352, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1920699028061376512', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1746765307809, 1, 0, NULL, NULL, NULL, NULL, 1746765307818, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1920846350795710464', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1746800432289, 1, 0, NULL, NULL, NULL, NULL, 1746800432329, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1921174385059270656', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1746878641752, 1, 0, NULL, NULL, NULL, NULL, 1746878647416, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1924007481823584256', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1747554104657, 1, 0, NULL, NULL, NULL, NULL, 1747554104837, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1924008117310849024', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1747554256168, 1, 0, NULL, NULL, NULL, NULL, 1747554256308, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1924022127829712896', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1747557596534, 1, 0, NULL, NULL, NULL, NULL, 1747557596545, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1924022153188474880', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1747557602580, 1, 0, NULL, NULL, NULL, NULL, 1747557602591, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1924022614276710400', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1747557712514, 1, 0, NULL, NULL, NULL, NULL, 1747557712653, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1924022685156253696', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1747557729411, 1, 0, NULL, NULL, NULL, NULL, 1747557729414, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1925765120265940992', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1747973158315, 1, 0, NULL, NULL, NULL, NULL, 1747973158745, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1925767340692729856', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1747973687705, 1, 0, NULL, NULL, NULL, NULL, 1747973687715, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('1925769127826944000', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1747974113791, 1, 0, NULL, NULL, NULL, NULL, 1747974113796, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('526426298994659390', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1748015455428, 1, 0, NULL, NULL, NULL, NULL, 1748015461936, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('527501479192301580', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1748271753165, 1, 0, NULL, NULL, NULL, NULL, 1748271753554, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('527501706271920178', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1748271807285, 1, 0, NULL, NULL, NULL, NULL, 1748271807290, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('527501733098688610', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1748271813681, 1, 0, NULL, NULL, NULL, NULL, 1748271813688, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('527501759686381633', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1748271820022, 1, 0, NULL, NULL, NULL, NULL, 1748271820032, NULL, NULL);
INSERT INTO `im_private_message` VALUES ('527512357983752279', '100005', '100001', '{\"message\": \"疯狂星期四  v我50\"}', 1748274346889, 1, 0, NULL, NULL, NULL, NULL, 1748274347119, NULL, NULL);

-- ----------------------------
-- Table structure for im_user
-- ----------------------------
DROP TABLE IF EXISTS `im_user`;
CREATE TABLE `im_user`  (
  `user_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `mobile` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` bigint NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint NULL DEFAULT NULL COMMENT '更新时间',
  `version` int UNSIGNED NULL DEFAULT NULL COMMENT '版本信息',
  `del_flag` int NULL DEFAULT NULL COMMENT '删除标识（0正常，1删除）',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of im_user
-- ----------------------------
INSERT INTO `im_user` VALUES ('100001', '张三', '$2a$10$XAZEo5EIaPlGiX6WS9URE.g8SrB0FwDsK7iLeB9y4B4FJ6KnwsuoC', '13545878589', NULL, NULL, NULL, 0);
INSERT INTO `im_user` VALUES ('100002', '李四', '$2a$10$XAZEo5EIaPlGiX6WS9URE.g8SrB0FwDsK7iLeB9y4B4FJ6KnwsuoC', '1000000000', NULL, NULL, NULL, 0);
INSERT INTO `im_user` VALUES ('100003', '吴某凡', '$2a$10$XAZEo5EIaPlGiX6WS9URE.g8SrB0FwDsK7iLeB9y4B4FJ6KnwsuoC', '1000000000', NULL, NULL, NULL, 0);
INSERT INTO `im_user` VALUES ('100004', '蔡某锟', '$2a$10$XAZEo5EIaPlGiX6WS9URE.g8SrB0FwDsK7iLeB9y4B4FJ6KnwsuoC', '1000000000', NULL, NULL, NULL, 0);
INSERT INTO `im_user` VALUES ('100005', '小红帽', '$2a$10$XAZEo5EIaPlGiX6WS9URE.g8SrB0FwDsK7iLeB9y4B4FJ6KnwsuoC', '1000000000', NULL, NULL, NULL, 0);
INSERT INTO `im_user` VALUES ('100006', 'amy', '$2a$10$XAZEo5EIaPlGiX6WS9URE.g8SrB0FwDsK7iLeB9y4B4FJ6KnwsuoC', '1000000000', NULL, NULL, NULL, 0);

-- ----------------------------
-- Table structure for im_user_data
-- ----------------------------
DROP TABLE IF EXISTS `im_user_data`;
CREATE TABLE `im_user_data`  (
  `user_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像',
  `gender` int NULL DEFAULT NULL COMMENT '性别',
  `birthday` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '生日',
  `location` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '地址',
  `self_signature` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '个性签名',
  `friend_allow_type` int NOT NULL DEFAULT 1 COMMENT '加好友验证类型（1无需验证，2需要验证）',
  `forbidden_flag` int NOT NULL DEFAULT 0 COMMENT '禁用标识（1禁用）',
  `disable_add_friend` int NOT NULL DEFAULT 0 COMMENT '管理员禁止添加好友：0未禁用，1已禁用',
  `silent_flag` int NOT NULL DEFAULT 0 COMMENT '禁言标识（1禁言）',
  `user_type` int NOT NULL DEFAULT 1 COMMENT '用户类型（1普通用户，2客服，3机器人）',
  `del_flag` tinyint NOT NULL DEFAULT 0 COMMENT '删除标识（0正常，1删除）',
  `extra` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '扩展字段',
  `create_time` bigint NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` bigint NULL DEFAULT NULL COMMENT '更新时间',
  `version` int UNSIGNED NULL DEFAULT NULL COMMENT '版本信息',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of im_user_data
-- ----------------------------
INSERT INTO `im_user_data` VALUES ('100001', '张三', 'https://img0.baidu.com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEG?w=500&h=500', 1, '1993-05-22 10:26:33', '福田区深南大道384号', '制片人', 28, 58, 204, 199, 322, 0, 'bxWZFhNdrO', 970, 616, NULL);
INSERT INTO `im_user_data` VALUES ('100002', '李四', 'https://img0.baidu.com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEG?w=500&h=500', 1, '1993-05-22 10:26:33', '福田区深南大道384号', '制片人', 28, 58, 204, 199, 322, 0, 'bxWZFhNdrO', 970, 616, NULL);
INSERT INTO `im_user_data` VALUES ('100003', '吴某凡', 'https://img0.baidu.com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEG?w=500&h=500', 1, '1993-05-22 10:26:33', '福田区深南大道384号', '制片人', 28, 58, 204, 199, 322, 0, 'bxWZFhNdrO', 970, 616, NULL);
INSERT INTO `im_user_data` VALUES ('100004', '蔡某锟', 'https://img0.baidu.com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEG?w=500&h=500', 1, '1993-05-22 10:26:33', '福田区深南大道384号', '制片人', 28, 58, 204, 199, 322, 0, 'bxWZFhNdrO', 970, 616, NULL);
INSERT INTO `im_user_data` VALUES ('100005', '小红帽', 'https://img0.baidu.com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEG?w=500&h=500', 1, '1993-05-22 10:26:33', '福田区深南大道384号', '制片人', 28, 58, 204, 199, 322, 0, 'bxWZFhNdrO', 970, 616, NULL);
INSERT INTO `im_user_data` VALUES ('100006', 'amy', 'https://img0.baidu.com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEG?w=500&h=500', 1, '1993-05-22 10:26:33', '福田区深南大道384号', '制片人', 28, 58, 204, 199, 322, 0, 'bxWZFhNdrO', 970, 616, NULL);
INSERT INTO `im_user_data` VALUES ('100007', 'Tong Wai Lam', 'https://img0Bbaidu}com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1993-05-22 10:26:33', '福田区深南大道384号', '制片人', 28, 58, 204, 199, 322, 0, 'bxWZFhNdrO', 970, 616, NULL);
INSERT INTO `im_user_data` VALUES ('100008', 'Choi Wai Yee', 'https://img0kbaiduDcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1995-02-27 14:21:29', '海珠区江南西路426号', '销售经理', 140, 185, 129, 929, 507, 1, '0bpLpNAdt5', 495, 123, NULL);
INSERT INTO `im_user_data` VALUES ('100009', 'Han Wai San', 'https://img0zbaidu5com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1996-09-01 10:10:20', '京华商圈华夏街540号', '客户服务经理', 617, 438, 213, 899, 99, 1, 'D3O6KzEPHE', 551, 670, NULL);
INSERT INTO `im_user_data` VALUES ('100010', 'Au Lik Sun', 'https://img0dbaiduycom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1999-11-01 05:29:25', '天河区大信商圈大信南路415号', '私人教练', 213, 403, 116, 900, 738, 1, 'OQVgYy4xhr', 159, 537, NULL);
INSERT INTO `im_user_data` VALUES ('100011', '沈晓明', 'https://img0pbaiduCcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1991-02-06 21:49:24', '成华区玉双路6号665号', '兽医', 334, 56, 574, 758, 87, 1, 'wY2pwOayIm', 120, 238, NULL);
INSERT INTO `im_user_data` VALUES ('100012', 'Loui Wing Kuen', 'https://img0\'baidu3com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1993-03-30 21:58:59', '黄浦区淮海中路868号', '园艺家', 10, 971, 841, 343, 728, 0, 'q0NUsirCUO', 699, 733, NULL);
INSERT INTO `im_user_data` VALUES ('100013', 'Che Hiu Tung', 'https://img0zbaidu-com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1999-01-02 07:02:22', '罗湖区田贝一路492号', '制片人', 768, 72, 125, 532, 935, 0, 'g9YhvWbuI1', 640, 51, NULL);
INSERT INTO `im_user_data` VALUES ('100014', 'Fung Suk Yee', 'https://img0@baiduPcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1993-06-14 01:39:12', '房山区岳琉路422号', '精算师', 927, 418, 73, 726, 911, 1, 'vQxW0oUfzu', 47, 699, NULL);
INSERT INTO `im_user_data` VALUES ('100015', 'Ying Wai Man', 'https://img0kbaidu~com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-12-26 04:27:59', '白云区小坪东路363号', '美容师', 659, 202, 295, 426, 750, 0, 'wFV6iOupLG', 370, 53, NULL);
INSERT INTO `im_user_data` VALUES ('100016', '曾詩涵', 'https://img0.baiduIcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1998-04-22 20:38:50', '紫马岭商圈中山五路575号', '客户服务经理', 570, 191, 896, 986, 271, 0, '5XP6goph7U', 477, 491, NULL);
INSERT INTO `im_user_data` VALUES ('100017', 'Lok Ka Man', 'https://img0Hbaiduvcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1992-06-17 10:17:59', '福田区景田东一街378号', '建筑师', 410, 286, 285, 939, 86, 0, 'S7K4gPQYEx', 357, 760, NULL);
INSERT INTO `im_user_data` VALUES ('100018', '方詩涵', 'https://img0obaiduFcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1990-08-19 04:11:42', '锦江区红星路三段711号', '牙医', 868, 999, 945, 506, 830, 1, 'yS4fS1bk4y', 95, 778, NULL);
INSERT INTO `im_user_data` VALUES ('100019', '张安琪', 'https://img0Ubaidupcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1993-08-02 14:56:06', '东城区东单王府井东街875号', '舞蹈演员', 549, 754, 131, 376, 976, 1, 'nmKvwvTyRN', 74, 630, NULL);
INSERT INTO `im_user_data` VALUES ('100020', 'Mo Ho Yin', 'https://img0xbaiduTcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1998-06-03 05:14:10', '锦江区人民南路四段260号', '信息安全分析师', 373, 670, 788, 364, 297, 1, 'FPnF6uc4KU', 880, 209, NULL);
INSERT INTO `im_user_data` VALUES ('100021', '唐安琪', 'https://img0&baidu4com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1993-03-13 18:13:58', '浦东新区橄榄路377号', '私人教练', 995, 749, 719, 247, 233, 1, 'arq3go9DTb', 853, 614, NULL);
INSERT INTO `im_user_data` VALUES ('100022', 'Cheng Wing Suen', 'https://img0 baiduQcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1994-10-11 16:30:02', '朝阳区三里屯路272号', '教授', 771, 443, 602, 854, 198, 1, 'WDOWQyJJGn', 227, 207, NULL);
INSERT INTO `im_user_data` VALUES ('100023', '方宇宁', 'https://img0Nbaidukcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1990-08-28 12:58:25', '闵行区宾川路22号', '纹身艺术家', 971, 664, 139, 468, 138, 1, 'nSdwVcwqAc', 440, 528, NULL);
INSERT INTO `im_user_data` VALUES ('100024', '戴子异', 'https://img0&baidu|com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1992-10-09 15:53:40', '珊瑚路697号', '美容师', 961, 861, 200, 19, 70, 0, 'fPwafh823h', 653, 720, NULL);
INSERT INTO `im_user_data` VALUES ('100025', 'Lee Ho Yin', 'https://img0Obaidupcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1993-03-09 20:29:46', '西城区复兴门内大街485号', '工程师', 660, 745, 88, 995, 209, 1, 'vD5G9GNdVY', 804, 922, NULL);
INSERT INTO `im_user_data` VALUES ('100026', 'Kong Sze Kwan', 'https://img0NbaiduZcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1999-01-06 01:11:34', '西城区复兴门内大街342号', '审计师', 173, 750, 539, 837, 372, 0, 'QOfGXIYoUW', 64, 963, NULL);
INSERT INTO `im_user_data` VALUES ('100027', 'Mo Kar Yan', 'https://img0rbaiduXcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1991-12-03 14:14:53', '罗湖区蔡屋围深南东路970号', '首席运营官', 47, 796, 243, 478, 319, 0, 'cMiPhZooWj', 518, 412, NULL);
INSERT INTO `im_user_data` VALUES ('100028', 'Sit Tsz Hin', 'https://img0Nbaidubcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-06-22 14:44:00', '朝阳区三里屯路911号', '歌手', 88, 630, 720, 580, 420, 1, 'XdUG12ypIj', 354, 639, NULL);
INSERT INTO `im_user_data` VALUES ('100029', '程晓明', 'https://img0Qbaidu.com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1999-06-22 09:03:06', '龙岗区学园一巷512号', '舞蹈演员', 242, 924, 896, 157, 718, 0, '11zxdjnh6Y', 870, 362, NULL);
INSERT INTO `im_user_data` VALUES ('100030', 'Yip Fat', 'https://img0lbaidugcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1998-07-03 02:57:17', '天河区大信商圈大信南路566号', '工程师', 744, 457, 584, 414, 214, 0, 'GfQgl6b9Pa', 635, 454, NULL);
INSERT INTO `im_user_data` VALUES ('100031', '谢云熙', 'https://img0@baidu9com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1991-04-24 21:01:51', '成华区玉双路6号348号', '护士', 517, 592, 78, 538, 38, 1, 'VZ6rBYutEp', 252, 330, NULL);
INSERT INTO `im_user_data` VALUES ('100032', '萧嘉伦', 'https://img0/baidubcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-02-19 20:01:27', '福田区深南大道647号', '药剂师', 807, 3, 303, 949, 702, 1, 'JhtX8aKCUG', 746, 235, NULL);
INSERT INTO `im_user_data` VALUES ('100033', '于震南', 'https://img0\'baidu;com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1994-12-22 08:29:17', '闵行区宾川路403号', '专案经理', 125, 579, 879, 692, 760, 1, 'VeiNYATOra', 875, 295, NULL);
INSERT INTO `im_user_data` VALUES ('100034', 'Lam Ka Fai', 'https://img0|baidu+com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1996-09-06 23:49:54', '福田区深南大道748号', '医生', 520, 20, 355, 257, 890, 0, 'r291j0uRUi', 35, 124, NULL);
INSERT INTO `im_user_data` VALUES ('100035', '崔璐', 'https://img0Xbaidu|com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1996-10-01 13:15:14', '徐汇区虹桥路89号', '办公室文员', 988, 886, 934, 268, 898, 1, '6k9jpYDqtd', 667, 949, NULL);
INSERT INTO `im_user_data` VALUES ('100036', '尹震南', 'https://img0Pbaiduscom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1993-03-23 04:25:08', '龙岗区布吉镇西环路374号', '财务分析师', 255, 636, 742, 919, 450, 0, 'm0ze6Fx3md', 843, 47, NULL);
INSERT INTO `im_user_data` VALUES ('100037', '薛杰宏', 'https://img0Kbaidu&com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1991-04-24 15:02:12', '东城区东单王府井东街665号', '生物化学家', 543, 804, 930, 763, 428, 0, 'spSfGeTr7Q', 447, 975, NULL);
INSERT INTO `im_user_data` VALUES ('100038', '余璐', 'https://img0>baidu/com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1990-04-04 09:58:20', '成华区二仙桥东三路731号', '理发师', 130, 950, 425, 520, 403, 0, 'HxXrhL7H20', 434, 503, NULL);
INSERT INTO `im_user_data` VALUES ('100039', 'Fu Fu Shing', 'https://img0Kbaidu;com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1990-08-25 16:12:46', '西城区复兴门内大街962号', '牙医', 278, 831, 252, 363, 810, 0, 'g1IOt9xZuL', 135, 949, NULL);
INSERT INTO `im_user_data` VALUES ('100040', 'Tang Chi Yuen', 'https://img0&baidu|com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1992-02-20 09:53:43', '锦江区红星路三段156号', '网页开发人员', 117, 980, 797, 346, 998, 1, 'GHV7la8o9z', 94, 669, NULL);
INSERT INTO `im_user_data` VALUES ('100041', '黎云熙', 'https://img0QbaiduAcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1997-01-29 18:37:16', '环区南街二巷784号', '水疗经理', 972, 666, 484, 52, 410, 1, 'O1wHV6T4yQ', 722, 484, NULL);
INSERT INTO `im_user_data` VALUES ('100042', 'Chan Wai San', 'https://img0-baiduScom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1996-11-29 14:37:04', '闵行区宾川路425号', '首席运营官', 710, 730, 556, 576, 327, 0, 'QeItdN7D0A', 17, 128, NULL);
INSERT INTO `im_user_data` VALUES ('100043', 'Sit Wing Fat', 'https://img0$baidu{com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1998-02-26 16:01:37', '天河区天河路804号', '牙医', 868, 356, 219, 209, 895, 0, 'DFnt8z97Vg', 489, 514, NULL);
INSERT INTO `im_user_data` VALUES ('100044', 'Mo Ming', 'https://img0bbaiduDcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1998-07-16 07:25:19', '京华商圈华夏街287号', '牙齿矫正医生', 679, 17, 888, 549, 828, 0, 'RWi2dTUvJr', 938, 414, NULL);
INSERT INTO `im_user_data` VALUES ('100045', 'Ma Wai Lam', 'https://img0Ybaidu_com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1995-01-27 11:57:36', '白云区小坪东路143号', '数据库经理', 798, 845, 233, 725, 920, 1, 'mwjG5ItzvI', 957, 727, NULL);
INSERT INTO `im_user_data` VALUES ('100046', '邵秀英', 'https://img0mbaidutcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1998-03-23 14:59:21', '锦江区人民南路四段275号', '饲养员', 957, 965, 246, 244, 853, 0, 'EjcddZXX2b', 39, 235, NULL);
INSERT INTO `im_user_data` VALUES ('100047', 'Kwok Wing Suen', 'https://img0FbaiduTcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1998-09-13 13:46:21', '浦东新区橄榄路493号', '社交媒体协调员', 31, 295, 965, 742, 786, 1, 'rbQpUSuikv', 800, 892, NULL);
INSERT INTO `im_user_data` VALUES ('100048', 'Mui Sum Wing', 'https://img0<baiduLcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1999-07-13 12:09:14', '龙岗区布吉镇西环路188号', '建筑师', 198, 282, 879, 866, 406, 0, 'sDD7v2dAFF', 260, 230, NULL);
INSERT INTO `im_user_data` VALUES ('100049', '罗秀英', 'https://img0#baiduEcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1997-03-16 08:11:31', '天河区大信商圈大信南路523号', '精算师', 432, 920, 960, 988, 727, 1, 'uUKcjEk4ID', 276, 350, NULL);
INSERT INTO `im_user_data` VALUES ('100050', 'Leung Chi Ming', 'https://img0sbaidulcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1998-01-17 05:26:06', '天河区大信商圈大信南路834号', '运营经理', 776, 520, 144, 357, 680, 0, 'ODM4mdbOap', 278, 335, NULL);
INSERT INTO `im_user_data` VALUES ('100051', '许璐', 'https://img0Lbaiduecom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1995-07-31 05:17:23', '海珠区江南西路291号', '网页开发人员', 183, 495, 967, 334, 695, 0, 'd44R9HbKU3', 552, 19, NULL);
INSERT INTO `im_user_data` VALUES ('100052', 'Lok Yun Fat', 'https://img0qbaidu\"com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1991-10-03 17:29:45', '徐汇区虹桥路600号', '水疗经理', 922, 543, 330, 214, 928, 0, '9Io3i8y3NM', 289, 68, NULL);
INSERT INTO `im_user_data` VALUES ('100053', 'Lo Ka Ling', 'https://img07baidutcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1990-11-22 00:55:47', '黄浦区淮海中路28号', '审计师', 321, 427, 16, 731, 423, 1, 'DhvPTvF4qy', 255, 811, NULL);
INSERT INTO `im_user_data` VALUES ('100054', '金岚', 'https://img08baiduhcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1998-10-25 19:01:27', '罗湖区蔡屋围深南东路303号', '技术支援人员', 639, 890, 1, 213, 685, 1, 'Bxzk0TCKuP', 771, 145, NULL);
INSERT INTO `im_user_data` VALUES ('100055', 'Heung Ho Yin', 'https://img0pbaiduJcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1998-04-02 19:07:04', '房山区岳琉路295号', '商务记者', 880, 670, 836, 310, 139, 0, 'KbR9uUsXuU', 801, 728, NULL);
INSERT INTO `im_user_data` VALUES ('100056', '龙睿', 'https://img0bbaidulcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-11-17 14:38:54', '天河区天河路926号', '纹身艺术家', 863, 757, 234, 259, 89, 1, 'gxvkes7JbF', 379, 641, NULL);
INSERT INTO `im_user_data` VALUES ('100057', 'Lok Ching Wan', 'https://img0Wbaidu|com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1990-08-17 05:56:37', '乐丰六路736号', '客户服务经理', 992, 690, 166, 943, 476, 0, 'kzcwJBeHjD', 611, 867, NULL);
INSERT INTO `im_user_data` VALUES ('100058', '苏睿', 'https://img0PbaiduFcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1998-08-10 10:28:55', '成华区双庆路315号', 'UX / UI设计员', 53, 169, 103, 668, 597, 1, 'Hzzgjc5TA4', 807, 615, NULL);
INSERT INTO `im_user_data` VALUES ('100059', 'Tsui Chi Ming', 'https://img0]baidu[com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1995-03-31 13:47:33', '天河区大信商圈大信南路777号', '农夫', 958, 676, 278, 247, 236, 1, 'Nat0oZNp3t', 801, 193, NULL);
INSERT INTO `im_user_data` VALUES ('100060', '陆睿', 'https://img0^baiduhcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1995-12-13 08:32:11', '福田区深南大道881号', '专案经理', 134, 114, 39, 657, 81, 0, 'bhkCVwn0O2', 197, 325, NULL);
INSERT INTO `im_user_data` VALUES ('100061', 'Sit On Kay', 'https://img0nbaidu~com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-10-25 07:45:50', '珊瑚路329号', '运营经理', 69, 474, 717, 243, 950, 0, 'WoRCYRZXM7', 246, 956, NULL);
INSERT INTO `im_user_data` VALUES ('100062', 'Hsuan Ka Ling', 'https://img0Wbaiduwcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1998-11-11 17:57:14', '锦江区红星路三段943号', '老师', 922, 304, 22, 183, 763, 0, 'pU0JghCydx', 383, 136, NULL);
INSERT INTO `im_user_data` VALUES ('100063', 'Lam Ling Ling', 'https://img0obaiduNcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1991-08-14 08:32:12', '紫马岭商圈中山五路764号', '歌手', 166, 310, 114, 895, 163, 1, 'S0ZsMd2reO', 636, 109, NULL);
INSERT INTO `im_user_data` VALUES ('100064', 'Kwong Sze Yu', 'https://img0hbaidu^com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1998-07-06 05:15:20', '成华区双庆路46号', '食品科学家', 41, 487, 961, 435, 424, 0, 'EFIGBSWBaL', 238, 325, NULL);
INSERT INTO `im_user_data` VALUES ('100065', 'Loui On Na', 'https://img08baidujcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1991-09-01 20:03:34', '罗湖区蔡屋围深南东路164号', '精算师', 137, 981, 548, 580, 168, 0, 'zOirY3sWIp', 590, 257, NULL);
INSERT INTO `im_user_data` VALUES ('100066', 'Chu Ming Sze', 'https://img0[baidu)com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1994-06-30 11:53:43', '罗湖区田贝一路42号', 'UX / UI设计员', 198, 796, 736, 489, 312, 1, 'Go9aK4DXos', 957, 710, NULL);
INSERT INTO `im_user_data` VALUES ('100067', '严睿', 'https://img0wbaiduncom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1997-12-17 01:41:33', '成华区二仙桥东三路486号', '多媒体动画师', 588, 148, 98, 102, 501, 0, 'hlRMM2pou6', 724, 302, NULL);
INSERT INTO `im_user_data` VALUES ('100068', 'Tang Wai Man', 'https://img0Kbaidu;com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1993-10-26 23:07:28', '黄浦区淮海中路610号', '歌手', 219, 764, 420, 554, 393, 0, 'gFgrlWGkL6', 443, 111, NULL);
INSERT INTO `im_user_data` VALUES ('100069', '陶子韬', 'https://img0qbaidu!com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1999-05-05 22:49:12', '罗湖区田贝一路810号', '导师', 171, 991, 893, 523, 698, 0, 'FrGfxqFbUo', 873, 976, NULL);
INSERT INTO `im_user_data` VALUES ('100070', 'Mo Wai Yee', 'https://img0vbaidujcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1993-02-24 03:51:30', '西城区复兴门内大街815号', '理发师', 519, 66, 572, 313, 755, 0, 'WcxpLOEh4B', 690, 816, NULL);
INSERT INTO `im_user_data` VALUES ('100071', '曹璐', 'https://img0@baidu.com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1996-06-14 09:34:08', '罗湖区蔡屋围深南东路70号', '农夫', 298, 222, 206, 334, 476, 0, 'YY9AzYam0L', 608, 710, NULL);
INSERT INTO `im_user_data` VALUES ('100072', '邹杰宏', 'https://img0]baidumcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1991-05-15 08:44:12', '京华商圈华夏街138号', '理发师', 194, 494, 322, 484, 84, 0, '9Jy8f91SSh', 24, 889, NULL);
INSERT INTO `im_user_data` VALUES ('100073', 'Ma Kwok Wing', 'https://img0=baiduccom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1995-09-01 12:29:10', '成华区双庆路838号', '生物化学家', 907, 325, 379, 423, 468, 0, 'vNU3hKl6Xp', 182, 313, NULL);
INSERT INTO `im_user_data` VALUES ('100074', '邓致远', 'https://img0bbaidu!com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1995-03-18 05:33:05', '浦东新区健祥路456号', '运营经理', 461, 252, 310, 903, 400, 0, 'rytlaWszI6', 230, 814, NULL);
INSERT INTO `im_user_data` VALUES ('100075', 'Yuen Ling Ling', 'https://img0Wbaiduqcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1998-06-01 18:31:02', '海淀区清河中街68号713号', '化妆师', 733, 906, 347, 499, 577, 0, 'yGYRw9zFF9', 213, 571, NULL);
INSERT INTO `im_user_data` VALUES ('100076', '罗詩涵', 'https://img05baidu*com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1994-02-25 09:32:10', '徐汇区虹桥路456号', '运营经理', 468, 220, 317, 498, 163, 1, 'kcbpwKFaEv', 823, 127, NULL);
INSERT INTO `im_user_data` VALUES ('100077', 'Loui Sum Wing', 'https://img0@baiduMcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1998-11-23 20:03:47', '京华商圈华夏街46号', '客户服务经理', 739, 653, 592, 973, 639, 0, 'TvkmvAfbQn', 421, 809, NULL);
INSERT INTO `im_user_data` VALUES ('100078', '高秀英', 'https://img0Xbaiduwcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1996-01-28 04:58:36', '海珠区江南西路519号', '网页开发人员', 366, 517, 648, 816, 243, 1, '0NSTUuYYGG', 396, 484, NULL);
INSERT INTO `im_user_data` VALUES ('100079', 'Mui Ho Yin', 'https://img0`baidu>com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1997-04-10 02:16:28', '东城区东单王府井东街646号', '建筑师', 372, 883, 273, 31, 885, 1, 'dI38cBETw9', 287, 975, NULL);
INSERT INTO `im_user_data` VALUES ('100080', 'Chan Hok Yau', 'https://img0>baiduRcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1995-12-26 05:15:19', '成华区玉双路6号802号', '软件开发员', 875, 127, 780, 748, 618, 0, 'y8NvyTMwk2', 968, 318, NULL);
INSERT INTO `im_user_data` VALUES ('100081', 'Yung On Na', 'https://img0 baiduacom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1998-10-25 09:03:56', '罗湖区蔡屋围深南东路730号', '护士', 109, 434, 337, 890, 149, 1, 'SIStrGohzK', 702, 425, NULL);
INSERT INTO `im_user_data` VALUES ('100082', '汤安琪', 'https://img0XbaiduZcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-09-12 15:09:04', '海珠区江南西路526号', '兽医助理', 127, 486, 317, 768, 612, 1, 'IqdCv1Ewzu', 209, 326, NULL);
INSERT INTO `im_user_data` VALUES ('100083', 'Yam Sze Yu', 'https://img0 baiduLcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1994-05-07 00:18:50', '罗湖区蔡屋围深南东路963号', '管家', 378, 905, 521, 245, 704, 0, 'uv9eCaq105', 479, 745, NULL);
INSERT INTO `im_user_data` VALUES ('100084', '宋安琪', 'https://img0mbaiduOcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1997-08-22 09:08:16', '福田区深南大道636号', '首席运营官', 185, 205, 977, 711, 313, 1, 'kqm3zblGZB', 900, 341, NULL);
INSERT INTO `im_user_data` VALUES ('100085', '廖岚', 'https://img0gbaiduZcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1994-03-06 11:44:09', '浦东新区橄榄路650号', '软件开发员', 564, 779, 800, 398, 168, 1, 'QnTVSLZBTT', 849, 831, NULL);
INSERT INTO `im_user_data` VALUES ('100086', 'Ti Kwok Yin', 'https://img0nbaiduQcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1990-08-20 05:19:00', '京华商圈华夏街969号', '婚礼筹办者', 596, 352, 233, 835, 139, 1, '7OvV8mTIaM', 422, 882, NULL);
INSERT INTO `im_user_data` VALUES ('100087', '宋震南', 'https://img0Mbaidu%com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-05-20 05:23:50', '西城区西長安街372号', '图书馆管理员', 414, 397, 463, 880, 749, 1, 'Fe05qVRfig', 221, 947, NULL);
INSERT INTO `im_user_data` VALUES ('100088', '韦震南', 'https://img0;baidugcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1990-06-04 01:32:14', '天河区大信商圈大信南路476号', '客戶協調員', 231, 608, 251, 414, 119, 0, 'aKzaODteog', 502, 158, NULL);
INSERT INTO `im_user_data` VALUES ('100089', 'Tang Wai Han', 'https://img0ubaidu}com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1999-01-01 03:38:44', '福田区景田东一街891号', '人力资源经理', 490, 244, 889, 224, 690, 1, 'MQCC6OpuRS', 912, 928, NULL);
INSERT INTO `im_user_data` VALUES ('100090', '汪安琪', 'https://img0\'baiduzcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-03-24 02:12:22', '东泰五街505号', '药剂师', 356, 841, 452, 875, 403, 0, 'lTHneDERlO', 856, 959, NULL);
INSERT INTO `im_user_data` VALUES ('100091', 'Koo Ka Ming', 'https://img06baidu\"com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1995-05-30 22:48:18', '朝阳区三里屯路110号', '多媒体动画师', 512, 682, 803, 281, 618, 1, 'kNqqpUtt1s', 877, 676, NULL);
INSERT INTO `im_user_data` VALUES ('100092', '蒋子异', 'https://img0ebaiduecom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1995-04-12 11:36:17', '天河区大信商圈大信南路915号', '专案经理', 380, 267, 811, 729, 821, 1, 'WOFKPzZtf8', 135, 654, NULL);
INSERT INTO `im_user_data` VALUES ('100093', '叶子韬', 'https://img0>baidu`com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1993-12-14 10:05:30', '罗湖区清水河一路460号', '纹身艺术家', 854, 393, 294, 81, 121, 0, '5lfG8Ttsu3', 598, 881, NULL);
INSERT INTO `im_user_data` VALUES ('100094', '杜璐', 'https://img0Mbaidu(com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1998-03-26 01:19:10', '福田区深南大道671号', '团体领导', 893, 751, 828, 342, 579, 1, 'OyW9MI45WG', 253, 412, NULL);
INSERT INTO `im_user_data` VALUES ('100095', '雷云熙', 'https://img0~baidu>com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1992-04-30 22:59:06', '海珠区江南西路552号', '护士', 900, 870, 659, 672, 777, 0, 'H8NYXF7pK5', 813, 386, NULL);
INSERT INTO `im_user_data` VALUES ('100096', 'Kao Tsz Ching', 'https://img0`baidu[com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1996-01-09 10:19:20', '罗湖区蔡屋围深南东路795号', '演员', 703, 454, 689, 515, 652, 1, '51oPT0HM8H', 605, 561, NULL);
INSERT INTO `im_user_data` VALUES ('100097', '侯睿', 'https://img0}baidu#com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1999-09-05 04:09:19', '东泰五街290号', '客戶協調員', 798, 873, 437, 929, 239, 0, 'UdTdIDWACs', 96, 348, NULL);
INSERT INTO `im_user_data` VALUES ('100098', '武璐', 'https://img0cbaidu-com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1992-03-18 17:29:32', '白云区机场路棠苑街五巷490号', '零售助理', 766, 259, 139, 780, 208, 0, 'EsiB450KqL', 406, 241, NULL);
INSERT INTO `im_user_data` VALUES ('100099', '陶震南', 'https://img0!baidu*com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1997-12-15 07:19:42', '紫马岭商圈中山五路683号', '兽医', 538, 914, 18, 944, 563, 0, 'fOnSg8fdHc', 788, 665, NULL);
INSERT INTO `im_user_data` VALUES ('100100', '严震南', 'https://img0wbaiduvcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1997-06-08 10:17:15', '京华商圈华夏街281号', '婚礼筹办者', 578, 702, 124, 6, 724, 0, 'VbQP9NepIi', 999, 178, NULL);
INSERT INTO `im_user_data` VALUES ('100101', 'Tsui On Na', 'https://img0MbaiduFcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1996-11-13 12:52:46', '成华区玉双路6号167号', '工程师', 857, 211, 452, 870, 629, 0, 'VcmTFdJf8G', 337, 153, NULL);
INSERT INTO `im_user_data` VALUES ('100102', '石云熙', 'https://img0Ibaidudcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1993-02-01 02:28:10', '坑美十五巷862号', '审计师', 178, 891, 761, 653, 44, 1, 'MDYM36Z9sh', 723, 861, NULL);
INSERT INTO `im_user_data` VALUES ('100103', 'Yue Wing Kuen', 'https://img0dbaiduEcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1991-01-16 13:58:41', '紫马岭商圈中山五路422号', '教授', 996, 726, 524, 881, 339, 1, 'fAhRaoGiue', 387, 847, NULL);
INSERT INTO `im_user_data` VALUES ('100104', 'Yeow Ching Wan', 'https://img0 baidu$com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1993-04-21 20:01:18', '東城区東直門內大街871号', '医生', 5, 557, 400, 611, 175, 1, 'TpKYScfmtn', 662, 170, NULL);
INSERT INTO `im_user_data` VALUES ('100105', '朱致远', 'https://img00baidu~com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1998-04-25 16:56:46', '朝阳区三里屯路915号', '客户服务经理', 474, 77, 293, 667, 241, 1, 'sJJK9xVdyV', 416, 241, NULL);
INSERT INTO `im_user_data` VALUES ('100106', 'Mo Kwok Ming', 'https://img0vbaidu\'com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1991-10-01 16:10:43', '乐丰六路700号', '管家', 80, 371, 825, 551, 855, 1, 'LIUrJ4EZIr', 260, 759, NULL);
INSERT INTO `im_user_data` VALUES ('100107', '方秀英', 'https://img0cbaiduTcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1995-01-02 22:53:08', '罗湖区蔡屋围深南东路861号', '办公室主管', 869, 150, 275, 717, 719, 1, 'ZDBUKjkkVD', 11, 418, NULL);
INSERT INTO `im_user_data` VALUES ('100108', 'Yeung Chiu Wai', 'https://img0fbaidu{com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1992-04-19 08:12:17', '西城区西長安街547号', '移动应用程式开发人员', 956, 781, 777, 82, 42, 0, 'Ml618p6u7O', 405, 282, NULL);
INSERT INTO `im_user_data` VALUES ('100109', '孟震南', 'https://img0+baidu^com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1995-01-30 17:28:17', '锦江区人民南路四段347号', '生物化学家', 112, 232, 359, 387, 859, 0, 'czktHGs8PM', 565, 365, NULL);
INSERT INTO `im_user_data` VALUES ('100110', 'Yuen Wai Yee', 'https://img0`baidu]com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1993-07-15 03:03:00', '珊瑚路134号', '兽医', 993, 736, 757, 676, 894, 0, 'ebFBZ0OTwC', 294, 289, NULL);
INSERT INTO `im_user_data` VALUES ('100111', '朱杰宏', 'https://img0Xbaidubcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1992-09-26 18:12:37', '徐汇区虹桥路509号', '销售经理', 577, 319, 936, 888, 822, 1, 'yX7NOknAar', 661, 102, NULL);
INSERT INTO `im_user_data` VALUES ('100112', 'Wong On Kay', 'https://img0=baidu=com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1998-01-05 21:20:23', '福田区景田东一街142号', '专案经理', 279, 433, 290, 249, 987, 0, 'm9W4yLDSXH', 695, 388, NULL);
INSERT INTO `im_user_data` VALUES ('100113', '陶秀英', 'https://img0ebaiduFcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1998-05-25 22:12:48', '白云区小坪东路902号', '建筑师', 793, 718, 676, 330, 367, 0, 'iwiXDsoLRW', 491, 133, NULL);
INSERT INTO `im_user_data` VALUES ('100114', 'Yip Ling Ling', 'https://img0gbaidu%com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1991-04-19 22:36:02', '锦江区红星路三段797号', '管家', 680, 65, 569, 897, 673, 0, 'MZxm2Ub1vG', 857, 457, NULL);
INSERT INTO `im_user_data` VALUES ('100115', '胡安琪', 'https://img0;baiduYcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1991-04-09 21:14:47', '成华区玉双路6号255号', '牙齿矫正医生', 710, 217, 319, 609, 18, 0, 'W9fak3BQTF', 596, 262, NULL);
INSERT INTO `im_user_data` VALUES ('100116', 'Chic Kwok Wing', 'https://img0^baidulcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1997-09-01 01:01:13', '房山区岳琉路890号', '零售助理', 945, 363, 311, 686, 578, 0, 'VQFs4pQ6PS', 178, 428, NULL);
INSERT INTO `im_user_data` VALUES ('100117', 'Tin Sze Kwan', 'https://img0&baiduzcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-04-14 08:45:13', '成华区双庆路675号', '工程师', 885, 278, 888, 680, 716, 0, '0BaFyhrQrX', 514, 686, NULL);
INSERT INTO `im_user_data` VALUES ('100118', 'Liao Chiu Wai', 'https://img0Sbaiduecom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1993-07-06 13:51:01', '闵行区宾川路211号', '舞蹈演员', 12, 417, 990, 884, 634, 0, 'hpINKWvwk5', 371, 383, NULL);
INSERT INTO `im_user_data` VALUES ('100119', '雷致远', 'https://img0AbaiduUcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1999-04-10 00:54:17', '罗湖区田贝一路824号', '运营经理', 724, 16, 424, 434, 998, 1, 'UnOdCJStyy', 899, 72, NULL);
INSERT INTO `im_user_data` VALUES ('100120', '蒋安琪', 'https://img0ebaidu|com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1993-02-19 08:21:25', '浦东新区橄榄路559号', '人力资源经理', 527, 692, 205, 730, 402, 0, 't9JGkAWlw8', 639, 837, NULL);
INSERT INTO `im_user_data` VALUES ('100121', '莫秀英', 'https://img0#baidupcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1993-04-06 13:52:52', '龙岗区学园一巷762号', '物流协调员', 314, 37, 596, 648, 174, 0, 'wtPnMVrC12', 720, 298, NULL);
INSERT INTO `im_user_data` VALUES ('100122', 'Wu Yu Ling', 'https://img0?baidujcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1991-01-18 03:05:54', '珊瑚路337号', '财务分析师', 824, 532, 764, 731, 771, 1, 'HAU3zcNuH5', 209, 917, NULL);
INSERT INTO `im_user_data` VALUES ('100123', 'Yuen Cho Yee', 'https://img06baiduXcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1998-02-15 02:16:08', '天河区大信商圈大信南路110号', '管家', 273, 598, 62, 777, 276, 1, 'lpVg5JQxCX', 351, 54, NULL);
INSERT INTO `im_user_data` VALUES ('100124', 'Cheng Ka Man', 'https://img0 baidugcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1991-01-02 17:32:44', '罗湖区蔡屋围深南东路956号', '首席运营官', 137, 825, 676, 571, 879, 0, 'jAIAH5mT9p', 930, 391, NULL);
INSERT INTO `im_user_data` VALUES ('100125', '龙睿', 'https://img0Qbaidu\'com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1999-05-25 01:31:20', '海淀区清河中街68号238号', '图象设计师', 182, 96, 478, 436, 315, 1, 'LNTgyzWTsv', 474, 321, NULL);
INSERT INTO `im_user_data` VALUES ('100126', 'Lo Wai Man', 'https://img07baidu.com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1999-10-30 05:06:43', '浦东新区健祥路753号', '商务记者', 663, 471, 621, 536, 499, 0, 'devyBUyYFq', 315, 443, NULL);
INSERT INTO `im_user_data` VALUES ('100127', '余宇宁', 'https://img0bbaiduscom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1993-03-10 14:26:56', '环区南街二巷818号', '化妆师', 507, 295, 336, 722, 903, 1, '3Aa5rjKPmH', 804, 528, NULL);
INSERT INTO `im_user_data` VALUES ('100128', '郝云熙', 'https://img0bbaiduVcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1990-07-14 07:41:14', '徐汇区虹桥路60号', '团体领导', 499, 920, 42, 315, 666, 1, 'CGn04k6Qj7', 505, 843, NULL);
INSERT INTO `im_user_data` VALUES ('100129', 'Mui On Na', 'https://img08baidu)com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1991-06-02 00:26:52', '福田区景田东一街950号', '客戶協調員', 589, 145, 438, 83, 265, 1, 'AurN1EnQnh', 371, 363, NULL);
INSERT INTO `im_user_data` VALUES ('100130', '贺云熙', 'https://img0~baiduucom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1994-02-17 21:45:57', '海淀区清河中街68号84号', '商务记者', 730, 560, 52, 662, 614, 0, '8LaSwpKUlk', 258, 657, NULL);
INSERT INTO `im_user_data` VALUES ('100131', '廖詩涵', 'https://img0xbaiduCcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1999-06-07 09:11:00', '海珠区江南西路83号', '移动应用程式开发人员', 759, 714, 483, 291, 566, 0, 'ZHxgulLWBo', 167, 303, NULL);
INSERT INTO `im_user_data` VALUES ('100132', '陆岚', 'https://img0BbaiduJcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1998-02-15 14:14:18', '浦东新区健祥路948号', '网页开发人员', 975, 342, 524, 257, 584, 0, 'QgUttAgCJr', 943, 10, NULL);
INSERT INTO `im_user_data` VALUES ('100133', '宋震南', 'https://img0QbaiduJcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1995-08-29 07:42:23', '坑美十五巷173号', '财务分析师', 334, 786, 817, 695, 255, 0, 'ezfrFvX9IN', 962, 680, NULL);
INSERT INTO `im_user_data` VALUES ('100134', '冯睿', 'https://img0NbaiduLcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-03-16 10:20:37', '越秀区中山二路991号', '办公室主管', 688, 968, 845, 182, 875, 0, '1CCFvvGXWu', 517, 783, NULL);
INSERT INTO `im_user_data` VALUES ('100135', 'To Siu Wai', 'https://img0Pbaidu-com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1990-11-30 19:49:14', '西城区复兴门内大街817号', '客戶協調員', 491, 727, 591, 484, 444, 1, 'y5r626ihMw', 470, 691, NULL);
INSERT INTO `im_user_data` VALUES ('100136', 'Tang Tin Lok', 'https://img0bbaidu!com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1997-10-09 13:40:07', '锦江区人民南路四段115号', '制片人', 906, 807, 857, 126, 600, 1, 'xiyPBzUT6E', 901, 870, NULL);
INSERT INTO `im_user_data` VALUES ('100137', '姜致远', 'https://img0/baiduNcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-12-02 00:46:16', '東城区東直門內大街171号', '专案经理', 289, 526, 558, 432, 599, 1, 'yZCwX8bHHa', 817, 315, NULL);
INSERT INTO `im_user_data` VALUES ('100138', '钟睿', 'https://img0zbaidu~com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1995-08-10 07:37:16', '黄浦区淮海中路422号', '财务分析师', 577, 733, 819, 743, 258, 1, 'huxXocv8Nj', 546, 119, NULL);
INSERT INTO `im_user_data` VALUES ('100139', '顾宇宁', 'https://img0Lbaiduocom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1995-06-15 02:21:24', '乐丰六路548号', '农夫', 960, 249, 572, 157, 139, 1, 'dmqXI9OS9l', 101, 940, NULL);
INSERT INTO `im_user_data` VALUES ('100140', '秦子韬', 'https://img0jbaiduccom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1999-02-01 11:23:44', '珊瑚路345号', '食品科学家', 143, 760, 580, 31, 286, 0, 'Sc59Yjfsty', 770, 322, NULL);
INSERT INTO `im_user_data` VALUES ('100141', '侯致远', 'https://img0MbaiduZcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-05-10 19:51:47', '成华区玉双路6号594号', '药剂师', 645, 188, 258, 680, 813, 0, 'QoukslVlHj', 305, 789, NULL);
INSERT INTO `im_user_data` VALUES ('100142', '高嘉伦', 'https://img0ubaiduycom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1994-04-23 01:14:55', '珊瑚路256号', '专案经理', 72, 981, 569, 406, 393, 0, 'NA7eNcAkbM', 565, 45, NULL);
INSERT INTO `im_user_data` VALUES ('100143', 'Chu Tsz Ching', 'https://img0^baidulcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1994-04-01 02:17:12', '徐汇区虹桥路764号', 'UX / UI设计员', 958, 63, 949, 333, 103, 1, 'SJK91zD1B6', 972, 34, NULL);
INSERT INTO `im_user_data` VALUES ('100144', '何致远', 'https://img0kbaidu@com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1996-06-19 08:34:31', '海珠区江南西路867号', '农夫', 753, 983, 685, 344, 167, 0, 'NxtSEgBdN1', 285, 309, NULL);
INSERT INTO `im_user_data` VALUES ('100145', '尹秀英', 'https://img0ubaiduCcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1999-09-01 11:19:07', '东城区东单王府井东街854号', '水疗经理', 851, 911, 546, 16, 974, 1, 'Ix7doJPbo4', 358, 947, NULL);
INSERT INTO `im_user_data` VALUES ('100146', 'Loui Wing Kuen', 'https://img0pbaiduJcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1999-07-07 13:29:34', '龙岗区布吉镇西环路815号', '纹身艺术家', 562, 4, 250, 768, 220, 0, 'PV1NvjOmZY', 446, 685, NULL);
INSERT INTO `im_user_data` VALUES ('100147', '曹子异', 'https://img0lbaiduPcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1990-03-05 21:26:41', '成华区玉双路6号272号', '物流协调员', 217, 370, 577, 317, 707, 0, 'GbBIKYs0uI', 226, 124, NULL);
INSERT INTO `im_user_data` VALUES ('100148', 'Chung Tsz Hin', 'https://img0XbaiduFcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1999-08-28 11:30:48', '乐丰六路861号', '销售经理', 692, 795, 602, 949, 91, 0, 'gN0TLsVs3r', 202, 828, NULL);
INSERT INTO `im_user_data` VALUES ('100149', '傅秀英', 'https://img0>baiduPcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1991-07-20 22:29:54', '成华区二仙桥东三路364号', '商务记者', 726, 433, 895, 876, 76, 0, 'cqC5CuX5iN', 906, 884, NULL);
INSERT INTO `im_user_data` VALUES ('100150', 'Choi Chun Yu', 'https://img0gbaiduccom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1994-03-02 17:32:45', '东泰五街665号', '舞蹈演员', 542, 847, 19, 400, 639, 0, '1biSrn6T4A', 494, 501, NULL);
INSERT INTO `im_user_data` VALUES ('100151', '田震南', 'https://img04baidutcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1998-04-09 10:03:21', '乐丰六路584号', '移动应用程式开发人员', 163, 280, 954, 579, 450, 1, 'cxIYBvXiq2', 845, 159, NULL);
INSERT INTO `im_user_data` VALUES ('100152', 'Tao Chun Yu', 'https://img0zbaidu6com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1993-01-06 19:30:27', '龙岗区学园一巷597号', '演员', 444, 880, 369, 559, 764, 1, 'WwmGsFJSpA', 574, 643, NULL);
INSERT INTO `im_user_data` VALUES ('100153', '侯震南', 'https://img0Kbaidu/com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1994-06-21 02:23:18', '坑美十五巷808号', '办公室文员', 422, 847, 348, 112, 948, 0, 'yfdBrMxzn4', 957, 252, NULL);
INSERT INTO `im_user_data` VALUES ('100154', '汪秀英', 'https://img0nbaiduRcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1991-06-23 18:26:49', '朝阳区三里屯路9号', '财务分析师', 200, 750, 977, 531, 210, 1, 'TpRpup8phU', 35, 520, NULL);
INSERT INTO `im_user_data` VALUES ('100155', 'Yue Chi Ming', 'https://img0Lbaidu3com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1991-12-28 01:42:14', '白云区机场路棠苑街五巷460号', '活动经理', 726, 906, 465, 156, 468, 1, 'ZY7R6gdcQt', 642, 42, NULL);
INSERT INTO `im_user_data` VALUES ('100156', 'Wong Ho Yin', 'https://img0Xbaidu?com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-05-23 08:02:42', '黄浦区淮海中路777号', '兽医助理', 51, 83, 552, 652, 960, 0, 'vAw9ZLBpcl', 72, 514, NULL);
INSERT INTO `im_user_data` VALUES ('100157', 'Choi Kwok Kuen', 'https://img0WbaiduLcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1998-02-05 02:28:40', '朝阳区三里屯路788号', '牙医', 55, 230, 272, 338, 502, 1, 'IQ4Wa6Ny5o', 609, 504, NULL);
INSERT INTO `im_user_data` VALUES ('100158', 'Tong Wing Fat', 'https://img0`baidu}com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1991-07-17 00:30:28', '龙岗区布吉镇西环路141号', '美容师', 384, 947, 50, 960, 298, 1, '5Mpf0dPKWT', 807, 63, NULL);
INSERT INTO `im_user_data` VALUES ('100159', 'Sheh Chiu Wai', 'https://img0ebaiduqcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1990-03-06 07:32:44', '海珠区江南西路946号', '教授', 294, 685, 147, 372, 552, 0, 'UVKwthA4GL', 400, 501, NULL);
INSERT INTO `im_user_data` VALUES ('100160', 'Lo Siu Wai', 'https://img0$baidugcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1998-10-20 14:00:57', '福田区深南大道346号', '客户经理', 159, 442, 348, 626, 270, 0, 'IObwC9lwuy', 905, 572, NULL);
INSERT INTO `im_user_data` VALUES ('100161', 'Sit Ka Man', 'https://img0/baidu@com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1990-12-11 13:21:49', '東城区東直門內大街900号', '保险销售代理', 479, 401, 381, 440, 354, 0, 'fqvch68SW8', 567, 248, NULL);
INSERT INTO `im_user_data` VALUES ('100162', '孟云熙', 'https://img0lbaiduLcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1993-01-24 13:05:59', '东泰五街50号', '零售助理', 39, 102, 871, 207, 444, 0, 'Qc8hz58X8b', 840, 819, NULL);
INSERT INTO `im_user_data` VALUES ('100163', '曾宇宁', 'https://img0Hbaidu`com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1992-01-19 14:39:10', '成华区双庆路397号', '物流协调员', 843, 385, 144, 311, 280, 1, 'yrTAdruh4T', 93, 463, NULL);
INSERT INTO `im_user_data` VALUES ('100164', 'Choi Tin Wing', 'https://img0cbaidu6com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1994-01-23 12:28:19', '浦东新区健祥路860号', '商务记者', 595, 554, 816, 889, 976, 1, 'n6dPVrlt6w', 768, 916, NULL);
INSERT INTO `im_user_data` VALUES ('100165', '石岚', 'https://img0Pbaidu com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1991-10-31 04:10:21', '越秀区中山二路759号', '图书馆管理员', 246, 339, 84, 355, 135, 0, 'NJgX89xiLK', 593, 187, NULL);
INSERT INTO `im_user_data` VALUES ('100166', '段秀英', 'https://img06baiduOcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1994-06-04 21:00:13', '白云区小坪东路225号', '财务分析师', 738, 732, 907, 747, 598, 0, 'AMxYAaEHAs', 216, 513, NULL);
INSERT INTO `im_user_data` VALUES ('100167', 'Tam Kwok Ming', 'https://img0Xbaidu2com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1993-09-21 10:44:15', '延庆区028县道405号', '老师', 783, 450, 896, 551, 704, 0, 'SDniIVVQmW', 134, 448, NULL);
INSERT INTO `im_user_data` VALUES ('100168', 'Lui Tin Lok', 'https://img0abaidu<com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1995-06-20 12:29:50', '海淀区清河中街68号455号', '图象设计师', 833, 261, 528, 166, 955, 0, 'kni2CbmaxH', 55, 844, NULL);
INSERT INTO `im_user_data` VALUES ('100169', '顾致远', 'https://img0Abaidu:com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1991-08-04 00:20:19', '延庆区028县道714号', '建筑师', 719, 970, 880, 958, 985, 1, 'KB3zCAMzTk', 243, 616, NULL);
INSERT INTO `im_user_data` VALUES ('100170', 'Ti Hiu Tung', 'https://img0Qbaidu7com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1999-08-14 06:33:26', '锦江区人民南路四段246号', '物流协调员', 360, 408, 126, 708, 553, 1, 'S8ExNSql02', 599, 228, NULL);
INSERT INTO `im_user_data` VALUES ('100171', '胡云熙', 'https://img0abaiduxcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1995-07-04 01:30:55', '珊瑚路163号', '管家', 889, 738, 921, 59, 123, 0, 'r9vARZ50Io', 281, 200, NULL);
INSERT INTO `im_user_data` VALUES ('100172', 'Fan Sau Man', 'https://img0~baiduMcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-07-02 12:11:28', '白云区小坪东路251号', '信息安全分析师', 577, 136, 932, 515, 479, 0, 'bYA92JMPfF', 855, 414, NULL);
INSERT INTO `im_user_data` VALUES ('100173', '宋岚', 'https://img0`baidu>com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1992-12-20 21:43:14', '白云区小坪东路714号', '数据库经理', 588, 945, 368, 341, 325, 1, 'EGoyWvm3bb', 642, 965, NULL);
INSERT INTO `im_user_data` VALUES ('100174', '周嘉伦', 'https://img0@baiduncom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1992-12-15 16:18:33', '东泰五街399号', '导师', 587, 22, 329, 511, 84, 1, 'PdyhgBE0i9', 92, 93, NULL);
INSERT INTO `im_user_data` VALUES ('100175', '何致远', 'https://img0zbaiduXcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1991-07-28 12:46:07', '乐丰六路265号', '农夫', 901, 917, 117, 695, 406, 0, 'E2utQCMKSj', 347, 727, NULL);
INSERT INTO `im_user_data` VALUES ('100176', 'Koon Ka Man', 'https://img0hbaidu[com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1994-11-16 13:58:50', '东泰五街752号', '活动经理', 960, 898, 700, 116, 732, 1, 'txo5zb6jKF', 179, 853, NULL);
INSERT INTO `im_user_data` VALUES ('100177', 'Siu Wing Fat', 'https://img0Lbaidu=com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1992-07-19 23:45:31', '福田区景田东一街937号', '商务记者', 646, 213, 835, 858, 769, 0, 'uSD7Jizt6f', 112, 515, NULL);
INSERT INTO `im_user_data` VALUES ('100178', '韩璐', 'https://img08baidu8com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1997-01-05 21:30:22', '徐汇区虹桥路566号', '销售经理', 484, 353, 401, 218, 669, 0, 'tqArlDuQww', 605, 460, NULL);
INSERT INTO `im_user_data` VALUES ('100179', 'Lok Ching Wan', 'https://img0$baidu,com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1992-12-14 13:59:57', '海淀区清河中街68号903号', '人力资源经理', 602, 165, 642, 848, 169, 0, 'fjNRbdoS1U', 250, 907, NULL);
INSERT INTO `im_user_data` VALUES ('100180', 'Koo Cho Yee', 'https://img0Pbaidu}com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1999-12-13 14:58:02', '西城区西長安街265号', '物流协调员', 420, 678, 218, 411, 151, 1, 'lCfm2Hwnxe', 620, 219, NULL);
INSERT INTO `im_user_data` VALUES ('100181', '崔秀英', 'https://img0:baiduqcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1998-08-07 14:08:21', '紫马岭商圈中山五路208号', '销售经理', 312, 642, 625, 264, 351, 0, 'sY9MJBP5Dk', 266, 532, NULL);
INSERT INTO `im_user_data` VALUES ('100182', 'Mok Lik Sun', 'https://img0SbaiduEcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1990-01-07 09:45:54', '白云区小坪东路654号', '销售经理', 460, 896, 85, 330, 768, 0, 'Y6erxSkM1g', 31, 254, NULL);
INSERT INTO `im_user_data` VALUES ('100183', '戴宇宁', 'https://img0abaidu4com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1997-05-12 20:53:05', '成华区双庆路497号', '演员', 958, 72, 921, 577, 52, 0, 'nCW6IhoLEl', 771, 622, NULL);
INSERT INTO `im_user_data` VALUES ('100184', 'Wan Suk Yee', 'https://img0Vbaidu,com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1997-09-25 07:09:51', '朝阳区三里屯路36号', '精算师', 288, 305, 886, 295, 711, 1, 'OuFKAR6JXd', 25, 256, NULL);
INSERT INTO `im_user_data` VALUES ('100185', 'Hui Hiu Tung', 'https://img0)baidu$com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1994-08-11 20:34:59', '罗湖区蔡屋围深南东路719号', '农夫', 980, 208, 346, 481, 840, 1, 'Kp7JWhOlnS', 974, 52, NULL);
INSERT INTO `im_user_data` VALUES ('100186', '阎安琪', 'https://img0fbaidu;com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1994-11-15 16:57:37', '锦江区红星路三段158号', '多媒体动画师', 796, 213, 389, 658, 554, 0, 'uyQhAbJArb', 709, 213, NULL);
INSERT INTO `im_user_data` VALUES ('100187', '任宇宁', 'https://img0`baidu[com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-02-13 11:52:06', '福田区景田东一街166号', '药剂师', 743, 224, 87, 354, 633, 1, 'E0yU8y3okU', 51, 79, NULL);
INSERT INTO `im_user_data` VALUES ('100188', 'Ti Ka Keung', 'https://img0Cbaidu-com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1996-11-13 06:48:04', '环区南街二巷213号', '软件开发员', 98, 775, 861, 133, 806, 1, 'k6JZTFhyCr', 363, 741, NULL);
INSERT INTO `im_user_data` VALUES ('100189', '萧云熙', 'https://img0IbaiduPcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1990-12-03 14:38:53', '龙岗区布吉镇西环路201号', '兽医助理', 995, 698, 985, 655, 151, 1, 'wiXrCmgoB7', 646, 720, NULL);
INSERT INTO `im_user_data` VALUES ('100190', '罗岚', 'https://img0#baidu]com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1993-07-11 21:16:29', '罗湖区田贝一路748号', '多媒体动画师', 977, 271, 551, 630, 1, 1, 'v1z1QSxD7i', 53, 833, NULL);
INSERT INTO `im_user_data` VALUES ('100191', '钟宇宁', 'https://img0WbaiduScom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1996-04-01 04:11:58', '东泰五街488号', '牙医', 191, 646, 36, 706, 164, 0, 'RCez1tVU2b', 92, 754, NULL);
INSERT INTO `im_user_data` VALUES ('100192', 'Fong Hui Mei', 'https://img0Hbaidu.com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1992-02-19 19:40:52', '海珠区江南西路586号', '时装设计师', 70, 875, 634, 237, 788, 1, 'Ja9i6pXAC0', 216, 905, NULL);
INSERT INTO `im_user_data` VALUES ('100193', '戴震南', 'https://img00baidu6com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1990-06-23 12:45:40', '東城区東直門內大街130号', '农夫', 54, 152, 122, 235, 74, 0, 'PefCiEVOaR', 38, 574, NULL);
INSERT INTO `im_user_data` VALUES ('100194', 'Loui Tsz Ching', 'https://img0GbaiduKcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1997-01-25 13:07:30', '天河区大信商圈大信南路600号', '制片人', 25, 829, 644, 85, 335, 0, 'wbyUMKKf7F', 262, 395, NULL);
INSERT INTO `im_user_data` VALUES ('100195', 'Dai Wai Man', 'https://img0pbaidu|com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1991-01-08 00:57:00', '房山区岳琉路86号', '教授', 203, 256, 840, 526, 296, 0, 'pQOdnL019E', 912, 635, NULL);
INSERT INTO `im_user_data` VALUES ('100196', 'Tang Ka Ming', 'https://img0\'baidu)com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1997-12-29 04:41:50', '黄浦区淮海中路707号', '图书馆管理员', 590, 629, 270, 417, 494, 1, 'ymIkeqC3B8', 634, 664, NULL);
INSERT INTO `im_user_data` VALUES ('100197', 'Choi Chun Yu', 'https://img0DbaiduDcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1992-01-28 14:59:16', '锦江区红星路三段729号', '商务记者', 621, 910, 23, 226, 256, 1, 'qpSq1dUHFJ', 656, 232, NULL);
INSERT INTO `im_user_data` VALUES ('100198', '薛詩涵', 'https://img0Vbaidu!com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1991-09-17 00:42:05', '环区南街二巷30号', '精算师', 133, 551, 574, 271, 90, 0, 'XWDBNvx5ca', 200, 958, NULL);
INSERT INTO `im_user_data` VALUES ('100199', '陶晓明', 'https://img0fbaidu4com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1991-08-30 20:57:30', '徐汇区虹桥路336号', '理发师', 463, 42, 965, 651, 397, 0, 'DBfPLtDKJS', 975, 259, NULL);
INSERT INTO `im_user_data` VALUES ('100200', '王震南', 'https://img0Cbaiduucom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1992-03-23 21:02:40', '東城区東直門內大街913号', '软件开发员', 388, 372, 944, 732, 615, 0, 'UXXciFgZyw', 700, 804, NULL);
INSERT INTO `im_user_data` VALUES ('100201', 'Tong Lik Sun', 'https://img0Abaidu<com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '2000-02-13 05:42:10', '白云区小坪东路212号', '图书馆管理员', 315, 384, 967, 92, 341, 0, '5PnXAJovxk', 128, 944, NULL);
INSERT INTO `im_user_data` VALUES ('100202', '崔致远', 'https://img0rbaidu(com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1995-05-29 04:24:43', '罗湖区清水河一路73号', '团体领导', 867, 841, 811, 348, 155, 0, 'bmugROANDC', 225, 23, NULL);
INSERT INTO `im_user_data` VALUES ('100203', '阎秀英', 'https://img0wbaidu}com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-08-02 06:21:15', '天河区天河路669号', '兽医助理', 843, 365, 924, 490, 59, 0, 'lLfqOcC2yo', 553, 535, NULL);
INSERT INTO `im_user_data` VALUES ('100204', 'Ku Chun Yu', 'https://img0Obaidu7com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1992-10-16 14:20:26', '罗湖区蔡屋围深南东路757号', '理发师', 291, 838, 806, 146, 24, 0, '3Lf4ZtzzKU', 165, 150, NULL);
INSERT INTO `im_user_data` VALUES ('100205', '王詩涵', 'https://img0Mbaidu~com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1991-10-10 07:51:39', '成华区双庆路32号', '保险销售代理', 47, 738, 558, 909, 65, 1, 'XvkyP2rRtV', 675, 315, NULL);
INSERT INTO `im_user_data` VALUES ('100206', '梁詩涵', 'https://img0SbaiduVcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1991-11-22 10:45:51', '福田区景田东一街736号', '美容师', 327, 493, 590, 911, 609, 1, 'Khm6Vu8lFi', 129, 99, NULL);
INSERT INTO `im_user_data` VALUES ('100207', '范璐', 'https://img0xbaidu/com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1996-05-19 23:18:00', '环区南街二巷642号', '工程师', 251, 311, 50, 819, 175, 1, '9zu2c786mm', 560, 228, NULL);
INSERT INTO `im_user_data` VALUES ('100208', '邱震南', 'https://img0sbaidubcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1993-12-16 10:27:25', '京华商圈华夏街687号', '舞蹈演员', 327, 695, 439, 280, 516, 0, 'Nbe7OFxecz', 96, 517, NULL);
INSERT INTO `im_user_data` VALUES ('100209', 'Chiang Wing Kuen', 'https://img0MbaiduRcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-02-01 18:54:02', '乐丰六路526号', '美容师', 973, 302, 545, 753, 811, 1, 'ih3S7NHfFo', 305, 753, NULL);
INSERT INTO `im_user_data` VALUES ('100210', '陈嘉伦', 'https://img0(baidu#com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1990-11-26 21:14:45', '罗湖区清水河一路372号', '运营经理', 578, 196, 391, 908, 539, 0, 'tzhLRytKhp', 688, 327, NULL);
INSERT INTO `im_user_data` VALUES ('100211', '郑宇宁', 'https://img03baidutcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1998-05-10 18:32:02', '京华商圈华夏街320号', '食品科学家', 773, 155, 762, 428, 281, 0, 'G6UjCpT9Qn', 205, 358, NULL);
INSERT INTO `im_user_data` VALUES ('100212', 'Hsuan Tak Wah', 'https://img0)baidu;com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-06-07 18:36:03', '龙岗区布吉镇西环路222号', '时装设计师', 491, 201, 188, 441, 409, 0, 'WKejjprzI3', 963, 738, NULL);
INSERT INTO `im_user_data` VALUES ('100213', '莫岚', 'https://img0Ebaiduqcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-04-23 18:09:00', '成华区二仙桥东三路84号', '团体领导', 236, 964, 670, 565, 841, 0, '7gr9KQ4pzF', 344, 952, NULL);
INSERT INTO `im_user_data` VALUES ('100214', 'Meng Hiu Tung', 'https://img0\"baiduRcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1998-12-12 15:46:07', '罗湖区田贝一路339号', '保险销售代理', 60, 616, 124, 191, 622, 1, 'RxxaqrxPNK', 127, 368, NULL);
INSERT INTO `im_user_data` VALUES ('100215', 'Ng Kar Yan', 'https://img0[baidujcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1993-10-31 23:48:48', '延庆区028县道262号', '水疗经理', 731, 951, 853, 444, 108, 0, '1bMv4k8FTv', 945, 439, NULL);
INSERT INTO `im_user_data` VALUES ('100216', 'Yin Ling Ling', 'https://img0Xbaidulcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1991-04-10 18:03:46', '成华区双庆路229号', '歌手', 848, 472, 317, 652, 621, 1, 'N1Hk4Whmqm', 122, 695, NULL);
INSERT INTO `im_user_data` VALUES ('100217', 'To Wing Sze', 'https://img0Pbaidu,com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1994-10-27 06:45:39', '海珠区江南西路466号', '客戶協調員', 700, 584, 259, 897, 146, 0, 'nfJ6f44NCA', 763, 555, NULL);
INSERT INTO `im_user_data` VALUES ('100218', '夏云熙', 'https://img0bbaidu2com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1995-11-12 23:13:10', '黄浦区淮海中路485号', '牙医', 968, 505, 714, 980, 658, 0, 'I9waZnafCT', 149, 757, NULL);
INSERT INTO `im_user_data` VALUES ('100219', '秦嘉伦', 'https://img0gbaiduLcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1998-05-19 02:15:46', '西城区西長安街872号', '饲养员', 757, 244, 304, 348, 749, 0, 'xxE4XewFxz', 357, 369, NULL);
INSERT INTO `im_user_data` VALUES ('100220', 'Kwan Siu Wai', 'https://img0ibaidu\'com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1991-06-10 12:58:28', '成华区双庆路701号', '美容师', 176, 361, 113, 123, 822, 1, 'lVbriiilFO', 173, 754, NULL);
INSERT INTO `im_user_data` VALUES ('100221', '范秀英', 'https://img00baiduqcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1998-10-27 21:09:58', '西城区西長安街824号', '牙医', 835, 93, 591, 519, 234, 1, 'sjeF9LIXG4', 481, 398, NULL);
INSERT INTO `im_user_data` VALUES ('100222', 'Lui Tin Wing', 'https://img0Cbaidu&com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1997-06-07 00:40:49', '白云区机场路棠苑街五巷839号', '活动经理', 657, 978, 550, 733, 713, 1, 'oAPCQwggVT', 41, 204, NULL);
INSERT INTO `im_user_data` VALUES ('100223', '顾璐', 'https://img0#baidu<com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-12-25 15:33:09', '越秀区中山二路93号', '客户服务经理', 263, 736, 881, 73, 757, 1, 'Wd4CeNgwBk', 833, 302, NULL);
INSERT INTO `im_user_data` VALUES ('100224', 'Yau Ting Fung', 'https://img0kbaiduKcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-11-16 03:19:02', '越秀区中山二路807号', '保险销售代理', 251, 897, 161, 417, 908, 0, 'i4iJZNwHaj', 136, 752, NULL);
INSERT INTO `im_user_data` VALUES ('100225', 'Tao Ka Ling', 'https://img0`baiduBcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1995-05-28 08:19:36', '成华区二仙桥东三路995号', '私人教练', 839, 842, 713, 528, 256, 1, 'XPrVvy9idj', 188, 26, NULL);
INSERT INTO `im_user_data` VALUES ('100226', 'Chang Lik Sun', 'https://img0Xbaidu@com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-09-15 07:39:03', '西城区复兴门内大街260号', '私人教练', 903, 829, 503, 994, 750, 1, 'XNyQrKi7qp', 840, 399, NULL);
INSERT INTO `im_user_data` VALUES ('100227', 'Tong Lai Yan', 'https://img0\\baiduCcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1990-03-03 00:44:15', '东城区东单王府井东街856号', '技术支援人员', 99, 142, 703, 311, 206, 1, 'aUbBra65Fr', 814, 529, NULL);
INSERT INTO `im_user_data` VALUES ('100228', '何震南', 'https://img00baidu@com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1998-10-25 05:38:50', '朝阳区三里屯路408号', '图象设计师', 618, 164, 234, 556, 677, 1, '8Z6dlSWkHB', 736, 255, NULL);
INSERT INTO `im_user_data` VALUES ('100229', '梁晓明', 'https://img0Ybaidu>com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1995-03-31 01:54:47', '福田区深南大道265号', '药剂师', 906, 525, 647, 714, 979, 0, 'xE1AfAtaIj', 412, 528, NULL);
INSERT INTO `im_user_data` VALUES ('100230', 'Yip Sze Yu', 'https://img0\\baidu3com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1992-10-11 14:21:47', '东城区东单王府井东街236号', '软件开发员', 143, 183, 717, 329, 107, 1, 'BwoELijt27', 531, 545, NULL);
INSERT INTO `im_user_data` VALUES ('100231', 'Yung Wai Han', 'https://img0+baiduMcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1991-02-02 23:58:24', '龙岗区布吉镇西环路82号', '软件开发员', 529, 344, 672, 46, 134, 0, 'p0cOtvBiNo', 593, 361, NULL);
INSERT INTO `im_user_data` VALUES ('100232', 'Leung Wai Man', 'https://img0_baiduMcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-02-03 13:58:35', '天河区大信商圈大信南路11号', '婚礼筹办者', 116, 965, 870, 769, 211, 0, '8ujBgpsN2a', 539, 987, NULL);
INSERT INTO `im_user_data` VALUES ('100233', '毛安琪', 'https://img0Tbaidu)com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1995-07-01 19:11:56', '東城区東直門內大街231号', '教授', 969, 158, 611, 719, 976, 1, 'xgQKC0mZAa', 860, 616, NULL);
INSERT INTO `im_user_data` VALUES ('100234', '张詩涵', 'https://img0[baidu*com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1998-12-19 01:45:50', '锦江区人民南路四段392号', '信息安全分析师', 961, 525, 16, 977, 365, 1, 'UuPZaqElxm', 994, 114, NULL);
INSERT INTO `im_user_data` VALUES ('100235', '郭璐', 'https://img0mbaidufcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1996-12-05 15:29:05', '锦江区人民南路四段929号', '舞蹈演员', 169, 200, 20, 248, 234, 0, '0WhyMe3SiE', 397, 54, NULL);
INSERT INTO `im_user_data` VALUES ('100236', 'Wan Ka Fai', 'https://img0IbaiduFcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1991-03-01 10:44:17', '龙岗区布吉镇西环路701号', '歌手', 84, 756, 413, 145, 229, 0, 'g8TgetUvQD', 401, 829, NULL);
INSERT INTO `im_user_data` VALUES ('100237', 'Ku Sau Man', 'https://img0ibaidu5com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1993-10-14 13:55:37', '环区南街二巷535号', '医生', 345, 488, 171, 302, 240, 1, 'ItzTXZsL9e', 404, 417, NULL);
INSERT INTO `im_user_data` VALUES ('100238', '尹震南', 'https://img0qbaidumcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1991-01-28 21:46:23', '天河区天河路850号', '牙齿矫正医生', 537, 55, 679, 294, 64, 1, 'Q2GPJiG1gW', 627, 12, NULL);
INSERT INTO `im_user_data` VALUES ('100239', '谢子异', 'https://img0Mbaiduecom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1994-04-17 10:58:29', '東城区東直門內大街594号', '信息安全分析师', 194, 579, 352, 158, 324, 0, 'oYufBVzBIW', 969, 727, NULL);
INSERT INTO `im_user_data` VALUES ('100240', 'Chung Wai Lam', 'https://img0ebaidu)com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1999-10-08 08:06:02', '徐汇区虹桥路521号', '多媒体动画师', 847, 294, 422, 682, 182, 0, 'TbyT7BYnaj', 59, 796, NULL);
INSERT INTO `im_user_data` VALUES ('100241', '陈宇宁', 'https://img0&baidu2com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1996-04-20 01:09:54', '東城区東直門內大街970号', '移动应用程式开发人员', 895, 364, 470, 290, 393, 1, 'MTaIKkfgVG', 194, 723, NULL);
INSERT INTO `im_user_data` VALUES ('100242', '龙震南', 'https://img03baiduNcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1999-02-20 11:37:18', '海淀区清河中街68号362号', '办公室主管', 224, 952, 455, 949, 654, 0, 'ZzUcss0hUw', 60, 959, NULL);
INSERT INTO `im_user_data` VALUES ('100243', '许晓明', 'https://img0@baiduqcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1998-08-11 23:09:09', '京华商圈华夏街936号', '人力资源经理', 96, 607, 989, 932, 22, 0, 'kQavZkV6re', 81, 909, NULL);
INSERT INTO `im_user_data` VALUES ('100244', '杜岚', 'https://img0rbaiduCcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1999-08-21 22:43:11', '闵行区宾川路497号', '保险销售代理', 166, 247, 579, 919, 923, 0, '7kaNsgSJiw', 751, 257, NULL);
INSERT INTO `im_user_data` VALUES ('100245', '向嘉伦', 'https://img07baidukcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1993-11-08 10:17:57', '黄浦区淮海中路248号', '信息安全分析师', 528, 573, 277, 552, 796, 1, 'IfBnhzIP1S', 241, 596, NULL);
INSERT INTO `im_user_data` VALUES ('100246', '邓岚', 'https://img0dbaiduOcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1995-10-05 20:46:14', '朝阳区三里屯路277号', '移动应用程式开发人员', 997, 36, 362, 327, 460, 1, 'QzDl2tdQxo', 880, 748, NULL);
INSERT INTO `im_user_data` VALUES ('100247', 'Yau Yun Fat', 'https://img0 baiduXcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1994-10-31 12:59:34', '延庆区028县道863号', '美容师', 948, 861, 257, 973, 166, 0, '0GKxTt3PyG', 705, 664, NULL);
INSERT INTO `im_user_data` VALUES ('100248', 'Lam Chi Ming', 'https://img0+baiduCcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1993-08-25 19:22:55', '京华商圈华夏街548号', '图象设计师', 249, 706, 449, 819, 558, 0, 'KnflKrsdFD', 219, 317, NULL);
INSERT INTO `im_user_data` VALUES ('100249', '谭云熙', 'https://img0Hbaiduscom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1991-09-21 21:02:53', '罗湖区蔡屋围深南东路742号', '工程师', 308, 48, 697, 110, 289, 1, 'FAeYAiPjyk', 858, 887, NULL);
INSERT INTO `im_user_data` VALUES ('100250', '谭秀英', 'https://img0~baidu1com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1993-12-23 18:57:14', '黄浦区淮海中路930号', '园艺家', 254, 435, 565, 585, 164, 0, 'K8B9Wn9ndu', 618, 710, NULL);
INSERT INTO `im_user_data` VALUES ('100251', 'Mok Chi Yuen', 'https://img0<baidu|com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1995-06-12 09:06:56', '黄浦区淮海中路65号', '客户经理', 764, 187, 285, 920, 505, 0, 'Bm4e0YyKoX', 892, 579, NULL);
INSERT INTO `im_user_data` VALUES ('100252', 'Cheng On Kay', 'https://img0 baiduvcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1990-07-06 17:56:45', '闵行区宾川路986号', '办公室主管', 313, 242, 391, 880, 290, 0, 'OOfKbKcAcl', 761, 134, NULL);
INSERT INTO `im_user_data` VALUES ('100253', '钱震南', 'https://img0Wbaidu~com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1993-11-03 22:48:39', '福田区景田东一街268号', '团体领导', 590, 301, 693, 609, 300, 0, 'VXwzoSmG14', 137, 773, NULL);
INSERT INTO `im_user_data` VALUES ('100254', '潘震南', 'https://img0dbaiduQcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1997-04-11 05:08:51', '白云区机场路棠苑街五巷514号', '教授', 233, 919, 747, 533, 970, 1, 'LPYxwMUxBu', 556, 674, NULL);
INSERT INTO `im_user_data` VALUES ('100255', 'Kwok Tsz Ching', 'https://img0(baiduucom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1993-04-06 02:43:24', '锦江区红星路三段850号', '人力资源经理', 414, 210, 133, 495, 801, 0, 'k13uNINTFN', 62, 288, NULL);
INSERT INTO `im_user_data` VALUES ('100256', '金晓明', 'https://img0Cbaiduzcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '2000-01-26 00:29:01', '成华区玉双路6号870号', '牙齿矫正医生', 501, 68, 625, 435, 307, 1, 'mk6PLIFWsM', 134, 237, NULL);
INSERT INTO `im_user_data` VALUES ('100257', 'Ng Hui Mei', 'https://img02baidu{com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1990-09-20 09:38:27', '西城区复兴门内大街916号', '水疗经理', 14, 739, 639, 262, 687, 0, '6ULHosRseT', 499, 814, NULL);
INSERT INTO `im_user_data` VALUES ('100258', '郭岚', 'https://img0.baiduncom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-11-30 20:29:56', '房山区岳琉路695号', '生物化学家', 836, 70, 956, 657, 402, 0, 'XPNUEY6bOG', 202, 43, NULL);
INSERT INTO `im_user_data` VALUES ('100259', '冯云熙', 'https://img07baidu(com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1998-04-17 17:06:21', '乐丰六路358号', '精算师', 3, 738, 844, 74, 214, 1, 'PFqqPYLt6n', 834, 538, NULL);
INSERT INTO `im_user_data` VALUES ('100260', '方詩涵', 'https://img0/baiduUcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1997-10-27 05:13:53', '罗湖区蔡屋围深南东路447号', '物流协调员', 356, 650, 512, 323, 653, 1, 'blouAZT2Dg', 417, 55, NULL);
INSERT INTO `im_user_data` VALUES ('100261', '马詩涵', 'https://img0Jbaidu5com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1999-08-01 05:14:32', '延庆区028县道741号', '信息安全分析师', 536, 152, 512, 241, 239, 1, 'CGt2lliqTI', 572, 599, NULL);
INSERT INTO `im_user_data` VALUES ('100262', '孔秀英', 'https://img0sbaidudcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1998-03-18 04:20:57', '罗湖区蔡屋围深南东路877号', '管家', 900, 642, 644, 190, 159, 0, '56P3dmYLw8', 114, 136, NULL);
INSERT INTO `im_user_data` VALUES ('100263', '邱嘉伦', 'https://img0bbaidu}com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1996-11-07 01:20:38', '闵行区宾川路463号', '商务记者', 413, 128, 642, 887, 785, 1, 'DcqHwgzhle', 249, 888, NULL);
INSERT INTO `im_user_data` VALUES ('100264', 'Shing Ka Ling', 'https://img0ibaidu2com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1998-03-25 02:28:43', '天河区大信商圈大信南路937号', '图象设计师', 499, 548, 20, 696, 202, 0, 'BlpjfPxP7y', 195, 710, NULL);
INSERT INTO `im_user_data` VALUES ('100265', '江詩涵', 'https://img0Ebaidu5com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1996-02-09 07:36:09', '海淀区清河中街68号768号', '美容师', 896, 825, 787, 246, 96, 1, 'C1WDwRfZKk', 501, 23, NULL);
INSERT INTO `im_user_data` VALUES ('100266', 'Kwong Ka Man', 'https://img0NbaiduVcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1999-12-29 07:09:57', '成华区二仙桥东三路474号', '社交媒体协调员', 603, 940, 672, 366, 812, 1, '3Vs0723qRV', 396, 284, NULL);
INSERT INTO `im_user_data` VALUES ('100267', 'Miu Tin Wing', 'https://img0>baiduecom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1998-09-19 12:30:14', '闵行区宾川路465号', '管家', 502, 195, 315, 78, 149, 0, 'hojbLZsyc7', 989, 158, NULL);
INSERT INTO `im_user_data` VALUES ('100268', '杨璐', 'https://img0Hbaidu5com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1995-06-27 14:41:26', '东泰五街937号', '物流协调员', 155, 629, 937, 988, 756, 1, '2EiCXQs6nq', 494, 62, NULL);
INSERT INTO `im_user_data` VALUES ('100269', 'Yeung On Na', 'https://img08baiduUcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1990-02-21 20:05:36', '坑美十五巷468号', '婚礼筹办者', 252, 865, 748, 958, 482, 1, 'hGlCkYiUi2', 667, 983, NULL);
INSERT INTO `im_user_data` VALUES ('100270', '贾致远', 'https://img07baidufcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1991-10-27 08:53:08', '越秀区中山二路808号', '移动应用程式开发人员', 759, 357, 484, 87, 788, 0, '9QBMeII1eb', 114, 990, NULL);
INSERT INTO `im_user_data` VALUES ('100271', '熊晓明', 'https://img0`baidu.com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1993-10-08 09:03:17', '西城区复兴门内大街102号', '数据库经理', 902, 393, 778, 416, 654, 0, 'oCp7P2QI1y', 600, 188, NULL);
INSERT INTO `im_user_data` VALUES ('100272', '蒋嘉伦', 'https://img0%baiduHcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1997-05-02 21:00:57', '白云区小坪东路412号', '运营经理', 471, 218, 815, 304, 817, 1, 'NmiiyNjxhB', 577, 999, NULL);
INSERT INTO `im_user_data` VALUES ('100273', '孔子韬', 'https://img0*baiduicom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-06-11 01:42:12', '环区南街二巷31号', '客户经理', 942, 844, 207, 814, 223, 0, 'XJ96EXYKSa', 907, 181, NULL);
INSERT INTO `im_user_data` VALUES ('100274', '黄璐', 'https://img0Tbaiduecom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1990-06-10 15:25:46', '京华商圈华夏街729号', '医生', 709, 925, 438, 743, 800, 1, '6FILSvCJ2L', 687, 883, NULL);
INSERT INTO `im_user_data` VALUES ('100275', 'Siu On Na', 'https://img05baidu>com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1990-02-05 18:22:26', '闵行区宾川路985号', '化妆师', 454, 235, 910, 974, 187, 0, 'IZBwNShrCV', 214, 384, NULL);
INSERT INTO `im_user_data` VALUES ('100276', '曾杰宏', 'https://img0<baiduNcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1990-04-28 14:17:51', '白云区小坪东路728号', '财务分析师', 823, 667, 795, 378, 426, 0, 'tjycDlJGJ2', 702, 44, NULL);
INSERT INTO `im_user_data` VALUES ('100277', 'Heung Yun Fat', 'https://img0ebaidutcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1990-05-28 18:41:15', '朝阳区三里屯路413号', '婚礼筹办者', 375, 211, 839, 592, 433, 1, '57kafEhtNX', 260, 867, NULL);
INSERT INTO `im_user_data` VALUES ('100278', 'Cho Wai San', 'https://img0<baidu;com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1991-12-31 23:16:44', '坑美十五巷233号', '时装设计师', 940, 702, 624, 655, 796, 0, 'Hr0KtMjzrs', 439, 158, NULL);
INSERT INTO `im_user_data` VALUES ('100279', 'Tin Ka Ling', 'https://img0Ibaidu!com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1995-07-03 16:50:46', '徐汇区虹桥路50号', '数据库经理', 137, 61, 932, 86, 296, 0, 'NHUwhZOb8n', 830, 975, NULL);
INSERT INTO `im_user_data` VALUES ('100280', 'Koon Kwok Kuen', 'https://img0\'baidu^com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1990-05-19 14:17:10', '紫马岭商圈中山五路553号', '饲养员', 80, 840, 330, 623, 255, 0, 'bQheDD9Yxl', 551, 757, NULL);
INSERT INTO `im_user_data` VALUES ('100281', '丁子异', 'https://img0)baidu\\com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1994-03-15 18:36:18', '西城区复兴门内大街113号', '作家', 746, 353, 251, 219, 845, 1, '8a4KYYuh1H', 920, 908, NULL);
INSERT INTO `im_user_data` VALUES ('100282', 'Chu Fat', 'https://img0qbaiduDcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1995-02-28 07:21:46', '罗湖区田贝一路718号', '理发师', 949, 877, 744, 690, 86, 1, 'Dpl5ZLnE5w', 181, 498, NULL);
INSERT INTO `im_user_data` VALUES ('100283', 'Tin Tsz Ching', 'https://img0;baiduVcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1999-08-30 23:37:37', '白云区机场路棠苑街五巷614号', '水疗经理', 950, 662, 616, 575, 421, 1, 'mVgdk263iB', 907, 800, NULL);
INSERT INTO `im_user_data` VALUES ('100284', 'Mui Kwok Kuen', 'https://img0Rbaiduqcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1991-08-01 11:35:14', '西城区西長安街720号', '管家', 53, 196, 591, 353, 196, 0, 'rrlFhV1Qvo', 670, 540, NULL);
INSERT INTO `im_user_data` VALUES ('100285', 'Lo Ho Yin', 'https://img0zbaidupcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1998-10-12 10:09:49', '罗湖区蔡屋围深南东路593号', '牙医', 426, 636, 825, 371, 455, 1, 'PjXxTHVZCw', 423, 650, NULL);
INSERT INTO `im_user_data` VALUES ('100286', 'Kwan Kar Yan', 'https://img01baidudcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1991-05-04 12:10:05', '环区南街二巷518号', '审计师', 412, 579, 213, 809, 487, 0, 'LWKUqpOzDZ', 938, 387, NULL);
INSERT INTO `im_user_data` VALUES ('100287', '石秀英', 'https://img0fbaidu(com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1990-06-28 11:38:48', '乐丰六路546号', '纹身艺术家', 89, 515, 256, 639, 451, 0, '8jigbar15V', 988, 623, NULL);
INSERT INTO `im_user_data` VALUES ('100288', '徐岚', 'https://img0GbaiduQcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1994-04-21 17:08:40', '房山区岳琉路710号', '演员', 395, 54, 541, 946, 721, 1, 'YWUdgeJnoh', 497, 116, NULL);
INSERT INTO `im_user_data` VALUES ('100289', 'Lee Chun Yu', 'https://img0cbaiduucom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1991-07-19 18:08:10', '龙岗区学园一巷741号', '管家', 213, 697, 781, 172, 824, 1, 'iuUysydfZ4', 153, 546, NULL);
INSERT INTO `im_user_data` VALUES ('100290', 'Fu Wai San', 'https://img0Ybaiduhcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1991-02-05 17:34:49', '天河区天河路588号', '纹身艺术家', 12, 586, 544, 982, 236, 0, 'k7Xqtlfdml', 777, 309, NULL);
INSERT INTO `im_user_data` VALUES ('100291', '石秀英', 'https://img0.baiduZcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1995-09-29 15:25:20', '罗湖区蔡屋围深南东路907号', '数据库经理', 774, 263, 10, 719, 965, 1, 'yDqfHREthu', 905, 226, NULL);
INSERT INTO `im_user_data` VALUES ('100292', '邓晓明', 'https://img0Lbaiduhcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1995-02-06 23:57:28', '徐汇区虹桥路361号', '园艺家', 60, 438, 859, 641, 903, 1, 'h5iP6ue6RS', 895, 836, NULL);
INSERT INTO `im_user_data` VALUES ('100293', 'Han Suk Yee', 'https://img0nbaidufcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1994-10-07 13:02:06', '环区南街二巷257号', '保险销售代理', 363, 163, 360, 547, 706, 0, '6ZuE5fGUp6', 563, 739, NULL);
INSERT INTO `im_user_data` VALUES ('100294', '苏宇宁', 'https://img0.baidu0com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1990-03-03 20:24:04', '浦东新区橄榄路22号', '客户经理', 964, 54, 478, 430, 764, 1, '9K6kv8TBsB', 503, 809, NULL);
INSERT INTO `im_user_data` VALUES ('100295', 'Yuen Sze Kwan', 'https://img0KbaiduTcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1996-12-07 09:12:06', '罗湖区田贝一路323号', '社交媒体协调员', 850, 427, 835, 817, 174, 1, 'NvlyAfNYqS', 587, 423, NULL);
INSERT INTO `im_user_data` VALUES ('100296', '龚云熙', 'https://img0Kbaidu7com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1996-02-02 05:15:23', '徐汇区虹桥路377号', '食品科学家', 99, 390, 787, 355, 21, 0, 'zvtYB5rLbb', 877, 69, NULL);
INSERT INTO `im_user_data` VALUES ('100297', '高杰宏', 'https://img0Wbaiduvcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1999-08-04 17:30:03', '黄浦区淮海中路973号', '兽医助理', 340, 899, 257, 560, 185, 0, 'WGGcvNCy7Z', 517, 660, NULL);
INSERT INTO `im_user_data` VALUES ('100298', 'Pak Yu Ling', 'https://img04baiduZcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1993-03-23 22:52:50', '黄浦区淮海中路891号', '图书馆管理员', 258, 591, 374, 724, 274, 1, 'Uu52tKrbtT', 849, 188, NULL);
INSERT INTO `im_user_data` VALUES ('100299', 'Fung Chi Yuen', 'https://img0jbaidu|com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1999-06-18 03:52:23', '越秀区中山二路399号', '人力资源经理', 746, 503, 315, 383, 72, 1, 'b42XTMIl0C', 302, 348, NULL);
INSERT INTO `im_user_data` VALUES ('100300', 'Chung Lik Sun', 'https://img0YbaiduPcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1997-06-26 16:00:05', '浦东新区橄榄路634号', '零售助理', 306, 557, 630, 193, 476, 0, 'Ww7qvM6VOX', 972, 312, NULL);
INSERT INTO `im_user_data` VALUES ('100301', '武子韬', 'https://img0rbaiduUcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1995-07-02 03:23:31', '东泰五街234号', '食品科学家', 994, 559, 983, 449, 695, 1, '0x6bX7wRoC', 233, 90, NULL);
INSERT INTO `im_user_data` VALUES ('100302', '戴宇宁', 'https://img0Ybaidu3com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1996-10-24 13:09:49', '東城区東直門內大街527号', '教授', 379, 756, 632, 95, 807, 1, 'b2mGpmeNoj', 755, 865, NULL);
INSERT INTO `im_user_data` VALUES ('100303', 'Yung Wai San', 'https://img0Sbaidu\'com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1993-03-22 23:27:17', '东泰五街9号', '技术支援人员', 346, 63, 515, 856, 237, 1, 'phEdjuVAxQ', 27, 10, NULL);
INSERT INTO `im_user_data` VALUES ('100304', '吴睿', 'https://img0cbaiduFcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1992-03-08 14:47:14', '白云区小坪东路987号', '制片人', 542, 307, 647, 776, 750, 1, 'A0SjvXOCf6', 980, 59, NULL);
INSERT INTO `im_user_data` VALUES ('100305', '罗宇宁', 'https://img0.baidu<com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-03-29 04:03:51', '紫马岭商圈中山五路783号', '农夫', 509, 611, 681, 66, 410, 0, 'baPVHVrQjS', 157, 863, NULL);
INSERT INTO `im_user_data` VALUES ('100306', '钱睿', 'https://img0kbaiduKcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1991-09-24 09:18:52', '龙岗区布吉镇西环路455号', '首席运营官', 881, 532, 608, 712, 261, 0, 'SLkWjwy6Dw', 137, 244, NULL);
INSERT INTO `im_user_data` VALUES ('100307', '胡璐', 'https://img04baiduacom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1991-02-01 02:52:59', '罗湖区蔡屋围深南东路122号', '饲养员', 572, 903, 724, 970, 231, 0, 'oSMoUqEw0q', 338, 360, NULL);
INSERT INTO `im_user_data` VALUES ('100308', '尹嘉伦', 'https://img0)baidu\"com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1995-12-15 07:59:14', '浦东新区健祥路313号', '客户经理', 797, 584, 670, 475, 754, 1, 'PUowm3M9qA', 682, 298, NULL);
INSERT INTO `im_user_data` VALUES ('100309', '程子韬', 'https://img0qbaidu2com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1997-01-03 04:34:05', '福田区景田东一街26号', '人力资源经理', 209, 940, 149, 338, 722, 1, 'OY9Kq2hEsj', 500, 178, NULL);
INSERT INTO `im_user_data` VALUES ('100310', 'Kwok Wing Suen', 'https://img0tbaidurcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1995-04-27 19:38:37', '成华区二仙桥东三路775号', '数据库经理', 566, 428, 179, 346, 908, 1, 'xGiAMUDzGo', 198, 118, NULL);
INSERT INTO `im_user_data` VALUES ('100311', '姚詩涵', 'https://img0pbaiduVcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1991-04-22 14:41:08', '成华区二仙桥东三路410号', '歌手', 105, 763, 299, 202, 984, 1, 'jYv1BDbwfB', 852, 486, NULL);
INSERT INTO `im_user_data` VALUES ('100312', 'Chung Sze Kwan', 'https://img0?baidu4com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1990-05-16 11:07:56', '天河区天河路843号', '客户经理', 805, 663, 938, 143, 901, 1, 'hRh66sH5ob', 763, 623, NULL);
INSERT INTO `im_user_data` VALUES ('100313', 'Tao Fat', 'https://img0Ebaiduecom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1994-12-04 18:57:46', '罗湖区清水河一路484号', '食品科学家', 955, 239, 42, 888, 859, 1, 's7Q3WNYGvS', 496, 431, NULL);
INSERT INTO `im_user_data` VALUES ('100314', '熊宇宁', 'https://img0=baiduNcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1999-04-27 07:58:07', '乐丰六路442号', '园艺家', 244, 967, 777, 245, 303, 1, 'bL5vcXp1UL', 336, 388, NULL);
INSERT INTO `im_user_data` VALUES ('100315', '钱杰宏', 'https://img0/baiduMcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-12-23 04:03:00', '天河区天河路313号', '人力资源经理', 218, 831, 107, 528, 209, 1, '9Z1KlrjO1p', 559, 116, NULL);
INSERT INTO `im_user_data` VALUES ('100316', '冯云熙', 'https://img0abaiduxcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1999-03-14 09:43:14', '白云区机场路棠苑街五巷943号', '工程师', 526, 566, 375, 586, 588, 0, 'LY370awjgN', 631, 131, NULL);
INSERT INTO `im_user_data` VALUES ('100317', '苏岚', 'https://img0ebaiduocom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1997-10-01 09:54:19', '龙岗区学园一巷577号', '客戶協調員', 511, 306, 751, 280, 141, 0, 'EswoWIeLIZ', 301, 942, NULL);
INSERT INTO `im_user_data` VALUES ('100318', '莫璐', 'https://img0*baiduOcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1996-08-08 05:38:24', '白云区小坪东路656号', '管家', 252, 393, 888, 280, 277, 0, 'Ch8boQ51kr', 268, 174, NULL);
INSERT INTO `im_user_data` VALUES ('100319', '杨嘉伦', 'https://img0%baiduJcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1993-05-16 11:13:19', '成华区双庆路20号', '专案经理', 850, 447, 427, 920, 358, 1, 'taNJI3sakH', 902, 631, NULL);
INSERT INTO `im_user_data` VALUES ('100320', '朱嘉伦', 'https://img0wbaiduucom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1994-10-04 09:42:17', '成华区双庆路281号', '客户服务经理', 432, 718, 120, 766, 817, 0, 'yJgtieRvhB', 588, 400, NULL);
INSERT INTO `im_user_data` VALUES ('100321', 'Tam Chiu Wai', 'https://img0Xbaidu]com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1998-02-16 09:14:42', '东泰五街182号', '运营经理', 128, 805, 564, 639, 948, 0, '4g6Gtp3Vb1', 662, 583, NULL);
INSERT INTO `im_user_data` VALUES ('100322', 'Kao Sau Man', 'https://img0Pbaidu]com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1993-01-29 17:04:14', '龙岗区学园一巷112号', '管家', 934, 750, 742, 486, 618, 0, '1OsD06AzWF', 544, 910, NULL);
INSERT INTO `im_user_data` VALUES ('100323', '郝子异', 'https://img0Obaidupcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1993-01-12 01:18:01', '成华区玉双路6号973号', '团体领导', 794, 789, 120, 846, 797, 1, 'kc5ZzIv7gx', 18, 362, NULL);
INSERT INTO `im_user_data` VALUES ('100324', 'Choi Wai Han', 'https://img0@baidu\'com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1993-04-04 07:01:36', '罗湖区清水河一路5号', '信息安全分析师', 407, 987, 294, 7, 894, 1, 'NNMNWacc2Y', 815, 803, NULL);
INSERT INTO `im_user_data` VALUES ('100325', 'Lok Ho Yin', 'https://img0_baiduScom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1999-02-28 15:13:43', '天河区大信商圈大信南路981号', '医生', 64, 983, 625, 210, 560, 0, 'rboju1sBhZ', 320, 48, NULL);
INSERT INTO `im_user_data` VALUES ('100326', '杜致远', 'https://img0,baidutcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1995-04-25 14:53:50', '乐丰六路564号', '医生', 625, 352, 58, 725, 346, 0, 'rE21Vzt10z', 114, 197, NULL);
INSERT INTO `im_user_data` VALUES ('100327', '尹璐', 'https://img0kbaiduIcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1994-07-03 01:11:48', '西城区复兴门内大街812号', '人力资源经理', 752, 325, 900, 357, 82, 1, 'uMcGgMqDcO', 493, 271, NULL);
INSERT INTO `im_user_data` VALUES ('100328', '邵晓明', 'https://img0Wbaiduscom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1992-08-24 17:07:58', '浦东新区健祥路488号', '建筑师', 316, 706, 216, 857, 750, 0, 'gAOD1JNqxv', 24, 860, NULL);
INSERT INTO `im_user_data` VALUES ('100329', '谢詩涵', 'https://img0tbaiduzcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-04-12 16:55:20', '浦东新区健祥路751号', '物流协调员', 195, 31, 862, 539, 549, 1, 'HKvqWGrSie', 58, 728, NULL);
INSERT INTO `im_user_data` VALUES ('100330', 'Pak Tak Wah', 'https://img0&baiduecom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1990-07-07 23:39:30', '東城区東直門內大街602号', '审计师', 936, 516, 305, 909, 579, 0, '72E0re22xE', 252, 734, NULL);
INSERT INTO `im_user_data` VALUES ('100331', '林子韬', 'https://img0Obaidu?com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1998-08-24 06:18:55', '黄浦区淮海中路609号', '市场总监', 315, 371, 771, 896, 685, 1, 'LmbaxMVLa0', 770, 475, NULL);
INSERT INTO `im_user_data` VALUES ('100332', 'Wu Suk Yee', 'https://img0Ubaidu$com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1990-03-03 19:16:45', '天河区大信商圈大信南路364号', '专案经理', 48, 721, 706, 286, 371, 0, '14Asq5qWXw', 407, 23, NULL);
INSERT INTO `im_user_data` VALUES ('100333', '潘子韬', 'https://img0fbaidu@com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1995-08-08 11:38:53', '西城区西長安街91号', '农夫', 873, 227, 936, 76, 435, 1, 'gHHPh8oRk2', 947, 180, NULL);
INSERT INTO `im_user_data` VALUES ('100334', '武震南', 'https://img0Vbaidu5com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1995-10-17 16:24:26', '白云区机场路棠苑街五巷643号', '时装设计师', 962, 954, 166, 437, 656, 0, 'zEnJYmLvRh', 402, 381, NULL);
INSERT INTO `im_user_data` VALUES ('100335', 'Tin Chun Yu', 'https://img0_baidu>com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1992-09-26 03:11:59', '天河区大信商圈大信南路307号', '理发师', 255, 569, 112, 378, 947, 0, 'NNRImIFtbE', 918, 335, NULL);
INSERT INTO `im_user_data` VALUES ('100336', '姚晓明', 'https://img0<baiduFcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1996-02-13 16:33:50', '成华区二仙桥东三路300号', '歌手', 221, 817, 510, 722, 18, 1, 'eUeOyl2UWN', 937, 877, NULL);
INSERT INTO `im_user_data` VALUES ('100337', '严安琪', 'https://img0Ebaiduscom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1991-05-16 10:34:45', '成华区玉双路6号372号', '园艺家', 151, 426, 598, 855, 43, 1, '5xpZStLFXM', 641, 855, NULL);
INSERT INTO `im_user_data` VALUES ('100338', '方嘉伦', 'https://img0RbaiduUcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1991-10-30 11:06:40', '西城区西長安街577号', '牙齿矫正医生', 78, 329, 149, 72, 501, 1, 'Iav8HzIB6j', 631, 949, NULL);
INSERT INTO `im_user_data` VALUES ('100339', '林云熙', 'https://img0:baidu6com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '2000-01-30 02:35:11', '罗湖区田贝一路594号', '人力资源经理', 516, 333, 506, 19, 867, 1, 'DyVjoqWqpM', 559, 104, NULL);
INSERT INTO `im_user_data` VALUES ('100340', '夏岚', 'https://img0\'baiduNcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1995-06-16 04:31:36', '成华区双庆路629号', '建筑师', 293, 103, 659, 319, 1, 1, '5wiDffeaOC', 488, 802, NULL);
INSERT INTO `im_user_data` VALUES ('100341', '杨嘉伦', 'https://img0Bbaidu+com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1997-05-22 10:14:17', '白云区机场路棠苑街五巷144号', '农夫', 380, 548, 490, 937, 999, 0, '1ywZz3lZeY', 517, 760, NULL);
INSERT INTO `im_user_data` VALUES ('100342', 'Hung Ming Sze', 'https://img0Tbaidurcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1991-04-13 20:35:58', '越秀区中山二路437号', '社交媒体协调员', 826, 973, 344, 964, 606, 1, 'j2duBwPnWg', 941, 964, NULL);
INSERT INTO `im_user_data` VALUES ('100343', '毛安琪', 'https://img02baiduVcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1997-08-25 00:04:24', '黄浦区淮海中路72号', '教授', 353, 189, 674, 398, 483, 0, 'raymnz7KGr', 405, 471, NULL);
INSERT INTO `im_user_data` VALUES ('100344', 'Heung Tin Wing', 'https://img0mbaidu5com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1991-10-19 22:01:45', '东泰五街515号', '医生', 42, 393, 304, 234, 414, 0, '1GNyHH39rT', 843, 473, NULL);
INSERT INTO `im_user_data` VALUES ('100345', 'Miu Sze Kwan', 'https://img0?baidu-com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1994-03-18 15:45:55', '福田区景田东一街781号', '园艺家', 799, 942, 100, 552, 896, 0, 'uPEbiUqpcJ', 149, 161, NULL);
INSERT INTO `im_user_data` VALUES ('100346', 'Chic Hui Mei', 'https://img0Ubaidu-com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1997-04-18 03:52:03', '龙岗区学园一巷268号', '制片人', 765, 519, 255, 263, 426, 1, 'j44JYCbKRu', 725, 497, NULL);
INSERT INTO `im_user_data` VALUES ('100347', '沈安琪', 'https://img0Vbaidu^com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1996-07-29 18:19:48', '罗湖区清水河一路523号', '化妆师', 871, 498, 824, 563, 843, 1, 'pf5BAYpB4x', 55, 56, NULL);
INSERT INTO `im_user_data` VALUES ('100348', '丁詩涵', 'https://img0DbaiduXcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1998-01-08 20:20:21', '坑美十五巷680号', '婚礼筹办者', 937, 978, 843, 837, 117, 1, 'ihetvcwiBr', 543, 791, NULL);
INSERT INTO `im_user_data` VALUES ('100349', '卢震南', 'https://img0\'baidu;com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1991-11-23 21:39:18', '成华区玉双路6号593号', '纹身艺术家', 31, 779, 584, 648, 355, 1, 'qYoVZ70gyJ', 102, 37, NULL);
INSERT INTO `im_user_data` VALUES ('100350', 'Yuen Yu Ling', 'https://img0]baidu*com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1999-04-12 04:02:13', '浦东新区健祥路211号', '婚礼筹办者', 673, 130, 705, 274, 657, 1, 'bRhFGJS3lK', 453, 960, NULL);
INSERT INTO `im_user_data` VALUES ('100351', '唐子韬', 'https://img0wbaidu*com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1997-01-23 15:52:13', '东城区东单王府井东街69号', '多媒体动画师', 372, 668, 951, 379, 19, 1, '05iHI8Tyjo', 691, 786, NULL);
INSERT INTO `im_user_data` VALUES ('100352', '石睿', 'https://img0obaidu]com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1998-09-12 05:41:20', '东泰五街77号', '客戶協調員', 494, 643, 431, 635, 194, 0, '5xNcTjj2Zd', 719, 948, NULL);
INSERT INTO `im_user_data` VALUES ('100353', '段安琪', 'https://img0Sbaidu~com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1993-09-26 00:45:39', '房山区岳琉路583号', '作家', 671, 404, 509, 547, 130, 0, 'Duryxr5xjy', 139, 905, NULL);
INSERT INTO `im_user_data` VALUES ('100354', '毛宇宁', 'https://img0\"baidu\\com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1998-06-22 21:09:46', '越秀区中山二路307号', '老师', 15, 79, 635, 568, 471, 0, 'ZBbbv5CpA0', 500, 242, NULL);
INSERT INTO `im_user_data` VALUES ('100355', '苏云熙', 'https://img0jbaidu[com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1999-12-09 21:09:23', '海珠区江南西路766号', '水疗经理', 71, 752, 806, 931, 111, 1, 'UPLIXY4j47', 387, 172, NULL);
INSERT INTO `im_user_data` VALUES ('100356', '史致远', 'https://img0fbaidukcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1992-10-29 01:46:11', '延庆区028县道639号', '保险销售代理', 182, 862, 77, 953, 671, 0, 'Whx0FDTOmX', 310, 775, NULL);
INSERT INTO `im_user_data` VALUES ('100357', 'Yeow Tsz Ching', 'https://img0DbaiduTcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1993-01-06 00:20:18', '罗湖区清水河一路243号', '图象设计师', 974, 235, 240, 39, 999, 0, '06dTPB7YW3', 621, 354, NULL);
INSERT INTO `im_user_data` VALUES ('100358', '许睿', 'https://img0@baidumcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1992-05-16 10:55:36', '乐丰六路286号', '食品科学家', 125, 622, 966, 883, 199, 1, 'RAhpLR6kdu', 482, 462, NULL);
INSERT INTO `im_user_data` VALUES ('100359', 'Chin Yu Ling', 'https://img04baidu|com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1993-10-01 15:48:28', '房山区岳琉路721号', '多媒体动画师', 846, 568, 571, 391, 820, 0, 'jmiY5ZKoUE', 842, 985, NULL);
INSERT INTO `im_user_data` VALUES ('100360', 'Chow Kwok Yin', 'https://img0ZbaiduVcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1995-12-06 14:47:35', '西城区西長安街991号', '演员', 696, 31, 390, 730, 560, 1, 'uQzJFc7y88', 287, 696, NULL);
INSERT INTO `im_user_data` VALUES ('100361', '刘璐', 'https://img0>baidu com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1999-04-04 08:32:32', '成华区双庆路848号', '私人教练', 67, 301, 980, 266, 43, 1, 'rZAmVFV6Ix', 668, 328, NULL);
INSERT INTO `im_user_data` VALUES ('100362', 'Pang Ho Yin', 'https://img0\"baiduwcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1996-06-30 08:00:49', '罗湖区蔡屋围深南东路920号', 'UX / UI设计员', 524, 254, 816, 762, 928, 1, 'AEXibBEffG', 34, 467, NULL);
INSERT INTO `im_user_data` VALUES ('100363', 'Han Ching Wan', 'https://img02baiduFcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1995-01-03 07:05:09', '浦东新区橄榄路565号', '客戶協調員', 927, 311, 249, 540, 826, 0, '3sU3h7zHDI', 104, 600, NULL);
INSERT INTO `im_user_data` VALUES ('100364', 'Tsui Yun Fat', 'https://img0(baiduScom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1998-12-04 20:49:59', '房山区岳琉路402号', '作家', 168, 571, 702, 23, 839, 0, 'wzkiTyCrd1', 287, 16, NULL);
INSERT INTO `im_user_data` VALUES ('100365', 'Hui Kwok Ming', 'https://img0|baidu(com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1991-12-06 14:51:03', '罗湖区田贝一路861号', '饲养员', 92, 102, 545, 925, 745, 0, 'MobhXJijDp', 274, 585, NULL);
INSERT INTO `im_user_data` VALUES ('100366', '雷璐', 'https://img0pbaidu?com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1992-02-24 18:58:04', '成华区玉双路6号242号', '物流协调员', 944, 649, 665, 263, 919, 1, 'nle3deIacQ', 403, 75, NULL);
INSERT INTO `im_user_data` VALUES ('100367', 'Ng Kwok Ming', 'https://img0lbaiduJcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1996-11-12 12:02:20', '天河区大信商圈大信南路941号', '团体领导', 338, 678, 166, 80, 689, 1, 'XlUOm7qLYA', 657, 751, NULL);
INSERT INTO `im_user_data` VALUES ('100368', 'Kwok Kwok Kuen', 'https://img0fbaidu>com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1994-11-09 22:52:52', '环区南街二巷371号', '客戶協調員', 434, 929, 536, 86, 675, 1, '4ryCvm9loJ', 365, 238, NULL);
INSERT INTO `im_user_data` VALUES ('100369', '黄云熙', 'https://img0>baidu/com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1994-05-11 16:07:51', '锦江区红星路三段724号', '水疗经理', 778, 917, 185, 473, 17, 0, 'AS0sl7tot2', 484, 169, NULL);
INSERT INTO `im_user_data` VALUES ('100370', '韩岚', 'https://img0xbaiduzcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1995-01-18 21:39:07', '闵行区宾川路173号', '图象设计师', 493, 249, 865, 330, 889, 0, 'Ipf7f1XHgg', 126, 942, NULL);
INSERT INTO `im_user_data` VALUES ('100371', '雷震南', 'https://img0Gbaidu=com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1994-04-20 18:00:01', '福田区深南大道174号', '数据库经理', 679, 885, 235, 397, 906, 1, 'IPhm7ahciX', 810, 776, NULL);
INSERT INTO `im_user_data` VALUES ('100372', '傅璐', 'https://img03baiduUcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1999-05-03 05:05:42', '环区南街二巷952号', 'UX / UI设计员', 797, 396, 820, 919, 612, 0, 'X0KmWI4GTy', 120, 606, NULL);
INSERT INTO `im_user_data` VALUES ('100373', '罗子异', 'https://img0cbaiduecom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1991-07-26 00:43:49', '白云区小坪东路140号', '软件开发员', 135, 66, 563, 242, 626, 0, '816hLB1PfA', 276, 329, NULL);
INSERT INTO `im_user_data` VALUES ('100374', 'Tong Sai Wing', 'https://img0}baidu com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1997-11-11 04:38:36', '天河区大信商圈大信南路457号', '农夫', 365, 192, 287, 293, 835, 1, 'is8adtvDOy', 544, 675, NULL);
INSERT INTO `im_user_data` VALUES ('100375', 'Chan Chi Yuen', 'https://img0ubaidutcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1996-08-07 15:19:48', '福田区深南大道880号', '保险销售代理', 41, 821, 962, 852, 326, 0, 'IEErriNfWo', 278, 885, NULL);
INSERT INTO `im_user_data` VALUES ('100376', '姚睿', 'https://img00baidujcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1996-06-18 21:32:25', '锦江区红星路三段359号', '歌手', 481, 120, 200, 263, 97, 1, 'FXew5DFbgQ', 508, 194, NULL);
INSERT INTO `im_user_data` VALUES ('100377', '蔡震南', 'https://img0sbaidu\"com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1990-05-28 09:30:15', '浦东新区健祥路156号', '私人教练', 401, 416, 313, 55, 641, 1, 'WgqwtGfdk6', 866, 674, NULL);
INSERT INTO `im_user_data` VALUES ('100378', '陆安琪', 'https://img0sbaiduPcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1996-04-07 00:10:45', '黄浦区淮海中路601号', '食品科学家', 938, 818, 953, 147, 140, 0, 'RZEdToV5vk', 632, 497, NULL);
INSERT INTO `im_user_data` VALUES ('100379', 'Che Tin Lok', 'https://img0fbaiduocom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1996-06-20 11:12:46', '东泰五街491号', '客户经理', 541, 439, 802, 78, 329, 0, '6e08CbBpi4', 773, 388, NULL);
INSERT INTO `im_user_data` VALUES ('100380', 'Tang Ka Keung', 'https://img0`baiduZcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1990-09-16 23:35:23', '珊瑚路41号', '人力资源经理', 202, 946, 960, 43, 89, 0, 'DtOo3IQ9NB', 195, 779, NULL);
INSERT INTO `im_user_data` VALUES ('100381', 'Cheung Ting Fung', 'https://img0Kbaidu;com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1999-09-09 21:31:41', '福田区景田东一街714号', '牙医', 561, 963, 143, 555, 588, 0, 'xAYA1zJDC8', 779, 530, NULL);
INSERT INTO `im_user_data` VALUES ('100382', 'Lo Wai San', 'https://img0$baidukcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1995-08-19 05:39:07', '锦江区人民南路四段275号', '理发师', 580, 53, 267, 991, 840, 0, 'NvwVp4eMlL', 209, 749, NULL);
INSERT INTO `im_user_data` VALUES ('100383', '高震南', 'https://img04baiduUcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1992-04-12 17:42:34', '越秀区中山二路712号', '精算师', 741, 888, 749, 611, 537, 0, 'H4IuRw98Nl', 15, 984, NULL);
INSERT INTO `im_user_data` VALUES ('100384', '谭睿', 'https://img0;baidu,com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1997-07-25 17:36:41', '成华区二仙桥东三路862号', '移动应用程式开发人员', 861, 102, 356, 551, 411, 1, 'qBqfhLjzc9', 786, 80, NULL);
INSERT INTO `im_user_data` VALUES ('100385', '胡安琪', 'https://img0pbaiduVcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-05-03 15:39:42', '東城区東直門內大街18号', '市场总监', 595, 368, 87, 396, 495, 1, 'UqFhxzxJab', 373, 685, NULL);
INSERT INTO `im_user_data` VALUES ('100386', 'Chin Wai Yee', 'https://img0Zbaidu/com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1999-10-08 19:27:47', '朝阳区三里屯路341号', '移动应用程式开发人员', 421, 412, 662, 133, 707, 0, 'HyHAmaKM03', 324, 740, NULL);
INSERT INTO `im_user_data` VALUES ('100387', '范安琪', 'https://img0Ebaidu0com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1997-03-15 20:28:47', '房山区岳琉路329号', '纹身艺术家', 21, 239, 70, 499, 188, 1, '6x2GifUAoo', 16, 885, NULL);
INSERT INTO `im_user_data` VALUES ('100388', 'Sit Lik Sun', 'https://img0cbaiduNcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-12-17 02:17:29', '闵行区宾川路835号', '市场总监', 642, 27, 249, 294, 437, 1, '8YlFMIkQ8L', 318, 251, NULL);
INSERT INTO `im_user_data` VALUES ('100389', 'Mak Kar Yan', 'https://img0=baidu[com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1995-06-29 21:10:32', '天河区天河路383号', '美容师', 976, 150, 730, 947, 652, 0, '9qWiW9VF8f', 704, 503, NULL);
INSERT INTO `im_user_data` VALUES ('100390', 'Miu Tsz Ching', 'https://img0tbaidubcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1993-07-07 02:18:34', '罗湖区清水河一路514号', '牙齿矫正医生', 871, 419, 807, 72, 498, 1, 'UXYS8EUUUu', 600, 567, NULL);
INSERT INTO `im_user_data` VALUES ('100391', 'Yuen Ching Wan', 'https://img0obaidu~com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1990-09-02 04:48:16', '越秀区中山二路665号', '化妆师', 302, 882, 65, 8, 602, 1, 'sjtSzov5Ma', 263, 390, NULL);
INSERT INTO `im_user_data` VALUES ('100392', 'Koon Wai Han', 'https://img0\"baidulcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1995-08-23 22:57:34', '成华区双庆路923号', '物流协调员', 946, 59, 705, 375, 924, 1, 'mReT8yatl4', 766, 137, NULL);
INSERT INTO `im_user_data` VALUES ('100393', 'Kam Ho Yin', 'https://img0MbaiduMcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1990-04-09 19:24:18', '珊瑚路741号', '食品科学家', 470, 984, 657, 69, 305, 1, 'vFecVynYIn', 221, 862, NULL);
INSERT INTO `im_user_data` VALUES ('100394', '徐晓明', 'https://img00baiduNcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1997-03-27 20:13:55', '西城区西長安街694号', '兽医', 499, 401, 199, 526, 343, 0, '32lUYFjNuL', 125, 999, NULL);
INSERT INTO `im_user_data` VALUES ('100395', 'Tse Ming Sze', 'https://img0dbaiduIcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-01-11 13:09:04', '龙岗区布吉镇西环路401号', '社交媒体协调员', 274, 279, 409, 55, 822, 1, 'QVNUXc0sj4', 64, 647, NULL);
INSERT INTO `im_user_data` VALUES ('100396', 'Yung Wai Lam', 'https://img0jbaiduPcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1992-02-21 06:47:49', '东城区东单王府井东街59号', '歌手', 893, 91, 292, 905, 334, 1, 'JqJx4iT84H', 129, 230, NULL);
INSERT INTO `im_user_data` VALUES ('100397', '郭云熙', 'https://img0{baiduscom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1996-03-16 23:09:31', '延庆区028县道686号', '药剂师', 812, 339, 763, 261, 106, 0, 'JVsXo7fH9g', 974, 848, NULL);
INSERT INTO `im_user_data` VALUES ('100398', '贾子韬', 'https://img0 baidu5com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1990-12-05 04:03:27', '罗湖区蔡屋围深南东路155号', '零售助理', 769, 786, 690, 125, 161, 1, 'Y0fOKqVOIz', 355, 767, NULL);
INSERT INTO `im_user_data` VALUES ('100399', '许宇宁', 'https://img03baiduCcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1995-11-15 13:00:12', '天河区大信商圈大信南路889号', '药剂师', 20, 565, 958, 448, 934, 0, 'm5hTmWkIG0', 52, 289, NULL);
INSERT INTO `im_user_data` VALUES ('100400', '阎璐', 'https://img0mbaiduUcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1994-01-04 17:02:46', '東城区東直門內大街461号', '私人教练', 14, 93, 218, 203, 793, 1, 'kMQ0Qgdhm6', 941, 445, NULL);
INSERT INTO `im_user_data` VALUES ('100401', 'Leung On Kay', 'https://img0+baidu$com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1995-02-18 06:32:22', '龙岗区布吉镇西环路116号', '老师', 881, 684, 200, 669, 429, 1, 'NBVT9KzwS8', 745, 716, NULL);
INSERT INTO `im_user_data` VALUES ('100402', 'Yung Chieh Lun', 'https://img0;baiduacom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1990-03-07 15:19:14', '徐汇区虹桥路240号', '数据库经理', 160, 208, 387, 855, 976, 0, 'HMUELHk89j', 555, 716, NULL);
INSERT INTO `im_user_data` VALUES ('100403', '田子异', 'https://img0%baiduScom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1997-09-10 09:37:29', '西城区复兴门内大街866号', '销售经理', 939, 889, 644, 305, 111, 1, 'FuOtLuHDsA', 223, 695, NULL);
INSERT INTO `im_user_data` VALUES ('100404', '潘震南', 'https://img0+baidu1com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1996-11-04 20:25:18', '锦江区人民南路四段293号', '多媒体动画师', 862, 310, 900, 341, 895, 0, 'v97vR47jZi', 789, 81, NULL);
INSERT INTO `im_user_data` VALUES ('100405', 'Heung Ting Fung', 'https://img0#baiduLcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1995-07-06 05:33:48', '成华区玉双路6号869号', '网页开发人员', 898, 982, 826, 725, 701, 1, 'pXeRhwB0BM', 17, 705, NULL);
INSERT INTO `im_user_data` VALUES ('100406', 'Fong Tsz Hin', 'https://img0`baidu com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1992-12-03 02:36:21', '罗湖区田贝一路771号', '运营经理', 355, 945, 44, 557, 718, 0, 'VI2OrgMoMV', 846, 135, NULL);
INSERT INTO `im_user_data` VALUES ('100407', 'Kwong Suk Yee', 'https://img0-baidu`com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1991-04-14 16:52:06', '珊瑚路550号', '专案经理', 179, 867, 820, 146, 529, 1, 'SLKy4mjS5l', 391, 367, NULL);
INSERT INTO `im_user_data` VALUES ('100408', '王睿', 'https://img0vbaiduRcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1994-09-17 23:09:53', '锦江区人民南路四段355号', '水疗经理', 227, 871, 25, 94, 728, 1, 'jLHbhtNn7n', 150, 219, NULL);
INSERT INTO `im_user_data` VALUES ('100409', 'Wong Lik Sun', 'https://img08baidu_com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-01-10 13:10:40', '房山区岳琉路786号', '社交媒体协调员', 860, 897, 317, 295, 200, 1, 'OCmTaeZX5p', 227, 732, NULL);
INSERT INTO `im_user_data` VALUES ('100410', 'Lui Wing Sze', 'https://img0obaidurcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1999-04-03 22:58:46', '坑美十五巷449号', '水疗经理', 52, 712, 984, 898, 481, 0, 'hMWE4eBG8r', 958, 788, NULL);
INSERT INTO `im_user_data` VALUES ('100411', 'Ying Chieh Lun', 'https://img0+baidu/com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1991-04-22 20:48:26', '锦江区红星路三段892号', '办公室主管', 107, 85, 504, 714, 452, 1, '4O81vUBxfH', 95, 7, NULL);
INSERT INTO `im_user_data` VALUES ('100412', 'Mak Wing Sze', 'https://img0_baidu1com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1996-03-29 02:16:25', '成华区二仙桥东三路670号', '建筑师', 648, 390, 5, 529, 867, 1, 'thZJx0jzU5', 111, 888, NULL);
INSERT INTO `im_user_data` VALUES ('100413', 'Wong Suk Yee', 'https://img0ibaidu7com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-12-09 11:41:33', '西城区西長安街981号', '老师', 615, 797, 76, 269, 660, 0, 'iKzZ7zGPVU', 790, 507, NULL);
INSERT INTO `im_user_data` VALUES ('100414', '宋睿', 'https://img0*baiduZcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1991-07-03 14:08:21', '越秀区中山二路135号', '客户服务经理', 777, 479, 953, 706, 892, 0, 'qKaEV4Jpuy', 789, 310, NULL);
INSERT INTO `im_user_data` VALUES ('100415', '汤震南', 'https://img0Tbaiduacom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1999-07-02 12:51:53', '海淀区清河中街68号344号', '农夫', 754, 421, 459, 935, 592, 1, 'miGtvbiGp7', 774, 578, NULL);
INSERT INTO `im_user_data` VALUES ('100416', '严致远', 'https://img0Kbaidu@com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1998-09-25 07:59:59', '坑美十五巷243号', '兽医助理', 1, 522, 340, 269, 616, 1, 'Vb7jS1FrhL', 790, 361, NULL);
INSERT INTO `im_user_data` VALUES ('100417', 'Cheng Tin Wing', 'https://img0bbaiduIcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1995-02-24 16:14:23', '浦东新区橄榄路602号', '软件开发员', 323, 985, 782, 527, 853, 1, '6OSJTIXHrf', 387, 326, NULL);
INSERT INTO `im_user_data` VALUES ('100418', 'To Chun Yu', 'https://img0kbaidu+com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1992-08-16 12:25:30', '房山区岳琉路623号', '精算师', 467, 250, 895, 701, 504, 1, 'MraxFId49Q', 823, 640, NULL);
INSERT INTO `im_user_data` VALUES ('100419', 'Hsuan Fat', 'https://img02baidufcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1992-08-26 12:07:11', '白云区小坪东路747号', '歌手', 842, 505, 695, 109, 172, 1, 'XC3aMjVevZ', 34, 790, NULL);
INSERT INTO `im_user_data` VALUES ('100420', 'Sheh Ho Yin', 'https://img0MbaiduEcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1992-05-22 04:31:50', '龙岗区学园一巷582号', '理发师', 952, 746, 994, 643, 129, 1, 'g4nF3svAa3', 452, 851, NULL);
INSERT INTO `im_user_data` VALUES ('100421', 'Fong Wing Sze', 'https://img0|baidu\\com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1998-03-23 07:20:04', '东泰五街321号', '精算师', 266, 113, 220, 483, 781, 0, 'kqKj06Rqjk', 939, 279, NULL);
INSERT INTO `im_user_data` VALUES ('100422', 'So Sau Man', 'https://img01baiduUcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1994-01-21 18:03:04', '福田区景田东一街541号', '人力资源经理', 222, 387, 886, 535, 389, 1, 'QhYuc4S76e', 75, 891, NULL);
INSERT INTO `im_user_data` VALUES ('100423', '蒋子韬', 'https://img0)baidu%com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1995-02-26 20:55:02', '京华商圈华夏街269号', '私人教练', 417, 414, 990, 254, 469, 1, '1kgdOJGs52', 608, 183, NULL);
INSERT INTO `im_user_data` VALUES ('100424', '薛震南', 'https://img0ibaidu\'com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1995-09-08 08:19:34', '白云区小坪东路530号', '商务记者', 589, 72, 191, 411, 345, 0, 'GenWjwiy6T', 776, 138, NULL);
INSERT INTO `im_user_data` VALUES ('100425', '丁震南', 'https://img0&baidu$com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1993-10-30 02:02:43', '成华区二仙桥东三路874号', '水疗经理', 22, 754, 202, 324, 863, 1, 'om6J6ekVmG', 256, 484, NULL);
INSERT INTO `im_user_data` VALUES ('100426', 'Tang Chun Yu', 'https://img0;baidu,com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1998-07-22 01:06:07', '坑美十五巷421号', '美容师', 176, 707, 417, 926, 214, 0, 'xJf6uGBTXj', 568, 53, NULL);
INSERT INTO `im_user_data` VALUES ('100427', 'Koon Suk Yee', 'https://img03baiduLcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1997-05-10 10:40:34', '東城区東直門內大街611号', '信息安全分析师', 34, 641, 692, 445, 935, 1, 'vxGqh2AzdJ', 830, 419, NULL);
INSERT INTO `im_user_data` VALUES ('100428', 'Yau Wing Fat', 'https://img0nbaidu&com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1998-08-31 00:03:19', '锦江区红星路三段984号', '客户服务经理', 111, 28, 572, 689, 327, 0, 'cDeHY1Ul2B', 199, 784, NULL);
INSERT INTO `im_user_data` VALUES ('100429', 'Yuen Ting Fung', 'https://img0pbaiduncom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1990-04-22 09:23:38', '锦江区红星路三段377号', '物流协调员', 942, 758, 483, 253, 438, 0, '45OnFDxbWP', 932, 557, NULL);
INSERT INTO `im_user_data` VALUES ('100430', 'Sit Ming Sze', 'https://img0Qbaidu5com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1992-04-02 12:14:32', '龙岗区学园一巷550号', '首席运营官', 46, 301, 506, 80, 284, 1, 'hVdjQkbm2Z', 626, 753, NULL);
INSERT INTO `im_user_data` VALUES ('100431', 'Wong Kar Yan', 'https://img0Nbaidu{com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1992-01-21 06:32:32', '成华区玉双路6号111号', '生物化学家', 520, 707, 131, 918, 925, 1, 'CTSqLxM3vn', 948, 218, NULL);
INSERT INTO `im_user_data` VALUES ('100432', 'Lee Wai Lam', 'https://img0Rbaidubcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1998-03-12 07:29:51', '浦东新区橄榄路695号', '护士', 821, 275, 935, 243, 717, 0, 'FqWwOXNtrd', 277, 322, NULL);
INSERT INTO `im_user_data` VALUES ('100433', 'To Lik Sun', 'https://img09baidu-com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1997-02-28 18:17:38', '东城区东单王府井东街871号', '活动经理', 397, 466, 324, 584, 192, 0, 'Stu63A7H5A', 903, 674, NULL);
INSERT INTO `im_user_data` VALUES ('100434', '李晓明', 'https://img0Obaidutcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1992-06-09 16:05:05', '罗湖区清水河一路426号', '客户服务经理', 615, 954, 957, 459, 282, 1, 'vbNfZbveaG', 472, 42, NULL);
INSERT INTO `im_user_data` VALUES ('100435', 'Fung Ling Ling', 'https://img0Ybaidulcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1992-02-04 14:41:34', '海淀区清河中街68号632号', '建筑师', 843, 14, 21, 753, 851, 0, 'kXSo9rq3ST', 13, 338, NULL);
INSERT INTO `im_user_data` VALUES ('100436', 'Wan Siu Wai', 'https://img0@baidu?com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1998-10-21 23:13:18', '朝阳区三里屯路670号', '客戶協調員', 867, 248, 836, 191, 144, 1, 'SNfp3qnkKz', 14, 377, NULL);
INSERT INTO `im_user_data` VALUES ('100437', '雷子异', 'https://img0.baidu]com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1997-07-17 04:47:52', '锦江区人民南路四段911号', '销售经理', 915, 556, 431, 705, 432, 1, 'yg3dSgGyes', 849, 885, NULL);
INSERT INTO `im_user_data` VALUES ('100438', 'Chung Ka Fai', 'https://img0fbaidu^com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1990-02-11 00:00:58', '越秀区中山二路440号', '牙齿矫正医生', 227, 526, 59, 512, 289, 1, '63hrjzQqqF', 603, 669, NULL);
INSERT INTO `im_user_data` VALUES ('100439', '王致远', 'https://img0Abaidu?com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1991-09-20 09:08:45', '海珠区江南西路479号', '水疗经理', 519, 433, 146, 797, 825, 1, 'kCYKC9NH9D', 929, 269, NULL);
INSERT INTO `im_user_data` VALUES ('100440', 'Heung Kwok Ming', 'https://img0obaiduWcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1996-06-01 16:08:20', '成华区双庆路889号', '客户服务经理', 391, 586, 197, 914, 736, 1, 'w1CjBvBcly', 386, 742, NULL);
INSERT INTO `im_user_data` VALUES ('100441', '萧震南', 'https://img0ebaiduGcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1990-11-05 14:51:11', '延庆区028县道936号', '多媒体动画师', 416, 476, 174, 608, 518, 0, 'P0fZOsf2n1', 616, 720, NULL);
INSERT INTO `im_user_data` VALUES ('100442', '田震南', 'https://img0Gbaiduacom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1990-05-18 15:18:07', '闵行区宾川路942号', '销售经理', 356, 553, 547, 446, 942, 1, 'DBE0tC5qzj', 139, 464, NULL);
INSERT INTO `im_user_data` VALUES ('100443', 'Sheh Chung Yin', 'https://img0QbaiduRcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1991-06-27 20:12:18', '海珠区江南西路926号', '精算师', 141, 802, 887, 560, 390, 1, 'VhJMqGX0gT', 818, 322, NULL);
INSERT INTO `im_user_data` VALUES ('100444', '江嘉伦', 'https://img0Sbaidugcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1996-04-28 18:38:00', '锦江区人民南路四段575号', '人力资源经理', 748, 583, 581, 365, 422, 1, 'qReUwaWQMH', 145, 304, NULL);
INSERT INTO `im_user_data` VALUES ('100445', '高安琪', 'https://img0,baidu.com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1999-02-20 07:02:26', '罗湖区蔡屋围深南东路547号', '技术支援人员', 525, 855, 28, 484, 719, 0, 'llbFgjqbAQ', 348, 222, NULL);
INSERT INTO `im_user_data` VALUES ('100446', '苏杰宏', 'https://img0wbaidu{com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1999-01-05 07:48:21', '福田区深南大道708号', '导师', 395, 413, 831, 541, 608, 0, 'gSydBxtfd8', 385, 400, NULL);
INSERT INTO `im_user_data` VALUES ('100447', 'Yin Wing Kuen', 'https://img0<baidu]com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '2000-01-23 11:09:33', '白云区机场路棠苑街五巷504号', '园艺家', 822, 829, 264, 505, 353, 1, 'ygfu5B2cta', 498, 308, NULL);
INSERT INTO `im_user_data` VALUES ('100448', 'Yung Tin Lok', 'https://img0\"baidu7com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1992-01-09 16:58:00', '罗湖区蔡屋围深南东路797号', '生物化学家', 966, 173, 448, 638, 216, 1, 'C5HKBuodt4', 553, 430, NULL);
INSERT INTO `im_user_data` VALUES ('100449', 'Yau Ming Sze', 'https://img0Kbaidu\"com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1997-06-28 21:21:04', '罗湖区清水河一路436号', '作家', 223, 584, 933, 685, 405, 0, '0qq2234OD1', 723, 911, NULL);
INSERT INTO `im_user_data` VALUES ('100450', 'Fu Lik Sun', 'https://img0<baiduIcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1994-04-27 18:16:26', '西城区西長安街977号', '图书馆管理员', 917, 511, 218, 25, 243, 1, 'N1T0AMO7C1', 243, 367, NULL);
INSERT INTO `im_user_data` VALUES ('100451', 'Ng Wai Han', 'https://img0JbaiduIcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1993-08-29 00:11:32', '东泰五街345号', '办公室文员', 392, 372, 258, 195, 626, 1, 'HYlOZFL9rX', 313, 981, NULL);
INSERT INTO `im_user_data` VALUES ('100452', '郝宇宁', 'https://img01baidu1com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1994-09-18 03:54:10', '西城区西長安街38号', '运营经理', 583, 289, 224, 662, 320, 1, 'XrQlvoMK56', 845, 749, NULL);
INSERT INTO `im_user_data` VALUES ('100453', '吕璐', 'https://img0rbaiduocom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1993-04-07 12:51:20', '福田区深南大道1号', '移动应用程式开发人员', 988, 858, 568, 538, 359, 1, 'YFhybYPjl3', 542, 879, NULL);
INSERT INTO `im_user_data` VALUES ('100454', 'Chin Ming', 'https://img0(baidu!com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1994-09-13 05:34:44', '罗湖区蔡屋围深南东路425号', '零售助理', 934, 824, 588, 534, 351, 1, 'zLti8iUpX6', 638, 126, NULL);
INSERT INTO `im_user_data` VALUES ('100455', '江睿', 'https://img0dbaidu-com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-08-27 03:41:29', '闵行区宾川路760号', '图书馆管理员', 278, 80, 25, 448, 160, 0, 'dFIMepcbJW', 346, 864, NULL);
INSERT INTO `im_user_data` VALUES ('100456', 'Wong Chi Yuen', 'https://img0pbaiduxcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1995-02-02 15:01:50', '天河区天河路730号', '市场总监', 481, 398, 514, 589, 430, 0, 'QJa2csh9Pd', 312, 600, NULL);
INSERT INTO `im_user_data` VALUES ('100457', '金璐', 'https://img0-baidu*com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1999-12-06 21:46:24', '海珠区江南西路921号', '零售助理', 604, 130, 703, 228, 678, 1, 'bnKMts8qsz', 518, 822, NULL);
INSERT INTO `im_user_data` VALUES ('100458', 'Ku Kar Yan', 'https://img0bbaidu]com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1997-07-10 13:06:03', '朝阳区三里屯路595号', '审计师', 408, 509, 803, 721, 563, 0, 'Rwrrj4mYSq', 227, 548, NULL);
INSERT INTO `im_user_data` VALUES ('100459', 'Ku Sau Man', 'https://img0_baidu-com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1998-01-26 12:06:42', '锦江区红星路三段608号', '财务分析师', 271, 448, 261, 629, 381, 1, 'JaA0EkHK40', 744, 483, NULL);
INSERT INTO `im_user_data` VALUES ('100460', 'Ying Ming Sze', 'https://img0Abaidu-com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1991-01-07 15:56:28', '珊瑚路357号', '客戶協調員', 433, 149, 263, 212, 390, 0, 'bU4cEotviO', 381, 238, NULL);
INSERT INTO `im_user_data` VALUES ('100461', 'Kam Kwok Yin', 'https://img0+baidu5com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1993-10-17 22:22:08', '海珠区江南西路452号', '客户经理', 141, 821, 801, 406, 629, 1, 'xQhPffyMKE', 569, 436, NULL);
INSERT INTO `im_user_data` VALUES ('100462', 'Tsang Ka Man', 'https://img0Abaidulcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1992-10-15 15:55:31', '罗湖区田贝一路497号', '图象设计师', 802, 278, 738, 689, 393, 0, '81V5jK2aXi', 172, 184, NULL);
INSERT INTO `im_user_data` VALUES ('100463', '于晓明', 'https://img0`baidu}com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1991-08-07 10:06:13', '越秀区中山二路259号', '网页开发人员', 177, 291, 839, 570, 198, 1, 'ZfZiGEviJe', 246, 615, NULL);
INSERT INTO `im_user_data` VALUES ('100464', '谭杰宏', 'https://img0Wbaidumcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1997-02-18 07:11:35', '東城区東直門內大街611号', '制片人', 51, 429, 227, 350, 978, 1, 'FrH2VJvs64', 542, 611, NULL);
INSERT INTO `im_user_data` VALUES ('100465', 'Yuen Ling Ling', 'https://img05baidu`com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1995-07-31 03:39:57', '成华区玉双路6号893号', '团体领导', 521, 237, 143, 484, 27, 1, 'qTw25ZiYbe', 641, 403, NULL);
INSERT INTO `im_user_data` VALUES ('100466', 'Hsuan Kwok Wing', 'https://img0<baiduecom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1994-08-13 02:30:55', '天河区大信商圈大信南路592号', '办公室主管', 110, 957, 269, 661, 390, 0, 'ImtZVSrOlS', 787, 220, NULL);
INSERT INTO `im_user_data` VALUES ('100467', '汤晓明', 'https://img04baidu%com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1998-03-18 08:54:37', '龙岗区布吉镇西环路13号', '婚礼筹办者', 365, 706, 712, 90, 188, 0, 'Bu815kN923', 769, 481, NULL);
INSERT INTO `im_user_data` VALUES ('100468', '向嘉伦', 'https://img0xbaiduHcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1995-10-11 03:02:27', '环区南街二巷714号', '客户经理', 111, 821, 293, 991, 529, 0, 'Vbonhq66wb', 573, 569, NULL);
INSERT INTO `im_user_data` VALUES ('100469', 'Tang Tin Wing', 'https://img0ibaiduYcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1991-02-12 07:08:33', '成华区二仙桥东三路580号', '私人教练', 150, 425, 135, 683, 967, 1, 'NSIOJlIRUd', 332, 316, NULL);
INSERT INTO `im_user_data` VALUES ('100470', 'Choi Yun Fat', 'https://img0~baiduocom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1993-06-23 02:47:32', '成华区双庆路732号', '管家', 866, 863, 27, 76, 480, 0, 'FP7kdFuhey', 476, 975, NULL);
INSERT INTO `im_user_data` VALUES ('100471', 'Yin Kwok Wing', 'https://img0*baidu0com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1999-09-06 22:10:36', '東城区東直門內大街181号', '园艺家', 980, 563, 429, 365, 732, 1, 'lyWf4RMpsp', 615, 986, NULL);
INSERT INTO `im_user_data` VALUES ('100472', '廖致远', 'https://img0^baiduGcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1994-03-30 23:52:08', '西城区复兴门内大街367号', '社交媒体协调员', 204, 394, 14, 576, 887, 1, 'rhrvnbWAVA', 79, 959, NULL);
INSERT INTO `im_user_data` VALUES ('100473', 'Yue Wai Man', 'https://img0Qbaidu#com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1997-11-27 12:22:42', '浦东新区健祥路478号', '导师', 429, 56, 846, 657, 302, 0, 'YI2kagHSQp', 12, 731, NULL);
INSERT INTO `im_user_data` VALUES ('100474', '陶晓明', 'https://img0CbaiduPcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1996-04-22 09:02:12', '海淀区清河中街68号866号', '导师', 919, 886, 743, 350, 322, 0, 'YaTbwlcw0c', 75, 939, NULL);
INSERT INTO `im_user_data` VALUES ('100475', 'Yung Hok Yau', 'https://img06baidu+com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1997-12-17 13:15:35', '越秀区中山二路317号', '运营经理', 896, 685, 308, 19, 554, 0, 'j8nNe6yYGJ', 9, 950, NULL);
INSERT INTO `im_user_data` VALUES ('100476', '李子韬', 'https://img0@baidutcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-12-02 05:08:23', '珊瑚路480号', 'UX / UI设计员', 253, 563, 266, 899, 736, 1, 'XqHeuMCSfP', 753, 336, NULL);
INSERT INTO `im_user_data` VALUES ('100477', '朱嘉伦', 'https://img0+baidu5com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-03-11 16:17:04', '白云区机场路棠苑街五巷914号', '牙医', 688, 985, 354, 776, 337, 1, '7HgT8070xy', 43, 98, NULL);
INSERT INTO `im_user_data` VALUES ('100478', 'Fung Wing Suen', 'https://img0Qbaidu(com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1997-09-29 03:36:34', '房山区岳琉路295号', '图象设计师', 186, 622, 814, 150, 826, 0, 'NfuTvKPSF4', 317, 477, NULL);
INSERT INTO `im_user_data` VALUES ('100479', 'Shing Ming', 'https://img0ebaiduJcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1998-10-19 12:57:08', '浦东新区橄榄路821号', '软件开发员', 493, 330, 259, 577, 668, 1, 'jKmg7Z1yvB', 941, 736, NULL);
INSERT INTO `im_user_data` VALUES ('100480', '崔秀英', 'https://img0Tbaidu!com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1994-10-12 18:16:48', '龙岗区学园一巷584号', '活动经理', 723, 961, 9, 523, 106, 1, '0Kr5E9DFip', 949, 808, NULL);
INSERT INTO `im_user_data` VALUES ('100481', '苏睿', 'https://img0lbaidu.com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1996-08-23 19:15:48', '罗湖区清水河一路138号', '客户服务经理', 646, 771, 542, 179, 320, 0, 'xA25SAMO3t', 278, 668, NULL);
INSERT INTO `im_user_data` VALUES ('100482', '邱岚', 'https://img0*baidulcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1997-11-05 18:38:32', '锦江区人民南路四段287号', '办公室文员', 152, 81, 530, 190, 894, 0, 'dmIoVPAdEu', 248, 921, NULL);
INSERT INTO `im_user_data` VALUES ('100483', '邓杰宏', 'https://img0 baidu.com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1992-01-04 05:46:17', '白云区小坪东路606号', '活动经理', 586, 48, 400, 311, 558, 0, 'VSVYyg6S36', 297, 99, NULL);
INSERT INTO `im_user_data` VALUES ('100484', '丁睿', 'https://img0zbaiduQcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1998-03-05 14:01:05', '天河区大信商圈大信南路265号', '园艺家', 471, 918, 454, 708, 730, 1, 'MsoG4kCJJn', 693, 419, NULL);
INSERT INTO `im_user_data` VALUES ('100485', '蒋詩涵', 'https://img0,baidu>com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1998-04-29 22:44:19', '锦江区红星路三段714号', '运营经理', 266, 263, 242, 136, 555, 1, 'tQ84WcfIeQ', 536, 286, NULL);
INSERT INTO `im_user_data` VALUES ('100486', 'To Ming Sze', 'https://img0AbaiduKcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1991-01-27 20:32:47', '海珠区江南西路706号', '教授', 719, 763, 822, 364, 254, 1, '80KFe08WbK', 897, 292, NULL);
INSERT INTO `im_user_data` VALUES ('100487', '石安琪', 'https://img0fbaiduwcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1991-12-22 11:31:55', '罗湖区蔡屋围深南东路367号', '销售经理', 877, 206, 695, 11, 847, 0, 'OTJFj8e063', 255, 889, NULL);
INSERT INTO `im_user_data` VALUES ('100488', '罗子异', 'https://img0\\baiduncom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1997-07-28 23:10:48', '福田区景田东一街519号', '社交媒体协调员', 512, 68, 126, 250, 555, 0, 'nPiODKmyeh', 963, 536, NULL);
INSERT INTO `im_user_data` VALUES ('100489', '宋子韬', 'https://img0/baiduncom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1997-12-24 22:22:57', '福田区景田东一街924号', '活动经理', 503, 912, 612, 274, 148, 1, 'aksEfnZAD9', 512, 658, NULL);
INSERT INTO `im_user_data` VALUES ('100490', '李嘉伦', 'https://img0XbaiduLcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1990-07-12 17:12:18', '天河区天河路595号', '理发师', 847, 46, 976, 644, 873, 1, 'htTOYvQrmu', 605, 487, NULL);
INSERT INTO `im_user_data` VALUES ('100491', '蔡云熙', 'https://img0 baiduLcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1994-10-10 10:34:13', '東城区東直門內大街718号', '市场总监', 398, 703, 447, 83, 330, 1, '29UBsWZ6kW', 792, 702, NULL);
INSERT INTO `im_user_data` VALUES ('100492', '梁杰宏', 'https://img0$baiduCcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1990-12-28 16:28:45', '東城区東直門內大街791号', '建筑师', 174, 539, 148, 80, 403, 1, 'RwEjAPQb5r', 594, 223, NULL);
INSERT INTO `im_user_data` VALUES ('100493', '钟子异', 'https://img0Kbaiduocom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1991-06-01 03:27:45', '东泰五街730号', '食品科学家', 483, 252, 547, 258, 565, 1, 'aqqsNbgRBX', 696, 444, NULL);
INSERT INTO `im_user_data` VALUES ('100494', 'Han Tsz Hin', 'https://img0_baiduacom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1997-10-16 02:43:11', '锦江区人民南路四段998号', '客户服务经理', 342, 806, 154, 562, 294, 0, 'iueZ5v3aDH', 432, 732, NULL);
INSERT INTO `im_user_data` VALUES ('100495', 'Han Ka Ming', 'https://img00baiduLcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 1, '1990-02-25 03:45:58', '成华区双庆路531号', '饲养员', 743, 14, 882, 40, 414, 0, 'uKOHQhjrJ6', 430, 361, NULL);
INSERT INTO `im_user_data` VALUES ('100496', 'Lo Wai San', 'https://img0kbaiduscom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 0, '1998-10-04 03:04:39', '徐汇区虹桥路196号', '市场总监', 660, 696, 385, 658, 736, 0, 'qXQ2NQ823N', 214, 298, NULL);
INSERT INTO `im_user_data` VALUES ('100497', '吕睿', 'https://img0\'baiduRcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1997-07-09 05:58:56', '环区南街二巷844号', '管家', 58, 129, 817, 919, 557, 0, 'QKGhLm06ip', 144, 606, NULL);
INSERT INTO `im_user_data` VALUES ('100498', '叶安琪', 'https://img0-baiduecom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1996-10-06 07:47:09', '坑美十五巷948号', '办公室文员', 539, 449, 39, 309, 513, 0, 'j4fpfqOQLz', 786, 482, NULL);
INSERT INTO `im_user_data` VALUES ('100499', 'Lok Wing Fat', 'https://img0ybaiduFcom/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEw=500&h=500', 0, '1997-12-05 23:21:08', '越秀区中山二路626号', '运营经理', 372, 37, 730, 745, 0, 0, '1UFp1Ihe0O', 497, 476, NULL);
INSERT INTO `im_user_data` VALUES ('100500', '石致远', 'https://img0Fbaidu+com/it/u=1472806772,699408928&fm=253&fmt=auto&app=120&f=JPEGw=500&h=500', 1, '1996-06-09 14:26:23', '白云区机场路棠苑街五巷206号', '药剂师', 409, 64, 138, 810, 458, 0, 'CW8hUwJlVS', 902, 452, NULL);

SET FOREIGN_KEY_CHECKS = 1;
