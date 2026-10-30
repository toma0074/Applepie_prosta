-- Apply once to the existing PROSTA Oracle schema (run as PROSTA_USER, UTF-8).

ALTER TABLE customer ADD (
    perfect_test_count NUMBER(10) DEFAULT 0 NOT NULL,
    total_exp          NUMBER(19) DEFAULT 0 NOT NULL
);

-- 現在のレベルを維持する (レベル = total_exp / 15 + 1)
UPDATE customer
   SET total_exp = (NVL(user_level, 1) - 1) * 15;

COMMIT;

COMMENT ON COLUMN customer.perfect_test_count IS '満点回数';
COMMENT ON COLUMN customer.total_exp IS '累計EXP';
