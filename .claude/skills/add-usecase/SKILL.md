---
name: add-usecase
description: クリーンアーキテクチャの4層構造（バックエンド・フロントエンド・自動テスト）に従って新しいユースケースを追加するスキル
---

# 新規ユースケース実装手順

ユーザーから `/add-usecase <機能概要>` で呼び出された場合、`.claude/rules/backend.md` および `.claude/rules/frontend.md` の依存制約を厳守し、以下のチェックポイント（CP）を順に進めてください。

CLAUDE.md の「チェックポイント運用」に従い、1つの CP を終えるたびに結果を報告して停止し、ユーザーが「次のチェックポイントに進む」と入力するまで次の CP に着手しないこと。最初は CP7-1 だけを実施する。

## CP7-1 〔設計〕実装計画の提示
コードを書く前に、以下の4層で新規作成・変更するファイルの一覧をユーザーに提示して停止する。
1. Backend 第1層（domain）: 必要なドメインルールやメソッドの追加
2. Backend 第2層（usecase）: `usecase/input/*UseCase.kt`、`usecase/interactor/*Interactor.kt`、必要に応じた `usecase/output/*Repository.kt` のメソッド追加
3. Backend 第3層（adapter）: `*RepositoryImpl.kt`、DTO、`*Controller.kt`
4. Frontend 第1〜3層: `domain/types.ts`、`usecase/TransactionRepository.ts`、`usecase/useTransactions.ts`、`adapter/transactionApi.ts`、`presentation/` コンポーネント

## CP7-2 〔実装〕バックエンドの実装
1. 内側の層（domain → usecase → adapter）の順に実装する。
2. 更新・削除系ユースケースの Interactor では、永続化メソッドを呼ぶ前に必ず `findById` で対象レコードの存在検証を行い、存在しない場合は `DomainException` をスローする。
3. Controller は必ず `usecase/input` のインターフェースのみを受け取り、`usecase/output` や `adapter/repository`（JPA）を直接参照しない。また、`adapter` から `framework` を import しない。
4. `cd backend && ./gradlew compileKotlin` が成功することを確認する。

## CP7-3 〔テスト〕バックエンドのテスト
1. 以下のテストを作成・更新する。
   - Interactor の単体テスト（MockK を使用）
   - Controller の統合テスト（MockMvc を使用）。Controller のコンストラクタに UseCase を追加した場合は、既存の `TransactionControllerTest` の `TestConfig` にも対応する Bean を追加する（追加しないと既存テストが `NoSuchBeanDefinitionException` で失敗する）
2. `cd backend && ./gradlew test` を実行し、Konsist アーキテクチャテストを含む全テストがパスすることを確認する。

## CP7-4 〔実装〕フロントエンドの実装
1. 内側の層（domain → usecase → adapter → presentation）の順に実装する。
2. `usecase/useTransactions.ts` は `adapter` を直接 import せず、`TransactionRepository` インターフェース経由で通信する。
3. `cd frontend && pnpm exec tsc --noEmit` が成功することを確認する。

## CP7-5 〔テスト〕フロントエンドのテスト
1. MSW ハンドラー（`src/mocks/handlers.ts`）と結合テスト（`TransactionSection.test.tsx`）に新機能のテストケースを追加する。
2. `cd frontend && pnpm vitest run` と `cd frontend && pnpm build` を実行し、テストと型チェックがパスすることを確認する。

## CP7-6 〔動作確認〕API の動作確認
1. PostgreSQL（`budget-app-db`）が稼働していることを確認し、バックエンドを `bootRun` でバックグラウンド起動する。
2. curl で、新機能の API について正常系と異常系（存在しない ID・カテゴリなど）を確認し、結果を報告する。確認のために登録したデータは、確認後にすべて削除する。
3. バックエンドは CP7-7 のために起動したままにする。

## CP7-7 〔動作確認〕画面での最終確認と完了報告
1. バックエンドが起動していなければ起動し、フロントエンド（`cd frontend && pnpm dev`）をバックグラウンドで起動する。
2. `cd backend && ./gradlew test` と `cd frontend && pnpm vitest run` を実行し、全テストの結果を報告する。
3. 追加・変更したファイルの一覧と、手順 2 のテスト結果を完了報告としてまとめ、ブラウザ（`http://localhost:3000/transactions`）での確認をユーザーに依頼して待機する。
