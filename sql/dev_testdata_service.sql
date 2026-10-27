-- =============================================================================
--  开发用测试数据：往 t_service 灌 10 万行
-- -----------------------------------------------------------------------------
--  用途：《数据库设计说明书》里讲的索引，得用真数据才看得出效果 ——
--        这份脚本生成 10 万行服务数据，用来做 EXPLAIN 对比实验。
--
--  ⚠️ 只在开发环境跑。跑完想清空：
--        TRUNCATE TABLE t_service;
--
--  生成原理（不用循环、不用存储过程，几秒完成）：
--    先造一张 0~9 的小表 tmp_seq，再把它自己连 5 次：
--      10 × 10 × 10 × 10 × 10 = 100000 行
--    每一行编号 n = a*10000 + b*1000 + c*100 + d*10 + e + 1，范围 1 ~ 100000。
-- =============================================================================

USE wedding;

DROP TABLE IF EXISTS tmp_seq;
CREATE TABLE tmp_seq (n INT);
INSERT INTO tmp_seq VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);

-- 先清空，保证可重复执行
TRUNCATE TABLE t_service;

INSERT INTO t_service (merchant_id, category_id, name, cover_url, description, content,
                       price, unit, audit_status, shelf_status,
                       view_count, sales_count, score, created_at)
SELECT
    -- 商家 1~50 轮着来
    x.n % 50 + 1                                        AS merchant_id,
    -- 分类 1~6 轮着来（对应：1套餐 2场地 3司仪 4摄影 5婚纱 6婚车）
    x.n % 6 + 1                                         AS category_id,
    -- 名字形如「摄影-演示-000123」，方便做关键词检索实验
    CONCAT(ELT(x.n % 6 + 1, '套餐','场地','司仪','摄影','婚纱','婚车'),
           '-演示-', LPAD(x.n, 6, '0'))                 AS name,
    CONCAT('/upload/demo/', x.n, '.jpg')                AS cover_url,
    CONCAT('这是第 ', x.n, ' 个演示服务的简介')            AS description,
    '演示用服务内容明细'                                  AS content,
    -- ★ 价格：1000 ~ 19999 均匀铺开，每隔 100 元约 5~6 条
    --   这样「价格区间」查询才有区分度，索引效果看得清
    1000 + (x.n % 19000)                                AS price,
    '每场'                                               AS unit,
    'APPROVED'                                          AS audit_status,
    1                                                   AS shelf_status,
    x.n % 5000                                          AS view_count,
    x.n % 300                                           AS sales_count,
    ROUND((x.n % 50) / 10 + 1, 1)                       AS score,
    -- ★ 创建时间：铺开在 2026-01-01 之后的 300 天里（做「函数导致索引失效」实验用）
    DATE_ADD('2026-01-01 00:00:00', INTERVAL (x.n % 300) DAY) AS created_at
FROM (
    SELECT a.n * 10000 + b.n * 1000 + c.n * 100 + d.n * 10 + e.n + 1 AS n
    FROM tmp_seq a, tmp_seq b, tmp_seq c, tmp_seq d, tmp_seq e
) x;

DROP TABLE tmp_seq;

SELECT COUNT(*) AS 总行数, MIN(price) AS 最低价, MAX(price) AS 最高价 FROM t_service;
