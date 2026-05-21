-- ========================================
-- 票务系统 — 数据库初始化脚本
-- MySQL 8.x
-- ========================================

CREATE DATABASE IF NOT EXISTS `ticket_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ticket_system`;

-- ---------- 1. user ----------
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(32)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(128) NOT NULL COMMENT '密码（加密存储）',
    `phone`       VARCHAR(16)  DEFAULT NULL COMMENT '手机号',
    `email`       VARCHAR(64)  DEFAULT NULL COMMENT '邮箱',
    `role`        TINYINT      NOT NULL DEFAULT 0 COMMENT '角色：0=普通用户 1=管理员',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=禁用 1=启用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------- 2. show ----------
CREATE TABLE `show` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '演出ID',
    `title`       VARCHAR(128) NOT NULL COMMENT '演出名称',
    `description` TEXT         COMMENT '演出描述',
    `venue`       VARCHAR(128) NOT NULL COMMENT '场馆',
    `cover_url`   VARCHAR(256) COMMENT '封面图',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0=下架 1=上架',
    `sale_start`  DATETIME     NOT NULL COMMENT '开售时间',
    `sale_end`    DATETIME     DEFAULT NULL COMMENT '停售时间',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_sale_start` (`sale_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演出表';

-- ---------- 3. show_session ----------
CREATE TABLE `show_session` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '场次ID',
    `show_id`     BIGINT       NOT NULL COMMENT '关联演出ID',
    `name`        VARCHAR(64)  NOT NULL COMMENT '场次名称',
    `start_time`  DATETIME     NOT NULL COMMENT '开始时间',
    `end_time`    DATETIME     DEFAULT NULL COMMENT '结束时间',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_show_id` (`show_id`),
    CONSTRAINT `fk_session_show` FOREIGN KEY (`show_id`) REFERENCES `show` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场次表';

-- ---------- 4. ticket_category ----------
CREATE TABLE `ticket_category` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '档次ID',
    `session_id`    BIGINT       NOT NULL COMMENT '关联场次ID',
    `name`          VARCHAR(32)  NOT NULL COMMENT '档次名称',
    `price`         DECIMAL(10,2) NOT NULL COMMENT '价格',
    `total_stock`   INT          NOT NULL DEFAULT 0 COMMENT '总库存',
    `remain_stock`  INT          NOT NULL DEFAULT 0 COMMENT '剩余库存',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    CONSTRAINT `fk_category_session` FOREIGN KEY (`session_id`) REFERENCES `show_session` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='票价档次表';

-- ---------- 5. order ----------
CREATE TABLE `order` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`      VARCHAR(32)  NOT NULL COMMENT '订单号',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `session_id`    BIGINT       NOT NULL COMMENT '场次ID',
    `category_id`   BIGINT       NOT NULL COMMENT '票价档次ID',
    `quantity`      INT          NOT NULL DEFAULT 1 COMMENT '购买数量',
    `total_amount`  DECIMAL(10,2) NOT NULL COMMENT '总金额',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0=待支付 1=已支付 2=已取消 3=已退款 4=已完成',
    `pay_time`      DATETIME     DEFAULT NULL COMMENT '支付时间',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_order_session` FOREIGN KEY (`session_id`) REFERENCES `show_session` (`id`),
    CONSTRAINT `fk_order_category` FOREIGN KEY (`category_id`) REFERENCES `ticket_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ---------- 6. inventory_log ----------
CREATE TABLE `inventory_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `category_id`   BIGINT       NOT NULL COMMENT '票价档次ID',
    `change_type`   TINYINT      NOT NULL COMMENT '类型：1=扣减 2=回滚 3=增加',
    `quantity`      INT          NOT NULL COMMENT '变更数量',
    `order_no`      VARCHAR(32)  DEFAULT NULL COMMENT '关联订单号',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变更流水表';

-- ========================================
-- 测试数据
-- ========================================

-- 管理员账号（密码需用BCrypt加密，此处为占位）
INSERT INTO `user` (`username`, `password`, `role`) VALUES
('admin', '$2a$10$placeholder_hashed_password', 1);

-- 测试演出
INSERT INTO `show` (`title`, `description`, `venue`, `status`, `sale_start`) VALUES
('周杰伦2026巡演-深圳站', '地表最强', '深圳大运中心', 1, NOW()),
('五月天2026演唱会-广州站', '人生无限公司', '广州天河体育场', 1, NOW());

-- 测试场次
INSERT INTO `show_session` (`show_id`, `name`, `start_time`) VALUES
(1, '2026-06-15 19:30场', '2026-06-15 19:30:00'),
(1, '2026-06-16 19:30场', '2026-06-16 19:30:00'),
(2, '2026-07-01 19:30场', '2026-07-01 19:30:00');

-- 测试票价档次
INSERT INTO `ticket_category` (`session_id`, `name`, `price`, `total_stock`, `remain_stock`) VALUES
(1, 'VIP', 1880.00, 100, 100),
(1, '一等座', 1280.00, 200, 200),
(1, '二等座', 880.00, 300, 300),
(2, 'VIP', 1880.00, 100, 100),
(2, '一等座', 1280.00, 200, 200),
(3, 'VIP', 1680.00, 100, 100),
(3, '看台票', 680.00, 500, 500);
