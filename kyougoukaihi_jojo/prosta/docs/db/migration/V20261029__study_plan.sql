-- Apply once to the existing PROSTA Oracle schema before deploying this version.

CREATE SEQUENCE seq_study_plan
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 9999999999;

CREATE TABLE study_plan (
    study_plan_id NUMBER(10) NOT NULL,
    user_id       NUMBER(5) NOT NULL,
    plan_date     DATE NOT NULL,
    title         VARCHAR2(100 CHAR) NOT NULL,
    details       VARCHAR2(500 CHAR),
    CONSTRAINT pk_study_plan PRIMARY KEY (study_plan_id),
    CONSTRAINT fk_study_plan_user
        FOREIGN KEY (user_id) REFERENCES customer (user_id)
);

CREATE INDEX ix_study_plan_user_date ON study_plan (user_id, plan_date);

COMMENT ON TABLE study_plan IS '学習予定';
COMMENT ON COLUMN study_plan.study_plan_id IS '学習予定ID';
COMMENT ON COLUMN study_plan.user_id IS 'ユーザーID';
COMMENT ON COLUMN study_plan.plan_date IS '予定日';
COMMENT ON COLUMN study_plan.title IS '予定タイトル';
COMMENT ON COLUMN study_plan.details IS '予定詳細';
