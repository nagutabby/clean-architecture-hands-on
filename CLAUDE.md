# Budget App 開発ガイドライン

シンプル家計簿アプリ（Spring Boot + Next.js）のモノレポです。
すべての実装においてクリーンアーキテクチャの依存性ルール（外側の層から内側の層へのみ依存）を厳守してください。

## 技術スタック
- Backend: Kotlin v2, Java 25 (LTS), Spring Boot v4, PostgreSQL v18, Flyway, JUnit 5, MockK, Konsist
- Frontend: TypeScript, Next.js v16 (App Router), React 19, Tailwind CSS v4, Vitest, Testing Library, MSW, pnpm v11
- Infra: Rancher Desktop / Docker Compose (PostgreSQL 18)

## 主要コマンド
- DB起動: `docker compose up -d`
- Backendテスト: `cd backend && ./gradlew test`
- Backend起動: `cd backend && ./gradlew bootRun`
- Frontendテスト: `cd frontend && pnpm vitest run`
- Frontend開発サーバー: `cd frontend && pnpm dev`

## 共通設計原則
1. 依存性のルール: ソースコードの依存関係は常に「外側の円」から「内側の円」へ向かうこと。内側の層から外側の層を import してはならない。
2. 層の飛び越え禁止: プレゼンテーション層（Controller / UI）は必ずユースケース層（UseCase / カスタムフック）を経由し、リポジトリ実装や外部通信を直接呼び出さないこと。
3. テストの維持: 新規クラスやユースケースを追加した際は、対応する単体テストまたは統合テストを必ず作成し、テストがパスすることを確認すること。

## チェックポイント運用
作業はチェックポイント（CP）単位で進める。
1. 1回の指示で実施するのは1つの CP だけ。複数の CP を連続して実行しない。
2. CP が完了したら「CPx-y 完了」と宣言し、作成・変更したファイル、実行したコマンドとその結果、次の CP の名前を報告して必ず停止する。
3. ユーザーが「次のチェックポイントに進む」と入力するまで、次の CP に着手しない。
4. 検証コマンドが失敗した場合は、その CP の中で原因を修正して再実行し、成功してから完了を報告する。失敗を次の CP に持ち越さない。
5. CP に書かれていないファイルは作成しない。
