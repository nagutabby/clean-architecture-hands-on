# Chapter 7: Claude Code の Skills と Hooks を活用した機能追加演習

## このチャプターの目的

クリーンアーキテクチャを採用すると、1つの機能を追加する際にも「Input Port → Interactor → Repository → Controller → テスト」のように複数の層へ順番にファイルを作成・変更する必要があります。手作業や単純な指示だけでは、層の飛び越えやテストの書き忘れが発生しがちです。

この最終チャプターでは、Claude Code の拡張機能である Agent Skills（`.claude/skills/`）と Hooks（`.claude/settings.json`）を構築し、設計ルールとテストを自動担保しながら新機能「家計簿明細の編集（更新）機能」をフルスタックで追加します。

1. 7-1. ユースケース追加スキル（`.claude/skills/add-usecase/SKILL.md`）の作成
2. 7-2. Hooks（`.claude/hooks/verify-changes.mjs` と `.claude/settings.json`）による自動検証の設定
3. 7-3. 実践演習：`/add-usecase` スキルを使った「明細更新機能」の実装
4. 7-4. フルスタック動作確認とハンズオンのまとめ

---

## 7-1. ユースケース追加スキル（`SKILL.md`）の作成

Chapter 2 で作成した `setup-env` スキルと同様に、新しいユースケースをクリーンアーキテクチャの4層すべてに漏れなく実装するための手順書をスキルとして定義します。

### 1. スキル用ディレクトリの作成

プロジェクトのルートディレクトリ（`budget-app-handson`）で以下のコマンドを実行します。

```bash
mkdir -p .claude/skills/add-usecase
```

### 2. `.claude/skills/add-usecase/SKILL.md` の作成

`.claude/skills/add-usecase/SKILL.md` を新規作成し、以下の内容を記述してください。

```markdown
---
name: add-usecase
description: クリーンアーキテクチャの4層構造（バックエンド・フロントエンド・自動テスト）に従って新しいユースケースを追加するスキル
---

# 新規ユースケース実装手順

ユーザーから `/add-usecase <機能概要>` で呼び出された場合、`.claude/rules/backend.md` および `.claude/rules/frontend.md` の依存制約を厳守し、以下の手順で実装とテストを行ってください。

## Step 1: 実装計画の提示
コードを書く前に、以下の4層で新規作成・変更するファイルの一覧をユーザーに提示する。
1. Backend 第1層（domain）: 必要なドメインルールやメソッドの追加
2. Backend 第2層（usecase）: `usecase/input/*UseCase.kt`、`usecase/interactor/*Interactor.kt`、必要に応じた `usecase/output/*Repository.kt` のメソッド追加
3. Backend 第3層（adapter）: `*RepositoryImpl.kt`、DTO、`*Controller.kt`
4. Frontend 第1〜3層: `domain/types.ts`、`usecase/TransactionRepository.ts`、`usecase/useTransactions.ts`、`adapter/transactionApi.ts`、`presentation/` コンポーネント

## Step 2: バックエンドの実装とテスト
1. 内側の層（domain → usecase → adapter）の順に実装する。
2. 更新・削除系ユースケースの Interactor では、永続化メソッドを呼ぶ前に必ず `findById` で対象レコードの存在検証を行い、存在しない場合は `DomainException` をスローする。
3. Controller は必ず `usecase/input` のインターフェースのみを受け取り、`usecase/output` や `adapter/repository`（JPA）を直接参照しない。また、`adapter` から `framework` を import しない。
4. 以下のテストを作成・更新する。
   - Interactor の単体テスト（MockK を使用）
   - Controller の統合テスト（MockMvc を使用）。Controller のコンストラクタに UseCase を追加した場合は、既存の `TransactionControllerTest` の `TestConfig` にも対応する Bean を追加する（追加しないと既存テストが `NoSuchBeanDefinitionException` で失敗する）
5. `cd backend && ./gradlew test` を実行し、Konsist アーキテクチャテストを含む全テストがパスすることを確認する。

## Step 3: フロントエンドの実装とテスト
1. 内側の層（domain → usecase → adapter → presentation）の順に実装する。
2. `usecase/useTransactions.ts` は `adapter` を直接 import せず、`TransactionRepository` インターフェース経由で通信する。
3. MSW ハンドラー（`src/mocks/handlers.ts`）と結合テスト（`TransactionSection.test.tsx`）に新機能のテストケースを追加する。
4. `cd frontend && pnpm vitest run` と `cd frontend && pnpm build` を実行し、テストと型チェックがパスすることを確認する。

## Step 4: 完了報告
追加・変更したファイル一覧と、バックエンド・フロントエンド双方のテスト実行結果をユーザーに報告する。
```

---

## 7-2. Hooks（`.claude/settings.json`）による自動検証の設定

Claude Code の Hooks 機能を使うと、ツール実行の前後（`PreToolUse` / `PostToolUse`）や応答完了時（`Stop`）に任意のコマンドを自動実行できます。

ここでは、Claude Code がファイルを新規作成（`Write`）または編集（`Edit`）した直後に、対象ファイルの拡張子に応じて自動検証スクリプトを実行するフックを設定します。

- Kotlin ファイル（`.kt`）を編集した直後：`./gradlew cleanTest test --tests "*ArchitectureTest"` を実行し、パッケージ間の依存ルール違反がないかをチェックする
- TypeScript ファイル（`.ts` / `.tsx`）を編集した直後：`pnpm exec tsc --noEmit` を実行し、型エラーを検知する

### 1. 検証スクリプト（`.claude/hooks/verify-changes.mjs`）の作成

JSON 内に複雑なワンライナーを埋め込むとエスケープの保守が困難になるため、独立した Node.js スクリプトとして切り出します。

ルートディレクトリ（`budget-app-handson`）でディレクトリを作成します。

```bash
mkdir -p .claude/hooks
```

`.claude/hooks/verify-changes.mjs` を新規作成し、以下の内容を記述してください。

```javascript
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

const input = JSON.parse(fs.readFileSync(0, 'utf8'));
const filePath = input.tool_input?.file_path ?? '';
const projectDir = process.env.CLAUDE_PROJECT_DIR || process.cwd();

function runCheck(cmd, args, cwd) {
  try {
    execFileSync(cmd, args, {
      cwd,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
    });
  } catch (err) {
    const output = [err.stdout, err.stderr].filter(Boolean).join('\n');
    process.stderr.write(`検証エラー (${cmd} ${args.join(' ')}):\n${output}\n`);
    process.exit(2);
  }
}

if (filePath.endsWith('.kt')) {
  const gradlew = process.platform === 'win32' ? 'gradlew.bat' : './gradlew';
  runCheck(gradlew, ['cleanTest', 'test', '--tests', '*ArchitectureTest'], path.join(projectDir, 'backend'));
} else if (filePath.endsWith('.ts') || filePath.endsWith('.tsx')) {
  runCheck('pnpm', ['exec', 'tsc', '--noEmit'], path.join(projectDir, 'frontend'));
}
```

### 2. `.claude/settings.json` の作成

プロジェクトルートの `.claude/settings.json` を作成し、環境変数 `$CLAUDE_PROJECT_DIR` を用いて上記スクリプトを呼び出す設定を記述します。

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "node \"$CLAUDE_PROJECT_DIR/.claude/hooks/verify-changes.mjs\""
          }
        ]
      }
    ]
  }
}
```

### 設定のポイント

- `$CLAUDE_PROJECT_DIR` の利用：Claude Code のセッション中にカレントディレクトリが移動していても、常にプロジェクトルートを基準に `backend/` と `frontend/` のパスを正しく解決します。
- `stderr` へのエラー出力と終了コード `2`：Claude Code のフック仕様では、終了コード `2` を返した際に **標準エラー出力（`stderr`）** に書き込まれた内容が AI へフィードバックされます。スクリプト内で `stdout` と `stderr` の両方を結合して `process.stderr.write` に流すことで、Gradle や `tsc` のエラー詳細を確実に Claude Code へ伝えて自律修正を促します。
- `cleanTest` を付ける理由：Konsist は Kotlin のソースファイルを直接読みますが、Gradle はソースファイルを `test` タスクの入力として扱いません。`import` 文だけを追加してバイトコードが変わらない場合、`test` が `UP-TO-DATE` と判定されて Konsist が実行されず、違反を見逃します。`cleanTest` で毎回再実行させます。
- 実行時間に関する補足：対象テストを `ArchitectureTest` のみに絞っていますが、Gradle（JVM）の起動には数秒〜十数秒の時間がかかります。
- 実装途中の失敗に関する補足：Output Port にメソッドを追加した直後など、実装クラスが未対応の間はコンパイルエラーでフックが失敗します。これは想定内の挙動で、AI は続けて実装クラスを修正するため、最終的にフックが成功すれば問題ありません。
- `Stop` への変更について：`Stop`（応答完了時に1回だけ実行）に変更する場合、`Stop` イベントの入力には `tool_input.file_path` が含まれないため、本スクリプトのままでは何も検証されません。その場合はスクリプトを書き換え、拡張子による分岐をやめて `backend` と `frontend` の両方の検証を無条件に実行してください。

---

## 7-3. 実践演習：`/add-usecase` スキルを使った「明細更新機能」の実装

ここまでで、設計ルール（`CLAUDE.md` / `.claude/rules/`）、実装手順スキル（`add-usecase`）、そして自動検証フック（`PostToolUse`）がすべて揃いました。

実際に Claude Code を起動し、新しいユースケースとして「登録済みの家計簿明細を編集・更新する機能」を追加します。

### 1. Claude Code の起動（または設定の再読み込み）

ルートディレクトリ（`budget-app-handson`）で Claude Code を起動します（すでに起動中の場合は一度 `/exit` で終了してから再起動すると新しいスキルとフックが確実に読み込まれます）。

```bash
claude
```

### 2. `/add-usecase` スキルの実行

チャット欄に以下のコマンドを入力して実行します。

```text
/add-usecase 家計簿明細の編集（更新）機能を追加してください。バックエンドには PUT /api/transactions/{id} エンドポイント（UpdateTransactionUseCase / UpdateTransactionInteractor）を追加し、存在しないカテゴリIDや存在しない明細IDの場合は DomainException をスローしてください。フロントエンドでは明細一覧の各行に「編集」ボタンを追加し、クリックするとその明細の内容を編集して保存できるUIを実装してください。
```

### 3. AI による実装と自動検証の観察

プロンプトを実行すると、Claude Code は以下の流れで作業を進めます。

1. `add-usecase` スキルの Step 1 に従い、変更対象となる全層のファイル一覧を提示します。
2. 各層のファイルを作成・編集するたびに `PostToolUse` フックが起動し、`ArchitectureTest` と `tsc --noEmit` が自動実行されます。
3. `UpdateTransactionInteractor` では、最初に `transactionRepository.findById(input.id)` と `categoryRepository.findById(input.categoryId)` を呼び出して対象の存在を検証し、存在しない場合は `DomainException` をスローします。存在が確認された明細に対してのみ、既存の `id` を保持した `Transaction` エンティティを構築して `transactionRepository.save()` を呼び出すことでレコードを更新します。
4. スキルの Step 2・Step 3 に従い、バックエンドの単体・統合テストおよびフロントエンドの MSW 結合テストが追加・実行されます。

---

## 7-4. フルスタック動作確認とハンズオンのまとめ

### 1. ブラウザでの最終確認

すべてのサーバーを起動し、`http://localhost:3000/transactions` にアクセスします。

1. Docker で PostgreSQL 18 が起動していること（`docker compose ps`）
2. バックエンドが起動していること（`cd backend && ./gradlew bootRun`）
3. フロントエンドが起動していること（`cd frontend && pnpm dev`）

ブラウザ上で「新規登録 → 金額やカテゴリの編集（更新） → 削除」の一連の操作を行い、画面上の明細一覧と「総収入・総支出・現在の残高」サマリーが正しく連動することを確かめてください。

### 2. 全テストスイートの実行確認

最後に、バックエンドとフロントエンドのすべての自動テストがパスすることを確認します。

```bash
cd backend && ./gradlew test
cd ../frontend && pnpm vitest run
```

### 3. ハンズオン全体のまとめ

本ハンズオンでは、シンプル家計簿アプリの構築を通じて以下の設計原則と開発ワークフローを実践しました。

1. 関心事の分離と4層構造
   ビジネスルール（`domain`）、アプリケーション手順（`usecase`）、入出力変換（`adapter`）、技術詳細（`framework` / `app`）を明確に分離しました。
2. 依存性逆転の原則（DIP）
   Use Case 層に `TransactionRepository` インターフェースを定義し、外側の実装クラスや API クライアントを注入することで、内側のコードが外部技術に依存しない構造を実現しました。
3. 多層的な自動テストとアーキテクチャ検証
   JUnit 5、MockK、MockMvc、Vitest、MSW に加え、Konsist によるアーキテクチャテストを導入し、依存性ルール違反を自動的に防止する仕組みを構築しました。
4. AI 開発におけるガードレール設計
   `CLAUDE.md`、`.claude/rules/`、Agent Skills（`SKILL.md`）、Hooks（`settings.json`）を組み合わせることで、AI エージェントが設計規約を遵守しながら自律的に実装と検証を進められる開発環境を整えました。
