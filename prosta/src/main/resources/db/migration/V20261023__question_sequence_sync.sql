-- question に固定IDで初期データを入れた後、seq_question が既存の最大IDより小さいと
-- 新規登録で主キー重複になる。シーケンスを最大IDより先へ進める（何度実行しても安全）。
-- 問題データ本体は「競合回避_大塚\データ投入.sql」を使う。
DECLARE
    v NUMBER;
    m NUMBER;
BEGIN
    SELECT NVL(MAX(question_id), 0) INTO m FROM question;
    SELECT seq_question.NEXTVAL INTO v FROM dual;
    WHILE v <= m LOOP
        SELECT seq_question.NEXTVAL INTO v FROM dual;
    END LOOP;
END;
/