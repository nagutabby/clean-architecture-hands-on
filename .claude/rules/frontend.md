---
paths:
  - "frontend/**/*.{ts,tsx}"
---

# フロントエンド（Next.js / TypeScript）アーキテクチャ規約

機能モジュール `src/features/<feature>/` 配下は以下の4層構造に従います。

## 4層のディレクトリ構成と依存制約

1. `domain/`（第1層：Entities）
   - 純粋な TypeScript の型定義（`types.ts`）およびフレームワーク非依存のビジネスルール関数（入力バリデーション等）を配置する。
   - 禁止事項: React の import、`fetch` 通信、DOM 操作、表示用フォーマット処理を含めてはならない。

2. `usecase/`（第2層：Use Cases）
   - 外部通信を抽象化するインターフェース（`*Repository.ts`）と、画面のユースケースと状態を管理するカスタムフック（`use*.ts`）を配置する。
   - 禁止事項: `adapter/` 配下の具体実装を直接 import せず、`*Repository` インターフェースを介して通信する（依存性逆転）。

3. `adapter/`（第3層：Interface Adapters - 外部通信）
   - `*Repository` インターフェースを実装する API クライアント（`*Api.ts`）を配置し、`fetch` とエラーハンドリングをカプセル化する。

4. `presentation/`（第3層：Interface Adapters - UI表示）
   - UI コンポーネント（`*.tsx`）と表示用ユーティリティ（`presentation/utils/formatCurrency.ts` 等）を配置する。
   - コンポーネント内に `fetch` を直接記述してはならない。
   - 純粋な表示コンポーネント（`BalanceSummaryCard`, `TransactionForm`, `TransactionList` 等）は `adapter/` を直接 import してはならない。`adapter/` の実装の注入（Composition Root）は機能ルートコンテナ（`TransactionSection.tsx`）のデフォルト引数でのみ行う。
