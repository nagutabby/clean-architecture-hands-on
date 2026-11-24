# クリーンアーキテクチャ実践ハンズオン

Claude Code に設計規約を守らせながら、Spring Boot（Kotlin）と Next.js で家計簿アプリを 4 層構成で作るハンズオンです。

English readers can start from [docs/en/](docs/en/). The chapters and slides are translated, but the sample app's UI text is Japanese.

## 作るもの

シンプルな家計簿アプリ（Budget App）です。収入・支出の明細を登録・編集・削除し、総収入・総支出・残高を表示します。

| 領域 | 技術 |
| :--- | :--- |
| バックエンド | Kotlin 2、Java 25、Spring Boot 4、PostgreSQL 18、Flyway |
| フロントエンド | Next.js 16（App Router）、React 19、TypeScript、Tailwind CSS 4、pnpm 11、Node.js 26 |
| テスト | JUnit 5、MockK、MockMvc、Konsist、Vitest、Testing Library、MSW |
| 環境 | Rancher Desktop（Docker 互換）、mise |
| AI 開発支援 | Claude Code（CLAUDE.md、rules、Skills、Hooks） |

学ぶことは次の 4 点です。

- バックエンド・フロントエンドとも、domain / usecase / adapter / framework の 4 層に分けること
- 依存の向きを外側から内側へ一方向に保つこと
- usecase にインターフェースを置き、実装を外側から注入すること（依存性逆転）
- 規約をテストとフックで自動検証し、AI に守らせること

## 事前に必要なもの

- macOS（手順は macOS を前提にしています）
- Homebrew
- Rancher Desktop、mise（Chapter 2 でインストール手順を説明します）
- Claude Code

## docs の構成

ドキュメントは言語ごとに分かれています。

| 言語 | 章 | スライド |
| :--- | :--- | :--- |
| 日本語 | [docs/ja/](docs/ja/) | [docs/ja/slides.html](docs/ja/slides.html) |
| English | [docs/en/](docs/en/) | [docs/en/slides.html](docs/en/slides.html) |

### 章の一覧

どちらの言語にも、次の 8 章があります（日本語版へのリンクです）。

| 章 | 内容 | 実施者 |
| :--- | :--- | :--- |
| [Chapter 0](docs/ja/chapter-00-clean-architecture-basics.md) | クリーンアーキテクチャの基礎 | 読む |
| [Chapter 1](docs/ja/chapter-01-database-and-structure.md) | データベース設計とプロジェクト全体構成 | 読む |
| [Chapter 2](docs/ja/chapter-02-setup-and-claude-rules.md) | Claude Code のルール設定と環境構築 | 人と AI |
| [Chapter 3](docs/ja/chapter-03-backend-implementation.md) | バックエンド実装 | AI |
| [Chapter 4](docs/ja/chapter-04-backend-testing.md) | バックエンドのテスト | AI |
| [Chapter 5](docs/ja/chapter-05-frontend-implementation.md) | フロントエンド実装 | AI |
| [Chapter 6](docs/ja/chapter-06-frontend-testing.md) | フロントエンドのテスト | AI |
| [Chapter 7](docs/ja/chapter-07-claude-code-skills.md) | Skills と Hooks による機能追加（明細の編集） | 人と AI |

Chapter 0 と 1 は読み物です。Chapter 2 以降は、手を動かしながら進めます。

### 各章の読み方

Chapter 3 以降の各章は、次の順に書かれています。

1. 冒頭の表に、その章のチェックポイント（CP）の一覧があります。
2. 「開始プロンプト」を Claude Code に入力します。Claude Code は最初の CP だけを実施して停止します。
3. 各 CP の節にある実装コード例と「確認ポイント」で、結果を確認します。
4. 問題がなければ、Claude Code に「次のチェックポイントに進む」と入力します。次の CP が 1 つだけ実施されます。
5. 問題があれば、進めずに「CPx-y をやり直して」と指示します。

実装コード例は正解の参照用です。Claude Code の出力が大きくずれたときは、コード例と見比べて修正を指示してください。

## チェックポイント運用

作業は「設計、実装、テスト、動作確認」のチェックポイントに区切って進めます。

| 種別 | AI が行うこと |
| :--- | :--- |
| 設計 | コードを書かず、クラス構成・依存方向・API を提示する |
| 実装 | 1 つの層（または部品）を実装し、コンパイルや型チェックを行う |
| テスト | テストを追加して実行し、規約違反を検査する |
| 動作確認 | サーバーを起動し、機能ごとに API と画面を確認する |

この運用ルールは、Chapter 2 で作成する `CLAUDE.md` の「チェックポイント運用」に書かれています。

## チェックポイントごとのブランチ

各チェックポイントを終えた時点のプロジェクトが、`checkpoint/` から始まるブランチに残っています。履歴は 1 本の線で、後のブランチは前のブランチの続きです。

| ブランチ | 内容 |
| :--- | :--- |
| `checkpoint/start` | ドキュメントだけの状態（プロジェクトなし） |
| `checkpoint/cp2-0` | 人が行う準備が終わった状態（`mise.toml`、`CLAUDE.md`、rules、`setup-env` スキル） |
| `checkpoint/cp2-1` 〜 `cp2-5` | Chapter 2 の環境構築 |
| `checkpoint/cp3-1` 〜 `cp3-10` | Chapter 3 のバックエンド実装 |
| `checkpoint/cp4-1` 〜 `cp4-7` | Chapter 4 のバックエンドのテスト |
| `checkpoint/cp5-1` 〜 `cp5-12` | Chapter 5 のフロントエンド実装 |
| `checkpoint/cp6-1` 〜 `cp6-7` | Chapter 6 のフロントエンドのテスト |
| `checkpoint/cp7-0` | 人が行う準備が終わった状態（`add-usecase` スキル、フック） |
| `checkpoint/cp7-1` 〜 `cp7-7` | Chapter 7 の更新機能 |

- 設計の CP などファイルの変更がない CP は、直前のブランチと同じ内容の空コミットです。
- 先に進めなくなったときに、該当ブランチの状態と自分の作業を見比べたり、そこから再開したりできます。
- ブランチを切り替えたあとは、依存パッケージが入っていないので、`cd frontend && pnpm install` を実行してください。PostgreSQL は `docker compose up -d` で起動します（`checkpoint/cp2-2` 以降）。
- `main` ブランチのプロジェクトは、Chapter 6 までを終えた状態です。明細の編集機能（Chapter 7）まで含めた最終状態は `checkpoint/cp7-7` です。

## スライド

講義用のスライドは単体の HTML ファイルです。ブラウザで開いてください。

- ← → キー、Space キー、画面クリックでページを移動できます
- URL の末尾に `#10` のようにページ番号を付けると、そのページから開けます
- 印刷（Ctrl/⌘+P）から PDF に保存できます。余白は「なし」、背景のグラフィックは「オン」にしてください
- 日本語版のフォントは Noto Sans JP です。インターネットに接続できない環境では、別のフォントで表示されます

## リポジトリの構成

```text
docs/
  README.md      docs の索引
  ja/            日本語版の章とスライド
  en/            英語版の章とスライド
backend/         Spring Boot（Kotlin）のプロジェクト（Chapter 6 まで終えた状態）
frontend/        Next.js のプロジェクト（Chapter 6 まで終えた状態）
.claude/         Claude Code の設定（rules、skills、hooks）
CLAUDE.md        Claude Code への共通ガイドライン
compose.yaml     PostgreSQL 18 の定義
.env.example     DB 接続情報のひな形（`.env` にコピーして使う。`.env` はコミットしない）
mise.toml        Java・Node.js・pnpm のバージョン定義
```

`backend/` と `frontend/` は、`main` では Chapter 6 まで終えた状態です。ハンズオンでは、このリポジトリのコードを使わず、自分の作業ディレクトリで同じものを作ります。

## 動作確認の方法

先に `cp .env.example .env` で DB 接続情報の `.env` を作成します。続いて、PostgreSQL、バックエンド、フロントエンドの順に起動します。

```bash
cp .env.example .env
docker compose up -d
cd backend && ./gradlew bootRun
cd frontend && pnpm install && pnpm dev
```

ブラウザで http://localhost:3000/transactions を開いて、明細の登録と削除を確認します。編集機能を確認する場合は、先に `git switch checkpoint/cp7-7` で最終状態に切り替えてください。テストは次のコマンドで実行できます。

```bash
cd backend && ./gradlew test
cd frontend && pnpm vitest run
```
