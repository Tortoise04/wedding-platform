-- 2026-10-30 迁移:处罚可解除 → t_penalty 增加解除留痕三列
-- 背景:处罚原来只能执行不能撤销,解封只能靠"启用商家"顺手把 BAN 罚单置 0,
--      且不记录是谁解的、什么时候解的、为什么解。本次加 release_operator_id / release_time / release_reason,
--      解除时把这三样落库留痕(见 PenaltyServiceImpl.releasePenalty)。
--
-- 用法(MySQL 起来后执行):
--   mysql --user=root --password --database=wedding < sql/migrate_penalty_release_20261030.sql
--   （--password 不带值时会交互提示；口令不进脚本）
--
-- 说明:三列都是可空的,存量罚单保持 NULL 即可(它们本来就没被解除过)。
--      Hibernate(ddl-auto=update)启动时也能补齐列,但这个脚本是可重复执行的那一份,便于核对。

-- 1) 加列(已存在则跳过:MySQL 8 不支持 ADD COLUMN IF NOT EXISTS,故先查 information_schema)
SET @has_release_operator := (SELECT COUNT(*) FROM information_schema.columns
                              WHERE table_schema = DATABASE() AND table_name = 't_penalty' AND column_name = 'release_operator_id');
SET @sql := IF(@has_release_operator = 0,
  'ALTER TABLE t_penalty ADD COLUMN release_operator_id bigint NULL COMMENT ''解除人,关联 t_user.id;未解除为空'' AFTER status',
  'SELECT ''release_operator_id 已存在'' AS msg');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_release_time := (SELECT COUNT(*) FROM information_schema.columns
                          WHERE table_schema = DATABASE() AND table_name = 't_penalty' AND column_name = 'release_time');
SET @sql := IF(@has_release_time = 0,
  'ALTER TABLE t_penalty ADD COLUMN release_time datetime NULL COMMENT ''解除时间;未解除为空'' AFTER release_operator_id',
  'SELECT ''release_time 已存在'' AS msg');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_release_reason := (SELECT COUNT(*) FROM information_schema.columns
                            WHERE table_schema = DATABASE() AND table_name = 't_penalty' AND column_name = 'release_reason');
SET @sql := IF(@has_release_reason = 0,
  'ALTER TABLE t_penalty ADD COLUMN release_reason varchar(500) NULL COMMENT ''解除原因'' AFTER release_time',
  'SELECT ''release_reason 已存在'' AS msg');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2) 自检:应看到 status 之后紧跟 release_operator_id / release_time / release_reason 三列
SHOW COLUMNS FROM t_penalty;
