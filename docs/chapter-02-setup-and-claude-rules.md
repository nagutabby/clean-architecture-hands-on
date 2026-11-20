# Chapter 2: Claude Code ルール設定と環境構築

## このチャプターの目的

このチャプターでは、人と AI（Claude Code）の役割を明確に分けて開発環境を構築します。

- 2-0（人が実施）: Rancher Desktop（Docker 互換）と mise をインストールし、Java 25・Node.js 26・pnpm 11 を揃えます。
- 2-1（人が実施）: 作業ディレクトリの作成、環境構築スキル（`SKILL.md`）の作成、およびクリーンアーキテクチャの設計ルール（`CLAUDE.md` / `.claude/rules/`）の配置を行います。
- 2-2（AI が実施）: 人が作成したスキルとルールを Claude Code に読み込ませ、PostgreSQL 18・Spring Boot 4・Next.js 16 の環境構築から初期ビルド確認までを AI に実行させます。以降のチャプターでも同様に Claude Code を活用して実装とテストを進めます。

---

## 2-0. 前提ツールのインストール（人が実施）

本ハンズオンでは、コンテナ実行環境に Docker Desktop ではなく、Docker 互換の Rancher Desktop を使用します。また、Java・Node.js・pnpm のバージョン管理には mise を使用します。

### 1. Rancher Desktop のインストール

Rancher Desktop は Docker CLI（`docker` / `docker compose`）を同梱しており、コンテナエンジンに `dockerd (moby)` を選択すると Docker Desktop と同じコマンドで操作できます。

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

### 2. mise のインストール

```bash
brew install mise
echo 'eval "$(mise activate zsh)"' >> ~/.zshrc  # bash / fish の場合はシェルに合わせて読み替える
exec $SHELL -l
```

### 3. Java 25 / Node.js 26 / pnpm 11 の導入

「2-1. 1」で作成するルートディレクトリ（`budget-app-handson`）で、ツールのバージョンを `mise.toml` に固定してインストールします。2-1 の 1 の直後に実施してください。

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

---

## 2-1. Claude Code スキルと設計ルールの作成（人が実施）

ここからの手順は、学習者自身がターミナルおよびエディタで実施します。

### 1. 作業用ルートディレクトリの作成

ターミナルを開き、プロジェクト全体を格納するルートディレクトリと `.claude` 配下の設定用ディレクトリを作成します。

```bash
mkdir -p budget-app-handson/.claude/skills/setup-env
mkdir -p budget-app-handson/.claude/rules
cd budget-app-handson
```

ここで 2-0 の 3 に進み、`mise.toml` の作成と `mise install` を実施してから、次の手順に進んでください。

### 2. 環境構築スキル（`.claude/skills/setup-env/SKILL.md`）の作成

最初に、次の「2-2」で Claude Code に環境構築を実行させるためのスキルファイルを作成します。

`.claude/skills/setup-env/SKILL.md` を新規作成し、以下の内容を記述してください。

````markdown
---
name: setup-env
description: Budget App（PostgreSQL 18, Spring Boot 4, Next.js 16）のローカル開発環境を構築し、起動確認までを実行するスキル
disable-model-invocation: true
---

# Budget App 環境構築手順

ユーザーから `/setup-env` で呼び出された場合、以下の手順を上から順に実行し、各ステップの完了を確認しながら開発環境を構築してください。

## 事前確認
1. 現在の作業ディレクトリがプロジェクトのルート（`budget-app-handson`）であることを確認する。
2. `docker --version`（Rancher Desktop の dockerd）、`java -version`（Java 25 必須）、`node -v`（v26）、`pnpm -v`（v11）が利用可能か確認する。Java が 25 でない場合や前提ツールが未インストールの場合は中断し、ユーザーに「2-0. 前提ツールのインストール」（mise による導入）を案内する。

## Step 1: PostgreSQL 18 の構築と起動
1. ルートディレクトリ直下に `compose.yaml` を以下の内容で作成する。

```yaml
services:
  db:
    image: postgres:18
    container_name: budget-app-db
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: budget_user
      POSTGRES_PASSWORD: budget_password
      POSTGRES_DB: budget_db
    volumes:
      - postgres_data:/var/lib/postgresql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U budget_user -d budget_db"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

2. `docker compose up -d` を実行する。
3. `docker compose ps` を実行し、`budget-app-db` の状態が `healthy` になるまで待機・確認する。

## Step 2: バックエンド（Spring Boot 4 / Kotlin 2.4）のセットアップ
1. ルートディレクトリで以下のコマンドを実行し、`backend` ディレクトリを生成する。

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

2. `backend/build.gradle.kts` の `dependencies` ブロックに以下のテスト用ライブラリを追記する。

```kotlin
	testImplementation("io.mockk:mockk:1.14.11")
	testImplementation("com.lemonappdev:konsist:0.18.1")
```

3. `backend/src/main/resources/application.properties` を削除し、`backend/src/main/resources/application.yml` を作成する。

```yaml
spring:
  application:
    name: budgetapp
  datasource:
    url: jdbc:postgresql://localhost:5432/budget_db
    username: budget_user
    password: budget_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      "[hibernate.format_sql]": true
```

4. `backend/src/main/resources/db/migration/V1__create_tables.sql` を作成する。

```sql
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(10) NOT NULL
);

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    amount INT NOT NULL,
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

5. `cd backend && ./gradlew test` を実行してビルドと初期テストの成功を確認する。その後、バックグラウンド等で `./gradlew bootRun` を起動し、Flyway マイグレーション（`now at version v1`）が適用されたことを確認したらプロセスを停止する。

## Step 3: フロントエンド（Next.js 16 / pnpm 11）のセットアップ
1. ルートディレクトリの `mise.toml` に `node = "26"` と `pnpm = "11"` が定義されていることを確認する。未定義の場合は中断し、ユーザーに「2-0. 前提ツールのインストール」を案内する。
2. `pnpm -v` を実行し、v11 であることを確認する。
3. 非対話オプションを指定して `frontend` プロジェクトを生成する。

```bash
pnpm create next-app@latest frontend \
  --typescript \
  --eslint \
  --react-compiler \
  --tailwind \
  --src-dir \
  --app \
  --import-alias "@/*" \
  --use-pnpm \
  --yes
```

4. `frontend` ディレクトリに移動し、テスト関連パッケージをインストールする。

```bash
cd frontend && pnpm add -D vitest @vitejs/plugin-react jsdom @testing-library/react @testing-library/dom @testing-library/jest-dom msw
```

5. クリーンアーキテクチャ構成用のディレクトリを作成する。

```bash
mkdir -p frontend/src/components/ui \
  frontend/src/features/transaction/domain \
  frontend/src/features/transaction/usecase \
  frontend/src/features/transaction/adapter \
  frontend/src/features/transaction/presentation/utils \
  frontend/src/mocks
```

6. `cd frontend && pnpm build` を実行し、Next.js のビルドがエラーなく完了することを確認する。

## Step 4: 完了報告
すべてのステップが完了したら、以下の項目を確認結果とともにユーザーへ報告する。
- Docker コンテナ `budget-app-db` の稼働状況
- Spring Boot プロジェクトの生成と Flyway v1 マイグレーション適用結果
- Next.js プロジェクトの生成とディレクトリ構成の準備完了
````

### 3. プロジェクト共通ガイドライン（`CLAUDE.md`）の作成

Claude Code がセッション開始時に自動で読み込む共通ルールファイルを作成します。
ルートディレクトリ直下に `CLAUDE.md` を新規作成し、以下の内容を記述してください。

```markdown
# Budget App 開発ガイドライン

シンプル家計簿アプリ（Spring Boot + Next.js）のモノレポです。
すべての実装においてクリーンアーキテクチャの依存性ルール（外側の層から内側の層へのみ依存）を厳守してください。

## 技術スタック
- Backend: Kotlin 2.4, Java 25, Spring Boot v4 (4.1), PostgreSQL v18, Flyway, JUnit 5, MockK 1.14, Konsist
- Frontend: TypeScript, Next.js v16 (App Router), React 19, Tailwind CSS v4, Vitest v5, Testing Library, MSW v3, pnpm v11
- Infra: Docker Compose (PostgreSQL 18)

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
```

### 4. 領域別アーキテクチャ規約（`.claude/rules/`）の作成

対象ディレクトリのファイルを編集する際だけに自動適用される詳細ルールを作成します。

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
   - `adapter.controller`: REST Controller、リクエスト/レスポンス DTO、`GlobalExceptionHandler` を配置する。Controller は `usecase.input` のみを受け取り、`usecase.output` や `framework` を直接呼び出してはならない。
   - `adapter.repository`: `usecase.output.*Repository` を実装する `*RepositoryImpl` を配置し、JPA Entity とドメインモデルの相互変換を行う。

4. `framework`（第4層：Frameworks & Drivers）
   - `framework.jpa.entity`: `@Entity` を付与した JPA エンティティを配置する。
   - `framework.jpa.repository`: Spring Data JPA インターフェース（`Jpa*Repository`）を配置する。
   - `framework.config`: CORS などのフレームワーク設定クラスを配置する。
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
```

ここまでで人による準備は完了です。

---

## 2-2. 開発環境の構築と動作確認（AI が実施）

ここからは、ルートディレクトリ（`budget-app-handson`）で Claude Code を起動し、AI に環境構築を実行させます。

### 1. Claude Code の起動とルールの読み込み確認

ターミナルで Claude Code を起動します。

```bash
claude
```

起動後、チャット欄に以下のプロンプトを入力し、`2-1` で作成したルールとスキルが認識されているかを確認します。

```text
CLAUDE.md と .claude/rules/ に定義された、バックエンドとフロントエンドの4層構造と依存禁止ルールを要約してください。また、利用可能なスキルを確認してください。
```

4層構造のルールと `setup-env` スキルが認識されている回答が返ってくれば準備完了です。

### 2. `/setup-env` スキルによる環境構築の実行

続いて、Claude Code のチャット欄に以下のスキルコマンドを入力し、AI に環境構築を実行させます。

```text
/setup-env
```

Claude Code が `.claude/skills/setup-env/SKILL.md` の手順に従い、以下の作業を順次実行します。コマンド実行の許可を求められた場合は内容を確認して承認してください。

1. `compose.yaml` の生成と Docker による PostgreSQL 18 の起動・ヘルスチェック
2. Spring Initializr からの `backend` プロジェクト生成、依存ライブラリの追加、`application.yml` と Flyway SQL の配置、起動およびマイグレーション確認
3. `mise.toml` の確認、`pnpm create next-app@latest` による `frontend` プロジェクト生成、テストライブラリの導入、4層用ディレクトリの作成、ビルド確認

### 3. AI による構築完了後の確認ポイント

Claude Code から完了報告が出力されたら、以下の状態になっていることを確認してください。

- ルートディレクトリ直下に `compose.yaml`、`backend/`、`frontend/` が作成されていること。
- PostgreSQL 18 コンテナ（`budget-app-db`）が稼働中であり、Flyway によるテーブル作成（`categories`, `transactions`）と初期マスタデータ投入が完了していること。
- バックエンドの `./gradlew test` およびフロントエンドの `pnpm build` がエラーなく完了していること。

これで開発環境と AI 開発基盤のセットアップはすべて完了です。次の Chapter 3 以降も、Claude Code にアーキテクチャ規約を遵守させながら実装とテストを進めていきます。
