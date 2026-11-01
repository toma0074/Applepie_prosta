# プレゼン資料

- [配布用ZIP](./PROSTA_presentation.zip): PDFと圧縮済みPPTXを収録。展開済みPDF・PPTXはリポジトリから削除し、ZIPのみ保持した。
- [本番発表台本](./本番発表台本.md): 発表時間・話す内容・当日の注意。資料との対応は発表前に確認する。
- [生成スクリプト](./make_pptx.ps1)と[スライド定義](./slides.json): PowerPointを使った資料生成用。手修正・動画埋込みなど、ZIP内の完成資料の全加工を再現するものではない。

## 生成する場合

WindowsでMicrosoft PowerPointがインストールされている環境が必要。次の例はこのフォルダをカレントディレクトリにして実行する。

```powershell
.\make_pptx.ps1 -Json .\slides.json -Out (Join-Path $PWD.Path 'PROSTA_generated.pptx')
```

整理作業では生成スクリプトの再実行・完成ZIPの再作成は行っていない。
