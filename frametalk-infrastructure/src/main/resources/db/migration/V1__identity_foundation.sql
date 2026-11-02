CREATE TABLE `app_user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `email`         VARCHAR(254) NOT NULL,
    `display_name`  VARCHAR(64)  NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `created_at`    DATETIME(3)  NOT NULL,
    `updated_at`    DATETIME(3)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_user_email` (`email`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'FrameTalk 账号';

CREATE TABLE `auth_session` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT       NOT NULL,
    `token_hash` CHAR(64)     NOT NULL,
    `remembered` BOOLEAN      NOT NULL DEFAULT FALSE,
    `expires_at` DATETIME(3)  NOT NULL,
    `created_at` DATETIME(3)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_auth_session_token` (`token_hash`),
    KEY `idx_auth_session_user` (`user_id`, `expires_at`),
    CONSTRAINT `fk_auth_session_user`
        FOREIGN KEY (`user_id`) REFERENCES `app_user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'FrameTalk 浏览器登录会话';

CREATE TABLE `email_verification_code` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `email`           VARCHAR(254) NOT NULL,
    `code_hash`       CHAR(64)     NOT NULL,
    `expires_at`      DATETIME(3)  NOT NULL,
    `consumed_at`     DATETIME(3)  NULL,
    `failed_attempts` INT          NOT NULL DEFAULT 0,
    `created_at`      DATETIME(3)  NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_email_code_active` (`email`, `consumed_at`, `expires_at`),
    KEY `idx_email_code_created` (`email`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'FrameTalk 邮箱注册验证码';
