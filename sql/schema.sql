-- =============================================================================
--  缘集 · 婚礼策划一站式服务平台 —— MySQL 建表脚本
-- -----------------------------------------------------------------------------
--  依据：《缘集_数据库设计说明书》v1.5（18 张表）
--  环境：MySQL 8.0 / InnoDB / utf8mb4_unicode_ci
--  库名：wedding
--
--  ⚠️ 本脚本会先 DROP 同名表再重建 —— 只在【开发环境】使用！
--     生产环境要改成 CREATE TABLE IF NOT EXISTS，并配合数据迁移。
--
--  ⚠️ 时间列的"生成方必须唯一"（见《数据库设计说明书》6.5 节）：
--     · 全部 18 张表的时间列一律【由应用生成】
--       （实体上标 @CreationTimestamp / @UpdateTimestamp）—— 表定义不再设 DEFAULT，
--       避免"应用生成 + 数据库生成"两套机制并存。
--     · 注意列名不是统一的 created_at：t_browse_history 用 browse_time；
--       t_order / t_dispute / t_service 等另有的 confirmed_at / publish_time 等属
--       【业务时间】，在业务方法里显式赋值，不走自动生成。
--     · 早期曾有 4 张表（t_user / t_browse_history / t_consultation / t_consult_message）
--       保留 DEFAULT CURRENT_TIMESTAMP，待建实体后再改；现在实体已全部建立，
--       这 4 张表也已改为应用生成（线上库已于 2026-10-16 ALTER DROP DEFAULT）。
--
--  执行方式：
--    mysql -u root -p < schema.sql
--  或者在 mysql 客户端里：
--    source sql/schema.sql   （在仓库根目录起 mysql 客户端）
--  执行后必须跟着跑一次 dev_seed.sql（灌演示账号 / 分类 / 店铺），否则登不进系统。
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS wedding
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE wedding;


-- =============================================================================
--  一、账号与商家域
-- =============================================================================

-- -----------------------------------------------------------------------------
--  1. t_user  用户账号表
--     需求：FR-U-01（注册 / 登录 / 密码找回 / 个人信息编辑）
--     说明：统一账号体系 —— 用户 / 商家 / 管理员共用本表，以 role 区分身份
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '主键',
    username    VARCHAR(50)  NOT NULL                          COMMENT '登录名，全局唯一',
    password    VARCHAR(100) NOT NULL                          COMMENT '密码密文（加密存储，不存明文）',
    real_name   VARCHAR(50)      DEFAULT NULL                  COMMENT '真实姓名',
    phone       VARCHAR(20)      DEFAULT NULL                  COMMENT '手机号',
    email       VARCHAR(100)     DEFAULT NULL                  COMMENT '邮箱',
    avatar      VARCHAR(255)     DEFAULT NULL                  COMMENT '头像文件路径',
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER'           COMMENT '身份：USER / MERCHANT / ADMIN',
    status      TINYINT      NOT NULL DEFAULT 1                COMMENT '1 正常 / 0 禁用',
    created_at  DATETIME     NOT NULL COMMENT '注册时间（由应用生成，见 6.5 节）',
    updated_at  DATETIME         DEFAULT NULL                  COMMENT '最近修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)                          COMMENT '用户名唯一',
    KEY idx_role (role)                                        COMMENT '按身份筛选'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户账号表';


-- -----------------------------------------------------------------------------
--  2. t_merchant  商家店铺表
--     需求：FR-M-01（入驻注册 / 信息提交 / 资质上传 / 进度查询 / 店铺维护）
--           FR-A-01（审核 / 检索 / 账号状态管理）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_merchant;
CREATE TABLE t_merchant (
    id                BIGINT       NOT NULL AUTO_INCREMENT        COMMENT '主键',
    user_id           BIGINT       NOT NULL                       COMMENT '关联 t_user.id，一个账号最多一个店铺',
    shop_name         VARCHAR(100) NOT NULL                       COMMENT '店铺名称',
    business_scope    VARCHAR(200)     DEFAULT NULL               COMMENT '经营范围',
    contact_phone     VARCHAR(20)      DEFAULT NULL               COMMENT '联系方式',
    description       VARCHAR(500)     DEFAULT NULL               COMMENT '店铺简介',
    logo              VARCHAR(255)     DEFAULT NULL               COMMENT '店铺 Logo 路径',
    license_url       VARCHAR(255)     DEFAULT NULL               COMMENT '营业执照文件路径',
    qualification_url VARCHAR(255)     DEFAULT NULL               COMMENT '服务资质证明文件路径',
    audit_status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING'     COMMENT '审核状态：PENDING / APPROVED / REJECTED',
    reject_reason     VARCHAR(255)     DEFAULT NULL               COMMENT '驳回原因',
    audit_time        DATETIME         DEFAULT NULL               COMMENT '审核时间',
    status            TINYINT      NOT NULL DEFAULT 1             COMMENT '账号状态：1 启用 / 0 停用',
    created_at        DATETIME     NOT NULL COMMENT '申请时间（由应用生成，见 6.5 节）',
    updated_at        DATETIME         DEFAULT NULL               COMMENT '最近修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id)                               COMMENT '一个账号只能开一个店铺',
    KEY idx_audit_status (audit_status)                           COMMENT '管理端按审核状态筛选',
    KEY idx_shop_name (shop_name)                                 COMMENT '管理端按店铺名称检索'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商家店铺表';


-- -----------------------------------------------------------------------------
--  3. t_penalty  违规处罚记录表
--     需求：FR-A-01-4（警告 / 服务下架 / 封禁）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_penalty;
CREATE TABLE t_penalty (
    id           BIGINT       NOT NULL AUTO_INCREMENT        COMMENT '主键',
    merchant_id  BIGINT       NOT NULL                       COMMENT '被处罚商家，关联 t_merchant.id',
    operator_id  BIGINT       NOT NULL                       COMMENT '执行处罚的管理员，关联 t_user.id',
    penalty_type VARCHAR(20)  NOT NULL                       COMMENT 'WARNING 警告 / SERVICE_OFF 服务下架 / BAN 封禁',
    target_id    BIGINT           DEFAULT NULL               COMMENT '处罚为"服务下架"时指向 t_service.id',
    reason       VARCHAR(500)     DEFAULT NULL               COMMENT '处罚原因',
    start_time   DATETIME         DEFAULT NULL               COMMENT '处罚生效时间',
    end_time     DATETIME         DEFAULT NULL               COMMENT '处罚结束时间，为空表示长期有效',
    status       TINYINT      NOT NULL DEFAULT 1             COMMENT '1 生效中 / 0 已解除',
    created_at   DATETIME     NOT NULL                       COMMENT '记录创建时间（应用生成，见 6.5 节）',
    PRIMARY KEY (id),
    KEY idx_merchant_id (merchant_id)                        COMMENT '查某商家的处罚历史',
    KEY idx_penalty_type (penalty_type)                      COMMENT '按处罚类型统计'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '违规处罚记录表';


-- -----------------------------------------------------------------------------
--  4. t_announcement  平台公告表
--     需求：FR-A-04（平台公告发布）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_announcement;
CREATE TABLE t_announcement (
    id           BIGINT       NOT NULL AUTO_INCREMENT        COMMENT '主键',
    publisher_id BIGINT       NOT NULL                       COMMENT '发布人（管理员），关联 t_user.id',
    title        VARCHAR(200) NOT NULL                       COMMENT '公告标题',
    content      TEXT             DEFAULT NULL               COMMENT '公告正文',
    status       TINYINT      NOT NULL DEFAULT 1             COMMENT '1 已发布 / 0 草稿',
    publish_time DATETIME         DEFAULT NULL               COMMENT '发布时间',
    created_at   DATETIME     NOT NULL                       COMMENT '创建时间（由应用生成，见 6.5 节）',
    PRIMARY KEY (id),
    KEY idx_publish_time (publish_time)                      COMMENT '按发布时间倒序展示'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '平台公告表';


-- -----------------------------------------------------------------------------
--  5. t_article  婚庆资讯表
--     需求：FR-A-04（婚庆资讯更新）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_article;
CREATE TABLE t_article (
    id           BIGINT       NOT NULL AUTO_INCREMENT        COMMENT '主键',
    publisher_id BIGINT       NOT NULL                       COMMENT '发布人（管理员），关联 t_user.id',
    title        VARCHAR(200) NOT NULL                       COMMENT '资讯标题',
    cover_url    VARCHAR(255)     DEFAULT NULL               COMMENT '封面图路径',
    content      TEXT             DEFAULT NULL               COMMENT '正文',
    author       VARCHAR(50)      DEFAULT NULL               COMMENT '作者 / 来源',
    view_count   INT          NOT NULL DEFAULT 0             COMMENT '阅读量',
    status       TINYINT      NOT NULL DEFAULT 1             COMMENT '1 已发布 / 0 草稿',
    publish_time DATETIME         DEFAULT NULL               COMMENT '发布时间',
    created_at   DATETIME     NOT NULL                       COMMENT '创建时间（由应用生成，见 6.5 节）',
    updated_at   DATETIME         DEFAULT NULL               COMMENT '最近修改时间（由应用生成，见 6.5 节）',
    PRIMARY KEY (id),
    KEY idx_publish_time (publish_time)                      COMMENT '按发布时间倒序展示',
    KEY idx_status (status)                                  COMMENT '只查已发布内容'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '婚庆资讯表';


-- =============================================================================
--  二、服务与检索域
-- =============================================================================

-- -----------------------------------------------------------------------------
--  6. t_category  服务分类表
--     需求：FR-U-02（全品类服务展示）、FR-U-03-2（按类型筛选）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_category;
CREATE TABLE t_category (
    id         BIGINT      NOT NULL AUTO_INCREMENT           COMMENT '主键',
    name       VARCHAR(50) NOT NULL                          COMMENT '分类名称（套餐/场地/司仪/摄影/婚纱/婚车）',
    sort_order INT         NOT NULL DEFAULT 0                COMMENT '展示排序，数值越小越靠前',
    status     TINYINT     NOT NULL DEFAULT 1                COMMENT '1 启用 / 0 停用',
    created_at DATETIME    NOT NULL COMMENT '创建时间（由应用生成，见 6.5 节）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)                                COMMENT '分类不重名'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '服务分类表';


-- -----------------------------------------------------------------------------
--  7. t_service  服务表
--     需求：FR-M-02（新增/编辑/分类/价格与档期/上下架/案例素材）
--           FR-U-02、FR-U-03（浏览与多维检索）
--     说明：本表是索引设计的重点 —— FR-U-03 要求关键词+分类+价格区间+商家+排序
--           任意组合检索，下列索引分别对应各筛选维度
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_service;
CREATE TABLE t_service (
    id           BIGINT        NOT NULL AUTO_INCREMENT       COMMENT '主键',
    merchant_id  BIGINT        NOT NULL                      COMMENT '所属商家，关联 t_merchant.id',
    category_id  BIGINT        NOT NULL                      COMMENT '所属分类，关联 t_category.id',
    name         VARCHAR(100)  NOT NULL                      COMMENT '服务名称',
    cover_url    VARCHAR(255)      DEFAULT NULL              COMMENT '封面图路径',
    description  VARCHAR(500)      DEFAULT NULL              COMMENT '服务简介',
    content      TEXT              DEFAULT NULL              COMMENT '服务内容明细',
    price        DECIMAL(10, 2) NOT NULL                     COMMENT '价格',
    unit         VARCHAR(20)       DEFAULT NULL              COMMENT '计价单位：每场 / 每套 / 每天',
    audit_status VARCHAR(20)   NOT NULL DEFAULT 'PENDING'    COMMENT '内容审核状态：PENDING / APPROVED / REJECTED',
    shelf_status TINYINT       NOT NULL DEFAULT 0            COMMENT '1 上架 / 0 下架',
    view_count   INT           NOT NULL DEFAULT 0            COMMENT '浏览量（排序依据）',
    sales_count  INT           NOT NULL DEFAULT 0            COMMENT '成交量（排序依据）',
    score        DECIMAL(3, 2) NOT NULL DEFAULT 0.00         COMMENT '平均评分（排序依据）',
    created_at   DATETIME      NOT NULL COMMENT '发布时间（由应用生成，见 6.5 节）',
    updated_at   DATETIME          DEFAULT NULL              COMMENT '最近修改时间',
    PRIMARY KEY (id),
    KEY idx_merchant_id (merchant_id)                        COMMENT '店铺服务列表、商家数据统计',
    KEY idx_category_id (category_id)                        COMMENT '按分类筛选',
    KEY idx_price (price)                                    COMMENT '价格区间筛选与排序',
    KEY idx_shelf_status (shelf_status)                      COMMENT '只查已上架服务',
    KEY idx_name (name)                                      COMMENT '关键词模糊检索',
    KEY idx_created_at (created_at)                          COMMENT '按发布时间排序',
    KEY idx_merchant_shelf (merchant_id, shelf_status)       COMMENT '商家端"我的服务"列表'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '服务表';


-- -----------------------------------------------------------------------------
--  8. t_service_schedule  服务档期表
--     需求：FR-M-02-4（价格与档期设置）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_service_schedule;
CREATE TABLE t_service_schedule (
    id            BIGINT   NOT NULL AUTO_INCREMENT           COMMENT '主键',
    service_id    BIGINT   NOT NULL                          COMMENT '关联 t_service.id',
    schedule_date DATE     NOT NULL                          COMMENT '可预约日期',
    status        TINYINT  NOT NULL DEFAULT 1                COMMENT '1 可预约 / 0 已占用或不可用',
    created_at    DATETIME NOT NULL COMMENT '创建时间（由应用生成，见 6.5 节）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_service_date (service_id, schedule_date)   COMMENT '同一服务同一天只有一条档期',
    KEY idx_schedule_date (schedule_date)                    COMMENT '按日期查可用服务'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '服务档期表';


-- -----------------------------------------------------------------------------
--  9. t_service_case  服务案例素材表
--     需求：FR-M-02-6（案例素材维护）、FR-U-02（案例展示）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_service_case;
CREATE TABLE t_service_case (
    id          BIGINT       NOT NULL AUTO_INCREMENT         COMMENT '主键',
    service_id  BIGINT       NOT NULL                        COMMENT '关联 t_service.id',
    image_url   VARCHAR(255) NOT NULL                        COMMENT '案例图片路径',
    title       VARCHAR(100)     DEFAULT NULL                COMMENT '案例标题',
    description VARCHAR(500)     DEFAULT NULL                COMMENT '案例说明',
    sort_order  INT          NOT NULL DEFAULT 0              COMMENT '展示排序',
  category    VARCHAR(20)      DEFAULT NULL              COMMENT '案例分类：场布 / 仪式 / 宴席 / 外景 / 其他',
    created_at  DATETIME     NOT NULL COMMENT '创建时间（由应用生成，见 6.5 节）',
    PRIMARY KEY (id),
    KEY idx_service_id (service_id)                          COMMENT '查某服务的全部案例'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '服务案例素材表';


-- -----------------------------------------------------------------------------
--  10. t_favorite  收藏表
--      需求：FR-U-06-2（服务收藏）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_favorite;
CREATE TABLE t_favorite (
    id         BIGINT   NOT NULL AUTO_INCREMENT              COMMENT '主键',
    user_id    BIGINT   NOT NULL                             COMMENT '收藏人，关联 t_user.id',
    service_id BIGINT   NOT NULL                             COMMENT '被收藏服务，关联 t_service.id',
    created_at DATETIME NOT NULL COMMENT '收藏时间（由应用生成，见 6.5 节）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_service (user_id, service_id)         COMMENT '防止重复收藏（幂等）',
    KEY idx_service_id (service_id)                          COMMENT '统计服务被收藏数'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '收藏表';


-- -----------------------------------------------------------------------------
--  11. t_browse_history  浏览记录表
--      需求：FR-U-06-3（记录并查看近期浏览过的服务）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_browse_history;
CREATE TABLE t_browse_history (
    id          BIGINT   NOT NULL AUTO_INCREMENT             COMMENT '主键',
    user_id     BIGINT   NOT NULL                            COMMENT '浏览人，关联 t_user.id',
    service_id  BIGINT   NOT NULL                            COMMENT '被浏览服务，关联 t_service.id',
    browse_time DATETIME NOT NULL COMMENT '浏览时间（由应用生成，见 6.5 节）',
    PRIMARY KEY (id),
    KEY idx_user_time (user_id, browse_time)                 COMMENT '按用户查最近浏览（联合索引）',
    KEY idx_service_id (service_id)                          COMMENT '统计服务浏览量'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '浏览记录表';


-- =============================================================================
--  三、交易与履约域
-- =============================================================================

-- -----------------------------------------------------------------------------
--  12. t_order  订单表
--      需求：FR-U-04（提交预约订单 / 金额计算 / 重复下单校验）
--            FR-U-05（订单管理）、FR-M-03（订单履约）
--      说明：status 字段是订单状态机的载体
--            PENDING 待审核 → CONFIRMED 已确认 → IN_PROGRESS 履约中 → COMPLETED 已完成
--            分支：CANCELLED 已取消
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_order;
CREATE TABLE t_order (
    id            BIGINT         NOT NULL AUTO_INCREMENT     COMMENT '主键',
    order_no      VARCHAR(32)    NOT NULL                    COMMENT '订单号，全局唯一，对外展示',
    user_id       BIGINT         NOT NULL                    COMMENT '下单用户，关联 t_user.id',
    merchant_id   BIGINT         NOT NULL                    COMMENT '接单商家，关联 t_merchant.id',
    service_id    BIGINT         NOT NULL                    COMMENT '所购服务，关联 t_service.id',
    wedding_date  DATE               DEFAULT NULL            COMMENT '婚礼日期',
    guest_count   INT                DEFAULT NULL            COMMENT '宾客人数',
    budget_min    DECIMAL(10, 2)     DEFAULT NULL            COMMENT '预算区间下限',
    budget_max    DECIMAL(10, 2)     DEFAULT NULL            COMMENT '预算区间上限',
    quantity      INT            NOT NULL DEFAULT 1          COMMENT '数量',
    amount        DECIMAL(10, 2) NOT NULL                    COMMENT '订单金额',
    remark        VARCHAR(500)       DEFAULT NULL            COMMENT '备注信息',
    status        VARCHAR(20)    NOT NULL DEFAULT 'PENDING'  COMMENT '订单状态（状态机）',
    cancel_reason VARCHAR(255)       DEFAULT NULL            COMMENT '取消原因 / 商家拒单原因',
  after_sale_status TINYINT      DEFAULT NULL            COMMENT '售后状态：1 处理中 / 2 已完结',
  after_sale_note VARCHAR(500)    DEFAULT NULL            COMMENT '售后处理说明',
  after_sale_at  DATETIME         DEFAULT NULL            COMMENT '售后处理时间',
    confirmed_at  DATETIME           DEFAULT NULL            COMMENT '接单确认时间',
    completed_at  DATETIME           DEFAULT NULL            COMMENT '服务完成时间',
    cancelled_at  DATETIME           DEFAULT NULL            COMMENT '取消时间',
    created_at    DATETIME       NOT NULL COMMENT '下单时间（由应用生成，见 6.5 节）',
    updated_at    DATETIME           DEFAULT NULL            COMMENT '最近修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no)                        COMMENT '订单号唯一（下单幂等基础）',
    KEY idx_user_id (user_id)                                COMMENT '用户端"我的订单"',
    KEY idx_merchant_id (merchant_id)                        COMMENT '商家端"本店铺订单"',
    KEY idx_service_id (service_id)                          COMMENT '统计服务成交量',
    KEY idx_status (status)                                  COMMENT '按状态筛选订单',
    KEY idx_created_at (created_at)                          COMMENT '按时间检索历史订单',
    KEY idx_user_service_status (user_id, service_id, status) COMMENT '重复下单校验'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单表';


-- -----------------------------------------------------------------------------
--  13. t_order_status_log  订单状态流转记录表
--      需求：FR-U-05-2（查看订单全量信息与状态流转记录）
--            FR-M-03（接单 / 拒单 / 履约推进每次操作留痕）
--      说明：同时是争议仲裁时还原履约过程的事实依据
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_order_status_log;
CREATE TABLE t_order_status_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT       COMMENT '主键',
    order_id      BIGINT       NOT NULL                      COMMENT '关联 t_order.id',
    from_status   VARCHAR(20)      DEFAULT NULL              COMMENT '变更前状态（首次创建时为空）',
    to_status     VARCHAR(20)  NOT NULL                      COMMENT '变更后状态',
    operator_id   BIGINT       NOT NULL                      COMMENT '操作人，关联 t_user.id',
    operator_role VARCHAR(20)  NOT NULL                      COMMENT '操作人身份：USER / MERCHANT / ADMIN',
    remark        VARCHAR(255)     DEFAULT NULL              COMMENT '本次流转说明（如拒单原因）',
    created_at    DATETIME     NOT NULL COMMENT '流转时间（由应用生成，见 6.5 节）',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id)                              COMMENT '查某订单的完整流转轨迹'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单状态流转记录表';


-- -----------------------------------------------------------------------------
--  14. t_consultation  咨询会话表
--      需求：FR-U-04-1（在线咨询）、FR-U-06-4（咨询记录）、FR-M-04-1（会话列表）
--      说明：只存会话级信息 —— 会话列表页只需读本表，进入会话后才读 t_consult_message
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_consultation;
CREATE TABLE t_consultation (
    id               BIGINT       NOT NULL AUTO_INCREMENT    COMMENT '主键',
    user_id          BIGINT       NOT NULL                   COMMENT '用户，关联 t_user.id',
    merchant_id      BIGINT       NOT NULL                   COMMENT '商家，关联 t_merchant.id',
    service_id       BIGINT           DEFAULT NULL           COMMENT '咨询针对的服务（可为空：直接咨询店铺）',
    last_message     VARCHAR(255)     DEFAULT NULL           COMMENT '最后一条消息摘要（列表页展示）',
    last_time        DATETIME         DEFAULT NULL           COMMENT '最后一条消息时间（列表排序）',
    user_unread      INT          NOT NULL DEFAULT 0         COMMENT '用户未读数',
    merchant_unread  INT          NOT NULL DEFAULT 0         COMMENT '商家未读数',
    created_at       DATETIME     NOT NULL COMMENT '会话创建时间（由应用生成，见 6.5 节）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_merchant (user_id, merchant_id)       COMMENT '同一对用户与商家只有一个会话',
    KEY idx_merchant_id (merchant_id)                        COMMENT '商家端咨询列表',
    KEY idx_last_time (last_time)                            COMMENT '按最近消息时间排序'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '咨询会话表';


-- -----------------------------------------------------------------------------
--  15. t_consult_message  咨询消息表
--      需求：FR-M-04-2（在线回复用户咨询）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_consult_message;
CREATE TABLE t_consult_message (
    id              BIGINT        NOT NULL AUTO_INCREMENT    COMMENT '主键',
    consultation_id BIGINT        NOT NULL                   COMMENT '所属会话，关联 t_consultation.id',
    sender_id       BIGINT        NOT NULL                   COMMENT '发送人，关联 t_user.id',
    sender_role     VARCHAR(20)   NOT NULL                   COMMENT '发送人身份：USER / MERCHANT',
    content         VARCHAR(1000) NOT NULL                   COMMENT '消息内容',
    is_read         TINYINT       NOT NULL DEFAULT 0         COMMENT '1 已读 / 0 未读',
    created_at      DATETIME      NOT NULL COMMENT '发送时间（由应用生成，见 6.5 节）',
    PRIMARY KEY (id),
    KEY idx_consultation_id (consultation_id)                COMMENT '拉取某会话的消息列表'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '咨询消息表';


-- =============================================================================
--  四、互动与治理域
-- =============================================================================

-- -----------------------------------------------------------------------------
--  16. t_evaluation  服务评价表
--      需求：FR-U-06-5（服务评价）、FR-M-04-3 / FR-M-04-4（查看评价 / 回复评价）、
--            FR-A-04-1（评价审核 —— 由管理员决定该条评价是否公开展示）
--      说明：以 order_id 作为唯一键实现"一单一评"
--            audit_status 是"公开闸门"：新评价落库为 PENDING，管理员放行为 APPROVED 后才对外展示；
--            对外展示的查询必须带 audit_status = 'APPROVED' 条件（服务详情页 / 商家端评价列表）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_evaluation;
CREATE TABLE t_evaluation (
    id            BIGINT        NOT NULL AUTO_INCREMENT      COMMENT '主键',
    order_id      BIGINT        NOT NULL                     COMMENT '关联 t_order.id，一单一评',
    user_id       BIGINT        NOT NULL                     COMMENT '评价人，关联 t_user.id',
    merchant_id   BIGINT        NOT NULL                     COMMENT '被评商家，关联 t_merchant.id',
    service_id    BIGINT        NOT NULL                     COMMENT '被评服务，关联 t_service.id',
    score         TINYINT       NOT NULL                     COMMENT '评分，1 ~ 5',
    content       VARCHAR(1000)     DEFAULT NULL             COMMENT '文字评价',
    images        VARCHAR(1000)     DEFAULT NULL             COMMENT '评价图片路径，多个以逗号分隔',
    audit_status  VARCHAR(20)   NOT NULL                     COMMENT '审核状态：PENDING 待审核 / APPROVED 已通过 / REJECTED 已驳回（不设默认值，由应用显式赋值）',
    reject_reason VARCHAR(255)      DEFAULT NULL             COMMENT '驳回原因（REJECTED 时有值）',
    audit_time    DATETIME          DEFAULT NULL             COMMENT '审核时间（由应用显式赋值）',
    reply_content VARCHAR(500)      DEFAULT NULL             COMMENT '商家回复内容',
    reply_time    DATETIME          DEFAULT NULL             COMMENT '回复时间',
    created_at    DATETIME      NOT NULL COMMENT '评价时间（由应用生成，见 6.5 节）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_id (order_id)                        COMMENT '保证一单一评（幂等）',
    KEY idx_merchant_id (merchant_id)                        COMMENT '商家端"本店铺评价"',
    KEY idx_service_id (service_id)                          COMMENT '计算服务平均分',
    KEY idx_audit_status (audit_status)                      COMMENT '管理端按审核状态筛选待审评价'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '服务评价表';


-- -----------------------------------------------------------------------------
--  17. t_dispute  争议仲裁表
--      需求：FR-U-06-6（发起平台介入）、FR-A-03（受理 / 组织举证 / 作出仲裁）
--      说明：status 是争议状态机的载体
--            PENDING 待受理 → PROCESSING 举证中 → ARBITRATING 仲裁中 → FINISHED 已裁决
--            分支：CLOSED 已关闭
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_dispute;
CREATE TABLE t_dispute (
    id            BIGINT         NOT NULL AUTO_INCREMENT     COMMENT '主键',
    dispute_no    VARCHAR(32)    NOT NULL                    COMMENT '争议单号，全局唯一',
    order_id      BIGINT         NOT NULL                    COMMENT '关联 t_order.id',
    user_id       BIGINT         NOT NULL                    COMMENT '发起人，关联 t_user.id',
    merchant_id   BIGINT         NOT NULL                    COMMENT '被投诉商家，关联 t_merchant.id',
    handler_id    BIGINT             DEFAULT NULL            COMMENT '处理的管理员，关联 t_user.id',
    reason        VARCHAR(500)   NOT NULL                    COMMENT '用户诉求',
    status        VARCHAR(20)    NOT NULL DEFAULT 'PENDING'  COMMENT '争议状态（状态机）',
    result_type   VARCHAR(20)        DEFAULT NULL            COMMENT '裁决结论：REFUND / CLOSE_ORDER / PENALTY / REJECT',
    refund_amount DECIMAL(10, 2)     DEFAULT NULL            COMMENT '退款金额（结论为退款时填写）',
    result        VARCHAR(500)       DEFAULT NULL            COMMENT '裁决说明',
    accepted_at   DATETIME           DEFAULT NULL            COMMENT '受理时间',
    finished_at   DATETIME           DEFAULT NULL            COMMENT '裁决完成时间',
    created_at    DATETIME       NOT NULL COMMENT '发起时间（由应用生成，见 6.5 节）',
    updated_at    DATETIME           DEFAULT NULL            COMMENT '最近修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dispute_no (dispute_no)                    COMMENT '争议单号唯一',
    KEY idx_order_id (order_id)                              COMMENT '查某订单的争议',
    KEY idx_status (status)                                  COMMENT '管理端按状态筛选待处理争议',
    KEY idx_merchant_id (merchant_id)                        COMMENT '统计商家被投诉情况'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '争议仲裁表';


-- -----------------------------------------------------------------------------
--  18. t_dispute_evidence  争议举证材料表
--      需求：FR-A-03-2（组织双方举证并汇总双方材料）
--            FR-M-03-6（对争议订单提交履约记录与交付凭证）
--      说明：submitter_role 区分材料由用户还是商家提交 —— 这是"双方材料"并列呈现的前提
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS t_dispute_evidence;
CREATE TABLE t_dispute_evidence (
    id             BIGINT        NOT NULL AUTO_INCREMENT     COMMENT '主键',
    dispute_id     BIGINT        NOT NULL                    COMMENT '关联 t_dispute.id',
    submitter_id   BIGINT        NOT NULL                    COMMENT '提交人，关联 t_user.id',
    submitter_role VARCHAR(20)   NOT NULL                    COMMENT '提交方身份：USER / MERCHANT',
    evidence_type  VARCHAR(20)   NOT NULL                    COMMENT '材料类型：IMAGE 图片 / TEXT 文字 / MIXED 图文',
    content        VARCHAR(1000)     DEFAULT NULL            COMMENT '文字说明 / 文字举证',
    file_url       VARCHAR(255)      DEFAULT NULL            COMMENT '凭证文件路径',
    created_at     DATETIME      NOT NULL COMMENT '提交时间（由应用生成，见 6.5 节）',
    PRIMARY KEY (id),
    KEY idx_dispute_id (dispute_id)                          COMMENT '汇总某争议的全部举证材料'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '争议举证材料表';


SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
--  建表完成：共 18 张表
--  账号与商家域 5 张：t_user / t_merchant / t_penalty / t_announcement / t_article
--  服务与检索域 6 张：t_category / t_service / t_service_schedule / t_service_case
--                    / t_favorite / t_browse_history
--  交易与履约域 4 张：t_order / t_order_status_log / t_consultation / t_consult_message
--  互动与治理域 3 张：t_evaluation / t_dispute / t_dispute_evidence
-- =============================================================================
