ALTER TABLE customer ADD (
    background_class VARCHAR2(32 CHAR),
    background_color VARCHAR2(7 CHAR)
);

COMMENT ON COLUMN customer.background_class IS '選択中の背景クラス';
COMMENT ON COLUMN customer.background_color IS 'カスタム背景色';
