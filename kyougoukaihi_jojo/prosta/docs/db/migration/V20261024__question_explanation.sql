-- 問題の解説カラムを追加（手動適用）
ALTER TABLE question ADD (explanation VARCHAR2(1000));

-- 解説の登録例（question_id は実データに合わせる）
-- UPDATE question SET explanation = 'ここに解説を入力' WHERE question_id = 1;
-- COMMIT;
