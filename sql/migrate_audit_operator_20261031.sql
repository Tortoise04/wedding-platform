-- 2026-10-31 迁移:审核操作人留痕 + 评价"驳回/隐藏"分离
-- 背景:管理端审核(商家入驻 / 服务 / 评价)此前只写 audit_time,不记是谁审的,无法留痕。
--      评价的"审核驳回"与"通过后人工隐藏"都复用 REJECTED,没有字段能区分。
--      本次给三张审核表加 audit_operator_id,并给 t_evaluation 加 hidden_at。
--
-- 用法(MySQL 起来后执行):
--   mysql --user=root --password --database=wedding < sql/migrate_audit_operator_20261031.sql
--   （--password 不带值时会交互提示；口令不进脚本）
--
-- 说明:这些列都是可空的,存量数据保持 NULL 即可(它们本来就没有留痕/隐藏标记)。
--      Hibernate(ddl-auto=update)启动时也能补齐列,但这个脚本是可重复执行的那一份,便于核对。

-- 1) t_merchant 加 audit_operator_id(已存在则跳过:MySQL 8 不支持 ADD COLUMN IF NOT EXISTS,故先查 information_schema)
SET @has_m := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 't_merchant' AND column_name = 'audit_operator_id');
SET @sql := IF(@has_m = 0,
  'ALTER TABLE t_merchant ADD COLUMN audit_operator_id bigint NULL COMMENT ''审核操作人,关联 t_user.id;未审核为空'' AFTER audit_time',
  'SELECT ''audit_operator_id(t_merchant) 已存在'' AS msg');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2) t_service 加 audit_operator_id
SET @has_s := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 't_service' AND column_name = 'audit_operator_id');
SET @sql := IF(@has_s = 0,
  'ALTER TABLE t_service ADD COLUMN audit_operator_id bigint NULL COMMENT ''审核操作人,关联 t_user.id;未审核为空'' AFTER audit_status',
  'SELECT ''audit_operator_id(t_service) 已存在'' AS msg');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 3) t_evaluation 加 audit_operator_id + hidden_at
SET @has_e := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 't_evaluation' AND column_name = 'audit_operator_id');
SET @sql := IF(@has_e = 0,
  'ALTER TABLE t_evaluation ADD COLUMN audit_operator_id bigint NULL COMMENT ''审核操作人,关联 t_user.id;未审核为空'' AFTER audit_time',
  'SELECT ''audit_operator_id(t_evaluation) 已存在'' AS msg');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has_h := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = 't_evaluation' AND column_name = 'hidden_at');
SET @sql := IF(@has_h = 0,
  'ALTER TABLE t_evaluation ADD COLUMN hidden_at datetime NULL COMMENT ''人工隐藏时间;非空=当前处于人工隐藏态,审核驳回时为 null'' AFTER audit_operator_id',
  'SELECT ''hidden_at 已存在'' AS msg');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 4) 自检:应看到 audit_operator_id / hidden_at 列
SHOW COLUMNS FROM t_merchant;
SHOW COLUMNS FROM t_service;
SHOW COLUMNS FROM t_evaluation;
