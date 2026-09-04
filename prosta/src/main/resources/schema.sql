-- 部署テーブルの作成
CREATE TABLE department  (
  dept_id NUMBER(2) PRIMARY KEY,
  dept_name VARCHAR(15 CHARACTERS) NOT NULL
);

-- 社員テーブルの作成
CREATE TABLE employee (
  emp_id NUMBER(5) PRIMARY KEY,
  emp_pass VARCHAR(16 CHARACTERS) NOT NULL,
  emp_name VARCHAR(30 CHARACTERS) NOT NULL,
  gender NUMBER(1) NOT NULL,
  address VARCHAR(60 CHARACTERS) NOT NULL,
  birthday DATE NOT NULL,
  authority NUMBER(1) NOT NULL,
  dept_id NUMBER(2) NOT NULL REFERENCES department(dept_id)
);

