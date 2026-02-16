# Task Reminder App (Android)

タスクごとの日次記録を管理し、最終記録からN日を表示するAndroidアプリの雛形です。

## 実装済み（初期生成）
- ホーム（タスク一覧）
- タスク詳細
  - 表表示タブ（最終記録日、N日、今日の状態、あり/なし入力）
  - カレンダータブ（月移動、日付セル入力）
- 設定画面（プレースホルダ）
- Room DB
  - `tasks`
  - `items`
  - `daily_records`
  - `app_settings`
- 毎日通知ワーカー（WorkManager）
- 通知欄 `Yes / No` アクションで記録保存

## 構成
- `app/src/main/java/com/example/taskreminder/`
  - `data/` DBとRepository
  - `ui/` 画面とViewModel
  - `notification/` 通知関連

## 実行
1. Android Studioで `task-reminder-app` を開く
2. Gradle同期
3. エミュレータまたは実機で起動

## 仕様書
- `docs/spec_v1_1.md`
- `docs/db_design_room_sqlite.md`
