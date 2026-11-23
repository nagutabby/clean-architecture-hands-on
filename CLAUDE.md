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
