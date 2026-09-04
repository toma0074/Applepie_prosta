-- シーケンスの作成
CREATE SEQUENCE seq_emp NOCACHE;

-- 部署テーブルへのレコード登録
INSERT INTO department VALUES(1, '営業部');
INSERT INTO department VALUES(2, '経理部');
INSERT INTO department VALUES(3, '総務部');

-- 社員テーブルへのレコード登録
INSERT INTO employee VALUES(NEXT VALUE FOR seq_emp,'1111','鈴木太郎',1,'東京都','1986-10-12',1,1);
INSERT INTO employee VALUES(NEXT VALUE FOR seq_emp,'2222','田中二郎',1,'千葉県','1979-07-02',2,2);
INSERT INTO employee VALUES(NEXT VALUE FOR seq_emp,'3333','渡辺花子',2,'大阪府','1988-04-23',2,2);

COMMIT;