-- ============================================================
-- 缘集 · 为订单补生成状态流转流水（可重复执行）
-- 目的：演示订单是直接写库的，没有 t_order_status_log，管理端「轨迹」看不到流转；
--       这里按订单当前状态反推一条合理的时间线（创建 → 接单 → 履约 → 完成 / 取消）。
-- ============================================================
USE wedding;

-- 1) 所有订单：创建 → PENDING（提交预约订单）
INSERT INTO t_order_status_log (order_id, from_status, to_status, operator_id, operator_role, remark, created_at)
SELECT o.id, NULL, 'PENDING', o.user_id, 'USER', '提交预约订单', o.created_at
FROM t_order o
WHERE NOT EXISTS (SELECT 1 FROM t_order_status_log l WHERE l.order_id = o.id);

-- 2) CONFIRMED 及以上：PENDING → CONFIRMED（商家接单）
INSERT INTO t_order_status_log (order_id, from_status, to_status, operator_id, operator_role, remark, created_at)
SELECT o.id, 'PENDING', 'CONFIRMED', COALESCE(m.user_id, o.merchant_id), 'MERCHANT', '商家接单确认', COALESCE(o.confirmed_at, o.created_at)
FROM t_order o LEFT JOIN t_merchant m ON m.id = o.merchant_id
WHERE o.status IN ('CONFIRMED','IN_PROGRESS','COMPLETED')
  AND NOT EXISTS (SELECT 1 FROM t_order_status_log l WHERE l.order_id = o.id AND l.to_status = 'CONFIRMED');

-- 3) IN_PROGRESS 及以上：CONFIRMED → IN_PROGRESS（开始履约）
INSERT INTO t_order_status_log (order_id, from_status, to_status, operator_id, operator_role, remark, created_at)
SELECT o.id, 'CONFIRMED', 'IN_PROGRESS', COALESCE(m.user_id, o.merchant_id), 'MERCHANT', '商家开始履约', COALESCE(o.confirmed_at, o.created_at)
FROM t_order o LEFT JOIN t_merchant m ON m.id = o.merchant_id
WHERE o.status IN ('IN_PROGRESS','COMPLETED')
  AND NOT EXISTS (SELECT 1 FROM t_order_status_log l WHERE l.order_id = o.id AND l.to_status = 'IN_PROGRESS');

-- 4) COMPLETED：IN_PROGRESS → COMPLETED（服务完成）
INSERT INTO t_order_status_log (order_id, from_status, to_status, operator_id, operator_role, remark, created_at)
SELECT o.id, 'IN_PROGRESS', 'COMPLETED', COALESCE(m.user_id, o.merchant_id), 'MERCHANT', '服务完成', COALESCE(o.completed_at, o.created_at)
FROM t_order o LEFT JOIN t_merchant m ON m.id = o.merchant_id
WHERE o.status = 'COMPLETED'
  AND NOT EXISTS (SELECT 1 FROM t_order_status_log l WHERE l.order_id = o.id AND l.to_status = 'COMPLETED');

-- 5) CANCELLED：PENDING → CANCELLED（取消）
INSERT INTO t_order_status_log (order_id, from_status, to_status, operator_id, operator_role, remark, created_at)
SELECT o.id, 'PENDING', 'CANCELLED', o.user_id, 'USER', COALESCE(o.cancel_reason, '取消订单'), COALESCE(o.cancelled_at, o.created_at)
FROM t_order o
WHERE o.status = 'CANCELLED'
  AND NOT EXISTS (SELECT 1 FROM t_order_status_log l WHERE l.order_id = o.id AND l.to_status = 'CANCELLED');

-- 体检
SELECT COUNT(*) AS logs_total FROM t_order_status_log;
SELECT order_id, COUNT(*) AS n FROM t_order_status_log GROUP BY order_id ORDER BY order_id;
