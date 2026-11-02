-- 2026-11-02 迁移:咨询会话记「最近一次来源订单」
-- 背景:t_consultation 原来只记 service_id(服务来源),会话头只能显示「咨询了哪项服务」;
--      用户从「我的订单」点「咨询商家」时,商家与用户都没法在会话里看到那笔订单的单号 / 金额 / 状态。
--      本次加 order_id 一列:用户从订单发起咨询时记下这单,后一次从服务发起则覆盖服务,
--      订单侧保持不变 —— 一列而已,不换键、不改存量数据的含义,老数据留 NULL 表示「不是从订单进来的」。
--
-- 用法(MySQL 起来后执行):
--   mysql --user=root --password --database=wedding < sql/migrate_consult_order_20261102.sql
--   （--password 不带值时会交互提示；口令不进脚本）
--
-- 说明:这一列 Hibernate(ddl-auto=update)启动时也会自动补齐,但新克隆 / CI 的库不一定跑过应用,
--      脚本要留着;幂等写法与 migrate_schedule_slot_20261028.sql 一致,重复执行不会报错。

-- 1) 加列(已存在则跳过:MySQL 8 不支持 ADD COLUMN IF NOT EXISTS,故先查 information_schema)
SET @has_order_id := (SELECT COUNT(*) FROM information_schema.columns
                      WHERE table_schema = DATABASE() AND table_name = 't_consultation' AND column_name = 'order_id');
SET @sql := IF(@has_order_id = 0,
  'ALTER TABLE t_consultation ADD COLUMN order_id BIGINT DEFAULT NULL COMMENT ''咨询来源订单（最近一次）'' AFTER service_id',
  'SELECT ''order_id 已存在'' AS msg');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2) 自检:应看到 order_id 排在 service_id 之后
SHOW COLUMNS FROM t_consultation;
