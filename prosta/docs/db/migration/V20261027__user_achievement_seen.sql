-- 実績の未確認マーク用。既存の解除済み実績は確認済み(1)として扱う。
ALTER TABLE user_achievement ADD (seen_flag NUMBER(1) DEFAULT 1 NOT NULL);
