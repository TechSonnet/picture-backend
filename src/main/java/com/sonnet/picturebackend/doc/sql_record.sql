-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    vipExpireTime datetime     null comment '会员过期时间',
    vipCode       varchar(128) null comment '会员兑换码',
    vipNumber     bigint       null comment '会员编号',
    shareCode     varchar(20)  DEFAULT NULL COMMENT '分享码',
    inviteUser    bigint       DEFAULT NULL COMMENT '邀请用户 id',

    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
    ) comment '用户' collate = utf8mb4_unicode_ci;

-- 图片表
CREATE TABLE IF NOT EXISTS picture (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'id',
    url VARCHAR(512) NOT NULL UNIQUE COMMENT '图片 url',
    name VARCHAR(128) NOT NULL COMMENT '图片名称',
    introduction VARCHAR(512) NULL COMMENT '简介',
    category VARCHAR(64) NULL COMMENT '分类',
    tags VARCHAR(512) NULL COMMENT '标签（JSON 数组）',
    pic_size BIGINT NULL COMMENT '图片体积',
    pic_width INT NULL COMMENT '图片宽度',
    pic_height INT NULL COMMENT '图片高度',
    pic_format VARCHAR(32) NULL COMMENT '图片格式',
    user_id BIGINT NOT NULL COMMENT '创建用户 id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    last_edit_time DATETIME NULL COMMENT '最后编辑时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    INDEX idx_name (name),
    FULLTEXT INDEX idx_intro_fulltext (introduction),
    INDEX idx_category (category),
    INDEX idx_user (user_id)
    ) COMMENT='图片' COLLATE = utf8mb4_unicode_ci;
