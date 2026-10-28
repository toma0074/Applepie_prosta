-- 基本情報カテゴリのサンプル問題（難易度 1〜3 を各2問）。
-- カテゴリは V20261022__category_seed.sql で追加される。同じ問題文があれば追加しないため、何度実行しても安全。
SET DEFINE OFF;

INSERT INTO question (question_id, category_id, difficulty, question_text, option_a, option_b, option_c, correct_option, explanation)
SELECT seq_question.NEXTVAL, c.category_id, 1,
  '2進数の 1011 を10進数で表すと、いくつになりますか？',
  '9', '11', '13', 2,
  '各桁の重みは右から 1, 2, 4, 8 です。8 + 2 + 1 = 11 になります。'
FROM category c
WHERE c.category_name = '基本情報'
  AND NOT EXISTS (SELECT 1 FROM question q WHERE q.question_text = '2進数の 1011 を10進数で表すと、いくつになりますか？');

INSERT INTO question (question_id, category_id, difficulty, question_text, option_a, option_b, option_c, correct_option, explanation)
SELECT seq_question.NEXTVAL, c.category_id, 1,
  'コンピュータの主記憶として使われ、電源を切ると記憶内容が消える半導体メモリはどれですか？',
  'ROM', 'RAM', 'フラッシュメモリ', 2,
  'RAM は揮発性メモリで、電源を切ると内容が消えます。ROM やフラッシュメモリは電源を切っても内容が保持されます。'
FROM category c
WHERE c.category_name = '基本情報'
  AND NOT EXISTS (SELECT 1 FROM question q WHERE q.question_text = 'コンピュータの主記憶として使われ、電源を切ると記憶内容が消える半導体メモリはどれですか？');

INSERT INTO question (question_id, category_id, difficulty, question_text, option_a, option_b, option_c, correct_option, explanation)
SELECT seq_question.NEXTVAL, c.category_id, 2,
  'OSI参照モデルで、IPアドレスを用いた経路選択（ルーティング）を担当する層はどれですか？',
  'トランスポート層', 'ネットワーク層', 'データリンク層', 2,
  'ネットワーク層（第3層）が IP によるアドレス指定と経路選択を担当します。トランスポート層は TCP/UDP、データリンク層は MAC アドレスによる隣接機器間の転送を担当します。'
FROM category c
WHERE c.category_name = '基本情報'
  AND NOT EXISTS (SELECT 1 FROM question q WHERE q.question_text = 'OSI参照モデルで、IPアドレスを用いた経路選択（ルーティング）を担当する層はどれですか？');

INSERT INTO question (question_id, category_id, difficulty, question_text, option_a, option_b, option_c, correct_option, explanation)
SELECT seq_question.NEXTVAL, c.category_id, 2,
  'データベースの正規化を行う主な目的として、最も適切なものはどれですか？',
  'データの重複を減らし、更新時の矛盾を防ぐ', '検索速度を必ず向上させる', 'データを暗号化して漏えいを防ぐ', 1,
  '正規化はデータの重複（冗長性）を排除し、更新・追加・削除時の不整合（異常）を防ぐための設計手法です。検索速度の向上や暗号化が目的ではありません。'
FROM category c
WHERE c.category_name = '基本情報'
  AND NOT EXISTS (SELECT 1 FROM question q WHERE q.question_text = 'データベースの正規化を行う主な目的として、最も適切なものはどれですか？');

INSERT INTO question (question_id, category_id, difficulty, question_text, option_a, option_b, option_c, correct_option, explanation)
SELECT seq_question.NEXTVAL, c.category_id, 3,
  'MTBF が 90 時間、MTTR が 10 時間のシステムの稼働率はいくつですか？',
  '80%', '90%', '99%', 2,
  '稼働率 = MTBF ÷ (MTBF + MTTR) = 90 ÷ (90 + 10) = 0.9 なので 90% です。'
FROM category c
WHERE c.category_name = '基本情報'
  AND NOT EXISTS (SELECT 1 FROM question q WHERE q.question_text = 'MTBF が 90 時間、MTTR が 10 時間のシステムの稼働率はいくつですか？');

INSERT INTO question (question_id, category_id, difficulty, question_text, option_a, option_b, option_c, correct_option, explanation)
SELECT seq_question.NEXTVAL, c.category_id, 3,
  '昇順に整列済みの 1,024 件のデータに2分探索法を適用したとき、最大で何回の比較で探索を終えられますか？',
  '10回', '32回', '512回', 1,
  '2分探索は1回の比較ごとに探索範囲が半分になります。2 の 10 乗 = 1,024 なので、最大でおよそ 10 回です。'
FROM category c
WHERE c.category_name = '基本情報'
  AND NOT EXISTS (SELECT 1 FROM question q WHERE q.question_text = '昇順に整列済みの 1,024 件のデータに2分探索法を適用したとき、最大で何回の比較で探索を終えられますか？');

COMMIT;
