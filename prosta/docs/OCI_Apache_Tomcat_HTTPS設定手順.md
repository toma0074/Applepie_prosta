# PROSTA: Duck DNS + Certbot + Apache + Tomcat のHTTPS設定手順

## 1. この資料の前提と結論

2026-10-30に確認した構成:

- DNS: **Duck DNS**（`名前.duckdns.org`）。
- 証明書: **Let's Encrypt / Certbot**。取得済みとの申告あり。
- OCIの同一サーバーにApacheとTomcatがある。
- `product` はローカル検証用、`oracle` は本番用。
- Apacheの種類・OS・接続ポート・証明書の使用先・外部TomcatへのWAR配置かどうかは、まだ実サーバーで確認していない。

**推奨は、既存のApache HTTP ServerでHTTPSを終端し、同じサーバーのTomcatへHTTPで転送する方式。nginxやLBの追加は必須ではない。**

```text
利用者
  ↓ https://YOUR_NAME.duckdns.org/prosta/
Duck DNS → OCI VMのパブリックIP
  ↓ TCP 443
Apache HTTP Server（証明書を使ってTLS終端）
  ↓ HTTP、127.0.0.1のTomcatポートへ
Tomcat → PROSTA → Oracle DB
```

「証明書をサーバーへ置いた」だけではHTTPSにならない。Apacheの443リスナーから、その証明書と対応する秘密鍵を参照する必要がある。

この資料は設定手順であり、実サーバーへ設定済みであることを示すものではない。

### 表記と注意

- `YOUR_NAME.duckdns.org`、`CERT_NAME`、`8080`、サービス名は例。確認した実値へ置き換える。
- Linuxコマンドは**OCI VMへSSH接続した端末**で実行する。Windows PowerShell用は別途明記する。
- ローカルの `product` と共通のプロファイル選択は変更しない。
- 秘密鍵、Duck DNSトークン、DB接続情報をチャット・Git・スクリーンショットへ掲載しない。
- 他のアプリを同居させている場合は、既存VirtualHost・Connectorを一括置換しない。
- 本番変更はバックアップ後、利用者の少ない時間帯で行う。

## 2. 最初に「今どう動いているか」を確認する

### 2-1. OSとサービス

Linux:

```bash
cat /etc/os-release
command -v apachectl apache2ctl httpd certbot
systemctl list-units --type=service --all | grep -Ei 'apache|httpd|tomcat|prosta'
sudo ss -ltnp
```

目安:

| 確認結果 | 判断 |
|---|---|
| `httpd` が80/443で待受 | Oracle Linux / RHEL系でよくあるApache HTTP Server |
| `apache2` が80/443で待受 | Ubuntu / Debian系でよくあるApache HTTP Server |
| Javaが8080等で待受 | Tomcatの可能性。次の手順で外部Tomcatか組込みか区別 |
| Javaが7778で待受 | 組込みTomcatの可能性。ただし番号だけでは断定しない |
| 8009が待受 | AJP連携の可能性。HTTP転送の例をそのまま上書きしない |
| nginx等が443で待受 | Apacheより手前に別プロキシがある可能性。443を取り合わない |
| OSがWindows | 下記Linux手順をそのまま使わず、Windows分岐へ |

Apacheはプロジェクト名にも使われる。「Apache Tomcatしかない」場合と「Apache HTTP ServerとTomcatの両方がある」場合を区別する。

Windows Serverだった場合:

```powershell
Get-Service | Where-Object { $_.Name -match 'Apache|Tomcat' }
Get-NetTCPConnection -State Listen |
    Where-Object { $_.LocalPort -in 80,443,8080,7778,8009 } |
    Select-Object LocalAddress,LocalPort,OwningProcess
```

Apacheのサービス実行パス・設定ルートはサービスのプロパティや `httpd.exe -V` で確認する。後述のVirtualHostの考え方は共通だが、ファイルパス・サービス操作・証明書更新手段はWindows用に読み替える。CertbotがWSLや別ホストで動いているなら、証明書の自動配布・Apache再読込も別途必要。Linuxのsystemdコマンドは実行しない。

### 2-2. Apacheの有効設定

存在するコマンドを一つ使う:

```bash
sudo apachectl -S
sudo apachectl -M
sudo apachectl -V
```

Ubuntu等では `apache2ctl`、環境によっては `httpd` を使用する。

- `-S`: ホスト名ごとのVirtualHost、80/443、設定ファイル名を確認。
- `-M`: `ssl_module`、`proxy_module`、`proxy_http_module`、`headers_module`、`rewrite_module` を確認。
- `-V`: `HTTPD_ROOT`、`SERVER_CONFIG_FILE` を確認。
- 表示された設定とそのInclude先で `ProxyPass`、`ProxyPassReverse`、`JkMount`、`SSLCertificateFile`、`SSLCertificateKeyFile` を調べる。
- Certbot作成の `*-le-ssl.conf` が既に有効なら、別の443 VirtualHostを重複作成せず既存を編集する。

よくある場所:

| OS | 設定 |
|---|---|
| Oracle Linux / RHEL系 | `/etc/httpd/conf/httpd.conf`、`/etc/httpd/conf.d/` |
| Ubuntu / Debian系 | `/etc/apache2/apache2.conf`、`/etc/apache2/sites-available/`、`sites-enabled/` |

### 2-3. Tomcatとアプリの起動方式

見つかったサービス名で確認する:

```bash
sudo systemctl status TOMCAT_SERVICE --no-pager
sudo systemctl cat TOMCAT_SERVICE
```

**出力に環境変数の秘密情報が含まれる場合があるため、そのまま共有しない。**

- `org.apache.catalina.startup.Bootstrap`、`catalina.base`、`webapps/prosta.war` がある → 外部TomcatへのWAR配備。
- `java -jar ...`、`JarLauncher` / `WarLauncher` 等 → Spring Bootの組込みTomcatで起動している可能性。
- 外部Tomcatは `CATALINA_BASE/conf/server.xml` のConnectorと、`webapps` / Context定義を確認。
- `ProxyPass ... ajp://...` / `JkMount` なら現在はAJP。既存の秘密情報は記録・公開しない。
- 他アプリが同じConnectorを使用しているかも確認する。

PROSTAのリポジトリはWAR出力に対応している。ただし、**実際のサーバーの起動方法はリポジトリだけでは確定できない。**

> 外部Tomcatでは `server.port` や `server.servlet.context-path` は外部コンテナーの待受ポート・配備先を変更しない。`application-oracle.properties` に7778の定義はない。7778や8080を決め打ちしない。

実際のポートが8080、配備先が `/prosta` の例:

```bash
curl -I http://127.0.0.1:8080/prosta/login
```

200または意図したリダイレクトか確認。404ならポート・WAR名・Context・起動ログを確認する。DB障害などで500なら先に解決する。APIは認証が必要なので疎通確認に使わない。

### 2-4. DNSと証明書

Windows PowerShell:

```powershell
Resolve-DnsName YOUR_NAME.duckdns.org -Type A
Resolve-DnsName YOUR_NAME.duckdns.org -Type AAAA
```

AレコードがVMの現在のパブリックIPに一致すること。AAAAが存在するなら、そのIPv6でも同じサービスへ到達できること。存在しないAAAAは問題ない。古いAAAAだけが残っていると一部の利用者や証明書更新が失敗する。

Linux:

```bash
sudo certbot certificates
sudo certbot plugins
sudo systemctl list-timers --all | grep -Ei 'certbot|letsencrypt'
```

記録するもの:

- Certificate Name（ドメインと同じとは限らず、`-0001` 等の場合もある）。
- Domains、有効期限、Certificate Path、Private Key Path。
- 更新方法（apache / webroot / standalone / manual / DNSプラグイン等）。
- timer、cron、snap等、実際に更新を動かす仕組み。

通常は次を参照する:

```text
/etc/letsencrypt/live/CERT_NAME/fullchain.pem
/etc/letsencrypt/live/CERT_NAME/privkey.pem
```

`fullchain.pem` は中間証明書込み。秘密鍵は `cat` しない。権限を緩めたりリポジトリへコピーしたりしない。

証明書の公開情報だけなら確認可能:

```bash
sudo openssl x509 -in /etc/letsencrypt/live/CERT_NAME/fullchain.pem \
  -noout -subject -issuer -dates -ext subjectAltName
```

## 3. 採用するルートを決める

| 実際の構成 | 進め方 |
|---|---|
| Apache HTTP Server → HTTP → 外部Tomcat | 以下の標準手順。最も変更が少ない |
| Apache HTTP Server → HTTP → 組込みTomcat | Apache手順は同じ。Tomcat側はSpring Boot設定で制御 |
| Apache → AJP → Tomcat | 現在のAJP設定をまず確認。同居アプリへの影響を評価後、HTTPループバック転送への変更を推奨。AJPを外部公開しない |
| OCI LBが手前にある | LBのリスナー・証明書・転送ヘッダー・VM側の信頼元を含む別経路。以下の「Apacheが最前段」の設定をそのまま適用しない |
| Apache HTTP Serverがない | Tomcat直接TLSか、HTTP Serverを導入する構成を選ぶ。まず現在の待受けを確定する |

以降は **Apacheがインターネットから直接80/443を受けるLinux VM** を標準にする。

## 4. 変更前のバックアップとネットワーク

1. 有効なApache設定、Tomcatの `server.xml` / Context、サービス設定を、日時付きでバックアップ。
2. 現在のApache構文チェック、アプリ疎通が正常か確認。
3. SSH接続を維持したまま作業。SSHポート・既存ルールを削除しない。
4. OCIコンソールでVMのVNICに適用されるNSGとサブネットのセキュリティ・リストを確認。
5. パブリックIP、Internet Gateway、サブネットのルートを確認。

公開するポート:

| ポート | 用途 | 許可 |
|---|---|---|
| TCP 443 | HTTPS | 利用者から到達可能 |
| TCP 80 | HTTPリダイレクト・HTTP-01証明書更新 | HTTP-01を使うなら外部から到達可能 |
| SSH | 管理 | 管理元IPに限定 |
| Tomcatの8080/7778等、AJPの8009 | 内部転送 | 同一VMならインターネットへ公開しない |

IPv6を公開する場合はIPv6のルールも別途確認する。セキュリティ・リストとNSGを併用している場合、片方から広く許可されていないか確認する。

OS側も確認:

```bash
sudo firewall-cmd --state
sudo firewall-cmd --list-all
```

firewalldが有効で、対象インターフェースのゾーンが確認できた場合に限り、そのゾーンでHTTP/HTTPSを許可する:

```bash
sudo firewall-cmd --zone=YOUR_ZONE --permanent --add-service=http
sudo firewall-cmd --zone=YOUR_ZONE --permanent --add-service=https
sudo firewall-cmd --reload
```

UbuntuでUFW使用中なら `sudo ufw status verbose` で確認し、必要な80/443のみ許可する。確認のためにファイアウォール全体を無効化しない。

## 5. ApacheでHTTPSを有効化

### 5-1. 必要なモジュール

Ubuntu / Debianで不足している場合:

```bash
sudo a2enmod ssl proxy proxy_http headers rewrite
```

Oracle Linux / RHEL系は `apachectl -M` で確認。SSLモジュールが未導入なら、OSとリポジトリに対応する `mod_ssl` パッケージを導入する。既存LoadModuleを重複追加しない。

80/443の `Listen` が既にあるか確認する。**Listen 443を重複記述すると起動失敗する。**

### 5-2. 証明書が取得済みの場合のVirtualHost例

以下は新しい設定のひな型。既存の同一ServerNameのVirtualHostがある場合はそこへ統合する。
8080と証明書名は実値へ置換する。公開URLとTomcatの配備先をどちらも `/prosta/` に揃える。

```apache
<VirtualHost *:80>
    ServerName YOUR_NAME.duckdns.org

    # Certbot webroot認証用。既存の更新方式を確認してから使用する。
    Alias /.well-known/acme-challenge/ /var/www/letsencrypt/.well-known/acme-challenge/
    <Directory "/var/www/letsencrypt/.well-known/acme-challenge/">
        Options None
        AllowOverride None
        Require all granted
    </Directory>

    RewriteEngine On
    RewriteCond %{REQUEST_URI} !^/\.well-known/acme-challenge/
    RewriteRule ^ https://YOUR_NAME.duckdns.org%{REQUEST_URI} [R=302,L]
</VirtualHost>

<VirtualHost *:443>
    ServerName YOUR_NAME.duckdns.org
    SSLEngine on
    SSLCertificateFile /etc/letsencrypt/live/CERT_NAME/fullchain.pem
    SSLCertificateKeyFile /etc/letsencrypt/live/CERT_NAME/privkey.pem
    SSLProtocol -all +TLSv1.2 +TLSv1.3

    ProxyRequests Off
    ProxyPreserveHost On
    ProxyAddHeaders On

    # 外部から来た転送ヘッダーを信頼しない。Apache自身で生成する。
    RequestHeader unset Forwarded
    RequestHeader unset X-Forwarded-For
    RequestHeader unset X-Forwarded-Host
    RequestHeader unset X-Forwarded-Proto
    RequestHeader unset X-Forwarded-Port
    RequestHeader unset X-Forwarded-Prefix
    RequestHeader unset X-Forwarded-Ssl
    RequestHeader set X-Forwarded-Proto "https"
    RequestHeader set X-Forwarded-Port "443"

    RedirectMatch 302 ^/$ /prosta/
    RedirectMatch 302 ^/prosta$ /prosta/
    ProxyPass        /prosta/ http://127.0.0.1:8080/prosta/
    ProxyPassReverse /prosta/ http://127.0.0.1:8080/prosta/
</VirtualHost>
```

補足:

- 上記の設定はApacheが最前段の場合。前段にLB/CDNがある場合、実IPの信頼範囲を別途設計する。
- `ProxyRequests On` は不要。公開フォワードプロキシにしない。
- `ProxyPreserveHost On` で公開ホスト名を維持する。PROSTAのOrigin判定と整合させる。
- `ProxyPass` の両側の末尾 `/` を揃える。
- TLS 1.3はApache/OpenSSLの対応が必要。構文チェックで非対応ならOSのサポート状況を確認し、適合する設定を使う。
- リダイレクトは最初302で検証し、安定後に必要に応じて301へ変更。ブラウザーに誤った恒久リダイレクトを記憶させない。
- 既存のアクセスログ・エラーログ出力先を確認し、ログ取得を維持する。

webroot用ディレクトリーを使う場合:

```bash
sudo mkdir -p /var/www/letsencrypt/.well-known/acme-challenge
```

Apacheから読める権限・SELinuxラベルを設定する。書込み権限を全員へ開放しない。

Ubuntuは実際のサイト設定ファイル名で `a2ensite` を実行する。Oracle Linux系の `conf.d/*.conf` は通常Includeされるが、必ず `-S` で有効化を確認する。

### 5-3. 反映

```bash
sudo apachectl configtest
```

Ubuntu等は対応する `apache2ctl configtest` を使用。**Syntax OKになるまでreloadしない。**

その後、確認済みサービス名でreload:

```bash
sudo systemctl reload httpd
```

Ubuntu等は `apache2`。失敗したら、同じコマンドを繰り返さず `status` とjournalを確認する。

Oracle LinuxでApacheからTomcatへの接続がSELinuxに拒否される場合は、auditログを確認する。プロキシ運用の方針に合う場合:

```bash
getsebool httpd_can_network_connect
sudo setsebool -P httpd_can_network_connect on
```

これはApacheのネットワーク接続権限を広げる変更。必要性を確認して行い、SELinux自体は無効化しない。

## 6. TomcatとPROSTAの設定

### 6-1. プロファイル

本番のサービス起動環境で `SPRING_PROFILES_ACTIVE=oracle` を指定する。共通設定の `product` を編集しなくても、環境変数などで上書き可能。

systemdのサービスに指定する例:

```ini
[Service]
Environment="SPRING_PROFILES_ACTIVE=oracle"
```

既存のdrop-inへ追記するか、管理方法に合わせて作成し、既存のDB設定等は保持する。
unit変更後は `systemctl daemon-reload` と対象アプリ/Tomcatの再起動が必要。
外部Tomcat全体へ指定すると同居するSpringアプリにも影響し得るので、同居アプリのプロファイル設定を確認する。

PROSTA本番設定に追加済み:

```properties
server.forward-headers-strategy=framework
server.servlet.session.cookie.secure=true
```

- `framework`: Spring側でプロキシ由来のHTTPS情報を扱う。
- `secure=true`: 組込みコンテナーではログインCookieをHTTPS限定にする。
- ローカルの `product` と共通設定は変更しない。
- 起動ログで **oracleがactive** か確認する。ファイル名だけで有効と判断しない。

### 6-2. 外部TomcatへWAR配備している場合の重要な違い

**Spring Bootの `server.*` 設定のすべてが外部Tomcatの設定へ反映されるわけではない。**
特にポート・配備パス・コンテナーのCookie設定を、この2行だけで設定済みと判断しない。

Apacheからのみ使用する専用HTTP Connectorの例:

```xml
<Connector address="127.0.0.1"
           port="8080"
           protocol="HTTP/1.1"
           connectionTimeout="20000"
           scheme="https"
           secure="true"
           proxyName="YOUR_NAME.duckdns.org"
           proxyPort="443" />
```

- 既存Connectorの必要な属性は保持する。新しい同一ポートのConnectorを重複追加しない。
- `scheme` / `secure` は、Tomcatがリダイレクトを生成する段階から公開側をHTTPSとして扱うためのもの。
- **内部通信自体にTLSをかける設定ではない。** `SSLEnabled="true"` を付けない。Apache→TomcatはHTTPのまま。
- 専用Connectorなのでループバック限定。別サーバーからも使用しているConnectorへ、そのまま適用しない。
- 同じConnectorでHTTP公開・他のホスト名も扱う場合は固定値方式を使わず、専用Connectorの追加などを検討する。
- 既に `RemoteIpValve` 等を使用している場合、転送ヘッダーの二重解釈を避けるため既存の信頼設定と整合させる。むやみにValveを追加しない。

Tomcatの配備設定でSecure Cookieが明示的にfalseになっていないか確認する。専用HTTPS Connectorで `request.isSecure()` がtrueとなる構成にし、最終的にはブラウザーの `Set-Cookie` で検証する。
必要なら、本番の対象アプリの有効なWeb設定で次を設定する（既存 `session-config` へ統合。新しいweb.xmlを丸ごと上書きしない）:

```xml
<session-config>
    <cookie-config>
        <http-only>true</http-only>
        <secure>true</secure>
    </cookie-config>
</session-config>
```

共通Tomcatの `conf/web.xml` への適用は他アプリにも影響するため安易に行わない。また、ローカルと共有するWARへ無条件に追加すると、ローカルHTTPにも影響する。本番限定の配備設定として管理する。

外部Tomcatの対応Servlet/Jakartaバージョンも確認する。HTTPS化のためにTomcatのメジャーバージョンを同時変更する必要はない。

### 6-3. 組込みTomcatの場合

- 本番プロファイルの2設定を適用。
- 待受けを `server.address=127.0.0.1` で制限することを推奨（同一VMのApacheからのみ接続する構成の場合）。
- `server.port` は実際の運用値を明示し、Apacheの転送先に合わせる。未指定の既定値を7778だと思い込まない。
- コンテナー自身のリダイレクトがHTTPへ戻る場合は、Spring処理前のコンテナーリダイレクトかを切り分ける。

## 7. 証明書の自動更新

### 7-1. 既存の更新方式を維持する

まず有効な証明書を再利用し、不要な再発行を繰り返さない。発行制限の原因になる。

| 認証方式 | 確認すること |
|---|---|
| apache | Apacheプラグインで検証・更新できるか |
| webroot | 更新時のwebrootとApacheのAliasが一致するか |
| standalone | 80番を一時使用するためApacheと競合しないか。無計画にApacheを停止しない |
| manual / DNS-01 | 更新用フックなしの手動運用では自動更新できない場合がある |

Apacheを常時稼働させる場合、既存のApacheプラグインまたはwebroot認証が扱いやすい。
更新方式の変更が必要な場合は、使用中Certbotの `--help` と公式手順を確認し、renewal設定ファイルを推測で編集しない。

webrootで新規取得が本当に必要な場合の例:

```bash
sudo certbot certonly --webroot -w /var/www/letsencrypt \
  -d YOUR_NAME.duckdns.org
```

**取得済み証明書が有効なら、上の再取得は通常不要。**

### 7-2. 更新成功後のApache再読込

Certbotが証明書を更新しても、Apacheが古い証明書を読み込んだままにならないようにする。
既存のinstaller/hookがreloadしているか確認し、なければdeploy hookを追加する。

Oracle Linux系のhookスクリプト例:

```sh
#!/bin/sh
set -eu
/usr/sbin/apachectl configtest
/usr/bin/systemctl reload httpd
```

`command -v apachectl systemctl` で実パスを確認する。Ubuntuでは `apache2ctl` と `apache2` に変更。
root管理で `/etc/letsencrypt/renewal-hooks/deploy/` に保存し、実行可能にする。
実際に設定する際は既存ファイルと重複しない名前にし、他者書込み不可にする。

検証:

```bash
sudo certbot renew --dry-run
```

deploy hookは通常dry-runでは実行されない。対応バージョンでは、影響を確認した上で:

```bash
sudo certbot renew --dry-run --run-deploy-hooks
```

またはhookの構文チェック・reloadを個別に確認する。timer/cronが有効かも確認し、同じ更新ジョブを重複登録しない。
HTTP-01を継続するなら、80番と `/.well-known/acme-challenge/` を閉じない。
証明書の有効期間を固定日数と決めつけず、実際の有効期限を監視する。

## 8. Duck DNSの運用

- Duck DNS管理画面で、VMの現在のパブリックIPv4に更新されているか確認。
- IP変更があり得る構成なら定期更新を自動化する。
- OCIの予約済みパブリックIPを使用できる場合、IP固定化でDNS更新漏れのリスクを減らせる。利用条件・費用はOCI契約で確認する。
- Duck DNSの更新APIはHTTPSを使用。アカウントトークンはサーバーの保護された設定へ保存し、URLごとログに残さない。
- DNS-01の自動化も可能だが、Duck DNSトークンの保護と対応ACMEクライアント/プラグインの確認が必要。今回は既存CertbotのHTTP-01方式が正常なら変更不要。

## 9. 切替後の確認チェックリスト

### サーバー内から

```bash
sudo apachectl configtest
sudo ss -ltnp
curl -I http://127.0.0.1:8080/prosta/login
curl -I --resolve YOUR_NAME.duckdns.org:443:127.0.0.1 \
  https://YOUR_NAME.duckdns.org/prosta/login
```

`--resolve` はDNS経路を使わずSNIと証明書のホスト名検証を維持する。Apacheがループバックを含むアドレスで443を待受ける構成の例。特定IPにbindしているなら、その実値へ置換する。`-k` で検証を省略して成功扱いにしない。

### 外部のWindows PCから

```powershell
Test-NetConnection YOUR_NAME.duckdns.org -Port 443
curl.exe -I http://YOUR_NAME.duckdns.org/prosta/login
curl.exe -I https://YOUR_NAME.duckdns.org/prosta/login
```

外部公開を禁止したバックエンドポートにも `Test-NetConnection` を実施し、インターネットから接続できないことを確認する。

- [ ] HTTPからHTTPSへ移動する（検証中は302）。
- [ ] 証明書の警告がなく、ホスト名・有効期限・証明書チェーンが正しい。
- [ ] HTTPSでログイン → マイページ → フレンド → フィード → サマリー → ログアウトができる。
- [ ] リダイレクト先が `http://`、`127.0.0.1`、8080等の内部URLにならない。
- [ ] 開発者ツールのNetworkで `Set-Cookie` の `Secure` / `HttpOnly` を確認する。
- [ ] CookieのPathが公開パス `/prosta` に適合する。外部TomcatではSameSiteの実値も確認する。
- [ ] CSS・JS・画像がHTTPSで読み込まれ、Mixed Contentがない。
- [ ] 更新系操作・fetchがOrigin不一致で403にならない。
- [ ] 別アカウントのタブ分離と、同一アカウントの新ログインによる旧認証失効が維持される。
- [ ] モバイル回線などVM外から接続できる。
- [ ] バックエンドポートは外部非公開。
- [ ] Certbotのdry-run、定期実行、更新後reloadが成功する。
- [ ] ローカル `product` は従来どおりHTTPでログインできる。

すべて安定してから恒久リダイレクトやHSTSを検討する。HSTSは設定後にHTTPへ戻しにくくなるため、未検証で長期間・includeSubDomains・preloadを指定しない。

## 10. よくある症状と戻し方

| 症状 | 主な確認先 |
|---|---|
| 443がタイムアウト | DNS、OCI NSG/セキュリティ・リスト、ルート、OSファイアウォール、待受け |
| 他サイトの証明書が出る | ServerName、SNI、443 VirtualHostの重複・選択順 |
| 証明書警告 | SANのホスト名、期限、fullchain、中間証明書、サーバー時刻 |
| 502/503 | Tomcatの状態、転送先ポート、Context、SELinux |
| 404 | `/prosta` の配備先、ProxyPassのパス・末尾スラッシュ |
| HTTPへ戻る/無限ループ | oracleの有効化、転送ヘッダー、Connectorのscheme/secure、前段LBの有無 |
| ログインが維持されない | HTTPSでアクセスしているか、Secure/Path、外部Tomcat側Cookie設定 |
| POST等だけ403 | Host・Origin・公開ポート・ProxyPreserveHost、同一オリジン構成 |
| 更新だけ失敗 | 古いA/AAAA、80番、認証方式/webroot、timer、秘密鍵の参照権限 |

詳細は既存の [タイムアウト確認資料](./OCI_HTTPS_接続タイムアウト.md) も参照。

戻す場合:

1. Apacheの変更前設定を復元し、構文チェック成功後にreload。
2. Connector/サービス設定を変更した場合は、そのバックアップを復元して該当サービスを再起動。
3. ネットワークルールは変更したものだけを戻す。SSHを遮断しない。
4. 証明書・秘密鍵は削除しない。DB初期化SQLは一切不要。
5. CookieがSecureの本番環境をHTTPへ戻すとログインできなくなる。HTTPへ戻す必要が本当にある場合は、本番設定も整合させて戻す。安易にproductへ切り替えて別DBへ接続しない。

## 11. もっと良い方法はあるか

| 方法 | 利点 | 注意・今回の判断 |
|---|---|---|
| **既存Apache + Certbot** | 既存構成を活用でき、変更が最小。証明書更新も自動化可能 | **今回の第一候補**。正常運用できるなら移行不要 |
| Caddyへ置換 | 証明書の取得・更新・読込みをまとめて自動管理しやすい | 新規の単一アプリなら候補。ただし既存Apacheと80/443が競合するので単純追加は不可。同居サイトの移行が必要 |
| OCI Load BalancerでTLS終端 | 複数VM、ヘルスチェック、入口の集約に向く | 費用・無料枠・証明書の更新配布・転送ヘッダー管理を確認。Network Load Balancerとは別。単一VMでは必ずしも簡単にならない |
| Tomcatで直接TLS | HTTP ServerなしでHTTPSを扱える | 外部Tomcatの証明書設定・更新後反映や権限を管理。既にApacheがある今回には利点が少ない |
| DNS-01認証 | 80番を公開せず証明書更新できる | DNS APIトークンと自動化が必要。HTTP-01が使えない場合の候補 |

結論: **今の構成ではApacheの継続利用が合理的。**「nginxを追加する」「LBを必ず作る」必要はない。まず既存の設定と更新方式を確定し、必要な箇所だけ直す。

## 12. 公式資料

- [Apache HTTP Server: Reverse Proxy / mod_proxy](https://httpd.apache.org/docs/2.4/mod/mod_proxy.html)
- [Apache HTTP Server: RequestHeader / mod_headers](https://httpd.apache.org/docs/2.4/mod/mod_headers.html)
- [Tomcat 10.1: HTTP Connector](https://tomcat.apache.org/tomcat-10.1-doc/config/http.html)
- [Spring Boot 3.1.5: How-to（プロキシ・外部コンテナー関連）](https://docs.spring.io/spring-boot/docs/3.1.5/reference/html/howto.html)
- [Certbot: 利用・証明書更新・フック](https://eff-certbot.readthedocs.io/en/stable/using.html)
- [Let's Encrypt: チャレンジ方式](https://letsencrypt.org/docs/challenge-types/)
- [Duck DNS: 更新API](https://www.duckdns.org/spec.jsp)
- [OCI: Load Balancer概要](https://docs.oracle.com/en-us/iaas/Content/Balance/Concepts/balanceoverview.htm)

OS、Tomcat、Certbotの実バージョンと構成に一致する公式資料を併用する。上記は調査用リンクであり、本サーバーのバージョン確定ではない。
