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
