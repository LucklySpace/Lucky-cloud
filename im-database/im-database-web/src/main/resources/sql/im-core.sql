/*
 Navicat Premium Data Transfer

 Source Server         : postgres_local
 Source Server Type    : PostgreSQL
 Source Server Version : 170005 (170005)
 Source Host           : localhost:5432
 Source Catalog        : im-core
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 170005 (170005)
 File Encoding         : 65001

 Date: 12/10/2025 19:46:28
*/


-- ----------------------------
-- Sequence structure for message_delivery_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."message_delivery_id_seq";
CREATE SEQUENCE "public"."message_delivery_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for outbox_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."outbox_id_seq";
CREATE SEQUENCE "public"."outbox_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Table structure for id_meta_info
-- ----------------------------
DROP TABLE IF EXISTS "public"."id_meta_info";
CREATE TABLE "public"."id_meta_info" (
  "id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "max_id" int8,
  "step" int4,
  "update_time" timestamp(6) NOT NULL,
  "version" int4
)
;

-- ----------------------------
-- Table structure for im_chat
-- ----------------------------
DROP TABLE IF EXISTS "public"."im_chat";
CREATE TABLE "public"."im_chat" (
  "chat_id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "chat_type" int4 NOT NULL,
  "owner_id" varchar(50) COLLATE "pg_catalog"."default",
  "to_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "is_mute" int2 NOT NULL,
  "is_top" int2 NOT NULL,
  "sequence" int8,
  "read_sequence" int8,
  "create_time" int8,
  "update_time" int8,
  "del_flag" int2,
  "version" int8
)
;
COMMENT ON COLUMN "public"."im_chat"."chat_id" IS '聊天ID';
COMMENT ON COLUMN "public"."im_chat"."chat_type" IS '聊天类型：0单聊，1群聊，2机器人，3公众号';
COMMENT ON COLUMN "public"."im_chat"."owner_id" IS '会话拥有者用户ID';
COMMENT ON COLUMN "public"."im_chat"."to_id" IS '对方用户ID或群组ID';
COMMENT ON COLUMN "public"."im_chat"."is_mute" IS '是否免打扰（1免打扰）';
COMMENT ON COLUMN "public"."im_chat"."is_top" IS '是否置顶（1置顶）';
COMMENT ON COLUMN "public"."im_chat"."sequence" IS '消息序列号';
COMMENT ON COLUMN "public"."im_chat"."read_sequence" IS '已读消息序列';
COMMENT ON COLUMN "public"."im_chat"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."im_chat"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."im_chat"."del_flag" IS '删除标识（1正常，0删除）';
COMMENT ON COLUMN "public"."im_chat"."version" IS '版本信息';

-- ----------------------------
-- Table structure for im_friendship
-- ----------------------------
DROP TABLE IF EXISTS "public"."im_friendship";
CREATE TABLE "public"."im_friendship" (
  "owner_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "to_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "remark" varchar(50) COLLATE "pg_catalog"."default",
  "del_flag" int4,
  "black" int4,
  "create_time" int8,
  "update_time" int8,
  "sequence" int8,
  "black_sequence" int8,
  "add_source" varchar(20) COLLATE "pg_catalog"."default",
  "extra" varchar(1000) COLLATE "pg_catalog"."default",
  "version" int8
)
;
COMMENT ON COLUMN "public"."im_friendship"."owner_id" IS '用户ID';
COMMENT ON COLUMN "public"."im_friendship"."to_id" IS '好友用户ID';
COMMENT ON COLUMN "public"."im_friendship"."remark" IS '备注';
COMMENT ON COLUMN "public"."im_friendship"."del_flag" IS '删除标识（1正常，0删除）';
COMMENT ON COLUMN "public"."im_friendship"."black" IS '黑名单状态（1正常，2拉黑）';
COMMENT ON COLUMN "public"."im_friendship"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."im_friendship"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."im_friendship"."sequence" IS '序列号';
COMMENT ON COLUMN "public"."im_friendship"."black_sequence" IS '黑名单序列号';
COMMENT ON COLUMN "public"."im_friendship"."add_source" IS '好友来源';
COMMENT ON COLUMN "public"."im_friendship"."extra" IS '扩展字段';
COMMENT ON COLUMN "public"."im_friendship"."version" IS '版本信息';

-- ----------------------------
-- Table structure for im_friendship_group
-- ----------------------------
DROP TABLE IF EXISTS "public"."im_friendship_group";
CREATE TABLE "public"."im_friendship_group" (
  "from_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "group_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "group_name" varchar(50) COLLATE "pg_catalog"."default",
  "sequence" int8,
  "create_time" int8,
  "update_time" int8,
  "del_flag" int2,
  "version" int8
)
;
COMMENT ON COLUMN "public"."im_friendship_group"."from_id" IS '用户ID';
COMMENT ON COLUMN "public"."im_friendship_group"."group_id" IS '分组ID';
COMMENT ON COLUMN "public"."im_friendship_group"."group_name" IS '分组名称';
COMMENT ON COLUMN "public"."im_friendship_group"."sequence" IS '序列号';
COMMENT ON COLUMN "public"."im_friendship_group"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."im_friendship_group"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."im_friendship_group"."del_flag" IS '删除标识（1正常，0删除）';
COMMENT ON COLUMN "public"."im_friendship_group"."version" IS '版本信息';

-- ----------------------------
-- Table structure for im_friendship_group_member
-- ----------------------------
DROP TABLE IF EXISTS "public"."im_friendship_group_member";
CREATE TABLE "public"."im_friendship_group_member" (
  "group_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "to_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "create_time" int8,
  "del_flag" int2,
  "version" int8
)
;
COMMENT ON COLUMN "public"."im_friendship_group_member"."to_id" IS '好友用户ID';
COMMENT ON COLUMN "public"."im_friendship_group_member"."create_time" IS '添加时间';
COMMENT ON COLUMN "public"."im_friendship_group_member"."del_flag" IS '删除标识（1正常，0删除）';
COMMENT ON COLUMN "public"."im_friendship_group_member"."version" IS '版本信息';

-- ----------------------------
-- Table structure for im_friendship_request
-- ----------------------------
DROP TABLE IF EXISTS "public"."im_friendship_request";
CREATE TABLE "public"."im_friendship_request" (
  "id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "from_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "to_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "remark" varchar(50) COLLATE "pg_catalog"."default",
  "read_status" int4,
  "add_source" varchar(20) COLLATE "pg_catalog"."default",
  "message" varchar(50) COLLATE "pg_catalog"."default",
  "approve_status" int4,
  "create_time" int8,
  "update_time" int8,
  "sequence" int8,
  "del_flag" int2,
  "version" int8
)
;
COMMENT ON COLUMN "public"."im_friendship_request"."id" IS '请求ID';
COMMENT ON COLUMN "public"."im_friendship_request"."from_id" IS '请求发起者';
COMMENT ON COLUMN "public"."im_friendship_request"."to_id" IS '请求接收者';
COMMENT ON COLUMN "public"."im_friendship_request"."remark" IS '备注';
COMMENT ON COLUMN "public"."im_friendship_request"."read_status" IS '是否已读（1已读）';
COMMENT ON COLUMN "public"."im_friendship_request"."add_source" IS '好友来源';
COMMENT ON COLUMN "public"."im_friendship_request"."message" IS '好友验证信息';
COMMENT ON COLUMN "public"."im_friendship_request"."approve_status" IS '审批状态（1同意，2拒绝）';
COMMENT ON COLUMN "public"."im_friendship_request"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."im_friendship_request"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."im_friendship_request"."sequence" IS '序列号';
COMMENT ON COLUMN "public"."im_friendship_request"."del_flag" IS '删除标识（1正常，0删除）';
COMMENT ON COLUMN "public"."im_friendship_request"."version" IS '版本信息';

-- ----------------------------
-- Table structure for im_group
-- ----------------------------
DROP TABLE IF EXISTS "public"."im_group";
CREATE TABLE "public"."im_group" (
  "group_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "owner_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "group_type" int4 NOT NULL,
  "group_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "mute" int2,
  "apply_join_type" int4 NOT NULL,
  "avatar" varchar(300) COLLATE "pg_catalog"."default",
  "max_member_count" int4,
  "introduction" varchar(100) COLLATE "pg_catalog"."default",
  "notification" varchar(1000) COLLATE "pg_catalog"."default",
  "status" int4,
  "sequence" int8,
  "create_time" int8,
  "update_time" int8,
  "extra" varchar(1000) COLLATE "pg_catalog"."default",
  "version" int8,
  "del_flag" int2 NOT NULL,
  "verifier" int2
)
;
COMMENT ON COLUMN "public"."im_group"."group_id" IS '群组ID';
COMMENT ON COLUMN "public"."im_group"."owner_id" IS '群主用户ID';
COMMENT ON COLUMN "public"."im_group"."group_type" IS '群类型（1私有群，2公开群）';
COMMENT ON COLUMN "public"."im_group"."group_name" IS '群名称';
COMMENT ON COLUMN "public"."im_group"."mute" IS '是否全员禁言（1不禁言，0禁言）';
COMMENT ON COLUMN "public"."im_group"."apply_join_type" IS '申请加群方式（0禁止申请，1需要审批，2允许自由加入）';
COMMENT ON COLUMN "public"."im_group"."avatar" IS '群头像';
COMMENT ON COLUMN "public"."im_group"."max_member_count" IS '最大成员数';
COMMENT ON COLUMN "public"."im_group"."introduction" IS '群简介';
COMMENT ON COLUMN "public"."im_group"."notification" IS '群公告';
COMMENT ON COLUMN "public"."im_group"."status" IS '群状态（1正常，0解散）';
COMMENT ON COLUMN "public"."im_group"."sequence" IS '消息序列号';
COMMENT ON COLUMN "public"."im_group"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."im_group"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."im_group"."extra" IS '扩展字段';
COMMENT ON COLUMN "public"."im_group"."version" IS '版本信息';
COMMENT ON COLUMN "public"."im_group"."del_flag" IS '删除标识（1正常，0删除）';
COMMENT ON COLUMN "public"."im_group"."verifier" IS '开启群验证（1验证，0不验证）';

-- ----------------------------
-- Table structure for im_group_invite_request
-- ----------------------------
DROP TABLE IF EXISTS "public"."im_group_invite_request";
CREATE TABLE "public"."im_group_invite_request" (
  "request_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "group_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "from_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "to_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "verifier_id" varchar(50) COLLATE "pg_catalog"."default",
  "message" varchar(200) COLLATE "pg_catalog"."default",
  "approve_status" int4 NOT NULL DEFAULT 0,
  "add_source" varchar(20) COLLATE "pg_catalog"."default",
  "expire_time" int8,
  "create_time" int8 NOT NULL,
  "update_time" int8,
  "del_flag" int2 NOT NULL DEFAULT 1,
  "version" int8 NOT NULL DEFAULT 1,
  "verifier_status" int4
)
;
COMMENT ON COLUMN "public"."im_group_invite_request"."request_id" IS '邀请请求ID';
COMMENT ON COLUMN "public"."im_group_invite_request"."group_id" IS '群组ID';
COMMENT ON COLUMN "public"."im_group_invite_request"."from_id" IS '邀请发起者用户ID';
COMMENT ON COLUMN "public"."im_group_invite_request"."to_id" IS '被邀请者用户ID';
COMMENT ON COLUMN "public"."im_group_invite_request"."verifier_id" IS '验证者用户ID（群主或管理员，引用im_user.user_id）';
COMMENT ON COLUMN "public"."im_group_invite_request"."message" IS '邀请验证信息';
COMMENT ON COLUMN "public"."im_group_invite_request"."approve_status" IS '审批状态（0:待处理, 1:同意, 2:拒绝）';
COMMENT ON COLUMN "public"."im_group_invite_request"."add_source" IS '邀请来源（如二维码、链接、搜索等）';
COMMENT ON COLUMN "public"."im_group_invite_request"."expire_time" IS '邀请过期时间（Unix时间戳）';
COMMENT ON COLUMN "public"."im_group_invite_request"."create_time" IS '创建时间（Unix时间戳）';
COMMENT ON COLUMN "public"."im_group_invite_request"."update_time" IS '更新时间（Unix时间戳）';
COMMENT ON COLUMN "public"."im_group_invite_request"."del_flag" IS '删除标识（1:正常, 0:删除）';
COMMENT ON COLUMN "public"."im_group_invite_request"."version" IS '版本信息（用于乐观锁）';
COMMENT ON COLUMN "public"."im_group_invite_request"."verifier_status" IS '群主或管理员验证 （0:待处理, 1:同意, 2:拒绝）';
COMMENT ON TABLE "public"."im_group_invite_request" IS '群聊邀请请求表';

-- ----------------------------
-- Table structure for im_group_member
-- ----------------------------
DROP TABLE IF EXISTS "public"."im_group_member";
CREATE TABLE "public"."im_group_member" (
  "group_member_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "group_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "member_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "role" int4 NOT NULL,
  "speak_date" int8,
  "mute" int2 NOT NULL,
  "alias" varchar(100) COLLATE "pg_catalog"."default",
  "join_time" int8,
  "leave_time" int8,
  "join_type" varchar(50) COLLATE "pg_catalog"."default",
  "extra" varchar(1000) COLLATE "pg_catalog"."default",
  "del_flag" int2 NOT NULL,
  "create_time" int8,
  "update_time" int8,
  "version" int8
)
;
COMMENT ON COLUMN "public"."im_group_member"."group_member_id" IS '群成员ID';
COMMENT ON COLUMN "public"."im_group_member"."group_id" IS '群组ID';
COMMENT ON COLUMN "public"."im_group_member"."member_id" IS '成员用户ID';
COMMENT ON COLUMN "public"."im_group_member"."role" IS '群成员角色（0普通成员，1管理员，2群主）';
COMMENT ON COLUMN "public"."im_group_member"."speak_date" IS '最后发言时间';
COMMENT ON COLUMN "public"."im_group_member"."mute" IS '是否禁言（1不禁言，0禁言）';
COMMENT ON COLUMN "public"."im_group_member"."alias" IS '群昵称';
COMMENT ON COLUMN "public"."im_group_member"."join_time" IS '加入时间';
COMMENT ON COLUMN "public"."im_group_member"."leave_time" IS '离开时间';
COMMENT ON COLUMN "public"."im_group_member"."join_type" IS '加入类型';
COMMENT ON COLUMN "public"."im_group_member"."extra" IS '扩展字段';
COMMENT ON COLUMN "public"."im_group_member"."del_flag" IS '删除标识（1正常，0删除）';
COMMENT ON COLUMN "public"."im_group_member"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."im_group_member"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."im_group_member"."version" IS '版本信息';

-- ----------------------------
-- Table structure for im_group_message
-- ----------------------------
DROP TABLE IF EXISTS "public"."im_group_message";
CREATE TABLE "public"."im_group_message" (
  "message_id" varchar(512) COLLATE "pg_catalog"."default" NOT NULL,
  "group_id" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "from_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "message_body" text COLLATE "pg_catalog"."default" NOT NULL,
  "message_time" int8 NOT NULL,
  "message_content_type" int4 NOT NULL,
  "extra" text COLLATE "pg_catalog"."default",
  "del_flag" int2 NOT NULL,
  "sequence" int8,
  "message_random" varchar(255) COLLATE "pg_catalog"."default",
  "create_time" int8 NOT NULL,
  "update_time" int8,
  "version" int8,
  "reply_to" varchar(255) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."im_group_message"."message_id" IS '消息ID';
COMMENT ON COLUMN "public"."im_group_message"."group_id" IS '群组ID';
COMMENT ON COLUMN "public"."im_group_message"."from_id" IS '发送者用户ID';
COMMENT ON COLUMN "public"."im_group_message"."message_body" IS '消息内容';
COMMENT ON COLUMN "public"."im_group_message"."message_time" IS '发送时间';
COMMENT ON COLUMN "public"."im_group_message"."message_content_type" IS '消息类型';
COMMENT ON COLUMN "public"."im_group_message"."extra" IS '扩展字段';
COMMENT ON COLUMN "public"."im_group_message"."del_flag" IS '删除标识（1正常，0删除）';
COMMENT ON COLUMN "public"."im_group_message"."sequence" IS '消息序列';
COMMENT ON COLUMN "public"."im_group_message"."message_random" IS '随机标识';
COMMENT ON COLUMN "public"."im_group_message"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."im_group_message"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."im_group_message"."version" IS '版本信息';
COMMENT ON COLUMN "public"."im_group_message"."reply_to" IS '被引用的消息 ID';

-- ----------------------------
-- Table structure for im_group_message_status
-- ----------------------------
DROP TABLE IF EXISTS "public"."im_group_message_status";
CREATE TABLE "public"."im_group_message_status" (
  "group_id" varchar(250) COLLATE "pg_catalog"."default" NOT NULL,
  "message_id" varchar(250) COLLATE "pg_catalog"."default" NOT NULL,
  "to_id" varchar(250) COLLATE "pg_catalog"."default" NOT NULL,
  "read_status" int4,
  "create_time" int8,
  "update_time" int8,
  "version" int8
)
;
COMMENT ON COLUMN "public"."im_group_message_status"."group_id" IS '群组ID';
COMMENT ON COLUMN "public"."im_group_message_status"."message_id" IS '消息ID';
COMMENT ON COLUMN "public"."im_group_message_status"."to_id" IS '接收者用户ID';
COMMENT ON COLUMN "public"."im_group_message_status"."read_status" IS '阅读状态（1已读）';
COMMENT ON COLUMN "public"."im_group_message_status"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."im_group_message_status"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."im_group_message_status"."version" IS '版本信息';

-- ----------------------------
-- Table structure for im_message_delivery
-- ----------------------------
DROP TABLE IF EXISTS "public"."im_message_delivery";
CREATE TABLE "public"."im_message_delivery" (
  "id" int8 NOT NULL DEFAULT nextval('message_delivery_id_seq'::regclass),
  "message_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "user_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "broker_id" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'PENDING'::character varying,
  "attempts" int4 NOT NULL DEFAULT 0,
  "last_attempt_at" timestamptz(6),
  "delivered_at" timestamptz(6),
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;
COMMENT ON COLUMN "public"."im_message_delivery"."message_id" IS '业务消息 ID';
COMMENT ON COLUMN "public"."im_message_delivery"."user_id" IS '接收用户 ID';
COMMENT ON COLUMN "public"."im_message_delivery"."broker_id" IS 'im-connect 实例（broker）ID，记录发送到哪个实例';
COMMENT ON COLUMN "public"."im_message_delivery"."status" IS '投递状态：PENDING / SENT(已发送到 im-connect) / DELIVERED(客户端已确认) / FAILED';
COMMENT ON COLUMN "public"."im_message_delivery"."attempts" IS '向该用户投递的尝试次数';
COMMENT ON COLUMN "public"."im_message_delivery"."last_attempt_at" IS '最后一次尝试时间';
COMMENT ON COLUMN "public"."im_message_delivery"."delivered_at" IS '客户端确认接收时间';
COMMENT ON COLUMN "public"."im_message_delivery"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."im_message_delivery"."updated_at" IS '更新时间';
COMMENT ON TABLE "public"."im_message_delivery" IS 'Per-user delivery state: 记录每条消息向每个目标用户的投递流程与状态';

-- ----------------------------
-- Table structure for im_outbox
-- ----------------------------
DROP TABLE IF EXISTS "public"."im_outbox";
CREATE TABLE "public"."im_outbox" (
  "id" int8 NOT NULL DEFAULT nextval('outbox_id_seq'::regclass),
  "message_id" varchar(64) COLLATE "pg_catalog"."default" NOT NULL,
  "payload" text COLLATE "pg_catalog"."default" NOT NULL,
  "exchange" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "routing_key" varchar(128) COLLATE "pg_catalog"."default" NOT NULL,
  "attempts" int4 NOT NULL DEFAULT 0,
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'PENDING'::character varying,
  "last_error" text COLLATE "pg_catalog"."default",
  "created_at" int8,
  "updated_at" int8,
  "next_try_at" int8
)
;
COMMENT ON COLUMN "public"."im_outbox"."id" IS '主键';
COMMENT ON COLUMN "public"."im_outbox"."message_id" IS '业务消息 ID（用于回溯/去重/关联业务数据）';
COMMENT ON COLUMN "public"."im_outbox"."payload" IS '要发送的 JSON 负载（建议尽量轻量：可仅包含 messageId + 必要路由信息）';
COMMENT ON COLUMN "public"."im_outbox"."exchange" IS '目标交换机名称';
COMMENT ON COLUMN "public"."im_outbox"."routing_key" IS '目标路由键（或 queue 名称）';
COMMENT ON COLUMN "public"."im_outbox"."attempts" IS '累积投递次数';
COMMENT ON COLUMN "public"."im_outbox"."status" IS '投递状态：PENDING(待投递) / SENT(已确认) / FAILED(失败，需要人工介入) / DLX(死信)';
COMMENT ON COLUMN "public"."im_outbox"."last_error" IS '投递失败时的错误信息';
COMMENT ON COLUMN "public"."im_outbox"."created_at" IS '创建时间';
COMMENT ON COLUMN "public"."im_outbox"."updated_at" IS '更新时间';
COMMENT ON COLUMN "public"."im_outbox"."next_try_at" IS '下一次重试时间（用以调度延迟重试）';
COMMENT ON TABLE "public"."im_outbox" IS 'Outbox table: 持久化要投递到 MQ 的消息，支持重试/幂等/确认回写';

-- ----------------------------
-- Table structure for im_single_message
-- ----------------------------
DROP TABLE IF EXISTS "public"."im_single_message";
CREATE TABLE "public"."im_single_message" (
  "message_id" varchar(512) COLLATE "pg_catalog"."default" NOT NULL,
  "from_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "to_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "message_body" text COLLATE "pg_catalog"."default" NOT NULL,
  "message_time" int8 NOT NULL,
  "message_content_type" int4 NOT NULL,
  "read_status" int4 NOT NULL,
  "extra" text COLLATE "pg_catalog"."default",
  "del_flag" int2 NOT NULL,
  "sequence" int8 NOT NULL,
  "message_random" varchar(20) COLLATE "pg_catalog"."default",
  "create_time" int8,
  "update_time" int8,
  "version" int8,
  "reply_to" varchar(255) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."im_single_message"."message_id" IS '消息ID';
COMMENT ON COLUMN "public"."im_single_message"."from_id" IS '发送者用户ID';
COMMENT ON COLUMN "public"."im_single_message"."to_id" IS '接收者用户ID';
COMMENT ON COLUMN "public"."im_single_message"."message_body" IS '消息内容';
COMMENT ON COLUMN "public"."im_single_message"."message_time" IS '发送时间';
COMMENT ON COLUMN "public"."im_single_message"."message_content_type" IS '消息类型';
COMMENT ON COLUMN "public"."im_single_message"."read_status" IS '阅读状态（1已读）';
COMMENT ON COLUMN "public"."im_single_message"."extra" IS '扩展字段';
COMMENT ON COLUMN "public"."im_single_message"."del_flag" IS '删除标识（1正常，0删除）';
COMMENT ON COLUMN "public"."im_single_message"."sequence" IS '消息序列';
COMMENT ON COLUMN "public"."im_single_message"."message_random" IS '随机标识';
COMMENT ON COLUMN "public"."im_single_message"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."im_single_message"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."im_single_message"."version" IS '版本信息';
COMMENT ON COLUMN "public"."im_single_message"."reply_to" IS '被引用的消息 ID';

-- ----------------------------
-- Table structure for im_user
-- ----------------------------
DROP TABLE IF EXISTS "public"."im_user";
CREATE TABLE "public"."im_user" (
  "user_id" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "user_name" varchar(255) COLLATE "pg_catalog"."default",
  "password" varchar(255) COLLATE "pg_catalog"."default",
  "mobile" varchar(255) COLLATE "pg_catalog"."default",
  "create_time" int8,
  "update_time" int8,
  "version" int8,
  "del_flag" int4
)
;
COMMENT ON COLUMN "public"."im_user"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."im_user"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."im_user"."version" IS '版本信息';
COMMENT ON COLUMN "public"."im_user"."del_flag" IS '删除标识（1正常，0删除）';

-- ----------------------------
-- Table structure for im_user_data
-- ----------------------------
DROP TABLE IF EXISTS "public"."im_user_data";
CREATE TABLE "public"."im_user_data" (
  "user_id" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default",
  "avatar" varchar(1024) COLLATE "pg_catalog"."default",
  "gender" int4,
  "birthday" varchar(50) COLLATE "pg_catalog"."default",
  "location" varchar(50) COLLATE "pg_catalog"."default",
  "self_signature" varchar(255) COLLATE "pg_catalog"."default",
  "friend_allow_type" int4 NOT NULL,
  "forbidden_flag" int4 NOT NULL,
  "disable_add_friend" int4 NOT NULL,
  "silent_flag" int4 NOT NULL,
  "user_type" int4 NOT NULL,
  "del_flag" int2 NOT NULL,
  "extra" varchar(1000) COLLATE "pg_catalog"."default",
  "create_time" int8,
  "update_time" int8,
  "version" int8
)
;
COMMENT ON COLUMN "public"."im_user_data"."user_id" IS '用户ID';
COMMENT ON COLUMN "public"."im_user_data"."name" IS '昵称';
COMMENT ON COLUMN "public"."im_user_data"."avatar" IS '头像';
COMMENT ON COLUMN "public"."im_user_data"."gender" IS '性别';
COMMENT ON COLUMN "public"."im_user_data"."birthday" IS '生日';
COMMENT ON COLUMN "public"."im_user_data"."location" IS '地址';
COMMENT ON COLUMN "public"."im_user_data"."self_signature" IS '个性签名';
COMMENT ON COLUMN "public"."im_user_data"."friend_allow_type" IS '加好友验证类型（1无需验证，2需要验证）';
COMMENT ON COLUMN "public"."im_user_data"."forbidden_flag" IS '禁用标识（1禁用）';
COMMENT ON COLUMN "public"."im_user_data"."disable_add_friend" IS '管理员禁止添加好友：0未禁用，1已禁用';
COMMENT ON COLUMN "public"."im_user_data"."silent_flag" IS '禁言标识（1禁言）';
COMMENT ON COLUMN "public"."im_user_data"."user_type" IS '用户类型（1普通用户，2客服，3机器人）';
COMMENT ON COLUMN "public"."im_user_data"."del_flag" IS '删除标识（1正常，0删除）';
COMMENT ON COLUMN "public"."im_user_data"."extra" IS '扩展字段';
COMMENT ON COLUMN "public"."im_user_data"."create_time" IS '创建时间';
COMMENT ON COLUMN "public"."im_user_data"."update_time" IS '更新时间';
COMMENT ON COLUMN "public"."im_user_data"."version" IS '版本信息';

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."message_delivery_id_seq"
OWNED BY "public"."im_message_delivery"."id";
SELECT setval('"public"."message_delivery_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."outbox_id_seq"
OWNED BY "public"."im_outbox"."id";
SELECT setval('"public"."outbox_id_seq"', 1, false);

-- ----------------------------
-- Primary Key structure for table id_meta_info
-- ----------------------------
ALTER TABLE "public"."id_meta_info" ADD CONSTRAINT "id_meta_info_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table im_chat
-- ----------------------------
CREATE INDEX "idx_chat_owner_to" ON "public"."im_chat" USING btree (
  "owner_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "to_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table im_chat
-- ----------------------------
ALTER TABLE "public"."im_chat" ADD CONSTRAINT "im_chat_pkey" PRIMARY KEY ("chat_id");

-- ----------------------------
-- Primary Key structure for table im_friendship
-- ----------------------------
ALTER TABLE "public"."im_friendship" ADD CONSTRAINT "im_friendship_pkey" PRIMARY KEY ("owner_id", "to_id");

-- ----------------------------
-- Indexes structure for table im_friendship_group
-- ----------------------------
CREATE UNIQUE INDEX "uniq_from_group" ON "public"."im_friendship_group" USING btree (
  "from_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "group_name" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table im_friendship_group
-- ----------------------------
ALTER TABLE "public"."im_friendship_group" ADD CONSTRAINT "im_friendship_group_pkey" PRIMARY KEY ("group_id");

-- ----------------------------
-- Primary Key structure for table im_friendship_group_member
-- ----------------------------
ALTER TABLE "public"."im_friendship_group_member" ADD CONSTRAINT "im_friendship_group_member_pkey" PRIMARY KEY ("group_id", "to_id");

-- ----------------------------
-- Primary Key structure for table im_friendship_request
-- ----------------------------
ALTER TABLE "public"."im_friendship_request" ADD CONSTRAINT "im_friendship_request_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table im_group
-- ----------------------------
ALTER TABLE "public"."im_group" ADD CONSTRAINT "im_group_pkey" PRIMARY KEY ("group_id");

-- ----------------------------
-- Indexes structure for table im_group_invite_request
-- ----------------------------
CREATE INDEX "idx_im_group_invite_request_from_id" ON "public"."im_group_invite_request" USING btree (
  "from_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_im_group_invite_request_group_id" ON "public"."im_group_invite_request" USING btree (
  "group_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_im_group_invite_request_to_id_status" ON "public"."im_group_invite_request" USING btree (
  "to_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "approve_status" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_im_group_invite_request_verifier_id" ON "public"."im_group_invite_request" USING btree (
  "verifier_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table im_group_invite_request
-- ----------------------------
ALTER TABLE "public"."im_group_invite_request" ADD CONSTRAINT "im_group_invite_request_pkey" PRIMARY KEY ("request_id");

-- ----------------------------
-- Indexes structure for table im_group_member
-- ----------------------------
CREATE INDEX "idx_group_id" ON "public"."im_group_member" USING btree (
  "group_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_igm_member_group" ON "public"."im_group_member" USING btree (
  "member_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "group_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_member_id" ON "public"."im_group_member" USING btree (
  "member_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table im_group_member
-- ----------------------------
ALTER TABLE "public"."im_group_member" ADD CONSTRAINT "im_group_member_pkey" PRIMARY KEY ("group_member_id");

-- ----------------------------
-- Indexes structure for table im_group_message
-- ----------------------------
CREATE INDEX "idx_group_msg_group" ON "public"."im_group_message" USING btree (
  "group_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table im_group_message
-- ----------------------------
ALTER TABLE "public"."im_group_message" ADD CONSTRAINT "im_group_message_pkey" PRIMARY KEY ("message_id");

-- ----------------------------
-- Primary Key structure for table im_group_message_status
-- ----------------------------
ALTER TABLE "public"."im_group_message_status" ADD CONSTRAINT "im_group_message_status_pkey" PRIMARY KEY ("group_id", "message_id", "to_id");

-- ----------------------------
-- Indexes structure for table im_message_delivery
-- ----------------------------
CREATE INDEX "idx_message_delivery_message_id" ON "public"."im_message_delivery" USING btree (
  "message_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_message_delivery_status" ON "public"."im_message_delivery" USING btree (
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_message_delivery_user_id" ON "public"."im_message_delivery" USING btree (
  "user_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table im_message_delivery
-- ----------------------------
ALTER TABLE "public"."im_message_delivery" ADD CONSTRAINT "uq_message_delivery_message_user" UNIQUE ("message_id", "user_id");

-- ----------------------------
-- Primary Key structure for table im_message_delivery
-- ----------------------------
ALTER TABLE "public"."im_message_delivery" ADD CONSTRAINT "message_delivery_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table im_outbox
-- ----------------------------
CREATE INDEX "idx_outbox_message_id" ON "public"."im_outbox" USING btree (
  "message_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table im_outbox
-- ----------------------------
ALTER TABLE "public"."im_outbox" ADD CONSTRAINT "outbox_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table im_single_message
-- ----------------------------
CREATE INDEX "idx_private_from" ON "public"."im_single_message" USING btree (
  "from_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_private_to" ON "public"."im_single_message" USING btree (
  "to_id" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table im_single_message
-- ----------------------------
ALTER TABLE "public"."im_single_message" ADD CONSTRAINT "im_private_message_pkey" PRIMARY KEY ("message_id");

-- ----------------------------
-- Primary Key structure for table im_user
-- ----------------------------
ALTER TABLE "public"."im_user" ADD CONSTRAINT "im_user_pkey" PRIMARY KEY ("user_id");

-- ----------------------------
-- Primary Key structure for table im_user_data
-- ----------------------------
ALTER TABLE "public"."im_user_data" ADD CONSTRAINT "im_user_data_pkey" PRIMARY KEY ("user_id");
