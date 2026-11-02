-- 2026-11-02 迁移：评价"人工隐藏"的操作人留痕（问题总账 G-01）
-- 背景：t_evaluation.audit_operator_id 只记"审核"（通过 / 驳回）的操作人；2026-10-31 上线的人工隐藏
--       接口不接收管理员身份，隐藏后查不到是谁隐藏的（《测试用例与记录》4.25 节 R-47 实测回库为 NULL）。
-- 本次单加一列 hidden_operator_id，与 audit_operator_id 分工：
--   audit_operator_id  —— 谁审的（通过 / 驳回），隐藏不改写它，恢复也不碰它
--   hidden_operator_id —— 谁隐藏的；恢复时清空（与 hidden_at 同生同灭）
--
-- 用法（MySQL 起来后执行）：
--   mysql --user=root --password --database=wedding < sql/migrate_hidden_operator_20261102.sql
--   （--password 不带值时会交互提示；口令不进脚本）
--
-- 说明：可空、存量数据保持 NULL（历史上"隐藏后又恢复"的评价本来就没有隐藏人可追）；
--      不建索引 —— 只用于展示追溯，没有"按隐藏人筛选"的查询。

-- ⚠️ 先固定连接字符集：Windows 控制台的 mysql 客户端默认可能按 gbk 解释输入，
--    中文 COMMENT 会被写成双重编码的乱码（2026-11-02 实测踩过）。
SET NAMES utf8mb4;

-- t_evaluation 加 hidden_operator_id（已存在则跳过：MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，故先查 information_schema）
SET @has_ho := (SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 't_evaluation' AND column_name = 'hidden_operator_id');
SET @sql := IF(@has_ho = 0,
  'ALTER TABLE t_evaluation ADD COLUMN hidden_operator_id bigint NULL COMMENT ''人工隐藏操作人,关联 t_user.id;恢复时清空'' AFTER hidden_at',
  'SELECT ''hidden_operator_id(t_evaluation) 已存在'' AS msg');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
