-- パスワードをBCryptでハッシュ化して保存するため、列を広げる（手動適用）
-- 既存の平文パスワードは、そのユーザーが次にログインしたときに自動でハッシュ化される
ALTER TABLE customer MODIFY (user_pass VARCHAR2(100));
