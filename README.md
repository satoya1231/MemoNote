# メモ帳（Android）

端末内にメモを保存できる、シンプルなAndroidメモ帳アプリです。

## できること

- メモの追加・編集・自動保存
- タイトルと本文のキーワード検索
- ピン留め（大切なメモを常に上部表示）
- チェックリストの作成・完了チェック
- ゴミ箱への移動、復元、完全削除
- SharedPreferencesによる端末内保存
- メモの更新日時表示

## 開発環境

- Android Studio Ladybug以降
- Kotlin 2.3.21
- Jetpack Compose / Material 3
- compileSdk 36、minSdk 26

## ビルド

Android Studioでこのフォルダを開き、`app` を実行してください。

コマンドラインでは、Android SDKとGradleを用意したうえで次を実行します。

```bash
gradle assembleDebug
```

生成されたAPKは `app/build/outputs/apk/debug/app-debug.apk` です。

GitHubへプッシュすると、GitHub ActionsでもデバッグAPKのビルドを実行します。

## データについて

メモは外部サーバーへ送信せず、アプリをインストールした端末内に保存します。アプリをアンインストールすると、通常は保存データも削除されます。
