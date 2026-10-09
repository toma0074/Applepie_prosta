
-- =========================================================================
-- 0. 既存オブジェクトの削除（再作成・クリーンアップ用）
--    ※初回実行時はエラー（テーブルが存在しない）になりますが無視して問題ありません。
-- =========================================================================
DROP TABLE favorite CASCADE CONSTRAINTS;
DROP TABLE answer CASCADE CONSTRAINTS;
DROP TABLE question_option CASCADE CONSTRAINTS;
DROP TABLE question CASCADE CONSTRAINTS;
DROP TABLE customer CASCADE CONSTRAINTS;
DROP TABLE category CASCADE CONSTRAINTS;

DROP SEQUENCE seq_user;
DROP SEQUENCE seq_question;
DROP SEQUENCE seq_answer;
DROP SEQUENCE seq_option;
DROP SEQUENCE seq_category;
DROP SEQUENCE seq_favorite;

-- =========================================================================
-- 1. シーケンスの作成
-- =========================================================================
CREATE SEQUENCE seq_user     START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 99999;
CREATE SEQUENCE seq_question START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 9999;
CREATE SEQUENCE seq_answer   START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 999999;
CREATE SEQUENCE seq_option   START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 9999;
CREATE SEQUENCE seq_category START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 99;
CREATE SEQUENCE seq_favorite START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 99;

-- =========================================================================
-- 2. テーブルの作成（リレーション依存関係を考慮した順序）
-- =========================================================================

-- (1) カテゴリテーブル
CREATE TABLE category (
    category_id   NUMBER(2),
    category_name VARCHAR2(20) NOT NULL,
    CONSTRAINT pk_category PRIMARY KEY (category_id)
);

COMMENT ON TABLE  category               IS 'カテゴリテーブル';
COMMENT ON COLUMN category.category_id   IS 'カテゴリーID';
COMMENT ON COLUMN category.category_name IS 'カテゴリー名';


-- (2) ユーザーテーブル
CREATE TABLE customer (
    user_id     NUMBER(5),
    user_name   VARCHAR2(20) NOT NULL,
    user_pass   VARCHAR2(16) NOT NULL,
    permission  NUMBER(1)    NOT NULL,
    delete_flag NUMBER(1)    NOT NULL,
    login_date DATE NOT NULL,
    login_count NUMBER(10) NOT NULL,
    login_flag NUMBER(1) NOT NULL,
    user_level NUMBER(10) NOT NULL,
    CONSTRAINT pk_customer PRIMARY KEY (user_id)
);

COMMENT ON TABLE  customer             IS 'ユーザーテーブル';
COMMENT ON COLUMN customer.user_id     IS 'ユーザーID';
COMMENT ON COLUMN customer.user_name   IS '名前';
COMMENT ON COLUMN customer.user_pass   IS 'パスワード';
COMMENT ON COLUMN customer.permission  IS '管理者権限';
COMMENT ON COLUMN customer.delete_flag IS '削除フラグ';
COMMENT ON COLUMN customer.login_date IS '最終ログイン日';
COMMENT ON COLUMN customer.login_count IS 'ログイン日数';
COMMENT ON COLUMN customer.login_flag IS 'ログインフラグ';
COMMENT ON COLUMN customer.user_level IS 'レベル';




-- (4) 問題集管理テーブル
CREATE TABLE question (
    question_id      NUMBER(4),
    category_id      NUMBER(2)      NOT NULL,
    difficulty       NUMBER(1)      NOT NULL,
    question_text    VARCHAR2(1000) NOT NULL,
    option_a         VARCHAR2(300)  NOT NULL,
    option_b         VARCHAR2(300)  NOT NULL,
    option_c         VARCHAR2(300)  NOT NULL,
    correct_option NUMBER(1)        NOT NULL,
    CONSTRAINT pk_question PRIMARY KEY (question_id),
    CONSTRAINT fk_category_2 FOREIGN KEY (category_id) REFERENCES category(category_id)
);

COMMENT ON TABLE  question                IS '問題集管理テーブル';
COMMENT ON COLUMN question.question_id    IS '問題ID';
COMMENT ON COLUMN question.category_id    IS 'カテゴリーID';
COMMENT ON COLUMN question.difficulty     IS '難易度';
COMMENT ON COLUMN question.question_text  IS '問題文';
COMMENT ON COLUMN question.option_a       IS '選択肢1';
COMMENT ON COLUMN question.option_b       IS '選択肢2';
COMMENT ON COLUMN question.option_c       IS '選択肢3';
COMMENT ON COLUMN question.correct_option IS '正解選択肢';




-- (5) 解答結果テーブル
CREATE TABLE answer (
    ans_id         NUMBER(6),
    user_id        NUMBER(5) NOT NULL,
    question_id    NUMBER(4) NOT NULL,
    ans_option     NUMBER(1) NOT NULL,
    correct_option NUMBER(1) NOT NULL,
    correct_flag   NUMBER(1) NOT NULL,
    ans_date       DATE      NOT NULL,
    CONSTRAINT pk_answer PRIMARY KEY (ans_id),
    CONSTRAINT fk_answer_user     FOREIGN KEY (user_id)     REFERENCES customer(user_id),
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES question(question_id)
);

COMMENT ON TABLE  answer                IS '解答結果テーブル';
COMMENT ON COLUMN answer.ans_id         IS '結果ID';
COMMENT ON COLUMN answer.user_id        IS 'ユーザーID';
COMMENT ON COLUMN answer.question_id    IS '問題ID';
COMMENT ON COLUMN answer.ans_option     IS '選択履歴';
COMMENT ON COLUMN answer.correct_option IS '正解選択肢';
COMMENT ON COLUMN answer.correct_flag   IS '正解フラグ';
COMMENT ON COLUMN answer.ans_date       IS '解答日時';



-- (6) お気に入り管理テーブル
CREATE TABLE favorite (
    favorite_id NUMBER(10), 
    user_id     NUMBER(5) NOT NULL,
    question_id NUMBER(4) NOT NULL,
    CONSTRAINT pk_favorite PRIMARY KEY (favorite_id),
    CONSTRAINT fk_favorite_user     FOREIGN KEY (user_id)     REFERENCES customer(user_id),
    CONSTRAINT fk_favorite_question FOREIGN KEY (question_id) REFERENCES question(question_id)
);

COMMENT ON TABLE  favorite             IS 'お気に入り管理テーブル';
COMMENT ON COLUMN favorite.favorite_id IS 'お気に入りID';
COMMENT ON COLUMN favorite.user_id     IS 'ユーザーID';
COMMENT ON COLUMN favorite.question_id IS '問題ID';

-- =========================================================================
-- 3. 確定
-- =========================================================================
COMMIT;


-- =========================================================================
-- 4. 追加要件（既存オブジェクト変更・新規テーブル追加）
-- =========================================================================

-- (1) 解答結果テーブル（answer）にカラム追加
ALTER TABLE answer ADD (
    ans_count NUMBER(5) NOT NULL
);

COMMENT ON COLUMN answer.ans_count IS '解答回数';


-- (2) 新規テーブル フレンドテーブル（friend）新規作成
CREATE TABLE friend (
    s_user_id NUMBER(10) NOT NULL,
    g_user_id NUMBER(10) NOT NULL,
    accept_flag NUMBER(1) NOT NULL,
    -- 申請ユーザーと受信ユーザーの組み合わせを主キー（複合主キー）に設定
    CONSTRAINT pk_friend PRIMARY KEY (s_user_id, g_user_id),
    -- 既存の顧客テーブル（customer）への外部キー制約（フォーリンキー）
    CONSTRAINT fk_friend_s_user FOREIGN KEY (s_user_id) REFERENCES customer(user_id),
    CONSTRAINT fk_friend_g_user FOREIGN KEY (g_user_id) REFERENCES customer(user_id)
);

COMMENT ON TABLE friend IS 'フレンドテーブル';
COMMENT ON COLUMN friend.s_user_id IS '申請ユーザーID';
COMMENT ON COLUMN friend.g_user_id IS '受信ユーザーID';
COMMENT ON COLUMN friend.accept_flag IS '承認フラグ';

-- =========================================================================
-- 5. 確定
-- =========================================================================
COMMIT;



