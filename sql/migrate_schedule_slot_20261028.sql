-- 2026-10-28 迁移:档期 → 时间与场地占用
-- 背景:t_service_schedule 原来"一个服务一天只能有一条"(唯一键 uk_service_date),
--      无法表达"同一天上午可约、下午已被占"。本次加 slot(时段) 与 venue(场地),
--      并把唯一键换成 (service_id, schedule_date, slot)。
--
-- 用法(MySQL 起来后执行):
--   mysql --user=root --password --database=wedding < sql/migrate_schedule_slot_20261028.sql
--   （--password 不带值时会交互提示；口令不进脚本）
--
-- 说明:两列也可以由 Hibernate(ddl-auto=update)启动时自动补齐,但**唯一键不会自动换**,
--      不换的话第二天再设同一天的另一时段会撞 uk_service_date 报 500。所以这个脚本必须跑。

-- 1) 加列(已存在则跳过:MySQL 8 不支持 ADD COLUMN IF NOT EXISTS,故先查 information_schema)
SET @has_slot := (SELECT COUNT(*) FROM information_schema.columns
                  WHERE table_schema = DATABASE() AND table_name = 't_service_schedule' AND column_name = 'slot');
SET @sql := IF(@has_slot = 0,
  'ALTER TABLE t_service_schedule ADD COLUMN slot varchar(10) NOT NULL DEFAULT ''整天'' COMMENT ''占用时段:整天/上午/下午/晚间'' AFTER status',
  'SELECT ''slot 已存在'' AS msg');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_venue := (SELECT COUNT(*) FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 't_service_schedule' AND column_name = 'venue');
SET @sql := IF(@has_venue = 0,
  'ALTER TABLE t_service_schedule ADD COLUMN venue varchar(100) NULL COMMENT ''场地名称(选填)'' AFTER slot',
  'SELECT ''venue 已存在'' AS msg');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2) 换唯一键:老键 (service_id, schedule_date) → 新键 (service_id, schedule_date, slot)
SET @has_old := (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE() AND table_name = 't_service_schedule' AND index_name = 'uk_service_date');
SET @sql := IF(@has_old > 0,
  'ALTER TABLE t_service_schedule DROP INDEX uk_service_date',
  'SELECT ''uk_service_date 已不存在'' AS msg');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_new := (SELECT COUNT(*) FROM information_schema.statistics
                 WHERE table_schema = DATABASE() AND table_name = 't_service_schedule' AND index_name = 'uk_service_date_slot');
SET @sql := IF(@has_new = 0,
  'ALTER TABLE t_service_schedule ADD UNIQUE KEY uk_service_date_slot (service_id, schedule_date, slot)',
  'SELECT ''uk_service_date_slot 已存在'' AS msg');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 3) 存量行补齐时段(老数据没有 slot,统一按"整天"算)
UPDATE t_service_schedule SET slot = '整天' WHERE slot IS NULL OR slot = '';

-- 4) 自检:应看到 slot / venue 两列 与 uk_service_date_slot 唯一键
SHOW COLUMNS FROM t_service_schedule;
SHOW INDEX FROM t_service_schedule WHERE Key_name IN ('uk_service_date', 'uk_service_date_slot');
