-- 基本情報カテゴリの追加問題（難易度 1〜3 を各3問、V20261031 と合わせて各5問）。
-- 同じ問題文があれば追加しないため、何度実行しても安全。
SET DEFINE OFF;

INSERT INTO question (question_id, category_id, difficulty, question_text, option_a, option_b, option_c, correct_option, explanation)
SELECT seq_question.NEXTVAL, c.category_id, 1,
  '1バイトは何ビットですか？',
  '4ビット', '8ビット', '16ビット', 2,
  '1バイトは 8 ビットです。1ビットは 0 か 1 のどちらかを表す最小単位で、8 ビットで 256 通り（0〜255）の値を表せます。'
FROM category c
WHERE c.category_name = '基本情報'
  AND NOT EXISTS (SELECT 1 FROM question q WHERE q.question_text = '1バイトは何ビットですか？');

INSERT INTO question (question_id, category_id, difficulty, question_text, option_a, option_b, option_c, correct_option, explanation)
SELECT seq_question.NEXTVAL, c.category_id, 1,
  'プログラムの命令を解釈して実行する、コンピュータの中心的な装置はどれですか？',
  'CPU', 'ハードディスク', 'ディスプレイ', 1,
  'CPU（中央処理装置）は命令の解釈・実行と演算を行う頭脳にあたる装置です。ハードディスクは記憶装置、ディスプレイは出力装置です。'
FROM category c
WHERE c.category_name = '基本情報'
  AND NOT EXISTS (SELECT 1 FROM question q WHERE q.question_text = 'プログラムの命令を解釈して実行する、コンピュータの中心的な装置はどれですか？');

INSERT INTO question (question_id, category_id, difficulty, question_text, option_a, option_b, option_c, correct_option, explanation)
SELECT seq_question.NEXTVAL, c.category_id, 1,
  'ファイルの内容を元に戻せる形で、データ量を小さくまとめる処理はどれですか？',
  '圧縮', '暗号化', '正規化', 1,
  '圧縮は、元のデータに復元できる形でデータ量を小さくする処理です。暗号化は第三者に読めなくする処理、正規化はデータベースの設計手法です。'
FROM category c
WHERE c.category_name = '基本情報'
  AND NOT EXISTS (SELECT 1 FROM question q WHERE q.question_text = 'ファイルの内容を元に戻せる形で、データ量を小さくまとめる処理はどれですか？');

INSERT INTO question (question_id, category_id, difficulty, question_text, option_a, option_b, option_c, correct_option, explanation)
SELECT seq_question.NEXTVAL, c.category_id, 2,
  '16進数の 1F を10進数で表すと、いくつになりますか？',
  '17', '31', '63', 2,
  '16進数の F は 10進数の 15 です。1F = 1 × 16 + 15 = 31 になります。'
FROM category c
WHERE c.category_name = '基本情報'
  AND NOT EXISTS (SELECT 1 FROM question q WHERE q.question_text = '16進数の 1F を10進数で表すと、いくつになりますか？');

INSERT INTO question (question_id, category_id, difficulty, question_text, option_a, option_b, option_c, correct_option, explanation)
SELECT seq_question.NEXTVAL, c.category_id, 2,
  'データ構造のスタックの特徴として正しいものはどれですか？',
  '最後に入れたデータが最初に取り出される（後入れ先出し）', '最初に入れたデータが最初に取り出される（先入れ先出し）', '好きな位置のデータを自由に取り出せる', 1,
  'スタックは後入れ先出し（LIFO）の構造です。先入れ先出し（FIFO）はキューの特徴です。'
FROM category c
WHERE c.category_name = '基本情報'
  AND NOT EXISTS (SELECT 1 FROM question q WHERE q.question_text = 'データ構造のスタックの特徴として正しいものはどれですか？');

INSERT INTO question (question_id, category_id, difficulty, question_text, option_a, option_b, option_c, correct_option, explanation)
SELECT seq_question.NEXTVAL, c.category_id, 2,
  '多要素認証の組合せとして適切なものはどれですか？',
  'パスワードと秘密の質問', 'パスワードと指紋認証', '2つの異なるパスワード', 2,
  '多要素認証は「知識（パスワード）」「所持（ICカードなど）」「生体（指紋など）」のうち、異なる種類の要素を組み合わせます。パスワードと秘密の質問、2つのパスワードはどちらも知識情報なので、同じ種類の組合せです。'
FROM category c
WHERE c.category_name = '基本情報'
  AND NOT EXISTS (SELECT 1 FROM question q WHERE q.question_text = '多要素認証の組合せとして適切なものはどれですか？');

INSERT INTO question (question_id, category_id, difficulty, question_text, option_a, option_b, option_c, correct_option, explanation)
SELECT seq_question.NEXTVAL, c.category_id, 3,
  '稼働率 0.9 の装置を2台並列に接続したシステム（どちらか1台が動けば稼働）の稼働率はいくつですか？',
  '81%', '99%', '90%', 2,
  '並列システムは両方が同時に止まったときだけ停止します。稼働率 = 1 − (1 − 0.9) × (1 − 0.9) = 1 − 0.01 = 0.99 なので 99% です。'
FROM category c
WHERE c.category_name = '基本情報'
  AND NOT EXISTS (SELECT 1 FROM question q WHERE q.question_text = '稼働率 0.9 の装置を2台並列に接続したシステム（どちらか1台が動けば稼働）の稼働率はいくつですか？');

INSERT INTO question (question_id, category_id, difficulty, question_text, option_a, option_b, option_c, correct_option, explanation)
SELECT seq_question.NEXTVAL, c.category_id, 3,
  '8ビットの2の補数表現で、2進数 11111111 が表す10進数はどれですか？',
  '255', '-1', '-127', 2,
  '2の補数表現では最上位ビットが符号を表します。11111111 は全ビットを反転して 1 を足すと 00000001 になるので、-1 を表します。符号なし整数として読んだときの 255 とは別です。'
FROM category c
WHERE c.category_name = '基本情報'
  AND NOT EXISTS (SELECT 1 FROM question q WHERE q.question_text = '8ビットの2の補数表現で、2進数 11111111 が表す10進数はどれですか？');

INSERT INTO question (question_id, category_id, difficulty, question_text, option_a, option_b, option_c, correct_option, explanation)
SELECT seq_question.NEXTVAL, c.category_id, 3,
  'SQLインジェクション対策として最も有効なものはどれですか？',
  'プレースホルダ（バインド機構）を用いて SQL を組み立てる', '入力できる文字数を制限する', 'ファイアウォールを設置する', 1,
  'プレースホルダを使うと、入力値が SQL の構文としては解釈されず単なる値として扱われます。文字数制限やファイアウォールだけでは根本的な対策になりません。'
FROM category c
WHERE c.category_name = '基本情報'
  AND NOT EXISTS (SELECT 1 FROM question q WHERE q.question_text = 'SQLインジェクション対策として最も有効なものはどれですか？');

COMMIT;
