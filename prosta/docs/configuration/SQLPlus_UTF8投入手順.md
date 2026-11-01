# SQL*PlusでUTF-8のSQLをOracleへ投入する手順

## 前提・重要な注意

- SQLファイルはUTF-8で保存し、転送時も文字コードを変換しない。UTF-8 BOMなしを推奨する。
- SQL*Plusを起動するマシンで、起動前に`NLS_LANG`の文字セット部分を`AL32UTF8`にする。WindowsからリモートDBへ接続する場合はWindows側、LinuxサーバーでSQL*Plusを動かす場合はそのLinux側の設定。
- `NLS_LANG`は入力バイトの文字コードをOracleクライアントへ伝える設定。DBの文字セットを変更するものではない。
- Oracleの`UTF8`は標準UTF-8と同一ではないため、今回のUTF-8ファイルには`AL32UTF8`を指定する。
- 絵文字はMS932（Windows-31J/CP932）では保存できない。誤った変換で「?」になった文字は、UTF-8で開き直しても復元できない。
- [現状最新.sql](../db/現状最新.sql)は再構築専用。PROSTA_USERの**全テーブル・全シーケンスを削除**して作り直す。記載対象以外の表・採番も削除される。絵文字だけを直す目的では全体を実行しない。
- 実行前に復元可能なバックアップを取得し、アプリを停止して書込みを防ぐ。Oracle DDLは暗黙コミットされるため、後からROLLBACKしても削除を取り消せない。
- SQLにはユーザー情報・パスワードハッシュが含まれる。転送先・権限・ログの共有範囲を制限する。

## 1. SQLファイルの文字コードを確認

VS Codeで元SQLを開き、右下の文字コード表示を確認する。

1. 文字化けしている場合は上書き保存しない。
2. 右下の文字コードを選択する。
3. 「エンコード付きで再度開く」からUTF-8を選ぶ。
4. 日本語と絵文字が正常に表示されることを確認する。
5. 必要なら「エンコード付きで保存」でUTF-8（BOMなし）を選ぶ。

「再度開く」は元のバイトを別の文字コードとして読み直す操作。「保存」は表示中の文字を変換して書き出す操作。文字化けした表示をUTF-8で保存すると、文字化けが固定される。

受領した元エクスポートは正常なUTF-8だった。現状最新の実績ID15は「🌱」、他の19件は元エクスポートの絵文字を採用済み。

## 2. Windows PowerShellから実行

以下はSQL*PlusがPATHにある場合。ない場合はインストール済みのsqlplus.exeの絶対パスを使用する。

```powershell
$env:NLS_LANG = 'AMERICAN_AMERICA.AL32UTF8'
$env:NLS_LANG
sqlplus /nolog
```

既にSQL*Plusを起動している場合は終了し、環境変数を設定した同じPowerShellから起動し直す。この設定はこのPowerShellとそこから起動する子プロセスに適用される。別のターミナルへは自動反映されない。

SQL*Plusで以下を実行する。接続先は実際のホスト・ポート・サービス名へ置き換える。パスワードはプロンプトで入力し、コマンドや資料に記載しない。

```sql
CONNECT PROSTA_USER@//DB_HOST:1521/SERVICE_NAME
```

Windowsのコマンドプロンプトを使用する場合は、SQL*Plusの起動前に次を実行する。

```bat
set NLS_LANG=AMERICAN_AMERICA.AL32UTF8
sqlplus /nolog
```

端末表示が崩れる場合はフォントや端末のコードページも確認する。`chcp 65001`だけではNLS_LANG設定の代わりにならない。端末の表示と、ファイル入力・DB保存は分けて確認する。

## 3. Linuxサーバー上で実行

SQLファイルをSFTP/SCP等で転送する。転送ツールの文字コード変換は無効にする。以下のパスは例であり、実際の配置先を使う。

```bash
export NLS_LANG=AMERICAN_AMERICA.AL32UTF8
printf '%s\n' "$NLS_LANG"
locale
sqlplus /nolog
```

端末は利用可能なUTF-8ロケールを使う。候補は`locale -a`で確認し、未導入のロケールを指定しない。SQLファイルをMS932へ変換する必要はない。

接続コマンドはWindowsと同じ。起動前の環境変数設定も、実行するシェルごとに必要。

## 4. 投入前に接続先とDB文字セットを確認

正しいPDB・サービスに接続できていることを確認する。

```sql
SHOW USER

SELECT SYS_CONTEXT('USERENV', 'DB_NAME') AS db_name,
       SYS_CONTEXT('USERENV', 'CON_NAME') AS container_name,
       SYS_CONTEXT('USERENV', 'SERVICE_NAME') AS service_name
FROM dual;

SELECT parameter, value
FROM nls_database_parameters
WHERE parameter IN ('NLS_CHARACTERSET', 'NLS_NCHAR_CHARACTERSET');
```

実績ICONはVARCHAR2なので、`NLS_CHARACTERSET`が重要。`AL32UTF8`なら今回の絵文字を保存できる。`NLS_NCHAR_CHARACTERSET=AL16UTF16`だけではVARCHAR2の絵文字保存を保証しない。

ユーザー提供の画面ではAL32UTF8 / AL16UTF16が確認された。ただし、別サーバーへ投入する場合はその接続先で再確認する。CHAR長さ指定もUnicode対応を意味しない。

## 5. エラー時に中断する設定と実行

バックアップ・接続先・アプリ停止・再構築の了承を確認してから実行する。

```sql
WHENEVER OSERROR EXIT FAILURE ROLLBACK
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
SET ECHO OFF
SET VERIFY OFF
SET DEFINE OFF
```

`WHENEVER SQLERROR`はSQL/PLSQLエラーで終了する。SQL*Plus自身の一部のSP2エラーには適用されないため、終了コードだけでなく出力も確認する。ROLLBACK指定でも既に実行されたDDLは戻らない。

Windowsでは次の実行例を使用する。

```sql
@"C:\Applepie_prosta\prosta\docs\db\現状最新.sql"
```

Linuxではサーバーへ配置した実際のパスを指定する。

```sql
@"/opt/prosta/import/latest.sql"
```

この例のLinuxパスにファイルを自動作成・転送する処理は含まれていない。日本語ファイル名の端末表示に問題がある場合は、内容を変換せず英字ファイル名で転送してもよい。

スプールを取得する場合は`SET ECHO OFF`を維持し、ログに機密データを出さない。確認中に`SELECT * FROM CUSTOMER`やパスワード・トークン列の出力をしない。

## 6. 投入後の確認

端末で絵文字が表示されるかだけでなく、ASCII表現で保存値を確認する。

```sql
SELECT achievement_id, ASCIISTR(icon) AS icon_code
FROM achievement
ORDER BY achievement_id;

SELECT COUNT(*) AS question_count FROM question;
SELECT COUNT(*) AS customer_count FROM customer;
SELECT COUNT(*) AS answer_count FROM answer;
SELECT COUNT(*) AS favorite_count FROM favorite;
SELECT COUNT(*) AS friend_connection_count FROM friend_connection;
SELECT COUNT(*) AS user_achievement_count FROM user_achievement;
```

このマージ版の期待件数:

| 対象 | 件数 |
|---|---:|
| QUESTION | 113 |
| CUSTOMER | 6 |
| ANSWER | 189 |
| FAVORITE | 2 |
| FRIEND_CONNECTION | 3 |
| USER_ACHIEVEMENT | 14 |
| ACHIEVEMENT | 20 |

実績ID15の`ASCIISTR(ICON)`は`\D83C\DF31`（🌱）が期待値。`?`なら保存値が壊れている。正しいコードなのに端末で四角や文字化けになる場合は、端末側の表示を確認する。アプリを起動後、ブラウザーの実績画面でも確認する。

確認後に終了する。

```sql
EXIT SUCCESS
```

PowerShellではSQL*Plus終了直後の`$LASTEXITCODE`、Linuxでは`echo $?`を確認する。エラー発生時は成功扱いにせず、ログと接続先・入力文字コードを調べる。途中失敗した再構築は不完全なDBを残し得るため、無条件でアプリを再開しない。

## 7. よくある誤解・問題

| 状況 | 対応 |
|---|---|
| 「DBがAL32UTF8だからMS932設定でもよい」 | ファイルの実際のバイトとNLS_LANGを合わせる。UTF-8ファイルにはAL32UTF8 |
| `ALTER SESSION`だけで入力をUTF-8にしたい | クライアント環境変数を起動前に設定する。DBセッションの言語・日付設定とは別 |
| SQL Developerで正常だがSQL*Plusで崩れる | SQL Developer/JDBC ThinはNLS_LANGを使わない。SQL*Plus側を別に設定 |
| MS932保存で絵文字が「?」になった | 元DBまたは正常なUTF-8原本から取得し直す。読込み変更だけでは復元不可 |
| UTF-8入力でもDBがJA16SJIS | VARCHAR2では絵文字を保存できない。DB/列設計変更は別途計画が必要 |
| SQLの実行に失敗した | DDLは戻らない。バックアップ復旧または再構築方針を確認 |

## 検証範囲・参考資料

リポジトリでは論理DDL・投入データ・全113問・絵文字の一致・MS932での表現不可をJUnit/H2で確認した。実サーバー上のSQL*Plus投入は実施していない。H2テストはOracle物理DDL・PL/SQLや実際のクライアント設定の検証を代替しない。

- [Oracle: Choosing a Character Set](https://docs.oracle.com/en/database/oracle/oracle-database/19/nlspg/choosing-character-set.html)
- [Oracle: NLS_LANG FAQ](https://www.oracle.com/apac/database/technologies/faq-nls-lang.html)
- [Oracle SQL*Plus: WHENEVER SQLERROR](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqpug/WHENEVER-SQLERROR.html)
- [Oracle: UNISTR](https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/UNISTR.html)
- [問題データ・マージ点検記録](../db/QUESTION_CONTENT_REVIEW.md)
