-- 创建库
create database if not exists ai_composer;
use ai_composer;

-- 用户表
DROP TABLE IF EXISTS  `user`;
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间(个人信息编辑时间)',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 初始化测试数据（密码是 12345678，MD5 加密 + 盐值 yupi）
INSERT INTO user (id, userAccount, userPassword, userName, userAvatar, userProfile, userRole) VALUES
                                                                                                  (1, 'admin', '10670d38ec32fa8102be6a37f8cb52bf', '管理员', 'https://www.codefather.cn/logo.png', '系统管理员', 'admin'),
                                                                                                  (2, 'user', '10670d38ec32fa8102be6a37f8cb52bf', '普通用户', 'https://www.codefather.cn/logo.png', '我是一个普通用户', 'user'),
                                                                                                  (3, 'test', '10670d38ec32fa8102be6a37f8cb52bf', '测试账号', 'https://www.codefather.cn/logo.png', '这是一个测试账号', 'user');
-- 文章表
DROP TABLE IF EXISTS `article`;
CREATE TABLE `article` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `taskId` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务ID（UUID）',
  `userId` bigint NOT NULL COMMENT '用户ID',
  `topic` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '选题',
  `mainTitle` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主标题',
  `subTitle` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '副标题',
  `outline` json DEFAULT NULL COMMENT '大纲（JSON格式）',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '正文（Markdown格式）',
  `fullContent` text COLLATE utf8mb4_unicode_ci COMMENT '完整图文（Markdown格式，含配图）',
  `coverImage` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图 URL',
  `images` json DEFAULT NULL COMMENT '配图列表（JSON数组）',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/COMPLETED/FAILED',
  `errorMessage` text COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `completedTime` datetime DEFAULT NULL COMMENT '完成时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_taskId` (`taskId`),
  KEY `idx_userId` (`userId`),
  KEY `idx_status` (`status`),
  KEY `idx_createTime` (`createTime`),
  KEY `idx_userId_status` (`userId`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

LOCK TABLES `article` WRITE;
UNLOCK TABLES;
