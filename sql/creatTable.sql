-- 用户表
create table user
(
    user_name     varchar(256)                       null comment '用户昵称',
    id            bigint auto_increment comment 'id'
        primary key,
    user_account  varchar(256)                       null comment '账号',
    avatar_url    varchar(1024)                      null comment '用户头像',
    gender        tinyint                            null comment '性别',
    user_password varchar(512)                       not null comment '密码',
    phone         varchar(128)                       null comment '电话',
    email         varchar(512)                       null comment '邮箱',
    user_status   int      default 0                 not null comment '状态 0 - 正常',
    create_time   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete     tinyint  default 0                 not null comment '是否删除',
    user_role     int      default 0                 not null comment '用户角色 0 - 普通用户 1 - 管理员',
    planet_code   varchar(512)                       null comment '星球编号',
    tags          varchar(1024)                      null comment '标签列表 JSON 存储'
)
    comment '用户';



alter table user
    add column tags varchar(1024) null comment '标签列表';

-- 标签表
create table tag
(
    id          bigint auto_increment comment 'id'
        primary key,
    tag_name    varchar(256)                       not null comment '标签名',
    user_id     bigint                             null comment '上传标签的用户的id',
    is_parent   bigint                             null comment '是否是父标签 0 - 不是父标签 1 - 是父标签',
    parent_id   bigint                             null comment '父标签id',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   tinyint  default 0                 not null comment '是否删除',
    constraint idx_tag_name
        unique (tag_name)
)
    comment '标签';

create index idx_user_id
    on tag (user_id);

-- 队伍表
create table team
(
    id          bigint auto_increment comment 'id'
        primary key,
    name        varchar(256)                       not null comment '队伍名称',
    description varchar(1024)                      null comment '描述',
    max_num     int      default 1                 not null comment '最大人数',
    expire_time datetime                           null comment '过期时间',
    creator_id  bigint comment '创建人用户id',
    status      int      default 0                 not null comment '0 - 公开，1 - 私有，2 - 加密',
    password    varchar(512)                       null comment '密码',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete   tinyint  default 0                 not null comment '是否删除'
)
    comment '队伍';

-- 队伍用户关系表
create table user_team
(
    id         bigint auto_increment comment 'id'
        primary key,
    user_id     bigint comment '用户id',
    team_id     bigint comment '队伍id',
    join_time   datetime                           null comment '加入时间',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    is_delete   tinyint  default 0                 not null comment '是否删除'
)
    comment '用户队伍关系';