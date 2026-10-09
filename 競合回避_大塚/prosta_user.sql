ALTER TABLE question MODIFY question_text VARCHAR2(1000);

-- --- Java 3択問題（9問） ---
INSERT INTO question VALUES (1, 1, 1, '次のうち、Javaで整数を表すデータ型はどれ？');
INSERT INTO question VALUES (2, 1, 1, '次のうち「繰り返し処理」を行う構文はどれ？');
INSERT INTO question VALUES (3, 1, 1, '次のうち、Javaのコメントとして正しいものはどれ？');
INSERT INTO question VALUES (4, 1, 2, '次のコードの出力として正しいものはどれか。' || CHR(10) || 'int x = 5;' || CHR(10) || '{' || CHR(10) || '    int y = x + 2;' || CHR(10) || '    System.out.println(y);' || CHR(10) || '}' || CHR(10) || 'System.out.println(x);');
INSERT INTO question VALUES (5, 1, 2, '次のコードの出力として正しいものはどれか。' || CHR(10) || 'int x = 10;' || CHR(10) || 'int y = 20;' || CHR(10) || 'if (x == 10)' || CHR(10) || '    y = 30;' || CHR(10) || 'System.out.println(y);');
INSERT INTO question VALUES (6, 1, 2, 'オーバーライド（override）に必要な条件として正しいものはどれか？');
INSERT INTO question VALUES (7, 1, 3, '次のコードの出力として正しいものはどれか。' || CHR(10) || 'int a = 3;' || CHR(10) || 'int b = 4;' || CHR(10) || 'a = b++ + ++a;' || CHR(10) || 'System.out.println(a + b);');
INSERT INTO question VALUES (8, 1, 3, '次のコードの出力として正しいものはどれか。' || CHR(10) || 'int x = 0;' || CHR(10) || 'for (int i = 1; i < 4; i++) {' || CHR(10) || '    x += i; if (i == 2) break;' || CHR(10) || '} ' || CHR(10) || 'System.out.println(x);');
INSERT INTO question VALUES (9, 1, 3, 'インターフェースに関する説明として正しいものはどれか？');

-- --- Spring 3択問題（9問） ---
INSERT INTO question VALUES (10, 2, 1, 'Spring Bootで、リクエストを受け取るクラスに付けるアノテーションはどれ？');
INSERT INTO question VALUES (11, 2, 1, 'Controllerから画面に値を渡すためによく使うものはどれ？');
INSERT INTO question VALUES (12, 2, 1, '次のコードの説明として正しいものはどれ？' || CHR(10) || 'model.addAttribute("empName", employee.getEmpName());');
INSERT INTO question VALUES (13, 2, 2, 'Thymeleafで、Controllerから渡された empName を表示する書き方として正しいものはどれ？');
INSERT INTO question VALUES (14, 2, 2, '次のコードで、BindingResult result を書く位置として正しいものはどれ？' || CHR(10) || 'public String login(@Valid @ModelAttribute LoginForm loginForm, BindingResult result, Model model) {}');
INSERT INTO question VALUES (15, 2, 2, 'return "redirect:/list"; の説明として正しいものはどれ？');
INSERT INTO question VALUES (16, 2, 3, 'getReferenceById(id) を使ったときに、The given id must not be null が出る原因として考えられるものはどれ？');
INSERT INTO question VALUES (17, 2, 3, 'ログイン成功時に、EntityをそのままSessionに保存するより、Beanに必要な情報だけ詰めて保存する理由として適切なのはどれ？');
INSERT INTO question VALUES (18, 2, 3, '次のコードでキャストが必要な理由として正しいものはどれ？' || CHR(10) || 'EmployeeBean user = (EmployeeBean) session.getAttribute("user");');

-- --- HTML 3択問題（9問）---
INSERT INTO question VALUES (19, 3, 1, 'HTMLでページのタイトルを設定するタグはどれ？');
INSERT INTO question VALUES (20, 3, 1, '画像を表示するタグはどれ？');
INSERT INTO question VALUES (21, 3, 1, '次のうち、正しいリスト構造はどれ？');
INSERT INTO question VALUES (22, 3, 2, '「好きな食べ物」という見出し（h2）と、「ラーメン」「カレー」「寿司」の3つを箇条書き（ul・li）で表示するHTMLとして正しいものを選んでください。');
INSERT INTO question VALUES (23, 3, 2, '「朝のルーティン」という見出し（h2）と、「起きる」「顔を洗う」「朝ごはんを食べる」を番号つきリスト（ol・li）で表示するHTMLとして正しいものを選んでください。');
INSERT INTO question VALUES (24, 3, 2, '「Googleで調べる」というテキストのリンクを作成します。リンク先は https://www.google.com です。正しいHTMLを選んでください。');
INSERT INTO question VALUES (25, 3, 3, '「名前」「年齢」の2列のテーブルを作り、「【名前】」「15歳」という1行のデータを表示するHTMLとして正しいものを選んでください。');
INSERT INTO question VALUES (26, 3, 3, '「名前を入力してください」というラベルと、テキスト入力欄、「送信」ボタンを作成するHTMLとして正しいものを選んでください。');
INSERT INTO question VALUES (27, 3, 3, 'タイトルが「私のページ」、本文に「はじめてのHTMLページです。」という段落を持つ、完全なHTMLファイルとして正しいものを選んでください。');

-- ---SQL 3択問題
INSERT INTO question VALUES (28, 4, 1, '"中間テスト"表からクラスごと、教科ごとの平均点を求め、クラス名、教科名の昇順に表示するSQL文中のaに入れる字句はどれか？' || CHR(10) || '中間テスト（クラス名,教科名,学生番号,名前,点数）' || CHR(10) || '[SQL文] SELECT クラス名,教科名,AVG(点数) AS 平均点 FROM 中間テスト [a]');
INSERT INTO question VALUES (29, 4, 1, '"BOOKS"表から書名に"UNIX"を含む行を全て探すために次のSQL文を用いる。aに指定する文字列としてして、適切なものはどれか。ここで、書名は"BOOKS"表の"書名"列に格納されている。' || CHR(10) || 'SELECT * FROM BOOKS WHERE 書名 LIKE ''[a]'''); -- ※提示文のLIKESを一般的なLIKEに補正しています
INSERT INTO question VALUES (30, 4, 1, '”得点”表から、学生ごとに全科目の点数の平均を算出し、平均が80点以上の学生の学生番号とその平均点を求める。aに入れる適切な字句はどれか。' || CHR(10) || '得点（学生番号,科目,点数）' || CHR(10) || '[SQL文] SELECT 学生番号,AVG(点数) FROM 得点 GROUP BY [ a ]');
INSERT INTO question VALUES (31, 4, 1, '埋込みSQLにおいて、問い合わせによって得られた導出表を1行ずつ親プログラムに引き渡す操作がある。この操作と関係の深い字句はどれか。');
INSERT INTO question VALUES (32, 4, 1, 'RDBMS において、特定の利用者だけに表の更新する権限を与える方法として、適切なものはどれか。');
INSERT INTO question VALUES (33, 4, 1, '表に対するSQLのGRANT文の説明として、適切なものはどれか。');
INSERT INTO question VALUES (34, 4, 1, 'SQLの構文として、正しいものはどれか。');
INSERT INTO question VALUES (35, 4, 2, '「正解フラグ（is_correct）が 1 のデータをグループ化し、そのカウントが2件以上のユーザー」を取得する正しいSQLはどれですか？');
INSERT INTO question VALUES (36, 4, 2, 'customer（顧客）テーブルに answer（解答）テーブルを LEFT JOIN しました。しかし、「解答していない（answerにデータがない）顧客」も含めて全員を表示したいのに、特定の書き方をすると未解答の顧客が消えてしまいます。全員を表示できる正しいSQLはどれですか？');
INSERT INTO question VALUES (37, 4, 2, 'customer テーブルに10行のデータがあります。そのうち nickname 列が NULL（空っぽ）のデータが3行あります。 SELECT COUNT(nickname) FROM customer; を実行した結果、返ってくる数値は何ですか？');
INSERT INTO question VALUES (38, 4, 2, '「まだ1度も解答（answerテーブルに記録）していない顧客の user_id」を取得しようとして、以下のSQLを書きました。' || CHR(10) || 'SELECT user_id FROM customer WHERE user_id NOT IN (SELECT user_id FROM answer);' || CHR(10) || 'もし、answer テーブルの user_id 列にたった1行でも NULL のデータが混ざっていた場合、このSQLの結果はどうなりますか？');
INSERT INTO question VALUES (39, 4, 2, 'Oracle Databaseにおいて、customer テーブルの last_name（姓）と first_name（名）の間にスペースを挟んで「山田 太郎」のように結合して取得したいです。正しい書き方はどれですか？');
INSERT INTO question VALUES (40, 4, 2, 'スコア（score）が高い順にユーザーを並び替えたい（ORDER BY score DESC）のですが、スコアが未入力（NULL）のユーザーがいます。普通に書くと NULL の人が一番上にきてしまいます。「NULLの人は一番下に落とした上で、スコアの高い順に並べる」正しいOracleの書き方はどれですか？');
INSERT INTO question VALUES (41, 4, 2, 'クイズの文章が登録されている question テーブルから、「文字として『%』という記号が含まれている問題」を検索したいです（例：「正解率は50%ですか？」など）。正しく検索できるSQLはどれですか？');
INSERT INTO question VALUES (42, 4, 2, '次のSQLのように、メインのクエリ（外側）のテーブルの列（c.user_id）を、サブクエリ（内側）の中で参照して比較するような構文のテーブル走査方式を、何サブクエリ（副問合せ）と呼びますか？' || CHR(10) || 'SELECT * FROM customer c WHERE EXISTS (SELECT 1 FROM answer a WHERE a.user_id = c.user_id);');
INSERT INTO question VALUES (43, 4, 2, '各ユーザーの解答データに、「ユーザーごと（user_idごと）に、解答した日時（ans_time）が古い順に 1, 2, 3... と連番（順位）」 を振って取得したいです。SELECT 文の中に書くべき正しい記述はどれですか？');
INSERT INTO question VALUES (44, 4, 2, 'チーム開発で、メンバーAとメンバーBが同時に同じデータベースを触っています。' || CHR(10) || 'メンバーAが UPDATE customer SET nickname = ''ネコ'' WHERE user_id = 1; を実行（まだ COMMIT はしていない）。' || CHR(10) || 'その直後、メンバーBが UPDATE customer SET nickname = ''イヌ'' WHERE user_id = 1; を実行しようとした。' || CHR(10) || 'このとき、メンバーBの画面（ツール）はどうなりますか？');
INSERT INTO question VALUES (45, 4, 3, 'customer テーブルの birthday（生年月日）列には、検索速度を上げるための「インデックス」が作成されています。しかし、書き方によってはインデックスが無視され、全行検索（フルスキャン）になってしまいます。インデックスが正しく使われる（最も高速に処理できる）SQLはどれですか？');
INSERT INTO question VALUES (46, 4, 3, '大量のデータ（数百万件）が入っている answer テーブルから、特定の条件に合うデータを抽出します。一般的に、サブクエリ側のデータ量が膨大であるとき、IN を使うよりも EXISTS を使った方が劇的に処理が早くなる理由として、正しいものはどれですか？');
INSERT INTO question VALUES (47, 4, 3, '10,000件の解答データがある answer テーブルがあります。このテーブルを、GROUP BY user_id, question_id のように2つの列を指定してグループ化しました。このSQLを実行した結果、取得できる行数（レコード数）は最大で何行になりますか？');
INSERT INTO question VALUES (48, 4, 3, '組織図やカテゴリの親子関係（木構造データ）を1つのSQLで辿りたいとき、Oracle Databaseで伝統的に使われる階層問い合わせ専用の構文（キーワード）はどれですか？');
INSERT INTO question VALUES (49, 4, 3, '2つのトランザクションが、お互いに相手がロックしているデータの解放を待ち続けてしまい、処理が永久に止まってしまう現象を「デッドロック」と言います。デッドロックが発生してしまう原因として、正しいものはどれですか？');
INSERT INTO question VALUES (50, 4, 3, 'データベース内部の機能（オプティマイザ）が、2つの大きなテーブルを結合する際、片方のテーブルをメモリ上に展開してハッシュテーブル（インデックスのようなもの）を作り、もう片方と高速に突き合わせる方式を何と呼びますか？');
INSERT INTO question VALUES (51, 4, 3, 'GROUP BY を書かずに、いきなり SELECT COUNT(*) FROM answer HAVING COUNT(*) > 5; と書きました。このSQLを実行するとどうなりますか？');
INSERT INTO question VALUES (52, 4, 3, 'テストの点数を高い順に並べ替えて順位をつけます。1位の人が2人（同点）いた場合、その次の人の順位は、RANK() では「3位」、DENSE_RANK() では「2位」になります。では、1位が3人いた場合の DENSE_RANK() での次の人の順位は何位ですか？');
INSERT INTO question VALUES (53, 4, 3, 'テーブルA、テーブルB、テーブルCの3つを順に LEFT JOIN します。' || CHR(10) || 'FROM A LEFT JOIN B ON A.id = B.id LEFT JOIN C ON B.id = C.id' || CHR(10) || 'このとき、テーブルBにデータが存在せずNULLになった行は、続くテーブルCとの結合（B.id = C.id）においてどのように扱われますか？');
INSERT INTO question VALUES (54, 4, 3, '大量のテストデータが入ったテーブルを空っぽにしたいです。DELETE FROM answer; ではなく、TRUNCATE TABLE answer; を使う最大のメリットと特徴はどれですか？');


-- ====================================================================
-- 選択肢と正解の登録 (question_option table)
-- correct_option: 1 = A, 2 = B, 3 = C
-- ====================================================================

-- --- Java 選択肢 (1?9) ---
INSERT INTO question_option VALUES (1, 1, 'double', 'int', 'char', 2);
INSERT INTO question_option VALUES (2, 2, 'for', 'case', 'break', 1);
INSERT INTO question_option VALUES (3, 3, '# コメント', '// コメント', '-- コメント', 2);
INSERT INTO question_option VALUES (4, 4, '5 と 7', '7 と 5', 'エラーになる', 2);
INSERT INTO question_option VALUES (5, 5, '10', '20', '30', 3);
INSERT INTO question_option VALUES (6, 6, 'メソッド名だけ同じであればよい', 'メソッド名・引数・戻り値の型がすべて同じであること', '戻り値の型が異なっていてもよい', 2);
INSERT INTO question_option VALUES (7, 7, '11', '12', '13', 3);
INSERT INTO question_option VALUES (8, 8, '1', '3', '4', 2);
INSERT INTO question_option VALUES (9, 9, 'インターフェースのメソッドはすべてprivateである', 'インターフェースは多重実装が可能である', 'インターフェースはインスタンス化できる', 2);

-- ---  Spring 選択肢 (10?18) ---

-- 選択肢の列 A, B, C をすべて 300文字 に拡張する
ALTER TABLE question_option MODIFY option_a VARCHAR2(300);
ALTER TABLE question_option MODIFY option_b VARCHAR2(300);
ALTER TABLE question_option MODIFY option_c VARCHAR2(300);

INSERT INTO question_option VALUES (10, 10, '@Controller', '@Repository', '@Autowired', 1); -- ※提示データの正解に合わせてA(@Controller)を1に設定
INSERT INTO question_option VALUES (11, 11, 'Model', 'Scanner', 'StringBuilder', 1);
INSERT INTO question_option VALUES (12, 12, 'DBに従業員名を保存している', '画面で使えるように empName という名前で値を渡している', '従業員名を暗号化している', 2);
INSERT INTO question_option VALUES (13, 13, '<p th:text="${empName}"></p>', '<p th:href="${empName}"></p>', '<p th:each="${empName}"></p>', 1);
INSERT INTO question_option VALUES (14, 14, '@Valid が付いた引数の直後', 'モデルModel の前ならどこでもよい', '@RequestMapping の前', 1);
INSERT INTO question_option VALUES (15, 15, 'list.html を直接表示する', '/list に再度リクエストを送る', 'Modelの中身をすべて保存する', 2);
INSERT INTO question_option VALUES (16, 16, '渡しているidがnullだから', 'HTMLファイルが存在しないから', '@Controllerがないから', 1);
INSERT INTO question_option VALUES (17, 17, 'Entityは必ずSessionに保存できないから', '不要な情報やDB管理用の情報まで持ち回らないようにするため', 'ThymeleafではEntityを表示できないため', 2);
INSERT INTO question_option VALUES (18, 18, 'getAttribute() の戻り値が Object 型だから', 'EmployeeBean はEntityではないから', 'セッションには文字列しか保存できないから', 1);

-- --- HTML 選択肢 (19?27) ---
INSERT INTO question_option VALUES (19, 19, '<head>', '<title>', '<h1>', 2);
INSERT INTO question_option VALUES (20, 20, '<image>', '<img>', '<pic>', 2);
INSERT INTO question_option VALUES (21, 21, '<ul>' || CHR(10) || '  <li>項目</li>' || CHR(10) || '</ul>', '<li>' || CHR(10) || '  <ul>項目</ul>' || CHR(10) || '</li>', '<ul>項目</ul>', 1);
INSERT INTO question_option VALUES (22, 22, '<h2>好きな食べ物</h2>' || CHR(10) || '<ul>' || CHR(10) || '  <li>ラーメン</li>' || CHR(10) || '  <li>カレー</li>' || CHR(10) || '  <li>寿司</li>' || CHR(10) || '</ul>', '<h1>好きな食べ物</h1>' || CHR(10) || '<ol>' || CHR(10) || '  <li>ラーメン</li>' || CHR(10) || '  <li>カレー</li>' || CHR(10) || '  <li>寿司</li>' || CHR(10) || '</ol>', '<h2>好きな食べ物</h2>' || CHR(10) || '<ul>' || CHR(10) || '  <p>ラーメン</p>' || CHR(10) || '  <p>カレー</p>' || CHR(10) || '  <p>寿司</p>' || CHR(10) || '</ul>', 1);
INSERT INTO question_option VALUES (23, 23, '<h2>朝のルーティン</h2>' || CHR(10) || '<ul>' || CHR(10) || '  <li>起きる</li>' || CHR(10) || '  <li>顔を洗う</li>' || CHR(10) || '  <li>朝ごはんを食べる</li>' || CHR(10) || '</ul>', '<h2>朝のルーティン</h2>' || CHR(10) || '<ol>' || CHR(10) || '  <p>起きる</p>' || CHR(10) || '  <p>顔を洗う</p>' || CHR(10) || '  <p>朝ごはんを食べる</p>' || CHR(10) || '</ol>', '<h2>朝のルーティン</h2>' || CHR(10) || '<ol>' || CHR(10) || '  <li>起きる</li>' || CHR(10) || '  <li>顔を洗う</li>' || CHR(10) || '  <li>朝ごはんを食べる</li>' || CHR(10) || '</ol>', 3);
INSERT INTO question_option VALUES (24, 24, '<link href="https://www.google.com">' || CHR(10) || 'Googleで調べる' || CHR(10) || '</link>', '<a href="https://www.google.com">Googleで調べる</a>', '<a src="https://www.google.com">Googleで調べる</a>', 2);
INSERT INTO question_option VALUES (25, 25, '<table><tr><td>名前</td><td>年齢</td></tr><th><td>【名前】</td><td>15歳</td></th></table>', '<table><th>名前</th><th>年齢</th><td>【名前】</td><td>15歳</td></table>', '<table>' || CHR(10) || '  <tr><th>名前</th><th>年齢</th></tr>' || CHR(10) || '  <tr><td>【名前】</td><td>15歳</td></tr>' || CHR(10) || '</table>', 3);
INSERT INTO question_option VALUES (26, 26, '<form>' || CHR(10) || '  <label for="name">名前を入力してください</label>' || CHR(10) || '  <input type="text" id="name" name="name">' || CHR(10) || '  <button type="submit">送信</button>' || CHR(10) || '</form>', '<form><label>名前を入力してください</label><input type="button" id="name"><button>送信</button></form>', '<form><p for="name">名前を入力してください</p><text id="name"></text><submit>送信</submit></form>', 1);
INSERT INTO question_option VALUES (27, 27, '<html><head><h1>私のページ</h1></head><body>はじめてのHTMLページです。</body></html>', '<!DOCTYPE html>' || CHR(10) || '<html lang="ja">' || CHR(10) || '  <head>' || CHR(10) || '    <meta charset="UTF-8">' || CHR(10) || '    <title>私のページ</title>' || CHR(10) || '  </head>' || CHR(10) || '  <body>' || CHR(10) || '    <p>はじめてのHTMLページです。</p>' || CHR(10) || '  </body>' || CHR(10) || '</html>', '<!DOCTYPE html><html lang="ja"><head><meta charset="UTF-8"></head><body><title>私のページ</title><p>はじめてのHTMLページです。</p></body></html>', 2);

-- --- SQL 選択肢 (28?54) ---
INSERT INTO question_option VALUES (28, 28, 'GROUP BY クラス名, 教科名 ORDER BY クラス名, AVG（点数）', 'GROUP BY クラス名, 教科名 ORDER BY クラス名, 教科名', 'GROUP BY クラス名, 教科名, 学生番号 ORDER BY クラス名, 教科名, 平均点', 2);
INSERT INTO question_option VALUES (29, 29, '%UNIX', '%UNIX%', 'UNIX%', 2);
INSERT INTO question_option VALUES (30, 30, '科目 HAVING AVG(点数) >=80', '科目 WHERE 点数 >=80', '学生番号 HAVING AVG(点数) >=80', 3);
INSERT INTO question_option VALUES (31, 31, 'CURSOR', 'ORDER BY', 'UNION', 1);
INSERT INTO question_option VALUES (32, 32, 'CONNECT文で接続を許可する。', 'CREATE ASSERTION文で表明して制限する。', 'GRANT文で許可する。', 3);
INSERT INTO question_option VALUES (33, 33, 'パスワードを設定してデータベースへの接続を制限する。', 'ビューを作成して、ビューの基となる表のアクセスできる行と列を制限する。', '表の利用者に対し、表への問い合わせ、更新、追加、削除などの操作権限を付与する。', 3);
INSERT INTO question_option VALUES (34, 34, 'SELECT 注文日,AVG(数量) FROM 注文明細', 'SELECT 注文日,AVG(数量) FROM 注文明細 GROUP BY 注文日', 'SELECT 注文日,AVG(SUM(数量)) FROM 注文明細 GROUP BY 注文日', 2);
INSERT INTO question_option VALUES (35, 35, 'SELECT user_id FROM answer WHERE is_correct = 1 AND COUNT(*) >= 2 GROUP BY user_id;', 'SELECT user_id FROM answer WHERE is_correct = 1 GROUP BY user_id HAVING COUNT(*) >= 2;', 'SELECT user_id FROM answer GROUP BY user_id HAVING is_correct = 1 AND COUNT(*) >= 2;', 2);
INSERT INTO question_option VALUES (36, 36, 'SELECT * FROM customer c LEFT JOIN answer a ON c.user_id = a.user_id WHERE a.question_id = 1;', 'SELECT * FROM customer c LEFT JOIN answer a ON c.user_id = a.user_id AND a.question_id = 1;', 'SELECT * FROM customer c LEFT JOIN answer a ON c.user_id = a.user_id WHERE a.question_id IS NOT NULL;', 2);
INSERT INTO question_option VALUES (37, 37, '10', '7', '3', 2);
INSERT INTO question_option VALUES (38, 38, 'NULLの行だけを除外して、正しく未解答の顧客が取得できる。', 'エラー（ORA-00936など）が発生して実行できない。', '結果が「0件（何も返ってこない）」になってしまう。', 3);
INSERT INTO question_option VALUES (39, 39, 'SELECT last_name + '' '' + first_name FROM customer;', 'SELECT last_name || '' '' || first_name FROM customer;', 'SELECT CONCAT(last_name, '' '', first_name) FROM customer;', 2);
INSERT INTO question_option VALUES (40, 40, 'ORDER BY score DESC NULLS LAST', 'ORDER BY score DESC NULLS FIRST', 'ORDER BY score DESC WHERE score IS NOT NULL', 1);
INSERT INTO question_option VALUES (41, 41, 'SELECT * FROM question WHERE question_text LIKE ''%%%'';', 'SELECT * FROM question WHERE question_text LIKE ''%%%'';', 'SELECT * FROM question WHERE question_text LIKE ''%#%'' ESCAPE ''#'';', 3);
INSERT INTO question_option VALUES (42, 42, 'スカラー・サブクエリ', '相関サブクエリ（相関副問合せ）', '表サブクエリ', 2);
INSERT INTO question_option VALUES (43, 43, 'ROW_NUMBER() OVER (ORDER BY ans_time PARTITION BY user_id)', 'ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY ans_time)', 'RANK() OVER (GROUP BY user_id ORDER BY ans_time)', 2);
INSERT INTO question_option VALUES (44, 44, 'エラーが表示されて、即座に処理が失敗する。', 'メンバーAの変更を上書きして、即座に「イヌ」に更新される。', 'メンバーAが COMMIT または ROLLBACK するまで、処理が一時停止（待機状態）になる。', 3);
INSERT INTO question_option VALUES (45, 45, 'SELECT * FROM customer WHERE TO_CHAR(birthday, ''YYYY'') = ''2000'';', 'SELECT * FROM customer WHERE birthday >= TO_DATE(''2000-01-01'', ''YYYY-MM-DD'') AND birthday <= TO_DATE(''2000-12-31'', ''YYYY-MM-DD'';', 'SELECT * FROM customer WHERE birthday + 1 > TO_DATE(''2000-01-01'', ''YYYY-MM-DD'';', 2);
INSERT INTO question_option VALUES (46, 46, 'EXISTS は、条件にマッチする行を「1件」見つけた時点でその探索を終了（打ち切り）するから。', 'EXISTS を使うと、データベースが自動的に一時テーブルを作成してメモリ上で処理するから。', 'IN は必ずインデックスを無視してフルスキャンを行う仕様になっているから。', 1);
INSERT INTO question_option VALUES (47, 47, 'user_id の種類の数（例えばユーザーが50人なら50行）', 'question_id の種類の数（例えば問題が20問なら20行）', 'user_id と question_id の「ユニークな組み合わせ」の数（最大10,000行）', 3);
INSERT INTO question_option VALUES (48, 48, 'START WITH ... CONNECT BY PRIOR ...', 'RECURSIVE LOOP ... UNTIL ...', 'MERGE INTO ... USING ...', 1);
INSERT INTO question_option VALUES (49, 49, '2人のメンバーが、全く同じ順番で同じ複数の行を更新したとき。', '1人のメンバーが、大量のデータを一度に UPDATE してコミットし忘れたとき。', '2人のメンバーが、それぞれ異なる順序で同じ複数の行を更新しようとしたとき。', 3);
INSERT INTO question_option VALUES (50, 50, 'ネステッドループ結合（Nested Loop Join）', 'ソートマージ結合（Sort Merge Join）', 'ハッシュ結合（Hash Join）', 3);
INSERT INTO question_option VALUES (51, 51, 'エラー（GROUP BYがないため実行不可）になる。', 'テーブル全体の行数が5件より多ければ「全体の行数」が1行だけ返り、5件以下なら「0件（空っぽ）」が返る。', 'エラーにはならないが、常に何も返ってこない（0件になる）。', 2);
INSERT INTO question_option VALUES (52, 52, '4位', '3位', '2位', 3);
INSERT INTO question_option VALUES (53, 53, 'B.id がNULLの行は自動的に無視され、テーブルCとは一切結合されない（結果から消える）。', 'B.id のNULLと C.id のNULLが「一致する」と判定され、テーブルCのデータと結合される。', 'B.id がNULLの行も残るが、C.id とは結合できないため、Cの列はすべてNULLとして結果に出力される。', 3);
INSERT INTO question_option VALUES (54, 54, 'TRUNCATE はデータを1行ずつ確認しながら消すため、安全性が高い。', 'TRUNCATE はDDL（データ定義言語）であり、ROLLBACK（取り消し）ができない代わりに、一瞬で超高速にデータを全消去できる。', 'TRUNCATE を使うと、外部キー制約（FOREIGN KEY）で繋がっている親テーブルのデータも連動して全消去してくれる。', 2);

commit;