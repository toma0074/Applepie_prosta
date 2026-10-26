--------------------------------------------------------
--  ファイルを作成しました - 月曜日-10月-26-2026   
--------------------------------------------------------
--------------------------------------------------------
--  【全削除】PROSTA_USER スキーマ内のオブジェクトをすべて削除してから再作成する
--  ※ 必ず PROSTA_USER で接続して実行すること（他ユーザーでは中断する）
--  ※ スクリプト実行(F5)で全体を流す。データも全て消えるため事前にバックアップすること
--------------------------------------------------------
SET DEFINE OFF;

BEGIN
  IF USER <> 'PROSTA_USER' THEN
    RAISE_APPLICATION_ERROR(-20001, 'PROSTA_USER で接続して実行してください（現在: ' || USER || '）');
  END IF;

  -- テーブル（外部キー・索引・制約・トリガーごと）
  FOR t IN (SELECT table_name FROM user_tables) LOOP
    EXECUTE IMMEDIATE 'DROP TABLE "' || t.table_name || '" CASCADE CONSTRAINTS PURGE';
  END LOOP;

  -- シーケンス
  FOR s IN (SELECT sequence_name FROM user_sequences) LOOP
    EXECUTE IMMEDIATE 'DROP SEQUENCE "' || s.sequence_name || '"';
  END LOOP;

  -- ビュー・シノニム・ストアド類
  FOR o IN (SELECT object_name, object_type FROM user_objects
             WHERE object_type IN ('VIEW', 'SYNONYM', 'PROCEDURE', 'FUNCTION', 'PACKAGE', 'TYPE')) LOOP
    EXECUTE IMMEDIATE 'DROP ' || o.object_type || ' "' || o.object_name || '"';
  END LOOP;
END;
/
--------------------------------------------------------
--------------------------------------------------------
--  DDL for Sequence SEQ_ANSWER
--------------------------------------------------------

   CREATE SEQUENCE  "PROSTA_USER"."SEQ_ANSWER"  MINVALUE 1 MAXVALUE 999999 INCREMENT BY 1 START WITH 41 CACHE 20 NOORDER  NOCYCLE  NOKEEP  NOSCALE  GLOBAL ;
--------------------------------------------------------
--  DDL for Sequence SEQ_CATEGORY
--------------------------------------------------------

   CREATE SEQUENCE  "PROSTA_USER"."SEQ_CATEGORY"  MINVALUE 1 MAXVALUE 99 INCREMENT BY 1 START WITH 21 CACHE 20 NOORDER  NOCYCLE  NOKEEP  NOSCALE  GLOBAL ;
--------------------------------------------------------
--  DDL for Sequence SEQ_FAVORITE
--------------------------------------------------------

   CREATE SEQUENCE  "PROSTA_USER"."SEQ_FAVORITE"  MINVALUE 1 MAXVALUE 9999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE  NOKEEP  NOSCALE  GLOBAL ;
--------------------------------------------------------
--  DDL for Sequence SEQ_FRIEND_CONNECTION
--------------------------------------------------------

   CREATE SEQUENCE  "PROSTA_USER"."SEQ_FRIEND_CONNECTION"  MINVALUE 1 MAXVALUE 9999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE  NOKEEP  NOSCALE  GLOBAL ;
--------------------------------------------------------
--  DDL for Sequence SEQ_OPTION
--------------------------------------------------------

   CREATE SEQUENCE  "PROSTA_USER"."SEQ_OPTION"  MINVALUE 1 MAXVALUE 9999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE  NOKEEP  NOSCALE  GLOBAL ;
--------------------------------------------------------
--  DDL for Sequence SEQ_QUESTION
--------------------------------------------------------

   CREATE SEQUENCE  "PROSTA_USER"."SEQ_QUESTION"  MINVALUE 1 MAXVALUE 9999 INCREMENT BY 1 START WITH 61 CACHE 20 NOORDER  NOCYCLE  NOKEEP  NOSCALE  GLOBAL ;
--------------------------------------------------------
--  DDL for Sequence SEQ_USER
--------------------------------------------------------

   CREATE SEQUENCE  "PROSTA_USER"."SEQ_USER"  MINVALUE 1 MAXVALUE 99999 INCREMENT BY 1 START WITH 41 CACHE 20 NOORDER  NOCYCLE  NOKEEP  NOSCALE  GLOBAL ;
--------------------------------------------------------
--  DDL for Sequence SEQ_USER_ACHIEVEMENT
--------------------------------------------------------

   CREATE SEQUENCE  "PROSTA_USER"."SEQ_USER_ACHIEVEMENT"  MINVALUE 1 MAXVALUE 999999 INCREMENT BY 1 START WITH 21 CACHE 20 NOORDER  NOCYCLE  NOKEEP  NOSCALE  GLOBAL ;
--------------------------------------------------------
--  DDL for Table ACHIEVEMENT
--------------------------------------------------------

  CREATE TABLE "PROSTA_USER"."ACHIEVEMENT" 
   (	"ACHIEVEMENT_ID" NUMBER(4,0), 
	"ACHIEVEMENT_NAME" VARCHAR2(50 CHAR), 
	"DESCRIPTION" VARCHAR2(200 CHAR), 
	"ICON" VARCHAR2(10 CHAR)
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
 NOCOMPRESS LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;
--------------------------------------------------------
--  DDL for Table ANSWER
--------------------------------------------------------

  CREATE TABLE "PROSTA_USER"."ANSWER" 
   (	"ANS_ID" NUMBER(6,0), 
	"USER_ID" NUMBER(5,0), 
	"QUESTION_ID" NUMBER(4,0), 
	"ANS_OPTION" NUMBER(1,0), 
	"CORRECT_OPTION" NUMBER(1,0), 
	"CORRECT_FLAG" NUMBER(1,0), 
	"ANS_DATE" DATE, 
	"ANS_COUNT" NUMBER(5,0) DEFAULT 1
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
 NOCOMPRESS LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;

   COMMENT ON COLUMN "PROSTA_USER"."ANSWER"."ANS_ID" IS '結果ID';
   COMMENT ON COLUMN "PROSTA_USER"."ANSWER"."USER_ID" IS 'ユーザーID';
   COMMENT ON COLUMN "PROSTA_USER"."ANSWER"."QUESTION_ID" IS '問題ID';
   COMMENT ON COLUMN "PROSTA_USER"."ANSWER"."ANS_OPTION" IS '選択履歴';
   COMMENT ON COLUMN "PROSTA_USER"."ANSWER"."CORRECT_OPTION" IS '正解選択肢';
   COMMENT ON COLUMN "PROSTA_USER"."ANSWER"."CORRECT_FLAG" IS '正解フラグ';
   COMMENT ON COLUMN "PROSTA_USER"."ANSWER"."ANS_DATE" IS '解答日時';
   COMMENT ON COLUMN "PROSTA_USER"."ANSWER"."ANS_COUNT" IS '解答回数';
   COMMENT ON TABLE "PROSTA_USER"."ANSWER"  IS '解答結果テーブル';
--------------------------------------------------------
--  DDL for Table CATEGORY
--------------------------------------------------------

  CREATE TABLE "PROSTA_USER"."CATEGORY" 
   (	"CATEGORY_ID" NUMBER(2,0), 
	"CATEGORY_NAME" VARCHAR2(20 CHAR)
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
 NOCOMPRESS LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;

   COMMENT ON COLUMN "PROSTA_USER"."CATEGORY"."CATEGORY_ID" IS 'カテゴリーID';
   COMMENT ON COLUMN "PROSTA_USER"."CATEGORY"."CATEGORY_NAME" IS 'カテゴリー名';
   COMMENT ON TABLE "PROSTA_USER"."CATEGORY"  IS 'カテゴリテーブル';
--------------------------------------------------------
--  DDL for Table CUSTOMER
--------------------------------------------------------

  CREATE TABLE "PROSTA_USER"."CUSTOMER" 
   (	"USER_ID" NUMBER(5,0), 
	"USER_NAME" VARCHAR2(20 CHAR), 
	"USER_PASS" VARCHAR2(100 BYTE), 
	"PERMISSION" NUMBER(1,0), 
	"DELETE_FLAG" NUMBER(1,0), 
	"LOGIN_DATE" DATE, 
	"LOGIN_COUNT" NUMBER(10,0), 
	"LOGIN_FLAG" NUMBER(1,0), 
	"USER_LEVEL" NUMBER(10,0), 
	"CONSECUTIVE_LOGIN_DAYS" NUMBER(10,0) DEFAULT 0, 
	"LAST_SEEN_AT" TIMESTAMP (6)
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
 NOCOMPRESS LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;

   COMMENT ON COLUMN "PROSTA_USER"."CUSTOMER"."USER_ID" IS 'ユーザーID';
   COMMENT ON COLUMN "PROSTA_USER"."CUSTOMER"."USER_NAME" IS '名前';
   COMMENT ON COLUMN "PROSTA_USER"."CUSTOMER"."USER_PASS" IS 'パスワード';
   COMMENT ON COLUMN "PROSTA_USER"."CUSTOMER"."PERMISSION" IS '権限(1:管理者, 0:一般)';
   COMMENT ON COLUMN "PROSTA_USER"."CUSTOMER"."DELETE_FLAG" IS '削除フラグ';
   COMMENT ON COLUMN "PROSTA_USER"."CUSTOMER"."LOGIN_DATE" IS '最終ログイン日';
   COMMENT ON COLUMN "PROSTA_USER"."CUSTOMER"."LOGIN_COUNT" IS 'ログイン日数';
   COMMENT ON COLUMN "PROSTA_USER"."CUSTOMER"."LOGIN_FLAG" IS 'ログインフラグ';
   COMMENT ON COLUMN "PROSTA_USER"."CUSTOMER"."USER_LEVEL" IS 'レベル';
   COMMENT ON TABLE "PROSTA_USER"."CUSTOMER"  IS 'ユーザーテーブル';
--------------------------------------------------------
--  DDL for Table FAVORITE
--------------------------------------------------------

  CREATE TABLE "PROSTA_USER"."FAVORITE" 
   (	"FAVORITE_ID" NUMBER(10,0), 
	"USER_ID" NUMBER(5,0), 
	"QUESTION_ID" NUMBER(4,0)
   ) SEGMENT CREATION DEFERRED 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
 NOCOMPRESS LOGGING
  TABLESPACE "USERS" ;

   COMMENT ON COLUMN "PROSTA_USER"."FAVORITE"."FAVORITE_ID" IS 'お気に入りID';
   COMMENT ON COLUMN "PROSTA_USER"."FAVORITE"."USER_ID" IS 'ユーザーID';
   COMMENT ON COLUMN "PROSTA_USER"."FAVORITE"."QUESTION_ID" IS '問題ID';
   COMMENT ON TABLE "PROSTA_USER"."FAVORITE"  IS 'お気に入り管理テーブル';
--------------------------------------------------------
--  DDL for Table FRIEND_CONNECTION
--------------------------------------------------------

  CREATE TABLE "PROSTA_USER"."FRIEND_CONNECTION" 
   (	"FRIEND_CONNECTION_ID" NUMBER(10,0), 
	"USER_LOW_ID" NUMBER(5,0), 
	"USER_HIGH_ID" NUMBER(5,0), 
	"REQUESTED_BY_ID" NUMBER(5,0), 
	"STATUS" VARCHAR2(16 CHAR), 
	"CREATED_AT" TIMESTAMP (6), 
	"UPDATED_AT" TIMESTAMP (6)
   ) SEGMENT CREATION DEFERRED 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
 NOCOMPRESS LOGGING
  TABLESPACE "USERS" ;
--------------------------------------------------------
--  DDL for Table QUESTION
--------------------------------------------------------

  CREATE TABLE "PROSTA_USER"."QUESTION" 
   (	"QUESTION_ID" NUMBER(4,0), 
	"CATEGORY_ID" NUMBER(2,0), 
	"DIFFICULTY" NUMBER(1,0), 
	"QUESTION_TEXT" VARCHAR2(1000 CHAR), 
	"OPTION_A" VARCHAR2(300 CHAR), 
	"OPTION_B" VARCHAR2(300 CHAR), 
	"OPTION_C" VARCHAR2(300 CHAR), 
	"CORRECT_OPTION" NUMBER(1,0), 
	"EXPLANATION" VARCHAR2(1000 CHAR)
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
 NOCOMPRESS LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;

   COMMENT ON COLUMN "PROSTA_USER"."QUESTION"."QUESTION_ID" IS '問題ID';
   COMMENT ON COLUMN "PROSTA_USER"."QUESTION"."CATEGORY_ID" IS 'カテゴリーID';
   COMMENT ON COLUMN "PROSTA_USER"."QUESTION"."DIFFICULTY" IS '難易度';
   COMMENT ON COLUMN "PROSTA_USER"."QUESTION"."QUESTION_TEXT" IS '問題文';
   COMMENT ON COLUMN "PROSTA_USER"."QUESTION"."OPTION_A" IS '選択肢1';
   COMMENT ON COLUMN "PROSTA_USER"."QUESTION"."OPTION_B" IS '選択肢2';
   COMMENT ON COLUMN "PROSTA_USER"."QUESTION"."OPTION_C" IS '選択肢3';
   COMMENT ON COLUMN "PROSTA_USER"."QUESTION"."CORRECT_OPTION" IS '正解選択肢';
   COMMENT ON TABLE "PROSTA_USER"."QUESTION"  IS '問題集管理テーブル';
--------------------------------------------------------
--  DDL for Table USER_ACHIEVEMENT
--------------------------------------------------------

  CREATE TABLE "PROSTA_USER"."USER_ACHIEVEMENT" 
   (	"USER_ACH_ID" NUMBER(6,0), 
	"USER_ID" NUMBER(5,0), 
	"ACHIEVEMENT_ID" NUMBER(4,0), 
	"UNLOCKED_DATE" DATE DEFAULT SYSDATE, 
	"SEEN_FLAG" NUMBER(1,0) DEFAULT 1
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 
 NOCOMPRESS LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;
REM INSERTING into PROSTA_USER.ACHIEVEMENT
SET DEFINE OFF;
Insert into PROSTA_USER.ACHIEVEMENT (ACHIEVEMENT_ID,ACHIEVEMENT_NAME,DESCRIPTION,ICON) values (1,'7日連続ログイン','7日以上ログインしました！','🔥');
Insert into PROSTA_USER.ACHIEVEMENT (ACHIEVEMENT_ID,ACHIEVEMENT_NAME,DESCRIPTION,ICON) values (2,'初めての満点','テストで満点を取りました！','🎯');
Insert into PROSTA_USER.ACHIEVEMENT (ACHIEVEMENT_ID,ACHIEVEMENT_NAME,DESCRIPTION,ICON) values (3,'満点マスター×3','満点を3回達成しました！','🏅');
Insert into PROSTA_USER.ACHIEVEMENT (ACHIEVEMENT_ID,ACHIEVEMENT_NAME,DESCRIPTION,ICON) values (4,'満点伝説×7','満点を7回達成しました！','👑');
Insert into PROSTA_USER.ACHIEVEMENT (ACHIEVEMENT_ID,ACHIEVEMENT_NAME,DESCRIPTION,ICON) values (5,'レベル10到達','レベルが10になりました！','🌟');
Insert into PROSTA_USER.ACHIEVEMENT (ACHIEVEMENT_ID,ACHIEVEMENT_NAME,DESCRIPTION,ICON) values (6,'レベル50到達','レベルが50になりました！勇者に覚醒！','⚔️');
REM INSERTING into PROSTA_USER.ANSWER
SET DEFINE OFF;
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (1,3,1,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (2,3,2,3,1,0,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (3,3,3,1,2,0,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (4,3,1,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (5,3,2,2,1,0,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (6,3,3,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (7,3,10,1,1,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (8,3,11,4,1,0,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (9,3,12,3,2,0,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (10,3,19,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (11,3,20,4,2,0,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (12,3,21,3,1,0,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (13,3,1,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (14,3,2,1,1,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (15,3,3,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (16,3,1,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (17,3,2,1,1,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (18,3,3,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (19,3,4,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (20,3,5,3,3,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (21,3,6,1,2,0,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (22,3,7,3,3,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (23,3,8,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (24,3,9,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (25,3,1,3,2,0,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (26,3,2,3,1,0,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (27,3,3,4,2,0,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (28,3,1,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (29,3,2,1,1,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (30,3,3,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (31,3,1,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (32,3,3,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (33,3,2,1,1,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (34,3,3,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (35,3,1,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (36,3,2,1,1,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (37,3,2,1,1,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (38,3,3,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.ANSWER (ANS_ID,USER_ID,QUESTION_ID,ANS_OPTION,CORRECT_OPTION,CORRECT_FLAG,ANS_DATE,ANS_COUNT) values (39,3,1,2,2,1,to_date('26-10-23','RR-MM-DD'),1);
REM INSERTING into PROSTA_USER.CATEGORY
SET DEFINE OFF;
Insert into PROSTA_USER.CATEGORY (CATEGORY_ID,CATEGORY_NAME) values (1,'Java');
Insert into PROSTA_USER.CATEGORY (CATEGORY_ID,CATEGORY_NAME) values (2,'Spring');
Insert into PROSTA_USER.CATEGORY (CATEGORY_ID,CATEGORY_NAME) values (3,'HTML');
Insert into PROSTA_USER.CATEGORY (CATEGORY_ID,CATEGORY_NAME) values (4,'SQL');
REM INSERTING into PROSTA_USER.CUSTOMER
SET DEFINE OFF;
Insert into PROSTA_USER.CUSTOMER (USER_ID,USER_NAME,USER_PASS,PERMISSION,DELETE_FLAG,LOGIN_DATE,LOGIN_COUNT,LOGIN_FLAG,USER_LEVEL,CONSECUTIVE_LOGIN_DAYS,LAST_SEEN_AT) values (3,'管理者1','$2a$10$8tbw5OvMuYKcxnIqMMuh8.ZFgNNJ1DQRY02L7V1O4/i9H23md2ptK',1,0,to_date('26-10-26','RR-MM-DD'),4,1,60,1,to_timestamp('26-10-26 17:57:47.192600000','RR-MM-DD HH24:MI:SSXFF'));
REM INSERTING into PROSTA_USER.FAVORITE
SET DEFINE OFF;
REM INSERTING into PROSTA_USER.FRIEND_CONNECTION
SET DEFINE OFF;
REM INSERTING into PROSTA_USER.QUESTION
SET DEFINE OFF;
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (44,4,2,'チーム開発で、メンバーAとメンバーBが同時に同じデータベースを触っています。
メンバーAが UPDATE customer SET nickname = ''ネコ'' WHERE user_id = 1; を実行（まだ COMMIT はしていない）。
その直後、メンバーBが UPDATE customer SET nickname = ''イヌ'' WHERE user_id = 1; を実行しようとした。
このとき、メンバーBの画面（ツール）はどうなりますか？','エラーが表示されて、即座に処理が失敗する。','メンバーAの変更を上書きして、即座に「イヌ」に更新される。','メンバーAが COMMIT または ROLLBACK するまで、処理が一時停止（待機状態）になる。',3,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (45,4,3,'customer テーブルの birthday（生年月日）列には、検索速度を上げるための「インデックス」が作成されています。しかし、書き方によってはインデックスが無視され、全行検索（フルスキャン）になってしまいます。インデックスが正しく使われる（最も高速に処理できる）SQLはどれですか？','SELECT * FROM customer WHERE TO_CHAR(birthday, ''YYYY'') = ''2000'';','SELECT * FROM customer WHERE birthday >= TO_DATE(''2000-01-01'', ''YYYY-MM-DD'') AND birthday <= TO_DATE(''2000-12-31'', ''YYYY-MM-DD'';','SELECT * FROM customer WHERE birthday + 1 > TO_DATE(''2000-01-01'', ''YYYY-MM-DD'';',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (46,4,3,'大量のデータ（数百万件）が入っている answer テーブルから、特定の条件に合うデータを抽出します。一般的に、サブクエリ側のデータ量が膨大であるとき、IN を使うよりも EXISTS を使った方が劇的に処理が早くなる理由として、正しいものはどれですか？','EXISTS は、条件にマッチする行を「1件」見つけた時点でその探索を終了（打ち切り）するから。','EXISTS を使うと、データベースが自動的に一時テーブルを作成してメモリ上で処理するから。','IN は必ずインデックスを無視してフルスキャンを行う仕様になっているから。',1,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (47,4,3,'10,000件の解答データがある answer テーブルがあります。このテーブルを、GROUP BY user_id, question_id のように2つの列を指定してグループ化しました。このSQLを実行した結果、取得できる行数（レコード数）は最大で何行になりますか？','user_id の種類の数（例えばユーザーが50人なら50行）','question_id の種類の数（例えば問題が20問なら20行）','user_id と question_id の「ユニークな組み合わせ」の数（最大10,000行）',3,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (48,4,3,'組織図やカテゴリの親子関係（木構造データ）を1つのSQLで辿りたいとき、Oracle Databaseで伝統的に使われる階層問い合わせ専用の構文（キーワード）はどれですか？','START WITH ... CONNECT BY PRIOR ...','RECURSIVE LOOP ... UNTIL ...','MERGE INTO ... USING ...',1,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (49,4,3,'2つのトランザクションが、お互いに相手がロックしているデータの解放を待ち続けてしまい、処理が永久に止まってしまう現象を「デッドロック」と言います。デッドロックが発生してしまう原因として、正しいものはどれですか？','2人のメンバーが、全く同じ順番で同じ複数の行を更新したとき。','1人のメンバーが、大量のデータを一度に UPDATE してコミットし忘れたとき。','2人のメンバーが、それぞれ異なる順序で同じ複数の行を更新しようとしたとき。',3,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (50,4,3,'データベース内部の機能（オプティマイザ）が、2つの大きなテーブルを結合する際、片方のテーブルをメモリ上に展開してハッシュテーブル（インデックスのようなもの）を作り、もう片方と高速に突き合わせる方式を何と呼びますか？','ネステッドループ結合（Nested Loop Join）','ソートマージ結合（Sort Merge Join）','ハッシュ結合（Hash Join）',3,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (51,4,3,'GROUP BY を書かずに、いきなり SELECT COUNT() FROM answer HAVING COUNT(*) > 5; と書きました。このSQLを実行するとどうなりますか？','エラー（GROUP BYがないため実行不可）になる。','テーブル全体の行数が5件より多ければ「全体の行数」が1行だけ返り、5件以下なら「0件（空っぽ）」が返る。','エラーにはならないが、常に何も返ってこない（0件になる）。',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (52,4,3,'テストの点数を高い順に並べ替えて順位をつけます。1位の人が2人（同点）いた場合、その次の人の順位は、RANK() では「3位」、DENSE_RANK() では「2位」になります。では、1位が3人いた場合の DENSE_RANK() での次の人の順位は何位ですか？','4位','3位','2位',3,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (53,4,3,'テーブルA、テーブルB、テーブルCの3つを順に LEFT JOIN します。
FROM A LEFT JOIN B ON A.id = B.id LEFT JOIN C ON B.id = C.id
このとき、テーブルBにデータが存在せずNULLになった行は、続くテーブルCとの結合（B.id = C.id）においてどのように扱われますか？','B.id がNULLの行は自動的に無視され、テーブルCとは一切結合されない（結果から消える）。','B.id のNULLと C.id のNULLが「一致する」と判定され、テーブルCのデータと結合される。','B.id がNULLの行も残るが、C.id とは結合できないため、Cの列はすべてNULLとして結果に出力される。',3,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (54,4,3,'大量のテストデータが入ったテーブルを空っぽにしたいです。DELETE FROM answer; ではなく、TRUNCATE TABLE answer; を使う最大のメリットと特徴はどれですか？','TRUNCATE はデータを1行ずつ確認しながら消すため、安全性が高い。','TRUNCATE はDDL（データ定義言語）であり、ROLLBACK（取り消し）ができない代わりに、一瞬で超高速にデータを全消去できる。','TRUNCATE を使うと、外部キー制約（FOREIGN KEY）で繋がっている親テーブルのデータも連動して全消去してくれる。',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (1,1,1,'次のうち、Javaで整数を表すデータ型はどれ？','double','int','char',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (2,1,1,'次のうち「繰り返し処理」を行う構文はどれ？','for','case','break',1,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (3,1,1,'次のうち、Javaのコメントとして正しいものはどれ？','# コメント','// コメント','-- コメント',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (4,1,2,'次のコードの出力として正しいものはどれか。
int x = 5;
{
    int y = x + 2;
    System.out.println(y);
}
System.out.println(x);','5 と 7','7 と 5','エラーになる',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (5,1,2,'次のコードの出力として正しいものはどれか。
int x = 10;
int y = 20;
if (x == 10)
    y = 30;
System.out.println(y);','10','20','30',3,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (6,1,2,'オーバーライド（override）に必要な条件として正しいものはどれか？','メソッド名だけ同じであればよい','メソッド名・引数・戻り値の型がすべて同じであること','戻り値の型が異なっていてもよい',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (7,1,3,'次のコードの出力として正しいものはどれか。
int a = 3;
int b = 4;
a = b++ + ++a;
System.out.println(a + b);','11','12','13',3,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (8,1,3,'次のコードの出力として正しいものはどれか。
int x = 0;
for (int i = 1; i < 4; i++) {
    x += i; if (i == 2) break;
} 
System.out.println(x);','1','3','4',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (9,1,3,'インターフェースに関する説明として正しいものはどれか？','インターフェースのメソッドはすべてprivateである','インターフェースは多重実装が可能である','インターフェースはインスタンス化できる',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (10,2,1,'Spring Bootで、リクエストを受け取るクラスに付けるアノテーションはどれ？','@Controller','@Repository','@Autowired',1,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (11,2,1,'Controllerから画面に値を渡すためによく使うものはどれ？','Model','Scanner','StringBuilder',1,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (12,2,1,'次のコードの説明として正しいものはどれ？
model.addAttribute("empName", employee.getEmpName());','DBに従業員名を保存している','画面で使えるように empName という名前で値を渡している','従業員名を暗号化している',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (13,2,2,'Thymeleafで、Controllerから渡された empName を表示する書き方として正しいものはどれ？','<p th:text="${empName}"></p>','<p th:href="${empName}"></p>','<p th:each="${empName}"></p>',1,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (14,2,2,'次のコードで、BindingResult result を書く位置として正しいものはどれ？
public String login(@Valid @ModelAttribute LoginForm loginForm, BindingResult result, Model model) {}','@Valid が付いた引数の直後','モデルModel の前ならどこでもよい','@RequestMapping の前',1,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (15,2,2,'return "redirect:/list"; の説明として正しいものはどれ？','list.html を直接表示する','/list に再度リクエストを送る','Modelの中身をすべて保存する',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (16,2,3,'getReferenceById(id) を使ったときに、The given id must not be null が出る原因として考えられるものはどれ？','渡しているidがnullだから','HTMLファイルが存在しないから','@Controllerがないから',1,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (17,2,3,'ログイン成功時に、EntityをそのままSessionに保存するより、Beanに必要な情報だけ詰めて保存する理由として適切なのはどれ？','Entityは必ずSessionに保存できないから','不要な情報やDB管理用の情報まで持ち回らないようにするため','ThymeleafではEntityを表示できないため',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (18,2,3,'次のコードでキャストが必要な理由として正しいものはどれ？
EmployeeBean user = (EmployeeBean) session.getAttribute("user");','getAttribute() の戻り値が Object 型だから','EmployeeBean はEntityではないから','セッションには文字列しか保存できないから',1,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (19,3,1,'HTMLでページのタイトルを設定するタグはどれ？','<head>','<title>','<h1>',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (20,3,1,'画像を表示するタグはどれ？','<image>','<img>','<pic>',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (21,3,1,'次のうち、正しいリスト構造はどれ？','<ul>
  <li>項目</li>
</ul>','<li>
  <ul>項目</ul>
</li>','<ul>項目</ul>',1,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (22,3,2,'「好きな食べ物」という見出し（h2）と、「ラーメン」「カレー」「寿司」の3つを箇条書き（ul・li）で表示するHTMLとして正しいものを選んでください。','<h2>好きな食べ物</h2>
<ul>
  <li>ラーメン</li>
  <li>カレー</li>
  <li>寿司</li>
</ul>','<h1>好きな食べ物</h1>
<ol>
  <li>ラーメン</li>
  <li>カレー</li>
  <li>寿司</li>
</ol>','<h2>好きな食べ物</h2>
<ul>
  <p>ラーメン</p>
  <p>カレー</p>
  <p>寿司</p>
</ul>',1,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (23,3,2,'「朝のルーティン」という見出し（h2）と、「起きる」「顔を洗う」「朝ごはんを食べる」を番号つきリスト（ol・li）で表示するHTMLとして正しいものを選んでください。','<h2>朝のルーティン</h2>
<ul>
  <li>起きる</li>
  <li>顔を洗う</li>
  <li>朝ごはんを食べる</li>
</ul>','<h2>朝のルーティン</h2>
<ol>
  <p>起きる</p>
  <p>顔を洗う</p>
  <p>朝ごはんを食べる</p>
</ol>','<h2>朝のルーティン</h2>
<ol>
  <li>起きる</li>
  <li>顔を洗う</li>
  <li>朝ごはんを食べる</li>
</ol>',3,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (24,3,2,'「Googleで調べる」というテキストのリンクを作成します。リンク先は https://google.com です。正しいHTMLを選んでください。','<link href="https://google.com">
Googleで調べる
</link>','<a href="https://google.com">Googleで調べる</a>','<a src="https://google.com">Googleで調べる</a>',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (25,3,3,'「名前」「年齢」の2列のテーブルを作り、「【名前】」「15歳」という1行のデータを表示するHTMLとして正しいものを選んでください。','<table><tr><td>名前</td><td>年齢</td></tr><th><td>【名前】</td><td>15歳</td></th></table>','<table><th>名前</th><th>年齢</th><td>【名前】</td><td>15歳</td></table>','<table>
  <tr><th>名前</th><th>年齢</th></tr>
  <tr><td>【名前】</td><td>15歳</td></tr>
</table>',3,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (26,3,3,'「名前を入力してください」というラベルと、テキスト入力欄、「送信」ボタンを作成するHTMLとして正しいものを選んでください。','<form>
  <label for="name">名前を入力してください</label>
  <input type="text" id="name" name="name">
  <button type="submit">送信</button>
</form>','<form><label>名前を入力してください</label><input type="button" id="name"><button>送信</button></form>','<form><p for="name">名前を入力してください</p><text id="name"></text><submit>送信</submit></form>',1,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (27,3,3,'タイトルが「私のページ」、本文に「はじめてのHTMLページです。」という段落を持つ、完全なHTMLファイルとして正しいものを選んでください。','<html><head><h1>私のページ</h1></head><body>はじめてのHTMLページです。</body></html>','<!DOCTYPE html>
<html lang="ja">
  <head>
    <meta charset="UTF-8">
    <title>私のページ</title>
  </head>
  <body>
    <p>はじめてのHTMLページです。</p>
  </body>
</html>','<!DOCTYPE html><html lang="ja"><head><meta charset="UTF-8"></head><body><title>私のページ</title><p>はじめてのHTMLページです。</p></body></html>',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (29,4,1,'"BOOKS"表から書名に"UNIX"を含む行を全て探すために次のSQL文を用いる。aに指定する文字列としてして、適切なものはどれか。ここで、書名は"BOOKS"表の"書名"列に格納されている。
SELECT * FROM BOOKS WHERE 書名 LIKE ''[a]''','%UNIX','%UNIX%','UNIX%',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (30,4,1,'”得点”表から、学生ごとに全科目の点数の平均を算出し、平均が80点以上の学生の学生番号とその平均点を求める。aに入れる適切な字句はどれか。
得点（学生番号,科目,点数）
[SQL文] SELECT 学生番号,AVG(点数) FROM 得点 GROUP BY [ a ]','科目 HAVING AVG(点数) >=80','科目 WHERE 点数 >=80','学生番号 HAVING AVG(点数) >=80',3,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (31,4,1,'埋込みSQLにおいて、問い合わせによって得られた導出表を1行ずつ親プログラムに引き渡す操作がある。この操作と関係の深い字句はどれか。','CURSOR','ORDER BY','UNION',1,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (32,4,1,'RDBMS において、特定の利用者だけに表の更新する権限を与える方法として、適切なものはどれか。','CONNECT文で接続を許可する。','CREATE ASSERTION文で表明して制限する。','GRANT文で許可する。',3,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (33,4,1,'表に対するSQLのGRANT文の説明として、適切なものはどれか。','パスワードを設定してデータベースへの接続を制限する。','ビューを作成して、ビューの基となる表のアクセスできる行と列を制限する。','表の利用者に対し、表への問い合わせ、更新、追加、削除などの操作権限を付与する。',3,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (34,4,1,'SQLの構文として、正しいものはどれか。','SELECT 注文日,AVG(数量) FROM 注文明細','SELECT 注文日,AVG(数量) FROM 注文明細 GROUP BY 注文日','SELECT 注文日,AVG(SUM(数量)) FROM 注文明細 GROUP BY 注文日',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (35,4,2,'「正解フラグ（is_correct）が 1 のデータをグループ化し、そのカウントが2件以上のユーザー」を取得する正しいSQLはどれですか？','SELECT user_id FROM answer WHERE is_correct = 1 AND COUNT() >= 2 GROUP BY user_id;','SELECT user_id FROM answer WHERE is_correct = 1 GROUP BY user_id HAVING COUNT() >= 2;','SELECT user_id FROM answer GROUP BY user_id HAVING is_correct = 1 AND COUNT() >= 2;',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (36,4,2,'customer（顧客）テーブルに answer（解答）テーブルを LEFT JOIN しました。しかし、「解答していない（answerにデータがない）顧客」も含めて全員を表示したいのに、特定の書き方をすると未解答の顧客が消えてしまいます。全員を表示できる正しいSQLはどれですか？','SELECT * FROM customer c LEFT JOIN answer a ON c.user_id = a.user_id WHERE a.question_id = 1;','SELECT * FROM customer c LEFT JOIN answer a ON c.user_id = a.user_id AND a.question_id = 1;','SELECT * FROM customer c LEFT JOIN answer a ON c.user_id = a.user_id WHERE a.question_id IS NOT NULL;',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (37,4,2,'customer テーブルに10行のデータがあります。そのうち nickname 列が NULL（空っぽ）のデータが3行あります。 SELECT COUNT(nickname) FROM customer; を実行した結果、返ってくる数値は何ですか？','10','7','3',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (38,4,2,'「まだ1度も解答（answerテーブルに記録）していない顧客の user_id」を取得しようとして、以下のSQLを書きました。
SELECT user_id FROM customer WHERE user_id NOT IN (SELECT user_id FROM answer);
もし、answer テーブルの user_id 列にたった1行でも NULL のデータが混ざっていた場合、このSQLの結果はどうなりますか？','NULLの行だけを除外して、正しく未解答の顧客が取得できる。','エラー（ORA-00936など）が発生して実行できない。','結果が「0件（何も返ってこない）」になってしまう。',3,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (39,4,2,'Oracle Databaseにおいて、customer テーブルの last_name（姓）と first_name（名）の間にスペースを挟んで「山田 太郎」のように結合して取得したいです。正しい書き方はどれですか？','SELECT last_name + '' '' + first_name FROM customer;','SELECT last_name || '' '' || first_name FROM customer;','SELECT CONCAT(last_name, '' '', first_name) FROM customer;',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (40,4,2,'スコア（score）が高い順にユーザーを並び替えたい（ORDER BY score DESC）のですが、スコアが未入力（NULL）のユーザーがいます。普通に書くと NULL の人が一番上にきてしまいます。「NULLの人は一番下に落とした上で、スコアの高い順に並べる」正しいOracleの書き方はどれですか？','ORDER BY score DESC NULLS LAST','ORDER BY score DESC NULLS FIRST','ORDER BY score DESC WHERE score IS NOT NULL',1,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (41,4,2,'クイズの文章が登録されている question テーブルから、「文字として『%』という記号が含まれている問題」を検索したいです（例：「正解率は50%ですか？」など）。正しく検索できるSQLはどれですか？','SELECT * FROM question WHERE question_text LIKE ''%%%'';','SELECT * FROM question WHERE question_text LIKE ''%%%'';','SELECT * FROM question WHERE question_text LIKE ''%#%'' ESCAPE ''#'';',3,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (42,4,2,'次のSQLのように、メインのクエリ（外側）のテーブルの列（c.user_id）を、サブクエリ（内側）の中で参照して比較するような構文のテーブル走査方式を、何サブクエリ（副問合せ）と呼びますか？
SELECT * FROM customer c WHERE EXISTS (SELECT 1 FROM answer a WHERE a.user_id = c.user_id);','スカラー・サブクエリ','相関サブクエリ（相関副問合せ）','表サブクエリ',2,null);
Insert into PROSTA_USER.QUESTION (QUESTION_ID,CATEGORY_ID,DIFFICULTY,QUESTION_TEXT,OPTION_A,OPTION_B,OPTION_C,CORRECT_OPTION,EXPLANATION) values (43,4,2,'各ユーザーの解答データに、「ユーザーごと（user_idごと）に、解答した日時（ans_time）が古い順に 1, 2, 3... と連番（順位）」 を振って取得したいです。SELECT 文の中に書くべき正しい記述はどれですか？','ROW_NUMBER() OVER (ORDER BY ans_time PARTITION BY user_id)','ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY ans_time)','RANK() OVER (GROUP BY user_id ORDER BY ans_time)',2,null);
REM INSERTING into PROSTA_USER.USER_ACHIEVEMENT
SET DEFINE OFF;
Insert into PROSTA_USER.USER_ACHIEVEMENT (USER_ACH_ID,USER_ID,ACHIEVEMENT_ID,UNLOCKED_DATE,SEEN_FLAG) values (1,3,5,to_date('26-10-23','RR-MM-DD'),1);
Insert into PROSTA_USER.USER_ACHIEVEMENT (USER_ACH_ID,USER_ID,ACHIEVEMENT_ID,UNLOCKED_DATE,SEEN_FLAG) values (2,3,6,to_date('26-10-23','RR-MM-DD'),1);
--------------------------------------------------------
--  DDL for Index PK_CATEGORY
--------------------------------------------------------

  CREATE UNIQUE INDEX "PROSTA_USER"."PK_CATEGORY" ON "PROSTA_USER"."CATEGORY" ("CATEGORY_ID") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;
--------------------------------------------------------
--  DDL for Index PK_ACHIEVEMENT
--------------------------------------------------------

  CREATE UNIQUE INDEX "PROSTA_USER"."PK_ACHIEVEMENT" ON "PROSTA_USER"."ACHIEVEMENT" ("ACHIEVEMENT_ID") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;
--------------------------------------------------------
--  DDL for Index PK_QUESTION
--------------------------------------------------------

  CREATE UNIQUE INDEX "PROSTA_USER"."PK_QUESTION" ON "PROSTA_USER"."QUESTION" ("QUESTION_ID") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;
--------------------------------------------------------
--  DDL for Index IX_FRIEND_CONNECTION_HIGH_STATUS
--------------------------------------------------------

  CREATE INDEX "PROSTA_USER"."IX_FRIEND_CONNECTION_HIGH_STATUS" ON "PROSTA_USER"."FRIEND_CONNECTION" ("USER_HIGH_ID", "STATUS") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  TABLESPACE "USERS" ;
--------------------------------------------------------
--  DDL for Index PK_CUSTOMER
--------------------------------------------------------

  CREATE UNIQUE INDEX "PROSTA_USER"."PK_CUSTOMER" ON "PROSTA_USER"."CUSTOMER" ("USER_ID") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;
--------------------------------------------------------
--  DDL for Index PK_ANSWER
--------------------------------------------------------

  CREATE UNIQUE INDEX "PROSTA_USER"."PK_ANSWER" ON "PROSTA_USER"."ANSWER" ("ANS_ID") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;
--------------------------------------------------------
--  DDL for Index PK_FAVORITE
--------------------------------------------------------

  CREATE UNIQUE INDEX "PROSTA_USER"."PK_FAVORITE" ON "PROSTA_USER"."FAVORITE" ("FAVORITE_ID") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  TABLESPACE "USERS" ;
--------------------------------------------------------
--  DDL for Index IX_FRIEND_CONNECTION_LOW_STATUS
--------------------------------------------------------

  CREATE INDEX "PROSTA_USER"."IX_FRIEND_CONNECTION_LOW_STATUS" ON "PROSTA_USER"."FRIEND_CONNECTION" ("USER_LOW_ID", "STATUS") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  TABLESPACE "USERS" ;
--------------------------------------------------------
--  DDL for Index UQ_USER_ACH
--------------------------------------------------------

  CREATE UNIQUE INDEX "PROSTA_USER"."UQ_USER_ACH" ON "PROSTA_USER"."USER_ACHIEVEMENT" ("USER_ID", "ACHIEVEMENT_ID") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;
--------------------------------------------------------
--  DDL for Index PK_FRIEND_CONNECTION
--------------------------------------------------------

  CREATE UNIQUE INDEX "PROSTA_USER"."PK_FRIEND_CONNECTION" ON "PROSTA_USER"."FRIEND_CONNECTION" ("FRIEND_CONNECTION_ID") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  TABLESPACE "USERS" ;
--------------------------------------------------------
--  DDL for Index PK_USER_ACHIEVEMENT
--------------------------------------------------------

  CREATE UNIQUE INDEX "PROSTA_USER"."PK_USER_ACHIEVEMENT" ON "PROSTA_USER"."USER_ACHIEVEMENT" ("USER_ACH_ID") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;
--------------------------------------------------------
--  DDL for Index UK_FRIEND_CONNECTION_PAIR
--------------------------------------------------------

  CREATE UNIQUE INDEX "PROSTA_USER"."UK_FRIEND_CONNECTION_PAIR" ON "PROSTA_USER"."FRIEND_CONNECTION" ("USER_LOW_ID", "USER_HIGH_ID") 
  PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  TABLESPACE "USERS" ;
--------------------------------------------------------
--  Constraints for Table FAVORITE
--------------------------------------------------------

  ALTER TABLE "PROSTA_USER"."FAVORITE" MODIFY ("USER_ID" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."FAVORITE" MODIFY ("QUESTION_ID" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."FAVORITE" ADD CONSTRAINT "PK_FAVORITE" PRIMARY KEY ("FAVORITE_ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  TABLESPACE "USERS"  ENABLE;
--------------------------------------------------------
--  Constraints for Table USER_ACHIEVEMENT
--------------------------------------------------------

  ALTER TABLE "PROSTA_USER"."USER_ACHIEVEMENT" MODIFY ("USER_ID" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."USER_ACHIEVEMENT" MODIFY ("ACHIEVEMENT_ID" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."USER_ACHIEVEMENT" MODIFY ("UNLOCKED_DATE" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."USER_ACHIEVEMENT" ADD CONSTRAINT "PK_USER_ACHIEVEMENT" PRIMARY KEY ("USER_ACH_ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS"  ENABLE;
  ALTER TABLE "PROSTA_USER"."USER_ACHIEVEMENT" ADD CONSTRAINT "UQ_USER_ACH" UNIQUE ("USER_ID", "ACHIEVEMENT_ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS"  ENABLE;
  ALTER TABLE "PROSTA_USER"."USER_ACHIEVEMENT" MODIFY ("SEEN_FLAG" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table ACHIEVEMENT
--------------------------------------------------------

  ALTER TABLE "PROSTA_USER"."ACHIEVEMENT" MODIFY ("ACHIEVEMENT_NAME" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."ACHIEVEMENT" MODIFY ("DESCRIPTION" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."ACHIEVEMENT" MODIFY ("ICON" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."ACHIEVEMENT" ADD CONSTRAINT "PK_ACHIEVEMENT" PRIMARY KEY ("ACHIEVEMENT_ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS"  ENABLE;
--------------------------------------------------------
--  Constraints for Table ANSWER
--------------------------------------------------------

  ALTER TABLE "PROSTA_USER"."ANSWER" MODIFY ("USER_ID" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."ANSWER" MODIFY ("QUESTION_ID" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."ANSWER" MODIFY ("ANS_OPTION" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."ANSWER" MODIFY ("CORRECT_OPTION" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."ANSWER" MODIFY ("CORRECT_FLAG" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."ANSWER" MODIFY ("ANS_DATE" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."ANSWER" ADD CONSTRAINT "PK_ANSWER" PRIMARY KEY ("ANS_ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS"  ENABLE;
  ALTER TABLE "PROSTA_USER"."ANSWER" MODIFY ("ANS_COUNT" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table CUSTOMER
--------------------------------------------------------

  ALTER TABLE "PROSTA_USER"."CUSTOMER" MODIFY ("USER_NAME" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."CUSTOMER" MODIFY ("USER_PASS" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."CUSTOMER" MODIFY ("PERMISSION" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."CUSTOMER" MODIFY ("DELETE_FLAG" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."CUSTOMER" MODIFY ("LOGIN_DATE" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."CUSTOMER" MODIFY ("LOGIN_COUNT" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."CUSTOMER" MODIFY ("LOGIN_FLAG" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."CUSTOMER" MODIFY ("USER_LEVEL" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."CUSTOMER" ADD CONSTRAINT "PK_CUSTOMER" PRIMARY KEY ("USER_ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS"  ENABLE;
  ALTER TABLE "PROSTA_USER"."CUSTOMER" MODIFY ("CONSECUTIVE_LOGIN_DAYS" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table FRIEND_CONNECTION
--------------------------------------------------------

  ALTER TABLE "PROSTA_USER"."FRIEND_CONNECTION" MODIFY ("FRIEND_CONNECTION_ID" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."FRIEND_CONNECTION" MODIFY ("USER_LOW_ID" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."FRIEND_CONNECTION" MODIFY ("USER_HIGH_ID" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."FRIEND_CONNECTION" MODIFY ("REQUESTED_BY_ID" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."FRIEND_CONNECTION" MODIFY ("STATUS" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."FRIEND_CONNECTION" MODIFY ("CREATED_AT" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."FRIEND_CONNECTION" MODIFY ("UPDATED_AT" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."FRIEND_CONNECTION" ADD CONSTRAINT "CK_FRIEND_CONNECTION_ORDER" CHECK (user_low_id < user_high_id) ENABLE;
  ALTER TABLE "PROSTA_USER"."FRIEND_CONNECTION" ADD CONSTRAINT "CK_FRIEND_CONNECTION_STATUS" CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')) ENABLE;
  ALTER TABLE "PROSTA_USER"."FRIEND_CONNECTION" ADD CONSTRAINT "PK_FRIEND_CONNECTION" PRIMARY KEY ("FRIEND_CONNECTION_ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  TABLESPACE "USERS"  ENABLE;
  ALTER TABLE "PROSTA_USER"."FRIEND_CONNECTION" ADD CONSTRAINT "UK_FRIEND_CONNECTION_PAIR" UNIQUE ("USER_LOW_ID", "USER_HIGH_ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  TABLESPACE "USERS"  ENABLE;
--------------------------------------------------------
--  Constraints for Table QUESTION
--------------------------------------------------------

  ALTER TABLE "PROSTA_USER"."QUESTION" MODIFY ("CATEGORY_ID" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."QUESTION" MODIFY ("DIFFICULTY" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."QUESTION" MODIFY ("QUESTION_TEXT" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."QUESTION" MODIFY ("OPTION_A" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."QUESTION" MODIFY ("OPTION_B" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."QUESTION" MODIFY ("OPTION_C" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."QUESTION" MODIFY ("CORRECT_OPTION" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."QUESTION" ADD CONSTRAINT "PK_QUESTION" PRIMARY KEY ("QUESTION_ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS"  ENABLE;
--------------------------------------------------------
--  Constraints for Table CATEGORY
--------------------------------------------------------

  ALTER TABLE "PROSTA_USER"."CATEGORY" MODIFY ("CATEGORY_NAME" NOT NULL ENABLE);
  ALTER TABLE "PROSTA_USER"."CATEGORY" ADD CONSTRAINT "PK_CATEGORY" PRIMARY KEY ("CATEGORY_ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS"  ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table ANSWER
--------------------------------------------------------

  ALTER TABLE "PROSTA_USER"."ANSWER" ADD CONSTRAINT "FK_ANSWER_USER" FOREIGN KEY ("USER_ID")
	  REFERENCES "PROSTA_USER"."CUSTOMER" ("USER_ID") ENABLE;
  ALTER TABLE "PROSTA_USER"."ANSWER" ADD CONSTRAINT "FK_ANSWER_QUESTION" FOREIGN KEY ("QUESTION_ID")
	  REFERENCES "PROSTA_USER"."QUESTION" ("QUESTION_ID") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table FAVORITE
--------------------------------------------------------

  ALTER TABLE "PROSTA_USER"."FAVORITE" ADD CONSTRAINT "FK_FAVORITE_USER" FOREIGN KEY ("USER_ID")
	  REFERENCES "PROSTA_USER"."CUSTOMER" ("USER_ID") ENABLE;
  ALTER TABLE "PROSTA_USER"."FAVORITE" ADD CONSTRAINT "FK_FAVORITE_QUESTION" FOREIGN KEY ("QUESTION_ID")
	  REFERENCES "PROSTA_USER"."QUESTION" ("QUESTION_ID") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table FRIEND_CONNECTION
--------------------------------------------------------

  ALTER TABLE "PROSTA_USER"."FRIEND_CONNECTION" ADD CONSTRAINT "FK_FRIEND_CONNECTION_LOW" FOREIGN KEY ("USER_LOW_ID")
	  REFERENCES "PROSTA_USER"."CUSTOMER" ("USER_ID") ENABLE;
  ALTER TABLE "PROSTA_USER"."FRIEND_CONNECTION" ADD CONSTRAINT "FK_FRIEND_CONNECTION_HIGH" FOREIGN KEY ("USER_HIGH_ID")
	  REFERENCES "PROSTA_USER"."CUSTOMER" ("USER_ID") ENABLE;
  ALTER TABLE "PROSTA_USER"."FRIEND_CONNECTION" ADD CONSTRAINT "FK_FRIEND_CONNECTION_REQUESTER" FOREIGN KEY ("REQUESTED_BY_ID")
	  REFERENCES "PROSTA_USER"."CUSTOMER" ("USER_ID") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table QUESTION
--------------------------------------------------------

  ALTER TABLE "PROSTA_USER"."QUESTION" ADD CONSTRAINT "FK_CATEGORY_2" FOREIGN KEY ("CATEGORY_ID")
	  REFERENCES "PROSTA_USER"."CATEGORY" ("CATEGORY_ID") ENABLE;
--------------------------------------------------------
--  Ref Constraints for Table USER_ACHIEVEMENT
--------------------------------------------------------

  ALTER TABLE "PROSTA_USER"."USER_ACHIEVEMENT" ADD CONSTRAINT "FK_UA_CUSTOMER" FOREIGN KEY ("USER_ID")
	  REFERENCES "PROSTA_USER"."CUSTOMER" ("USER_ID") ENABLE;
  ALTER TABLE "PROSTA_USER"."USER_ACHIEVEMENT" ADD CONSTRAINT "FK_UA_ACHIEVEMENT" FOREIGN KEY ("ACHIEVEMENT_ID")
	  REFERENCES "PROSTA_USER"."ACHIEVEMENT" ("ACHIEVEMENT_ID") ENABLE;
