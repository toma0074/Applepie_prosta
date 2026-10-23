-- =====================================================================
-- PROSTA スキーマ改善用 DDL (既存データ保持 / DROP・TRUNCATE なし)
-- 実行ユーザー: PROSTA_USER
-- 実行前に必ずバックアップ(expdp 等)を取得してください。
-- Spring の schema.sql 自動実行の対象外にするため db フォルダに配置しています。
-- =====================================================================
SET DEFINE OFF;

-- ---------------------------------------------------------------------
-- 0. 事前チェック (結果が 0 行であることを確認してから 1 以降を実行)
-- ---------------------------------------------------------------------
-- USER_NAME の重複 (1 の UNIQUE 追加が失敗する原因)
-- SELECT user_name, COUNT(*) FROM customer GROUP BY user_name HAVING COUNT(*) > 1;
-- お気に入りの重複 (7 の UNIQUE 追加が失敗する原因)
-- SELECT user_id, question_id, COUNT(*) FROM favorite GROUP BY user_id, question_id HAVING COUNT(*) > 1;

-- ---------------------------------------------------------------------
-- 1. CUSTOMER.USER_NAME に UNIQUE 制約
--    (論理削除ユーザーの名前も重複不可)
-- ---------------------------------------------------------------------
ALTER TABLE customer ADD CONSTRAINT uq_customer_user_name UNIQUE (user_name);

-- ---------------------------------------------------------------------
-- 2. パスワード列の拡張 (BCrypt 等のハッシュ値 60 文字に対応)
-- ---------------------------------------------------------------------
ALTER TABLE customer MODIFY (user_pass VARCHAR2(100 CHAR));

-- ---------------------------------------------------------------------
-- 3. BYTE 指定 → CHAR 指定 (日本語・絵文字で溢れないように)
-- ---------------------------------------------------------------------
ALTER TABLE customer    MODIFY (user_name VARCHAR2(20 CHAR));
ALTER TABLE category    MODIFY (category_name VARCHAR2(20 CHAR));
ALTER TABLE achievement MODIFY (
    achievement_name VARCHAR2(50 CHAR),
    description      VARCHAR2(200 CHAR),
    icon             VARCHAR2(10 CHAR)
);
ALTER TABLE question MODIFY (
    question_text VARCHAR2(1000 CHAR),
    option_a      VARCHAR2(300 CHAR),
    option_b      VARCHAR2(300 CHAR),
    option_c      VARCHAR2(300 CHAR)
);

-- ---------------------------------------------------------------------
-- 4. ID 列の桁数拡張 (拡大のみなので既存データ・FK は維持)
-- ---------------------------------------------------------------------
ALTER TABLE customer          MODIFY (user_id NUMBER(10,0));
ALTER TABLE answer            MODIFY (user_id NUMBER(10,0));
ALTER TABLE favorite          MODIFY (user_id NUMBER(10,0));
ALTER TABLE user_achievement  MODIFY (user_id NUMBER(10,0));
ALTER TABLE friend_connection MODIFY (
    user_low_id     NUMBER(10,0),
    user_high_id    NUMBER(10,0),
    requested_by_id NUMBER(10,0)
);

ALTER TABLE question MODIFY (question_id NUMBER(10,0));
ALTER TABLE answer   MODIFY (question_id NUMBER(10,0));
ALTER TABLE favorite MODIFY (question_id NUMBER(10,0));

ALTER TABLE answer           MODIFY (ans_id NUMBER(12,0));
ALTER TABLE user_achievement MODIFY (user_ach_id NUMBER(10,0));

-- ---------------------------------------------------------------------
-- 5. シーケンス上限の拡張 (現在値は維持される)
-- ---------------------------------------------------------------------
ALTER SEQUENCE seq_user             MAXVALUE 9999999999;
ALTER SEQUENCE seq_question         MAXVALUE 9999999999;
ALTER SEQUENCE seq_answer           MAXVALUE 999999999999;
ALTER SEQUENCE seq_favorite         MAXVALUE 9999999999;
ALTER SEQUENCE seq_user_achievement MAXVALUE 9999999999;
ALTER SEQUENCE seq_category         MAXVALUE 9999;
ALTER SEQUENCE seq_option           MAXVALUE 9999999999;

-- ---------------------------------------------------------------------
-- 6. CHECK 制約 (NOVALIDATE: 既存行は検査せず、今後の INSERT/UPDATE のみ検査)
--    PERMISSION は値の定義が未確定のため対象外
-- ---------------------------------------------------------------------
ALTER TABLE customer ADD CONSTRAINT ck_customer_delete_flag CHECK (delete_flag IN (0, 1)) ENABLE NOVALIDATE;
ALTER TABLE customer ADD CONSTRAINT ck_customer_login_flag  CHECK (login_flag  IN (0, 1)) ENABLE NOVALIDATE;
ALTER TABLE answer   ADD CONSTRAINT ck_answer_correct_flag  CHECK (correct_flag IN (0, 1)) ENABLE NOVALIDATE;
ALTER TABLE answer   ADD CONSTRAINT ck_answer_ans_option    CHECK (ans_option BETWEEN 1 AND 4) ENABLE NOVALIDATE;
ALTER TABLE answer   ADD CONSTRAINT ck_answer_correct_opt   CHECK (correct_option BETWEEN 1 AND 4) ENABLE NOVALIDATE;
ALTER TABLE question ADD CONSTRAINT ck_question_difficulty  CHECK (difficulty BETWEEN 1 AND 3) ENABLE NOVALIDATE;
ALTER TABLE question ADD CONSTRAINT ck_question_correct_opt CHECK (correct_option BETWEEN 1 AND 4) ENABLE NOVALIDATE;

ALTER TABLE friend ADD CONSTRAINT ck_friend_not_self CHECK (s_user_id <> g_user_id) ENABLE NOVALIDATE;

ALTER TABLE friend_connection ADD CONSTRAINT ck_fc_requester
    CHECK (requested_by_id IN (user_low_id, user_high_id)) ENABLE NOVALIDATE;

-- ---------------------------------------------------------------------
-- 7. UNIQUE 制約 / インデックス
-- ---------------------------------------------------------------------
ALTER TABLE favorite ADD CONSTRAINT uq_favorite_user_question UNIQUE (user_id, question_id);

CREATE INDEX ix_answer_user_date      ON answer (user_id, ans_date);
CREATE INDEX ix_answer_question       ON answer (question_id);
CREATE INDEX ix_question_category     ON question (category_id);
CREATE INDEX ix_user_ach_achievement  ON user_achievement (achievement_id);
CREATE INDEX ix_friend_g_user         ON friend (g_user_id);
CREATE INDEX ix_fc_requested_by       ON friend_connection (requested_by_id);

-- ---------------------------------------------------------------------
-- 8. FRIEND_CONNECTION の日時にデフォルト値
-- ---------------------------------------------------------------------
ALTER TABLE friend_connection MODIFY (
    created_at TIMESTAMP(6) DEFAULT SYSTIMESTAMP,
    updated_at TIMESTAMP(6) DEFAULT SYSTIMESTAMP
);

-- ---------------------------------------------------------------------
-- 9. コメント整備
-- ---------------------------------------------------------------------
COMMENT ON COLUMN customer.login_count            IS 'ログイン日数(累計)';
COMMENT ON COLUMN customer.consecutive_login_days IS '連続ログイン日数';
COMMENT ON COLUMN customer.last_seen_at           IS '最終アクセス日時';
COMMENT ON TABLE  friend_connection               IS 'フレンド関係テーブル(LOW<HIGH で1ペア1行)';

-- ---------------------------------------------------------------------
-- 10. QUESTION に解説文カラムを追加 (NULL 許可・既存データはそのまま)
-- ---------------------------------------------------------------------
ALTER TABLE question ADD (explanation VARCHAR2(2000 CHAR));
COMMENT ON COLUMN question.explanation IS '解説文';

COMMIT;

-- =====================================================================
-- 別途判断が必要な項目 (このスクリプトでは未対応)
--  * FRIEND と FRIEND_CONNECTION の統一
--    (両方 Entity があり使用中のため、コード移行後に FRIEND を削除)
--  * FRIEND.S_USER_ID / G_USER_ID は NUMBER(10) で、手順4により CUSTOMER.USER_ID と桁数が一致
--  * PERMISSION の値の定義統一 (サンプルは管理者1=1、コードは 2=管理者)
--  * QUESTION の選択肢(4択化 / QUESTION_OPTION テーブル利用)
--  * QUESTION への削除フラグ追加 (Entity 側の変更も必要)
--  * パスワードのハッシュ化 (アプリ側で BCrypt 化し、既存ユーザーの再ハッシュも必要)
-- =====================================================================
