-- ============================================================
-- 缘集 · 补充真实感数据（用于功能测试；可重复执行 INSERT IGNORE）
-- 4 家新商家 + 18 个服务 + 14 笔订单 + 6 条评价 + 公告/资讯
-- 订单的 user_id 统一用演示用户 user(3)，便于在「我的订单」里翻页、筛选状态。
-- ============================================================
USE wedding;

-- ---------- 1. 商家账号 ----------
INSERT IGNORE INTO t_user (id, username, password, real_name, phone, role, status, created_at) VALUES
(106, 'xinghe',  '123456', '星河婚典', '13800000106', 'MERCHANT', 1, NOW()),
(107, 'yinuo',   '123456', '一诺摄影', '13800000107', 'MERCHANT', 1, NOW()),
(108, 'baisha',  '123456', '白纱婚纱馆', '13800000108', 'MERCHANT', 1, NOW()),
(109, 'shunfeng','123456', '顺风婚车', '13800000109', 'MERCHANT', 1, NOW());

-- ---------- 2. 店铺 ----------
INSERT IGNORE INTO t_merchant (id, user_id, shop_name, business_scope, contact_phone, description, audit_status, status, created_at) VALUES
(106, 106, '星河婚典',   '婚礼套餐 / 场地', '13800000106', '主打梦幻星空主题婚礼，擅长灯光与舞美设计。',           'APPROVED', 1, NOW()),
(107, 107, '一诺摄影',   '婚礼跟拍 / 婚纱摄影', '13800000107', '十年纪实摄影，擅长捕捉婚礼中最自然的情绪瞬间。',       'APPROVED', 1, NOW()),
(108, 108, '白纱婚纱馆', '婚纱礼服',       '13800000108', '上百款主纱与敬酒服，含试穿、妆造与清洗。',               'APPROVED', 1, NOW()),
(109, 109, '顺风婚车',   '婚车车队',       '13800000109', '多款豪华车型，主车与跟车统一调度，准点接亲。',           'APPROVED', 1, NOW());

-- ---------- 3. 服务 ----------
INSERT IGNORE INTO t_service (id, merchant_id, category_id, name, description, price, unit, audit_status, shelf_status, view_count, sales_count, score, created_at) VALUES
-- 星河婚典（套餐 2 + 场地 2）
(300001, 106, 2, '星空主题婚礼套餐', '含星空顶棚、灯光舞美与四大金刚，适合 20 桌以内。', 35999.00, '套', 'APPROVED', 1, 980, 460, 4.9, NOW()),
(300002, 106, 2, '森系户外婚礼',     '绿植花艺 + 户外仪式，含桌椅与音响。',             26999.00, '套', 'APPROVED', 1, 760, 320, 4.8, NOW()),
(300003, 106, 1, '星光宴会厅',       '星光顶宴会厅，可容纳 150 人，含舞台与灯光。',       15999.00, '场', 'APPROVED', 1, 640, 280, 4.7, NOW()),
(300004, 106, 1, '小型仪式布置',     '20 人以内小型仪式，含简单布置与花艺。',             6999.00, '场', 'APPROVED', 1, 520, 210, 4.6, NOW()),
-- 一诺摄影（摄影 5）
(300005, 107, 4, '双机位婚礼跟拍',   '主摄 + 副摄，全天跟拍，精修 80 张。',              7999.00, '天', 'APPROVED', 1, 1680, 640, 4.9, NOW()),
(300006, 107, 4, '婚礼快剪视频',     '当天快剪，仪式后 2 小时出片。',                     3999.00, '条', 'APPROVED', 1, 1120, 430, 4.8, NOW()),
(300007, 107, 4, '新娘跟妆',         '早妆 + 晚妆 + 补妆，含试妆。',                      2999.00, '次', 'APPROVED', 1, 940, 390, 4.7, NOW()),
(300008, 107, 4, '婚纱照旅拍',       '周边城市旅拍，含 2 套服装与精修 50 张。',           8999.00, '套', 'APPROVED', 1, 720, 260, 4.8, NOW()),
(300009, 107, 4, '婚礼 MV 拍摄',     '3-5 分钟婚礼 MV，含脚本与后期。',                   5999.00, '条', 'APPROVED', 1, 560, 190, 4.7, NOW()),
-- 白纱婚纱馆（婚纱 4）
(300010, 108, 5, '主纱租赁 · 法式缎面', '法式缎面主纱，含头纱与试穿。',                   3999.00, '套', 'APPROVED', 1, 1320, 520, 4.9, NOW()),
(300011, 108, 5, '敬酒服租赁',       '红色敬酒服，含清洗与改衣。',                       1999.00, '套', 'APPROVED', 1, 880, 360, 4.6, NOW()),
(300012, 108, 5, '伴娘服租赁（3 套）', '伴娘服 3 套，含清洗。',                           999.00, '组', 'APPROVED', 1, 640, 300, 4.5, NOW()),
(300013, 108, 5, '中式秀禾服租赁',   '秀禾服 + 凤冠，含妆造。',                           2599.00, '套', 'APPROVED', 1, 700, 280, 4.8, NOW()),
-- 顺风婚车（婚车 3）
(300014, 109, 6, '劳斯莱斯主婚车',   '白色劳斯莱斯主车，含司机与鲜花装饰。',             4599.00, '辆', 'APPROVED', 1, 860, 340, 4.9, NOW()),
(300015, 109, 6, '奔驰 S 级车队（6 辆）', '奔驰 S 级主车 + 5 辆跟车，统一装饰。',          6999.00, '队', 'APPROVED', 1, 720, 280, 4.8, NOW()),
(300016, 109, 6, '复古老爷车',       '复古老爷车，适合中式或主题婚礼。',                 3599.00, '辆', 'APPROVED', 1, 540, 200, 4.7, NOW()),
(300017, 109, 6, '商务车跟车（5 辆）', '别克 GL8 跟车 5 辆，接送亲友。',                   2999.00, '队', 'APPROVED', 1, 480, 180, 4.5, NOW()),
-- 补充：一诺摄影 婚礼花絮
(300018, 107, 4, '婚礼花絮跟拍',     '半天花絮跟拍，精修 30 张。',                       2599.00, '半天', 'APPROVED', 1, 620, 240, 4.6, NOW());

-- ---------- 4. 订单（14 笔，覆盖五种状态） ----------
INSERT IGNORE INTO t_order
  (order_no, user_id, merchant_id, service_id, wedding_date, guest_count, budget_min, budget_max, quantity, amount, remark, status, confirmed_at, completed_at, cancelled_at, cancel_reason, created_at)
VALUES
('O20261018B001', 3, 106, 300001, '2026-12-12', 150, 30000.00, 38000.00, 1, 35999.00, '想要星空顶，长辈较多。', 'PENDING',   NULL,   NULL,   NULL,  NULL, NOW()),
('O20261018B002', 3, 106, 300003, '2026-12-20', 120, 14000.00, 18000.00, 1, 15999.00, '宴会厅要带舞台。',       'CONFIRMED', NOW(),  NULL,   NULL,  NULL, NOW()),
('O20261018B003', 3, 107, 300005, '2026-11-08',  80,  7000.00,  9000.00, 1,  7999.00, '双机位，要自然抓拍。',   'COMPLETED', NOW(),  NOW(),  NULL,  NULL, NOW()),
('O20261018B004', 3, 108, 300010, '2026-11-15',   0,  3500.00,  4500.00, 1,  3999.00, '法式缎面主纱，S 码。',   'IN_PROGRESS', NOW(), NULL,  NULL,  NULL, NOW()),
('O20261018B005', 3, 109, 300014, '2026-12-12',   0,  4000.00,  5000.00, 1,  4599.00, '白色劳斯莱斯。',         'CONFIRMED', NOW(),  NULL,   NULL,  NULL, NOW()),
('O20261018B006', 3, 107, 300006, '2026-11-08',   0,  3000.00,  4500.00, 1,  3999.00, '仪式后两小时要成片。',   'COMPLETED', NOW(),  NOW(),  NULL,  NULL, NOW()),
('O20261018B007', 3, 106, 300002, '2026-10-20', 100, 24000.00, 29000.00, 1, 26999.00, '户外森系，需备雨天。',   'COMPLETED', NOW(),  NOW(),  NULL,  NULL, NOW()),
('O20261018B008', 3, 108, 300011, '2026-11-15',   0,  1500.00,  2200.00, 1,  1999.00, '红色敬酒服。',           'PENDING',   NULL,   NULL,   NULL,  NULL, NOW()),
('O20261018B009', 3, 107, 300007, '2026-11-08',   0,  2500.00,  3200.00, 1,  2999.00, '早妆 + 晚妆。',         'CANCELLED', NULL,   NULL,   NOW(), '档期冲突', NOW()),
('O20261018B010', 3, 106, 300004, '2026-10-28',  20,  6000.00,  7500.00, 1,  6999.00, '小型仪式，20 人内。',   'IN_PROGRESS', NOW(), NULL,  NULL,  NULL, NOW()),
('O20261018B011', 3, 109, 300015, '2026-12-20',   0,  6000.00,  7500.00, 1,  6999.00, '奔驰车队，6 辆。',      'COMPLETED', NOW(),  NOW(),  NULL,  NULL, NOW()),
('O20261018B012', 3, 108, 300013, '2026-11-22',   0,  2200.00,  3000.00, 1,  2599.00, '中式秀禾服。',           'PENDING',   NULL,   NULL,   NULL,  NULL, NOW()),
('O20261018B013', 3, 107, 300008, '2026-09-18',   0,  8000.00, 10000.00, 1,  8999.00, '旅拍，去周边城市。',     'CANCELLED', NULL,   NULL,   NOW(), '时间调整', NOW()),
('O20261018B014', 3, 106, 300001, '2026-08-18', 150, 30000.00, 38000.00, 1, 35999.00, '已完成的星空婚礼。',     'COMPLETED', NOW(),  NOW(),  NULL,  NULL, NOW());

-- ---------- 5. 评价（已完成订单，部分已过审、部分待审） ----------
INSERT IGNORE INTO t_evaluation (order_id, user_id, merchant_id, service_id, score, content, audit_status, audit_time, created_at)
SELECT id, user_id, merchant_id, service_id, 5, '现场效果比预期还好，灯光和星空顶很出片。', 'APPROVED', NOW(), NOW()
FROM t_order WHERE order_no = 'O20261018B003';
INSERT IGNORE INTO t_evaluation (order_id, user_id, merchant_id, service_id, score, content, audit_status, audit_time, created_at)
SELECT id, user_id, merchant_id, service_id, 4, '快剪很及时，仪式后两小时就拿到了。', 'APPROVED', NOW(), NOW()
FROM t_order WHERE order_no = 'O20261018B006';
INSERT IGNORE INTO t_evaluation (order_id, user_id, merchant_id, service_id, score, content, audit_status, audit_time, created_at)
SELECT id, user_id, merchant_id, service_id, 5, '户外森系布置很用心，雨天预案也准备了。', 'PENDING', NULL, NOW()
FROM t_order WHERE order_no = 'O20261018B007';
INSERT IGNORE INTO t_evaluation (order_id, user_id, merchant_id, service_id, score, content, audit_status, audit_time, created_at)
SELECT id, user_id, merchant_id, service_id, 5, '车队准点，司机很专业。', 'APPROVED', NOW(), NOW()
FROM t_order WHERE order_no = 'O20261018B011';
INSERT IGNORE INTO t_evaluation (order_id, user_id, merchant_id, service_id, score, content, audit_status, audit_time, created_at)
SELECT id, user_id, merchant_id, service_id, 5, '星空婚礼很惊艳，长辈也很满意。', 'APPROVED', NOW(), NOW()
FROM t_order WHERE order_no = 'O20261018B014';

-- ---------- 6. 公告 / 资讯 ----------
INSERT IGNORE INTO t_announcement (publisher_id, title, content, status, publish_time, created_at) VALUES
(1, '缘集平台服务保障再升级：支持争议在线举证', '为解决婚礼服务纠纷取证难的问题，平台已上线争议在线举证功能：当事人可上传合同、聊天记录、现场照片等材料，由管理员统一仲裁。', 1, NOW(), NOW()),
(1, '冬季婚礼档期提醒', '12 月为婚礼旺季，热门场地与司仪档期紧张，建议提前 2 个月预约，并确认天气预案。', 1, NOW(), NOW());

INSERT IGNORE INTO t_article (publisher_id, title, content, author, view_count, status, publish_time, created_at) VALUES
(1, '婚礼预算怎么分配才不超支？', '建议按「场地 30% / 餐饮 30% / 摄影摄像 15% / 服饰 10% / 其他 15%」分配，并为突发项预留 5%-10% 的机动预算。', '缘集编辑部', 480, 1, NOW(), NOW()),
(1, '户外婚礼的雨天预案清单', '户外婚礼一定要准备 Plan B：帐篷或备用厅、雨伞与防滑垫、备用鞋、以及和场地方的书面确认。', '缘集编辑部', 360, 1, NOW(), NOW());

-- ---------- 7. 体检 ----------
SELECT 'merchants_approved' AS t, COUNT(*) AS n FROM t_merchant WHERE audit_status='APPROVED' AND status=1
UNION ALL SELECT 'services', COUNT(*) FROM t_service
UNION ALL SELECT 'orders', COUNT(*) FROM t_order
UNION ALL SELECT 'evaluations', COUNT(*) FROM t_evaluation
UNION ALL SELECT 'announcements', COUNT(*) FROM t_announcement
UNION ALL SELECT 'articles', COUNT(*) FROM t_article;
