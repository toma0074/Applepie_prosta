-- category が空のDB向けの初期データ（Java / Spring / HTML / SQL）。
-- 同名のカテゴリが既にあれば追加しないため、何度実行しても安全。
-- 上から順に seq_category から 1〜4 が採番される（既存の問題データの category_id と一致）。
INSERT INTO category (category_id, category_name)
SELECT seq_category.NEXTVAL, 'Java' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM category WHERE category_name = 'Java');

INSERT INTO category (category_id, category_name)
SELECT seq_category.NEXTVAL, 'Spring' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM category WHERE category_name = 'Spring');

INSERT INTO category (category_id, category_name)
SELECT seq_category.NEXTVAL, 'HTML' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM category WHERE category_name = 'HTML');

INSERT INTO category (category_id, category_name)
SELECT seq_category.NEXTVAL, 'SQL' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM category WHERE category_name = 'SQL');

INSERT INTO category (category_id, category_name)
SELECT seq_category.NEXTVAL, '基本情報' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM category WHERE category_name = '基本情報');

COMMIT;