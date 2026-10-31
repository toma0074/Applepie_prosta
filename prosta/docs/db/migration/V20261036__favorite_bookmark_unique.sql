-- ブックマーク(FAVORITE)の二重登録防止。適用前に重複がないか確認し、あれば古い方を残して削除する。
-- SELECT user_id, question_id, COUNT(*) FROM favorite GROUP BY user_id, question_id HAVING COUNT(*) > 1;
DELETE FROM favorite f WHERE f.rowid NOT IN (SELECT MIN(rowid) FROM favorite GROUP BY user_id, question_id);
ALTER TABLE favorite ADD CONSTRAINT uq_favorite_user_question UNIQUE (user_id, question_id);
COMMENT ON TABLE favorite IS 'ブックマーク管理テーブル(問題の後で見返す印)';
COMMENT ON COLUMN favorite.favorite_id IS 'ブックマークID';
COMMIT;
