-- answer.ans_count が未作成のDB向けの補完SQL（チーム共有DDL「4.(1)」の内容）。
-- 既存の解答行があっても NOT NULL 追加で失敗しないよう DEFAULT 1 を付ける。
-- 何度実行しても安全。SQL Developerで「スクリプトの実行(F5)」を使うこと。
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM user_tab_columns
    WHERE table_name = 'ANSWER' AND column_name = 'ANS_COUNT';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE answer ADD (ans_count NUMBER(5) DEFAULT 1 NOT NULL)';
    END IF;
END;
/

COMMENT ON COLUMN answer.ans_count IS '解答回数';

COMMIT;
