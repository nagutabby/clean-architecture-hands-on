# Chapter 7: Claude Code の Skills と Hooks を活用した機能追加演習

## このチャプターの目的

クリーンアーキテクチャを採用すると、1つの機能を追加する際にも「Input Port → Interactor → Repository → Controller → テスト」のように複数の層へ順番にファイルを作成・変更する必要があります。手作業や単純な指示だけでは、層の飛び越えやテストの書き忘れが起きがちです。

この最終チャプターでは、Claude Code の拡張機能である Agent Skills の `.claude/skills/` と Hooks の `.claude/settings.json`を構築し、設計ルールの遵守とテストを自動で保証しながら、新機能「家計簿明細の編集機能」、つまり更新機能をフルスタックで追加します。

7-1・7-2 は人が実施する準備です。7-3 で `/add-feature` スキルを実行すると、Claude Code が CP7-1〜CP7-7 を1つずつ進めます。

| CP | 種別 | 内容 |
| :--- | :--- | :--- |
| 7-1 | 人による準備 | `add-feature` スキルの作成 |
| 7-2 | 人による準備 | `verify-changes.mjs` と `settings.json` によるフックの設定 |
| CP7-1 | 設計 | 変更対象の全層のファイル一覧、つまり実装計画を提示させ、承認する |
| CP7-2 | 実装 | バックエンド：domain → usecase → adapter の順に更新 API を実装 |
| CP7-3 | テスト | バックエンドのテスト：Interactor 単体・Controller 統合・全テスト |
| CP7-4 | 実装 | フロントエンド：domain → usecase → adapter → presentation の順に編集 UI を実装 |
| CP7-5 | テスト | フロントエンドのテスト：MSW ハンドラー・結合テスト・ビルド |
| CP7-6 | 動作確認 | curl による更新 API の動作確認 |
| CP7-7 | 動作確認 | 新規登録 → 編集 → 削除の順の画面での最終確認と、全テストの実行 |

---

## 7-1. 人が実施する準備：ユースケース追加スキル `SKILL.md` の作成

Chapter 2 で作成した `setup-env` スキルと同様に、新しいユースケースをクリーンアーキテクチャの4層すべてに漏れなく実装するための手順書をスキルとして定義します。

### 1. スキル用ディレクトリの作成

プロジェクトのルートディレクトリ `budget-app-handson`で以下のコマンドを実行します。

```bash
mkdir -p .claude/skills/add-feature
```

### 2. `.claude/skills/add-feature/SKILL.md` の作成

`.claude/skills/add-feature/SKILL.md` を新規作成し、以下の内容を記述してください。

```markdown
---
name: add-feature
description: クリーンアーキテクチャの4層構造（バックエンド・フロントエンド・自動テスト）に従って新しいユースケースを追加するスキル
---

# 新規ユースケース実装手順

ユーザーから `/add-feature <機能概要>` で呼び出された場合、`.claude/rules/backend.md` および `.claude/rules/frontend.md` の依存制約を厳守し、以下のチェックポイント（CP）を順に進めてください。

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
```

このスキルには、チェックポイントごとに止まる進め方そのものが書き込まれています。`CLAUDE.md` の「チェックポイント運用」と合わせて、機能追加でも1つずつ確認しながら進められます。

---

## 7-2. 人が実施する準備：Hooks `.claude/settings.json` による自動検証の設定

Claude Code の Hooks 機能を使うと、ツール実行の前後である `PreToolUse` と `PostToolUse`、および応答完了時の `Stop` に任意のコマンドを自動実行できます。

ここでは、Claude Code がファイルを `Write` で新規作成または `Edit` で編集した直後に、対象ファイルの拡張子に応じて自動検証スクリプトを実行するフックを設定します。

- 拡張子 `.kt` の Kotlin ファイルを編集した直後：`./gradlew cleanTest test --tests "*ArchitectureTest"` を実行し、パッケージ間の依存ルール違反がないか確認する
- 拡張子 `.ts` / `.tsx` の TypeScript ファイルを編集した直後：`pnpm exec tsc --noEmit` を実行し、型エラーを検知する

### 1. 検証スクリプト `.claude/hooks/verify-changes.mjs` の作成

JSON の中に複雑なワンライナーを書くと、エスケープの管理が難しくなります。そのため、独立した Node.js スクリプトに切り出します。

ルートディレクトリ `budget-app-handson`でディレクトリを作成します。

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

プロジェクトルートの `.claude/settings.json` を作成し、環境変数 `$CLAUDE_PROJECT_DIR` を使って、上記のスクリプトを呼び出す設定を書きます。

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

- `$CLAUDE_PROJECT_DIR` の利用：セッション中にカレントディレクトリが変わっても、常にプロジェクトルートを基準に `backend/` と `frontend/` のパスを正しく解決します。
- `stderr` へのエラー出力と終了コード `2`：Claude Code のフックは、終了コード `2` を返すと、**標準エラー出力の `stderr`** に書き込まれた内容を AI にフィードバックします。スクリプト内で `stdout` と `stderr` の両方を結合して `process.stderr.write` に流すことで、Gradle や `tsc` のエラー詳細が確実に Claude Code に伝わり、AI が自分で修正できます。
- `cleanTest` を付ける理由：Konsist は Kotlin のソースファイルを直接読みますが、Gradle はソースファイルを `test` タスクの入力として扱いません。`import` 文だけを追加してバイトコードが変わらない場合、`test` が `UP-TO-DATE` と判定されて Konsist が実行されず、違反を見逃します。`cleanTest` で毎回再実行させます。
- 実行時間に関する補足：対象テストを `ArchitectureTest` のみに絞っていますが、JVM 上で動く Gradle の起動には数秒〜十数秒かかります。
- 実装途中の失敗に関する補足：Output Port にメソッドを追加した直後など、実装クラスが未対応の間はコンパイルエラーでフックが失敗します。これは想定内の挙動で、AI は続けて実装クラスを修正するため、最終的にフックが成功すれば問題ありません。
- `Stop` への変更について：`Stop` は応答完了時に1回だけ実行されます。`Stop` に変更する場合、`Stop` イベントの入力には `tool_input.file_path` が含まれないため、このスクリプトのままでは何も検証されません。その場合はスクリプトを書き換え、拡張子による分岐をやめて `backend` と `frontend` の両方の検証を無条件に実行してください。

---

## 7-3. 実践演習：`/add-feature` スキルを使った「明細更新機能」の実装

ここまでで、設計ルールの `CLAUDE.md` と `.claude/rules/`、実装手順スキルの `add-feature`、そして自動検証フックの `PostToolUse` がすべて揃いました。

実際に Claude Code を起動し、新しいユースケースとして「登録済みの家計簿明細を編集・更新する機能」を追加します。進め方は Chapter 3〜6 と同じで、CP を1つ終えるたびに Claude Code が停止するので、確認して「次のチェックポイントに進む」と入力します。

### 1. Claude Code の起動、または設定の再読み込み

ルートディレクトリ `budget-app-handson`で Claude Code を起動します。すでに起動中の場合は、いったん `/exit` で終了して再起動すると、新しいスキルとフックを確実に読み込めます。

```bash
claude
```

### 2. `/add-feature` スキルの実行

チャット欄に以下のコマンドを入力して実行します。Claude Code は CP7-1 の設計だけを実施して停止します。

```text
/add-feature 家計簿明細の編集（更新）機能を追加してください。バックエンドには PUT /api/transactions/{id} エンドポイント（UpdateTransactionUseCase / UpdateTransactionInteractor）を追加し、存在しないカテゴリIDや存在しない明細IDの場合は DomainException をスローしてください。フロントエンドでは明細一覧の各行に「編集」ボタンを追加し、クリックするとその明細の内容を編集して保存できるUIを実装してください。
```

### 3. チェックポイントごとの確認

各 CP で Claude Code が停止したら、次の内容を確認して「次のチェックポイントに進む」と入力します。実装系の CP である CP7-2 と CP7-4 では、ファイルを作成・編集するたびに `PostToolUse` フックが起動し、`.kt` に対する `ArchitectureTest` と、`.ts` / `.tsx` に対する `tsc --noEmit` が自動実行されます。

#### CP7-1 〔設計〕実装計画の提示

Claude Code は、変更対象となる全層のファイル一覧を提示して停止します。コードはまだ書かれません。

- バックエンドの `domain` / `usecase` / `adapter`、フロントエンドの `domain` / `usecase` / `adapter` / `presentation` のすべてが一覧に含まれていること
- Input Port である `UpdateTransactionUseCase`、`UpdateTransactionInteractor`、`save` による更新を行う `TransactionRepository` の利用が計画されていること
- Controller が `usecase/input` のみに依存する計画になっていること

#### CP7-2 〔実装〕バックエンドの実装

- 内側の層から順にファイルが作成・編集され、`UpdateTransactionInteractor` が、最初に `transactionRepository.findById(input.id)` と `categoryRepository.findById(input.categoryId)` で対象の存在を検証し、存在しない場合に `DomainException` をスローしていること
- 存在が確認された場合のみ、既存の `id` を保持した `Transaction` を構築して `transactionRepository.save()` を呼び出していること
- フックが失敗した場合は、Claude Code が自動で修正して再実行していること。途中の一時的な失敗は想定内です
- `compileKotlin` が成功していること

#### CP7-3 〔テスト〕バックエンドのテスト

- MockK を使った `UpdateTransactionInteractorTest` と、`PUT /api/transactions/{id}` の MockMvc テストが追加されていること
- 既存の `TransactionControllerTest` の `TestConfig` に `UpdateTransactionUseCase` の Bean が追加されていること
- `./gradlew test` が、`ArchitectureTest` を含めてすべてパスしていること

#### CP7-4 〔実装〕フロントエンドの実装

- 内側の層から順に、型・`TransactionRepository.updateTransaction`・`useTransactions`・`transactionApi`・編集 UI が実装されていること
- `useTransactions.ts` が `adapter` を import していないこと
- `tsc --noEmit` が成功していること

#### CP7-5 〔テスト〕フロントエンドのテスト

- MSW ハンドラーに `PUT` が追加され、`TransactionSection.test.tsx` に編集のテストケースが追加されていること
- `pnpm vitest run` と `pnpm build` が成功していること

#### CP7-6 〔動作確認〕更新 API の動作確認

- `PUT /api/transactions/{id}` が成功し、`GET /api/transactions` の内容と `summary` に更新が反映されること
- 存在しない明細 ID、存在しないカテゴリ ID で更新すると 400 とエラーメッセージが返ること

#### CP7-7 〔動作確認〕画面での最終確認と完了報告

Claude Code がバックエンドとフロントエンドを起動し、全テストの結果を報告して待機します。ブラウザで `http://localhost:3000/transactions` を開き、次の操作を確認してください。

- 新規登録 → 「編集」で金額やカテゴリを変更して保存 → 一覧と「総収入・総支出・現在の残高」が更新後の内容に連動すること
- 編集した明細を「削除」すると、一覧とサマリーから消えること
- Claude Code の報告で、`./gradlew test` と `pnpm vitest run` の全テストがパスしていること

確認が終わったら、Claude Code に「起動中のサーバーをすべて停止して」と指示します。

---

## 7-4. ハンズオンのまとめ

本ハンズオンでは、シンプル家計簿アプリの構築を通じて以下の設計原則と開発ワークフローを実践しました。

1. 関心事の分離と4層構造
   ビジネスルールの `domain`、アプリケーション手順の `usecase`、入出力変換の `adapter`、技術詳細の `framework` / `app` を明確に分離しました。
2. 依存性逆転の原則、つまり DIP
   Use Case 層に `TransactionRepository` インターフェースを定義し、外側の実装クラスや API クライアントを注入することで、内側のコードが外部技術に依存しない構造にしました。
3. 多層的な自動テストとアーキテクチャ検証
   JUnit 5、MockK、MockMvc、Vitest、MSW に加え、Konsist によるアーキテクチャテストを導入し、依存性ルール違反を自動で防ぐ仕組みを作りました。
4. AI 開発におけるガードレール設計
   `CLAUDE.md`、`.claude/rules/`、Agent Skills の `SKILL.md`、Hooks の `settings.json`を組み合わせることで、AI エージェントが設計規約を守りながら、自分で実装と検証を進められる開発環境を整えました。
