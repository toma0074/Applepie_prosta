-- 背景の「NEW」バッジ用に、背景選択画面を最後に確認した時点のレベルを保持する。
-- 既存ユーザーは現在のレベルで初期化し、導入時に一斉にバッジが付かないようにする。
ALTER TABLE customer ADD (background_seen_level NUMBER(10,0) DEFAULT 1 NOT NULL);
COMMENT ON COLUMN customer.background_seen_level IS '背景選択画面を最後に確認したレベル';
UPDATE customer SET background_seen_level = GREATEST(1, NVL(user_level, 1));
COMMIT;
