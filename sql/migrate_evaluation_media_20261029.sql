-- 评价媒体化 + 匿名（2026-10-29）
-- 目标：t_evaluation 支持「图片最多 9 张 + 视频 1 个 + 可选匿名」。
-- 幂等：列已存在则跳过；不重建表、不动存量行（存量 7 条自动取 anonymous=0、video_url=NULL）。
USE wedding;

SET @has_video := (SELECT COUNT(*) FROM information_schema.columns
                   WHERE table_schema = 'wedding' AND table_name = 't_evaluation' AND column_name = 'video_url');
SET @sql_video := IF(@has_video = 0,
  'ALTER TABLE t_evaluation ADD COLUMN video_url varchar(255) NULL COMMENT ''评价视频路径，一条评价最多 1 个'' AFTER images',
  'SELECT ''video_url 已存在，跳过'' AS skip_note');
PREPARE stmt_video FROM @sql_video;
EXECUTE stmt_video;
DEALLOCATE PREPARE stmt_video;

SET @has_anon := (SELECT COUNT(*) FROM information_schema.columns
                  WHERE table_schema = 'wedding' AND table_name = 't_evaluation' AND column_name = 'anonymous');
SET @sql_anon := IF(@has_anon = 0,
  'ALTER TABLE t_evaluation ADD COLUMN anonymous tinyint(1) NOT NULL DEFAULT 0 COMMENT ''是否匿名展示：1=门户只看得到匿名用户'' AFTER video_url',
  'SELECT ''anonymous 已存在，跳过'' AS skip_note');
PREPARE stmt_anon FROM @sql_anon;
EXECUTE stmt_anon;
DEALLOCATE PREPARE stmt_anon;
