# Chapter 2: Claude Code ルール設定と環境構築

## このチャプターの目的

このチャプターでは、人と AI である Claude Code の役割を明確に分けて開発環境を構築します。

- 2-0 は人が実施します。Homebrew、Docker 互換の Rancher Desktop、mise をシステムにインストールします。
- 2-1 は人が実施します。作業ディレクトリを作成し、mise で言語・ランタイムを導入し、環境構築スキルの `SKILL.md` を作成し、クリーンアーキテクチャの設計ルールである `CLAUDE.md` と `.claude/rules/` を配置します。
- 2-2 は AI が実施します。人が作成したスキルとルールを Claude Code に読み込ませ、PostgreSQL 18・Spring Boot 4・Next.js 16 の環境構築から初期ビルド、マイグレーションの確認まで、途中で止めずに最後まで AI に実行させます。なお、Chapter 2 にチェックポイントはありません。

---

## 2-0. 前提ツールのインストール：人が実施

本ハンズオンでは、コンテナの実行環境に Docker 互換の Rancher Desktop を、Java・Node.js・pnpm のバージョン管理に mise を使います。

### 1. Homebrew のインストール確認

以降のインストールには Homebrew を使います。次のコマンドで、インストール済みか確認します。

```bash
brew --version
```

`command not found` と表示された場合は、Homebrew 公式サイト https://brew.sh/ の手順でインストールします。

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

インストール完了時に表示される「Next steps」のコマンド、例えば `brew` を PATH に追加する `eval "$(/opt/homebrew/bin/brew shellenv)"` を実行し、もう一度 `brew --version` を実行し、バージョンが表示されることを確認します。

### 2. Rancher Desktop のインストール

Rancher Desktop は Docker CLI として `docker` と `docker compose` を同梱しており、コンテナエンジンに `dockerd (moby)` を選択すると Docker Desktop と同じコマンドで操作できます。

```bash
brew install --cask rancher
```

インストール後、Rancher Desktop を起動し、初回設定で以下を選択します。

- Container Engine: `dockerd (moby)`
- Configure PATH: `Automatic`

起動完了後、次のコマンドで動作を確認します。

```bash
docker --version
docker compose version
```

### 3. mise のインストール

```bash
brew install mise
echo 'eval "$(mise activate zsh)"' >> ~/.zshrc  # bash / fish の場合はシェルに合わせて読み替える
exec $SHELL -l
```

---

## 2-1. プロジェクト初期化と Claude Code 設定の作成：人が実施

ここからは、ターミナルとエディタを使って、上から順に自分で進めます。

### 1. 作業用ルートディレクトリの作成

ターミナルを開き、プロジェクト全体を置くルートディレクトリと、`.claude` 配下の設定用ディレクトリを作成し、ルートディレクトリに移動します。

```bash
mkdir -p budget-app-handson/.claude/skills/setup-env
mkdir -p budget-app-handson/.claude/rules
cd budget-app-handson
```

勉強会で配布された `.env.example` を、このルートディレクトリ `budget-app-handson` 直下に置きます。DB のユーザー名・パスワード・DB 名といった接続情報が書かれたファイルで、Step 2 で `.env` にコピーまたはリネームして使います。

```text
DB_NAME=budget_db
DB_USERNAME=budget_user
DB_PASSWORD=budget_password
```

`.env.example` の値は勉強会専用です。自分のプロジェクトでは、パスワードを推測されにくい値に変え、`.env` を Git にコミットしないでください。

### 2. `mise.toml` による Java 25 / Node.js 26 / pnpm 11 の導入

ルートディレクトリ `budget-app-handson` 直下に `mise.toml` を作成し、プロジェクトで使うツールのバージョンを固定し、インストールします。

```bash
cat > mise.toml <<'EOF'
[tools]
java = "temurin-25"
node = "26"
pnpm = "11"
EOF
mise trust
mise install
```

次のコマンドで、Java 25・Node.js 26・pnpm 11 が有効になっていることを確認します。

```bash
java -version   # 25.x
node -v         # v26.x
pnpm -v         # 11.x
```

### 3. 環境構築スキル `.claude/skills/setup-env/SKILL.md` の作成

次の「2-2」で Claude Code に環境構築を実行させるためのスキルファイルを作成します。

`.claude/skills/setup-env/SKILL.md` を新規作成し、以下の内容を記述してください。

````markdown
---
name: setup-env
description: Budget App（PostgreSQL 18, Spring Boot 4, Next.js 16）のローカル開発環境を構築し、起動確認までを実行するスキル
disable-model-invocation: true
---

# Budget App 環境構築手順

ユーザーから `/setup-env` で呼び出された場合、すべてのコマンドはプロジェクトルート（`budget-app-handson`）を基準に実行し、以下の Step を順に進めて開発環境を構築してください。

Chapter 2 の環境構築にはチェックポイント（CP）を設けない。CLAUDE.md の「チェックポイント運用」の例外として、Step ごとに停止せず、最後まで連続して実行すること。中断するのは、前提ツールの不足・ポートの競合・既存ファイルとの差分の確認が必要なときだけで、中断した場合は理由を報告してユーザーの判断を待つ。

## 冪等性の原則
`/setup-env` は何度実行しても成功し、2 回目以降に既存の成果物を壊さないこと。各手順は「現状を確認し、足りない分だけ実行する」。
- 作成済みのファイル・ディレクトリは上書きしない。内容が手順と異なる場合も上書きせず、差分を報告してユーザーの判断を待つ（後の章で編集したファイルを壊さないため）。
- すでに目的の状態になっている手順は実行せずスキップする。スキップはエラーではなく、成功として扱う。
- 最後の完了報告に、実行した手順とスキップした手順（理由つき）を分けて書く。

## Step 1 事前確認
1. 現在の作業ディレクトリがプロジェクトのルート（`budget-app-handson`）であることを確認する。
2. `docker --version`（Rancher Desktop の dockerd）、`java -version`（Java 25 必須）、`node -v`（v26）、`pnpm -v`（v11）が利用可能か確認する。Java が 25 でない場合や前提ツールが未インストールの場合は中断し、ユーザーに「2-0 / 2-1 の前提ツール導入」を案内する。
3. 使用するポート（PostgreSQL: 5432、Spring Boot: 8080、Next.js: 3000）が他のプロセスに使われていないか `lsof -nP -iTCP:<ポート> -sTCP:LISTEN` で確認する。
   - 5432 を占有しているのが、このプロジェクト自身のコンテナ `budget-app-db`（`docker ps` で確認）である場合は、再実行とみなして中断せず、Step 2 へ進む。
   - それ以外でいずれかのポートがすでに使用されている場合は、既存のプロセスやコンテナを勝手に終了・削除せず、占有しているプロセス情報（PID・コマンド名・ポート番号）をユーザーに報告して処理を中断する。
   - ユーザー自身で該当プロセスを停止した後に、再度 `/setup-env` を実行するよう案内する。

## Step 2 PostgreSQL 18 の構築と起動
1. ルートディレクトリ直下に、配布された `.env.example` があることを確認する。`.env` が無ければ `cp .env.example .env` で作成する（`.env` が既にある場合は上書きしない）。`.env.example` が無い場合は中断し、ユーザーにファイルの配置を依頼する。DB のユーザー名・パスワード・DB 名はこの `.env` にだけ置き、`compose.yaml` や `application.yml` には書かない。
2. Git で管理している場合は、`.gitignore` に `.env` が含まれていることを確認し、無ければ追記する。
3. ルートディレクトリ直下に `compose.yaml` を以下の内容で作成する。すでに存在して内容が同じならスキップする。内容が異なる場合は上書きせず、差分を報告して確認を待つ。

```yaml
services:
  db:
    image: postgres:18
    container_name: budget-app-db
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: ${DB_USERNAME:?Set DB_USERNAME in .env}
      POSTGRES_PASSWORD: ${DB_PASSWORD:?Set DB_PASSWORD in .env}
      POSTGRES_DB: ${DB_NAME:?Set DB_NAME in .env}
    volumes:
      - postgres_data:/var/lib/postgresql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME} -d ${DB_NAME}"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

4. `docker compose up -d` を実行する（コンテナがすでに稼働中なら、何も変更されない）。
5. `docker compose ps` を実行し、`budget-app-db` の状態が `healthy` になるまで待機・確認する。

## Step 3 バックエンド（Spring Boot 4 / Kotlin）のセットアップ
1. `backend/build.gradle.kts` がすでに存在する場合は、この手順をスキップする（`tar` が既存のソースを上書きするため、実行してはならない）。存在しない場合は、ルートディレクトリで以下のコマンドを実行し、`backend` ディレクトリを生成する（Spring Initializr から最新の Spring Boot 4.x を取得する）。

```bash
curl -fsSL https://start.spring.io/starter.tgz \
  -d type=gradle-project-kotlin \
  -d language=kotlin \
  -d javaVersion=25 \
  -d dependencies=web,data-jpa,validation,flyway,postgresql \
  -d groupId=com.example \
  -d artifactId=budgetapp \
  -d packageName=com.example.budgetapp \
  -d baseDir=backend \
  | tar -xzf -
```

2. `backend/build.gradle.kts` の `dependencies` ブロックを確認し、Flyway の PostgreSQL モジュールとテスト用ライブラリが含まれるよう以下を追記する（すでに記載されている行は重複させない）。

```kotlin
	runtimeOnly("org.flywaydb:flyway-database-postgresql")
	testImplementation("io.mockk:mockk:1.14.11")
	testImplementation("com.lemonappdev:konsist:0.18.1")
```

3. `backend/src/main/resources/application.properties` が存在する場合のみ削除し、`backend/src/main/resources/application.yml` を作成する。`application.yml` がすでに存在する場合は上書きせず、DB 接続情報が `${DB_NAME}` などの変数参照になっていることだけを確認する（認証情報が直書きなら報告する）。

```yaml
spring:
  application:
    name: budgetapp
  config:
    import: optional:file:../.env[.properties]  # DB 接続情報はルートの .env から読む（Gradle の実行ディレクトリは backend）
  datasource:
    url: jdbc:postgresql://localhost:5432/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
```

4. `backend/src/main/resources/db/migration/V1__create_tables.sql` が存在しない場合のみ作成する。存在する場合は変更しない（適用済みの Flyway マイグレーションを変更すると、チェックサム不一致で起動に失敗する）。

```sql
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE'))
);

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    amount INT NOT NULL CHECK (amount >= 1),
    transaction_date DATE NOT NULL,
    memo TEXT,
    CONSTRAINT fk_categories FOREIGN KEY (category_id) REFERENCES categories(id)
);

INSERT INTO categories (name, type) VALUES ('食費', 'EXPENSE');
INSERT INTO categories (name, type) VALUES ('日用品', 'EXPENSE');
INSERT INTO categories (name, type) VALUES ('交通費', 'EXPENSE');
INSERT INTO categories (name, type) VALUES ('給与', 'INCOME');
INSERT INTO categories (name, type) VALUES ('副業', 'INCOME');
```

5. サブシェル `(cd backend && ./gradlew test)` を実行してビルドと初期テストの成功を確認する。
6. 8080 が使用中でないことを確認したうえで、サブシェル `(cd backend && ./gradlew bootRun)` でバックエンドを一時起動し、起動完了後にプロセスを停止する（`lsof` で 8080 にプロセスが残っていないことを確認する）。マイグレーションは適用済みなら再実行されない。その後、以下のコマンドで Flyway によるテーブル作成と初期データ投入を直接確認する。

```bash
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "\dt"'
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT * FROM categories;"'
```

## Step 4 フロントエンド（Next.js 16 / pnpm 11）のセットアップ
1. ルートディレクトリの `mise.toml` に `node = "26"` と `pnpm = "11"` が定義されていることを確認する。
2. `frontend/package.json` がすでに存在する場合は、この手順（生成と `.git` の削除）をスキップする（`create-next-app` は空でないディレクトリに生成できない）。存在しない場合は、非対話オプションを指定してルートディレクトリ直下に `frontend` プロジェクトを生成する。

```bash
pnpm create next-app@latest frontend \
  --ts \
  --tailwind \
  --eslint \
  --app \
  --src-dir \
  --import-alias "@/*" \
  --use-pnpm \
  --yes
```

   `create-next-app` は `frontend/.git` を生成するため、モノレポのルートで一括管理できるよう、生成した場合に限り `rm -rf frontend/.git` で削除する。

3. `frontend/node_modules` が存在しない場合は、先に `pnpm --dir frontend install` で依存パッケージをインストールする（ブランチ切り替え直後など、`package.json` だけがある状態に対応する）。続いて、`frontend/package.json` の `devDependencies` を確認し、未導入のパッケージだけを追加する（導入済みのパッケージを再度 `add` して、バージョンを更新しない）。カレントディレクトリはルートに保ったまま、以下を実行する。

```bash
pnpm --dir frontend add -D vitest @vitejs/plugin-react jsdom @testing-library/react @testing-library/dom @testing-library/jest-dom msw
```

4. クリーンアーキテクチャ構成用のディレクトリを作成する。

```bash
mkdir -p frontend/src/components/ui \
  frontend/src/features/transaction/domain \
  frontend/src/features/transaction/usecase \
  frontend/src/features/transaction/adapter \
  frontend/src/features/transaction/presentation/utils \
  frontend/src/mocks
```

5. `pnpm --dir frontend build` を実行し、Next.js のビルドがエラーなく完了することを確認する。

## Step 5 完了報告
すべての Step が完了したら、以下の項目を確認結果とともにユーザーへ報告する。
- Docker コンテナ `budget-app-db` の稼働状況
- Spring Boot プロジェクトの生成と `categories` / `transactions` テーブルおよび初期マスタデータの作成結果
- Next.js プロジェクトの生成とディレクトリ構成・ビルド完了結果
- 実行した手順とスキップした手順の一覧（再実行の場合は、スキップした理由を含める）
````

### 4. プロジェクト共通ガイドライン `CLAUDE.md` の作成

Claude Code がセッション開始時に自動で読み込む共通ルールファイルを作成します。
ルートディレクトリ直下に `CLAUDE.md` を新規作成し、以下の内容を記述してください。

```markdown
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
作業はチェックポイント（CP）単位で進める。ただし、Chapter 2 の `/setup-env`（環境構築）は CP を設けず、1回の指示で最後まで連続して実行する。
1. 1回の指示で実施するのは1つの CP だけ。複数の CP を連続して実行しない。
2. CP が完了したら「CPx-y 完了」と宣言し、作成・変更したファイル、実行したコマンドとその結果、次の CP の名前を報告して必ず停止する。
3. ユーザーが「次のチェックポイントに進む」と入力するまで、次の CP に着手しない。
4. 検証コマンドが失敗した場合は、その CP の中で原因を修正して再実行し、成功してから完了を報告する。失敗を次の CP に持ち越さない。
5. CP に書かれていないファイルは作成しない。
```

### 5. 領域別アーキテクチャ規約 `.claude/rules/` の作成

対象ディレクトリのファイルを編集するときだけ自動で適用される、詳細なルールを作成します。

#### `.claude/rules/backend.md` の作成

```markdown
---
paths:
  - "backend/**/*.kt"
---

# バックエンド（Spring Boot / Kotlin）アーキテクチャ規約

パッケージ `com.example.budgetapp` 配下は以下の4層構造に従います。

## 4層のパッケージ構成と依存制約

1. `domain`（第1層：Entities）
   - 値オブジェクト（`Amount`）、エンティティ（`Transaction`, `Category`）、ドメインサービス（`BalanceSummary`）、独自例外（`DomainException`）を配置する。
   - 禁止事項: `org.springframework.*`、`jakarta.persistence.*`、および `usecase`・`adapter`・`framework` パッケージの import を一切禁止する。

2. `usecase`（第2層：Use Cases）
   - `usecase.input`: Input Port インターフェース（`*UseCase`）と入力データクラスを配置する。
   - `usecase.output`: Output Port インターフェース（`*Repository`）を配置する。
   - `usecase.interactor`: `*UseCase` を実装する `*Interactor` クラスを配置する。トランザクション境界（`@Transactional`）は Interactor に付与する。
   - 禁止事項: `adapter`・`framework` パッケージおよび `jakarta.persistence.*` の import を禁止する。

3. `adapter`（第3層：Interface Adapters）
   - `adapter.controller`: REST Controller、リクエスト/レスポンス DTO、`GlobalExceptionHandler` を配置する。
   - `adapter.repository`: `usecase.output.*Repository` を実装する `*RepositoryImpl`、JPA Entity（`adapter.repository.entity`）、Spring Data JPA インターフェース（`adapter.repository.jpa`）を配置し、JPA Entity とドメインモデルの相互変換を行う。
   - 禁止事項: 外側の第4層（`framework` パッケージ）の import を一切禁止する。また、`adapter.controller` は `usecase.input` のみを受け取り、`usecase.output` や `adapter.repository`（JPA）を直接呼び出してはならない。

4. `framework`（第4層：Frameworks & Drivers）
   - `framework.config`: CORS などのフレームワーク全体設定クラスを配置する。
```

#### `.claude/rules/frontend.md` の作成

```markdown
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
```

ここまでで人による準備は完了です。

---

## 2-2. 開発環境の構築と動作確認：AI が実施

ここからは、ルートディレクトリ `budget-app-handson`で Claude Code を起動し、AI に環境構築を実行させます。

### 1. Claude Code の起動とルールの読み込み確認

ターミナルで Claude Code を起動します。

```bash
claude
```

起動後、チャット欄に以下のプロンプトを入力し、`2-1` で作成したルールとスキルが認識されているか確認します。

```text
CLAUDE.md と .claude/rules/ に定義された、バックエンドとフロントエンドの4層構造と依存禁止ルールを要約してください。また、利用可能なスキルを確認してください。
```

回答に4層構造のルールと `setup-env` スキルが含まれていれば、準備完了です。

### 2. `/setup-env` スキルによる環境構築の実行

続いて、Claude Code のチャット欄に以下のスキルコマンドを入力し、AI に環境構築を実行させます。

```text
/setup-env
```

Claude Code は `.claude/skills/setup-env/SKILL.md` の手順に従い、次の Step を順に最後まで実行して、完了報告を出します。Step ごとには停止しません。コマンド実行の許可を求められた場合は内容を確認して承認してください。

| Step | 内容 |
| :--- | :--- |
| Step 1 | 前提ツールのバージョンと、ポート 5432, 8080, 3000 の空き状況の確認 |
| Step 2 | `.env` の作成、`compose.yaml` の生成、Docker による PostgreSQL 18 の起動・ヘルスチェック |
| Step 3 | Spring Initializr からの `backend` 生成、依存ライブラリの追加、`.env` を参照する `application.yml` と Flyway SQL の配置、`psql` によるテーブルと初期マスタデータの確認 |
| Step 4 | `mise.toml` の確認、`frontend` の生成、テストライブラリの導入、4層用ディレクトリの作成、ビルド確認 |
| Step 5 | 実行した手順とスキップした手順の一覧による完了報告 |

途中で止まるのは、前提ツールの不足・ポートの競合・既存ファイルとの差分の確認が必要なときだけです。止まった場合は、報告された理由を確認して対処し、もう一度 `/setup-env` を実行します。再実行しても問題ありません。

### 3. AI による構築完了後の確認ポイント

完了報告が出たら、次の状態になっているか確認してください。

- ルートディレクトリ直下に `.env`、`compose.yaml`、`backend/`、`frontend/` が作成されていること。
- `.env` が Git で追跡されていないこと。`git status` に表示されなければ追跡されていません。
- PostgreSQL 18 コンテナ `budget-app-db` が稼働中であり、初期データ5件を持つ `categories` テーブルと `transactions` テーブルが作成されていること。
- バックエンドの `./gradlew test` およびフロントエンドの `pnpm --dir frontend build` がエラーなく完了していること。

これで開発環境と AI 開発基盤のセットアップはすべて完了です。次の Chapter 3 以降も、Claude Code にアーキテクチャ規約を守らせながら、実装とテストを進めていきます。
