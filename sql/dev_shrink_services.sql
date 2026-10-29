-- =============================================================================
--  开发用服务数据「瘦身」脚本：把 dev_testdata_service.sql 生成的 10 万行演示服务
--  压缩到「每个商家保留 @keep 条」（默认 20），总量控制在千条以内，
--  供商家端服务管理 / 前端流畅演示。
--
--  用法（可重复执行，第二次执行时因已无多余演示服务，删除 0 行，不报错）：
--     mysql -u root -p --default-character-set=utf8mb4 -e "source sql/dev_shrink_services.sql"
--     （-p 后不要跟口令：回车后由 mysql 交互提示）
--
--  ⚠️ 只清理「演示服务」（name LIKE '%-演示-%'，即 dev_testdata_service.sql 的产物）；
--     不动 schema.sql 表结构、不动 dev_seed.sql 的账号与商家种子、不动非演示服务。
--     演示服务无外键约束，但有逻辑关联（订单/收藏/评价/档期/案例），先清关联再删本体。
-- =============================================================================

USE wedding;

-- 每个商家保留的演示服务条数（可按需改，≤20 时总量 = 商家数 × 该值，约千条以内）
SET @keep := 20;

-- ① 清理前各表行数
SELECT '清理前' AS 阶段, 't_service' AS 表名, COUNT(*) AS 行数 FROM t_service
UNION ALL SELECT '清理前','t_order',COUNT(*) FROM t_order
UNION ALL SELECT '清理前','t_order_status_log',COUNT(*) FROM t_order_status_log
UNION ALL SELECT '清理前','t_favorite',COUNT(*) FROM t_favorite
UNION ALL SELECT '清理前','t_browse_history',COUNT(*) FROM t_browse_history
UNION ALL SELECT '清理前','t_evaluation',COUNT(*) FROM t_evaluation
UNION ALL SELECT '清理前','t_service_schedule',COUNT(*) FROM t_service_schedule
UNION ALL SELECT '清理前','t_service_case',COUNT(*) FROM t_service_case
UNION ALL SELECT '清理前','t_penalty',COUNT(*) FROM t_penalty;

-- ② 找出要删除的演示服务：每个商家按 id 升序保留前 @keep 条，其余删除
DROP TEMPORARY TABLE IF EXISTS tmp_del_services;
CREATE TEMPORARY TABLE tmp_del_services (id BIGINT PRIMARY KEY)
AS
SELECT id FROM (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY merchant_id ORDER BY id) AS rn
    FROM t_service
    WHERE name LIKE '%-演示-%'
) t WHERE rn > @keep;

-- ③ 先清关联数据（订单及其流水、收藏、浏览、评价、档期、案例），再删服务本体；
--    处罚记录 target_id 指向被下架服务 → 置空留痕（罚单本身不删，追溯仍在）
DELETE lo FROM t_order_status_log  lo JOIN t_order o ON o.id = lo.order_id JOIN tmp_del_services d ON d.id = o.service_id;
DELETE o  FROM t_order             o  JOIN tmp_del_services d ON d.id = o.service_id;
DELETE f  FROM t_favorite          f  JOIN tmp_del_services d ON d.id = f.service_id;
DELETE b  FROM t_browse_history    b  JOIN tmp_del_services d ON d.id = b.service_id;
DELETE e  FROM t_evaluation        e  JOIN tmp_del_services d ON d.id = e.service_id;
DELETE s  FROM t_service_schedule  s  JOIN tmp_del_services d ON d.id = s.service_id;
DELETE c  FROM t_service_case      c  JOIN tmp_del_services d ON d.id = c.service_id;
UPDATE t_penalty p JOIN tmp_del_services d ON d.id = p.target_id SET p.target_id = NULL;
DELETE sv FROM t_service sv JOIN tmp_del_services d ON d.id = sv.id;

DROP TEMPORARY TABLE IF EXISTS tmp_del_services;

-- ④ 清理后各表行数
SELECT '清理后' AS 阶段, 't_service' AS 表名, COUNT(*) AS 行数 FROM t_service
UNION ALL SELECT '清理后','t_order',COUNT(*) FROM t_order
UNION ALL SELECT '清理后','t_order_status_log',COUNT(*) FROM t_order_status_log
UNION ALL SELECT '清理后','t_favorite',COUNT(*) FROM t_favorite
UNION ALL SELECT '清理后','t_browse_history',COUNT(*) FROM t_browse_history
UNION ALL SELECT '清理后','t_evaluation',COUNT(*) FROM t_evaluation
UNION ALL SELECT '清理后','t_service_schedule',COUNT(*) FROM t_service_schedule
UNION ALL SELECT '清理后','t_service_case',COUNT(*) FROM t_service_case
UNION ALL SELECT '清理后','t_penalty',COUNT(*) FROM t_penalty;

-- ⑤ 抽查：每商家保留的演示服务条数（应 ≤ @keep）
SELECT merchant_id, COUNT(*) AS 演示服务数
FROM t_service WHERE name LIKE '%-演示-%'
GROUP BY merchant_id
ORDER BY 演示服务数 DESC
LIMIT 5;
