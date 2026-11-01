# 問題追加・全問点検結果

点検日: 2026-11-02

後続更新: 全テーブルのIDを連番化し、参照IDと次回採番を同期した。以下の過去のテスト結果は連番化前の実行記録であり、今回の変更後はユーザー指定によりテスト未実行。点検表と問題テスト内の対象IDは新IDに追従させた。

**配置前の最終確認（同日・後続依頼）:** 連番化後・実績ID6＝レベル25／ID7＝レベル50・ID7アイコン「⚔️」の現行状態で、全テストとWAR生成を `.\mvnw.cmd clean verify` により実施。602件中601件成功、失敗0、エラー0、任意起動プレビュー1件スキップ、終了コード0。問題113問の検証119件と最新DBの検証5件を含む。全15テーブルの連番・採番開始値・制約と参照整合性をH2 Oracleモードで確認済み。上の「テスト未実行」は最終確認前の経緯であり、実Oracleへの適用は引き続き未実施。

対象は [現状最新.sql](./現状最新.sql) のQUESTION初期データ。**既存68問と追加45問の計113問**について、問題文、3選択肢、正答番号、解説を点検した。問題は新規に作成し、Web上の問題文や解説を転載していない。

## 件数と難易度

| カテゴリ | 既存 | 追加（難易度1/2/3） | 反映後（難易度1/2/3） | 合計 |
|---|---:|---|---|---:|
| Java | 9 | 3 / 3 / 3 | 6 / 6 / 6 | 18 |
| Spring | 9 | 3 / 3 / 3 | 6 / 6 / 6 | 18 |
| HTML | 9 | 3 / 3 / 3 | 6 / 6 / 6 | 18 |
| SQL | 26 | 3 / 3 / 3 | 9 / 13 / 13 | 35 |
| 基本情報 | 15 | 3 / 3 / 3 | 8 / 8 / 8 | 24 |
| 合計 | 68 | 15 / 15 / 15 | 35 / 39 / 39 | 113 |

- 追加時は新規ID81～125を使用。後の全テーブル連番化で既存問題は1～68、追加問題は69～113に変更し、SEQ_QUESTIONは114から開始する。正答番号・問題内容は保持。本書の点検表・変更一覧は連番化後のIDに更新済み。
- 難易度1は用語・基本構文、2は短いコードや具体例の理解、3は既存問題と同程度の制御フロー、MVCのエラー処理、結合・集計の注意点、計算と前提条件の理解を中心とした。高度なフレームワーク内部仕様や難解なアルゴリズムは追加しない。
- SQLはOracle固有の仕様と標準的なSQLの仕様を区別。練習用の列を使う場合は問題文で前提を示した。
- Javaは17、Springのトランザクションの既定値はこのアプリに対応するFramework 6.0を基準にした。

## 点検方法と参照資料

単一の記事の記述だけでは判断せず、仕様・公式資料と別サイトの説明を照合した。資料が異なる定義や概算を用いる場合は、問題に定義・対象バージョン・数え方を明示した。計算問題は再計算し、実行できるコード・SQLの重要な例は自動テストでも確認した。

表の資料キーは以下の資料群を示す。各群は複数サイトを含む。外部へ送信したのは一般的な技術仕様の検索語のみで、SQLファイル、顧客情報、資格情報は送信していない。

| キー | 参照した資料・照合範囲 |
|---|---|
| J | [Java SE 17 JLS（型）](https://docs.oracle.com/javase/specs/jls/se17/html/jls-4.html)、[クラス・オーバーライド](https://docs.oracle.com/javase/specs/jls/se17/html/jls-8.html)、[式・評価順序](https://docs.oracle.com/javase/specs/jls/se17/html/jls-15.html)、[文・例外処理](https://docs.oracle.com/javase/specs/jls/se17/html/jls-14.html)、[Baeldung（共変戻り値）](https://www.baeldung.com/java-covariant-return-type)、[GeeksforGeeks（型）](https://www.geeksforgeeks.org/data-types-in-java/)、[InfoQ（インターフェースのprivateメソッド）](https://www.infoq.com/news/2017/09/java9-private-interface-methods/) |
| S | [Spring MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html)、[Springのリダイレクト属性](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/redirecting-passing-data.html)、[Springのトランザクション](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)、[Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/)、[Thymeleaf](https://www.thymeleaf.org/documentation.html)、[Jakarta Servlet](https://jakarta.ee/specifications/servlet/)、[Baeldung（リダイレクト）](https://www.baeldung.com/spring-redirect-and-forward)、[RequestParam](https://www.baeldung.com/spring-request-param)、[PathVariable](https://www.baeldung.com/spring-pathvariable)、[トランザクション](https://www.baeldung.com/spring-transactional-propagation-isolation) |
| H | [WHATWG（リスト・段落）](https://html.spec.whatwg.org/multipage/grouping-content.html)、[フォーム](https://html.spec.whatwg.org/multipage/forms.html)、[フォームコントロール](https://html.spec.whatwg.org/multipage/form-control-infrastructure.html)、[表](https://html.spec.whatwg.org/multipage/tables.html)、[MDN（リスト）](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/ul)、[ラベル](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/label)、[ボタン](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/button)、[画像](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/img)、[readonly](https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes/readonly) |
| D | [Oracle SQL Reference](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/)、[Oracle集計関数](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Aggregate-Functions.html)、[LIKE・ESCAPE](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Pattern-matching-Conditions.html)、[PostgreSQL集計関数](https://www.postgresql.org/docs/current/functions-aggregate.html)、[副問合せ・NULL](https://www.postgresql.org/docs/current/functions-subquery.html) |
| O | Oracle固有仕様はDのOracle資料と、[OracleTutorial（DENSE_RANK）](https://www.oracletutorial.com/oracle-analytic-functions/oracle-dense_rank/)、[TRUNCATE](https://www.oracletutorial.com/oracle-basics/oracle-truncate-table/)、[Use The Index, Luke（DATEの範囲検索）](https://use-the-index-luke.com/sql/where-clause/obfuscation/dates)を照合。他DBMSのTRUNCATEのロールバック可否をOracleへ一般化しない |
| F | [Stanford（ビット・バイト）](https://web.stanford.edu/class/cs101/bits-bytes.html)、[ハードウェア](https://web.stanford.edu/class/cs101/hardware-1.html)、[Wikipedia（RAM）](https://en.wikipedia.org/wiki/Random-access_memory)、[可逆圧縮](https://en.wikipedia.org/wiki/Lossless_compression)、[2の補数](https://en.wikipedia.org/wiki/Two%27s_complement)、[Stack](https://en.wikipedia.org/wiki/Stack_(abstract_data_type))、[Queue](https://en.wikipedia.org/wiki/Queue_(abstract_data_type))、大学のデータ構造教材を含む公開資料検索で照合 |
| B | [Wikipedia（二分探索）](https://en.wikipedia.org/wiki/Binary_search)、[GeeksforGeeks（二分探索の計算量）](https://www.geeksforgeeks.org/dsa/complexity-analysis-of-binary-search/)。概算log2(n)と厳密な最悪回数を区別し、1,024件の全探索位置・不一致の境界を実装で検査 |
| A | [Wikipedia（稼働率）](https://en.wikipedia.org/wiki/Availability)、[CMUの稼働率教材](https://users.ece.cmu.edu/~koopman/des_s99/avail/)などの公開資料検索を照合。MTBFの定義差、直列・並列の違い、独立性を明示して再計算 |
| N | [RFC 1034（DNS）](https://www.rfc-editor.org/rfc/rfc1034.txt)、[RFC 950（サブネット）](https://www.rfc-editor.org/rfc/rfc950.txt)、[RFC 791（IP）](https://www.rfc-editor.org/rfc/rfc791)、[Cloudflare（DNS）](https://www.cloudflare.com/learning/dns/what-is-dns/)、[サブネット](https://www.cloudflare.com/learning/network-layer/what-is-a-subnet/) |
| X | [NIST（認証要素）](https://pages.nist.gov/800-63-3/sp800-63b.html)、[Cloudflare（MFA）](https://www.cloudflare.com/learning/access-management/what-is-multi-factor-authentication/)、[OWASP（パラメータ化）](https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html)、[Oracle JDBC PreparedStatement](https://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html) |

## 既存問題の主な修正

- ID1: charも整数型なので、問う条件を「32ビットの符号付き整数」に限定。
- ID6: 共変戻り値を認める仕様を説明し、オーバーライド可能な宣言を問う形に変更。
- ID9: Java 17ではprivate/default/staticメソッドもあることを明記。
- ID14: Stringを返すコードにreturn文を追加し、BindingResultの位置の説明を整理。
- ID15: Modelが絶対に引き継がれないという説明を撤回。一部属性がURLに現れる設定とフラッシュ属性を区別。
- ID20、21、27: HTMLとSVGのimage、li内の入れ子ul、文字コード指定手段の説明を修正。
- ID28: 重複していた「として」を削除。
- ID33: Oracleでは集計関数の入れ子が可能なことと、この選択肢が不適切な理由を区別。
- ID34、50: 不正なCOUNT()をCOUNT(*)へ修正。ID34の練習用is_correct列の前提を明記。
- ID35、37: NULL行を除くWHEREの条件と、NOT INのFALSE/UNKNOWNを正確に説明。
- ID40: LIKEの末尾ワイルドカードを追加し、重複選択肢も解消。先頭・中間・末尾の%を検索可能に。
- ID41、42、43: 論理的な副問合せ評価と実行計画の違い、同時刻の順序、Oracleのロック待ちの前提を明記。
- ID44: SQLの閉じ括弧不足を解消。年末の時刻付きDATEを取りこぼさない翌年初未満の条件に変更。「索引が必ず使われ最速」という断定を撤回。
- ID45: EXISTSが常にINより高速という前提を撤回。存在判定と実行計画への依存を問う形に変更。
- ID48、49、53: デッドロックの検出、ハッシュ結合のメモリ不足時の処理、OracleのTRUNCATEの対象を明確化。「永久停止」「一瞬」などの断定を解消。
- ID58: MTBFの定義を明示。
- ID59: 二分探索の最悪回数を10回から11回へ修正し、比較の数え方を定義。
- ID60、66: 文字化けした範囲・数式を修正。ID66は装置停止の独立性を明示。
- ID62: 可逆圧縮と非可逆圧縮を区別。

## 全113問の確認一覧

正答A/B/CはCORRECT_OPTIONの1/2/3に対応する。「確認」は既存の内容を維持、「修正」は問題文・選択肢・解説のいずれかを変更、「追加」は新規問題。記載する観点と資料群を使って、誤答選択肢との区別も確認した。

| ID | 難易度 | 正答 | 確認観点 | 資料 | 対応 |
|---:|---:|:---:|---|---|---|
| 1 | 1 | B | intのビット数とcharの分類 | J | 修正 |
| 2 | 1 | A | forとcase/breakの役割 | J | 確認 |
| 3 | 1 | B | Javaのコメント構文 | J | 確認 |
| 4 | 2 | B | ブロックスコープ、出力7・5 | J・実行 | 確認 |
| 5 | 2 | C | ifでyを30へ代入 | J・実行 | 確認 |
| 6 | 2 | B | シグネチャ、アクセス、共変戻り値 | J | 修正 |
| 7 | 3 | C | 評価順序、前置・後置、出力13 | J・実行 | 確認 |
| 8 | 3 | B | 加算後にbreak、出力3 | J・実行 | 確認 |
| 9 | 3 | B | 複数インターフェースの実装 | J | 修正 |
| 10 | 1 | A | MVCの@Controller | S | 確認 |
| 11 | 1 | A | ビューへのModel | S | 確認 |
| 12 | 1 | B | 名前付きaddAttribute | S | 確認 |
| 13 | 2 | A | Thymeleafのth:text | S | 確認 |
| 14 | 2 | A | BindingResultの位置 | S | 修正 |
| 15 | 2 | B | redirectの再リクエストと属性 | S | 修正 |
| 16 | 3 | A | getReferenceByIdのnull ID | S | 確認 |
| 17 | 3 | B | セッションに保存する情報の限定 | S | 確認 |
| 18 | 3 | A | getAttributeの戻り値Object | S・J | 確認 |
| 19 | 1 | B | titleと本文のh1の区別 | H | 確認 |
| 20 | 1 | B | HTMLのimgとSVGのimage | H | 修正 |
| 21 | 1 | A | ul直下のliとリストの入れ子 | H | 修正 |
| 22 | 2 | A | h2・ul・li | H | 確認 |
| 23 | 2 | C | 番号付きol・li | H | 確認 |
| 24 | 2 | B | aとhref | H | 確認 |
| 25 | 3 | C | table・tr・th・tdの構造 | H | 確認 |
| 26 | 3 | A | label・input・submit | H | 確認 |
| 27 | 3 | B | head/title、body/p、DOCTYPE | H | 修正 |
| 28 | 1 | B | LIKEの前後の% | D | 修正 |
| 29 | 1 | C | 学生ごとのGROUP BYとHAVING | D | 確認 |
| 30 | 1 | A | 埋込みSQLのカーソル | D | 確認 |
| 31 | 1 | C | 更新権限とGRANT | D | 確認 |
| 32 | 1 | C | GRANTの権限付与 | D | 確認 |
| 33 | 1 | B | 通常列と集計、Oracleの入れ子集計 | D・O | 修正 |
| 34 | 2 | B | WHERE→GROUP BY→HAVING COUNT(*) | D・実行 | 修正 |
| 35 | 2 | B | LEFT JOINのONとNULL排除 | D | 修正 |
| 36 | 2 | B | COUNT(列)はNULLを除き7件 | D | 確認 |
| 37 | 2 | C | NOT INのNULLと0行 | D・実行 | 修正 |
| 38 | 2 | B | Oracleの文字列連結演算子 | D・O | 確認 |
| 39 | 2 | A | OracleのDESC NULLS LAST | D・O | 確認 |
| 40 | 2 | C | %をESCAPEして任意位置を検索 | D・実行 | 修正 |
| 41 | 2 | B | 相関副問合せと最適化 | D | 修正 |
| 42 | 2 | B | PARTITION BYとORDER BYの順 | D・O | 修正 |
| 43 | 2 | C | 同じ行の更新ロック待ち | D・O | 修正 |
| 44 | 3 | B | 索引向け範囲、年末23:59:59の包含 | D・O・実行 | 修正 |
| 45 | 3 | A | EXISTSの意味と性能の非保証 | D | 修正 |
| 46 | 3 | C | 複合キーの組合せ数の上限 | D | 確認 |
| 47 | 3 | A | Oracleの階層問い合わせ | D・O | 確認 |
| 48 | 3 | C | ロック待ちの循環と検出 | D・O | 修正 |
| 49 | 3 | C | ハッシュ構築と照合 | D・O | 修正 |
| 50 | 3 | B | GROUP BYなしのHAVINGと0行 | D・実行 | 修正 |
| 51 | 3 | C | DENSE_RANKは順位を飛ばさず2位 | D・O | 確認 |
| 52 | 3 | C | NULLの結合キーと2段LEFT JOIN | D | 確認 |
| 53 | 3 | B | Oracle永続表のTRUNCATE | D・O | 修正 |
| 54 | 1 | B | 1011=8+2+1=11 | F・再計算 | 確認 |
| 55 | 1 | B | 主記憶用の揮発性RAM | F | 確認 |
| 56 | 2 | B | ネットワーク層でIP経路選択 | N | 確認 |
| 57 | 2 | A | 正規化は冗長性・更新不整合を削減 | D | 確認 |
| 58 | 3 | B | 正常90時間、修復10時間で90% | A・再計算 | 修正 |
| 59 | 3 | A | 1,024件の正確な最悪11回 | B・実行 | 修正 |
| 60 | 1 | B | 8ビット、符号なし0から255 | F | 修正 |
| 61 | 1 | A | CPUは命令の解釈・実行 | F | 確認 |
| 62 | 1 | A | 完全復元には可逆圧縮 | F | 修正 |
| 63 | 2 | B | 16進1F=16+15=31 | F・再計算 | 確認 |
| 64 | 2 | A | スタックはLIFO | F | 確認 |
| 65 | 2 | B | 知識と生体は別の認証要素 | X | 確認 |
| 66 | 3 | B | 独立な並列装置で99% | A・実行 | 修正 |
| 67 | 3 | B | 8ビット2の補数11111111は-1 | F・再計算 | 確認 |
| 68 | 3 | A | SQL構造とバインド値を分離 | X | 確認 |
| 69 | 1 | A | booleanはtrue/false | J | 追加 |
| 70 | 1 | B | 配列の先頭は添字0 | J | 追加 |
| 71 | 1 | C | 初期化後のfinal変数は再代入不可 | J | 追加 |
| 72 | 2 | A | 整数除算7/2は3 | J・実行 | 追加 |
| 73 | 2 | B | String.equalsは内容比較 | J・実行 | 追加 |
| 74 | 2 | C | 拡張forで2+4+6=12 | J・実行 | 追加 |
| 75 | 3 | A | &&短絡評価で0除算を回避 | J・実行 | 追加 |
| 76 | 3 | B | try→catch→finallyでA・B・C | J・実行 | 追加 |
| 77 | 3 | C | インスタンスメソッドの動的選択 | J | 追加 |
| 78 | 1 | A | DIで依存先を外部から注入 | S | 追加 |
| 79 | 1 | B | サービス層の@Service | S | 追加 |
| 80 | 1 | C | GETの@GetMapping | S | 追加 |
| 81 | 2 | A | クエリの@RequestParam | S | 追加 |
| 82 | 2 | B | パスの@PathVariable | S | 追加 |
| 83 | 2 | C | @ResponseBodyの本文出力 | S | 追加 |
| 84 | 3 | A | エラー時に保存せず入力へ戻す | S | 追加 |
| 85 | 3 | B | URLに含めないフラッシュ属性 | S | 追加 |
| 86 | 3 | C | Framework 6.0既定のRuntimeException rollback | S | 追加 |
| 87 | 1 | A | 段落のp | H | 追加 |
| 88 | 1 | B | 改行のbr | H | 追加 |
| 89 | 1 | C | 画像の代替テキストalt | H | 追加 |
| 90 | 2 | A | labelのforとinputのid | H | 追加 |
| 91 | 2 | B | 同じフォームのradioのname | H | 追加 |
| 92 | 2 | C | GETフォームのクエリ文字列 | H | 追加 |
| 93 | 3 | A | disabled非送信、readonly送信 | H | 追加 |
| 94 | 3 | B | colspanは列方向 | H | 追加 |
| 95 | 3 | C | 送信しないtype=button | H | 追加 |
| 96 | 1 | A | DISTINCTで重複除去 | D | 追加 |
| 97 | 1 | B | DESCで降順 | D | 追加 |
| 98 | 1 | C | NULL判定はIS NULL | D | 追加 |
| 99 | 2 | A | SUMはNULLを除き30 | D・再計算 | 追加 |
| 100 | 2 | B | COALESCEの最初の非NULLは5 | D | 追加 |
| 101 | 2 | C | 具体例のINNER JOINは1行 | D | 追加 |
| 102 | 3 | A | LEFT JOINでCOUNT(右の主キー)は0 | D | 追加 |
| 103 | 3 | B | 一意キーを追加して同点順序を決定 | D・O | 追加 |
| 104 | 3 | C | UNION ALLは3+2=5行 | D・再計算 | 追加 |
| 105 | 1 | A | AND(1,0)=0 | F・再計算 | 追加 |
| 106 | 1 | B | 4KiB=4,096バイト | F・再計算 | 追加 |
| 107 | 1 | C | CPUと主記憶の速度差を補うキャッシュ | F | 追加 |
| 108 | 2 | A | FIFOでAを取り出す | F | 追加 |
| 109 | 2 | B | DNSの名前・アドレス等の問い合わせ | N | 追加 |
| 110 | 2 | C | 問題数で重み付けした24/30=80% | F・実行 | 追加 |
| 111 | 3 | A | /26はホスト6ビット、64-2=62 | N・実行 | 追加 |
| 112 | 3 | B | 8ビット2の補数は-128から127 | F・再計算 | 追加 |
| 113 | 3 | C | 独立な直列装置で0.9×0.8=72% | A・実行 | 追加 |

## 自動検証

[LatestQuestionSeedTest.java](../../src/test/java/jp/co/sss/pr/validation/LatestQuestionSeedTest.java) は既存のJUnit/Mavenテストに追加し、次で実行した。

```powershell
.\mvnw.cmd -Dtest=LatestQuestionSeedTest test
```

結果: **119件成功、失敗0、エラー0、スキップ0、終了コード0**。

- 全113件について、列構成、ID一意、カテゴリ、難易度、正答範囲、選択肢の非空・非重複、解説の存在、VARCHAR2の文字数上限を確認。
- 追加45問の各カテゴリ・難易度の件数を厳密に検査し、既存68問のID集合と採番開始値も検査。
- 点検表が全113問を重複なく含み、難易度・正答番号がSQLと一致することを検査。
- 全113件の実際のINSERTをH2 Oracleモードの制約付きQUESTIONへ読み込み。
- SQLの正答を使い、HAVINGの5件/6件境界、NULLを含むNOT IN、%の先頭/中間/末尾、年初・年末23:59:59・翌年初の範囲を検証。
- ID4、5、7、8、72から76のコードをSQLから取り出し、JDKでコンパイルして出力を確認。
- 二分探索は探索値-1から1,024まで実行して最大11回を確認。稼働率、サブネット、正答率も計算。

## 適用範囲・注意

### 実DBエクスポートとのマージ

共有された実DBエクスポートを[現状最新.sql](./現状最新.sql)にマージした。競合の採用方針はユーザー確認済み。添付原本は変更せず、実DBへの適用も行っていない。

| テーブル | マージ後の件数 | 採用内容 |
|---|---:|---|
| QUESTION | 113 | 点検済み既存68問と追加45問をすべて現状最新から維持 |
| CATEGORY | 5 | 双方一致 |
| CUSTOMER | 6 | エクスポートの全ユーザー・登録情報・学習状況・最終アクセス時刻を保持 |
| ANSWER | 189 | エクスポートの全解答履歴を保持（147件追加） |
| ACHIEVEMENT | 20 | 最終的に受領した正常な元SQLの絵文字に統一。ID15だけ新規指定「🌱」 |
| USER_ACHIEVEMENT | 14 | エクスポートを保持（10件追加） |
| FAVORITE | 2 | エクスポートから追加 |
| FRIEND_CONNECTION | 3 | エクスポートから追加 |

- CUSTOMERの全6ユーザーはLOGIN_FLAG=0、ACTIVE_LOGIN_TOKEN=NULLに初期化した。パスワードは元のBCryptハッシュを変更せず保持した。認証情報を含むため、このSQLの共有・バージョン管理時には注意が必要。
- マージ時の採番開始値はSEQ_ANSWER=241、SEQ_FAVORITE=61、SEQ_FRIEND_CONNECTION=41、SEQ_USER=101、SEQ_USER_ACHIEVEMENT=81をエクスポートから採用し、SEQ_QUESTION=126を維持した。後続の連番化で、それぞれ190、3、4、7、15、114に更新。SEQ_CATEGORYは6、データのない表の採番と未使用のSEQ_OPTIONは1から開始する。
- 接続先をPROSTA_USERに限定するガードと、全テーブル・全シーケンスの削除方式はユーザー指定で維持した。エクスポートと現状最新の再作成対象は同じ15テーブル・15シーケンスだが、実DBに別のテーブル・シーケンスがあれば、それらも削除されて復元されない。
- ブックマークのUQ_FAVORITE_USER_QUESTION（USER_ID・QUESTION_IDの一意制約）は現状最新を維持した。エクスポートにはこの制約がなかったが、取り込んだ2件に重複はない。
- 問題報告のREASON・STATUSのVARCHAR2長さ単位は、ユーザー指定で現状最新の未指定を維持した。エクスポートのBYTE明示は採用しなかった。
- 列順・引用符・TIMESTAMPの既定精度・制約に伴う索引の表現差を除き、他の列型・既定値・NOT NULL・主キー・一意制約・CHECK・外部キー・索引に意味のある差はなかった。物理ストレージ設定とDDL配置は現状最新を維持し、拡張表・採番の重複作成はない。
- 実績ICONについては当初「エクスポート側に絵文字がある」と誤って説明したが、原本の文字コードを再確認して双方とも「?」と訂正し、ユーザー確認の上で修正対象から除外した。

[LatestDatabaseSeedTest.java](../../src/test/java/jp/co/sss/pr/validation/LatestDatabaseSeedTest.java)と既存の問題テストを同時実行した。

```powershell
.\mvnw.cmd "-Dtest=LatestDatabaseSeedTest,LatestQuestionSeedTest" test
```

結果: **122件成功、失敗0、エラー0、スキップ0、終了コード0**。

- 15テーブルの論理DDLとマージ後の全データをH2 Oracleモードに読み込み、主キー・NOT NULL・一意制約・CHECK・外部キーを検証。
- 件数、各採番が投入済み最大IDより大きいこと、問題採番126、全ユーザーのログアウト状態、BCryptハッシュ保持を検証。
- ブックマークの重複・存在しないユーザー/問題への参照・不正な予定出題数が拒否されることを検証。
- 既存解答履歴の正答番号と修正後問題の正答番号が全件一致することを検証。
- 添付とのローカル列単位比較で、承認済み問題内容・ログイン状態以外の全エクスポート値の保持も確認した。
- Oracle固有の物理DDL・PL/SQL削除処理はH2検証対象外。実Oracle上での再構築成功を確認したものではない。

### 実績アイコンとサーバー投入時の文字コード

SQL*Plusの具体的な起動設定・投入前確認・実行・投入後検証は[SQLPlus UTF-8投入手順](../configuration/SQLPlus_UTF8投入手順.md)を参照。

**最終更新:** ユーザーが提示したデスクトップ上の元SQLをバイト単位で確認したところ、正常なUTF-8で置換文字は0件、全20件の実績絵文字が正常に含まれていた。元ファイルを変更せず、ID15を除く19件のICONを元SQLに合わせて反映した。ID15は依頼に基づく「🌱」を維持した（元SQLでは「🧩」）。以前の貼り付け添付で見られた「?」や文字化けは、この元SQLの状態を表していない。元SQLをMS932として読み込まず、UTF-8として開くこと。下記の「原本未受領」「残り19件は?」の記述は元SQLの提示前の経緯である。

後続依頼によりID15「伸びしろ発見」のICONだけを「🌱」（U+1F331）に変更した。ほか19件はエクスポートと同じ「?」。受領した貼り付け添付はUTF-8として正常に復号でき、全20件の元ICONはU+003Fだった。元のMS932ファイル自体は未受領であり、貼り付け前のファイルの文字コードや、元DB内での値は直接確認していない。

- MS932（Windows-31J/CP932）はSQLファイルの保存文字コードであり、Oracle DBの文字セット名ではない。Oracle全環境共通の「デフォルト」とは判断できない。エクスポートツール・バージョン・環境の設定を確認する必要がある。
- MS932は「🌱」を表現できない。MS932への保存・変換時に拒否または「?」への置換が起こり得る。一度「?」として書き出された絵文字は、UTF-8で開き直しても復元できない。ほかの実績の絵文字を復元する場合は、元DBからUTF-8で再エクスポートする必要がある。
- [現状最新.sql](./現状最新.sql)はUTF-8のまま保存・読込みする。サーバーがWindowsでもLinuxでも、OSの既定値ではなく実行クライアントの読込み設定を合わせる。
- ICONはVARCHAR2(10 CHAR)なので、保存できる文字はDBのNLS_CHARACTERSETに依存する。CHAR指定は長さ単位であり、Unicode対応を意味しない。AL32UTF8なら「🌱」を表現できるが、JA16SJIS等ではUTF-8入力でも保存できない。NLS_NCHAR_CHARACTERSETがUnicodeでも、VARCHAR2列の制限は解消しない。
- SQL Developer等でスクリプトを開く場合は読込み文字コードをUTF-8に設定する。SQL*PlusでUTF-8ファイルを実行する場合は、起動前にクライアントのNLS_LANGの文字セット部分をAL32UTF8へ設定する。これはクライアントから送るバイトの宣言であり、DB文字セットを変更する設定ではない。JDBC ThinはNLS_LANGを使用しないため、SQL Developer/JDBCの設定とSQL*Plusの設定を混同しない。
- この調査では実DBへ接続していない。サーバー投入可否の最終判断は、実際の接続先PDBで下記を確認してから行う。DB文字セットの変更や列型変更は今回行っていない。

```sql
SELECT parameter, value
FROM nls_database_parameters
WHERE parameter IN ('NLS_CHARACTERSET', 'NLS_NCHAR_CHARACTERSET');

SELECT achievement_id, icon, ASCIISTR(icon) AS icon_code
FROM achievement
ORDER BY achievement_id;
```

ID15の投入後はASCIISTR(ICON)が`\D83C\DF31`となることを確認する。MS932入力は「そのファイル内の全文字を表現できる」場合に限り適切なクライアント設定で使えるが、今回の絵文字付きSQLをMS932化して投入してはいけない。

変更後は上記Mavenコマンドで**123件成功、失敗0、エラー0、スキップ0、終了コード0**。ID15だけが指定絵文字であること、残り19件が元データと一致すること、UTF-8往復で文字を保持できること、JavaのMS932エンコーダーでは表現できないことを検証した。H2への実際のINSERTでも絵文字を保持した。

資料:

- [Oracle: Choosing a Character Set](https://docs.oracle.com/en/database/oracle/oracle-database/19/nlspg/choosing-character-set.html)（DB文字セットと文字長単位）
- [Oracle: NLS_LANG FAQ](https://www.oracle.com/apac/database/technologies/faq-nls-lang.html)（クライアント文字セット、JDBCとの違い）
- [Oracle: UNISTR](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/UNISTR.html)（補助文字のサロゲート表現。UNISTRを使ってもVARCHAR2保存時のDB文字セット制限は残る）

### SQLファイルの整理

2026-11-02にリポジトリ内のSQLを31本から16本に整理した後、ユーザー指定で増分SQL15本を削除し、現状最新の1本に統一した。アプリ・テストから旧SQLを読み込む参照はない。

- 再構築・点検済み問題データの正本: [現状最新.sql](./現状最新.sql)。
- 廃止した増分SQL: 学習機能のまとめ更新1本と古いDBの補完14本。機能の必要構造は現状最新へ統合済み。直前バックアップ後に再構築する運用へ統一したため、migrationフォルダを削除した。
- 削除: 旧再構築DDL・旧54問データなど4本、社員管理サンプル2本、現行定義と重複・不一致がある旧スキーマ改善DDL1本、点検前の問題・解説を持つV20261031～V20261033の3本、まとめ更新SQLと重複する個別5本。
- バックアップ取得だけでは再構築SQLの固定データは更新されない。エクスポート後に増えたユーザー・履歴等を維持するには、再構築前に直前バックアップとのマージか別途復元手順が必要。
- 古い問題投入SQLは廃止した。点検済み113問を既存DBへデータ保持のまま反映する増分SQLは、この整理では作成・実行していない。

- **SQLファイルへの反映のみ。実Oracle DBへの接続・実行・適用はしていない。**
- [現状最新.sql](./現状最新.sql) は既存表・シーケンスを削除する再構築専用SQLである。既存DBへ追加データだけを入れる用途で、このファイル全体を実行してはいけない。
- H2でのデータ読込み・SQL例の検証はOracle全DDLや実行計画の検証を代替しない。Oracle固有の挙動は資料照合で確認し、索引利用や処理時間を保証する設問にはしていない。
- 点検対象はSQLファイルの113問。実DBに管理画面から独自登録された問題があっても、この点検には含まれない。
- 資料照合と実行・再計算で判明した誤り・曖昧さは修正した。全環境・将来バージョンにおける無誤りを保証するものではない。
