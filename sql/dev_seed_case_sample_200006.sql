-- 样例素材：service 200006「全天跟拍 · 婚礼纪实摄影」（商家 102 拾光影像）
-- 用途：演示服务详情页的案例区与列表页封面；学习/毕设用途，图片来源 Pexels 免费图库。
-- 可重放：先按 service_id 清空本服务旧案例，再插入 3 条，最后把首图设为该服务封面。
-- 生成时间：2026-10-29；备份见 sql/_backup/before_cases_20261029-141130.sql 与 before_covers_20261029-141130.sql

DELETE FROM t_service_case WHERE service_id = 200006;

INSERT INTO t_service_case (service_id, image_url, video_url, title, description, category, sort_order, created_at) VALUES
(200006, '/uploads/202610/cb1d2be4-3f3a-4a52-9d2e-7c8a5f6b1e01.jpg', NULL, '晨曦迎亲 · 新人合影', '清晨迎亲环节的双人合影，自然光侧逆光处理，呈现新人刚到场时的真实神态。', '外景', 1, NOW()),
(200006, '/uploads/202610/cb1d2be4-3f3a-4a52-9d2e-7c8a5f6b1e02.jpg', NULL, '仪式纪实 · 交换对戒', '仪式核心环节抓拍，长焦压缩背景突出手部动作，黑白影调保留现场光的层次。', '仪式', 2, NOW()),
(200006, '/uploads/202610/cb1d2be4-3f3a-4a52-9d2e-7c8a5f6b1e03.jpg', NULL, '双机位跟拍 · 现场纪实', '双机位分工记录：主机位拍仪式流程，副机位跟拍花絮，当日中午交付精选预告。', '其他', 3, NOW());

UPDATE t_service SET cover_url = '/uploads/202610/cb1d2be4-3f3a-4a52-9d2e-7c8a5f6b1e01.jpg' WHERE id = 200006;
