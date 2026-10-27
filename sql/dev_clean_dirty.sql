-- ============================================================
-- 缘集 · 清理历史脏数据（可重复执行）
-- 目标：删掉早期演示/测试遗留的「演示」服务、挂在已不存在商家上的订单、
--       测试账号、被封测试店铺与罚单；把「缘集测试套餐」改为正式名称。
-- 说明：不改表结构；只删历史脏数据，保留真实商家与服务、以及新补的演示数据。
-- ============================================================
USE wedding;

-- 1) 测试账号（5/6/7/8）及其咨询会话/消息
DELETE m FROM t_consult_message m
  JOIN t_consultation c ON c.id = m.consultation_id
  WHERE c.user_id IN (5, 6, 7, 8);
DELETE FROM t_consultation WHERE user_id IN (5, 6, 7, 8);

-- 2) 脏订单：商家 id 已不存在（历史演示数据挂在 merchant_id=42 等上）
--    先清关联的争议/举证/流转日志，再删订单
DELETE e FROM t_dispute_evidence e
  JOIN t_dispute d ON d.id = e.dispute_id
  WHERE d.order_id IN (SELECT id FROM t_order WHERE merchant_id NOT IN (SELECT id FROM t_merchant));
DELETE FROM t_dispute WHERE order_id IN (SELECT id FROM t_order WHERE merchant_id NOT IN (SELECT id FROM t_merchant));
DELETE FROM t_order_status_log WHERE order_id IN (SELECT id FROM t_order WHERE merchant_id NOT IN (SELECT id FROM t_merchant));
DELETE FROM t_order WHERE merchant_id NOT IN (SELECT id FROM t_merchant);

-- 3) 1000 条「演示」服务（名字带「演示」），连同其档期/案例/收藏/浏览/评价
DELETE FROM t_service_schedule WHERE service_id IN (SELECT id FROM t_service WHERE name LIKE '%演示%');
DELETE FROM t_service_case    WHERE service_id IN (SELECT id FROM t_service WHERE name LIKE '%演示%');
DELETE FROM t_favorite        WHERE service_id IN (SELECT id FROM t_service WHERE name LIKE '%演示%');
DELETE FROM t_browse_history  WHERE service_id IN (SELECT id FROM t_service WHERE name LIKE '%演示%');
DELETE FROM t_evaluation      WHERE service_id IN (SELECT id FROM t_service WHERE name LIKE '%演示%');
DELETE FROM t_service WHERE name LIKE '%演示%';

-- 4) 被封测试店铺 + 罚单 + 测试账号
DELETE FROM t_penalty WHERE merchant_id IN (SELECT id FROM t_merchant WHERE shop_name = '临时被封店铺');
DELETE FROM t_merchant WHERE shop_name = '临时被封店铺';
DELETE FROM t_user WHERE username IN ('smoketest_2882','regtest_1792140894','stat_1792140908','ban_1792140908');

-- 5) 「缘集测试套餐」改为正式名称（保留其订单与争议）
UPDATE t_service
   SET name = '缘集优选 · 经典婚礼套餐',
       description = '含场地布置、四大金刚与当日督导，适合 10-20 桌的中式婚礼。'
 WHERE id = 131071;

-- 6) 体检
SELECT 'users' AS t, COUNT(*) AS n FROM t_user
UNION ALL SELECT 'merchants', COUNT(*) FROM t_merchant
UNION ALL SELECT 'services', COUNT(*) FROM t_service
UNION ALL SELECT 'orders', COUNT(*) FROM t_order
UNION ALL SELECT 'evaluations', COUNT(*) FROM t_evaluation
UNION ALL SELECT 'disputes', COUNT(*) FROM t_dispute
UNION ALL SELECT 'penalties', COUNT(*) FROM t_penalty;
