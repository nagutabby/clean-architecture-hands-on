---
name: setup-env
description: Budget App（PostgreSQL 18, Spring Boot 4, Next.js 16）のローカル開発環境を構築し、起動確認までを実行するスキル
disable-model-invocation: true
---

# Budget App 環境構築手順

ユーザーから `/setup-env` で呼び出された場合、すべてのコマンドはプロジェクトルート（`budget-app-handson`）を基準に実行し、以下のチェックポイント（CP）を順に進めて開発環境を構築してください。

CLAUDE.md の「チェックポイント運用」に従い、1つの CP を終えるたびに結果を報告して停止し、ユーザーが「次のチェックポイントに進む」と入力するまで次の CP に着手しないこと。最初は CP2-1 だけを実施する。

## CP2-1 〔設計〕事前確認
1. 現在の作業ディレクトリがプロジェクトのルート（`budget-app-handson`）であることを確認する。
2. `docker --version`（Rancher Desktop の dockerd）、`java -version`（Java 25 必須）、`node -v`（v26）、`pnpm -v`（v11）が利用可能か確認する。Java が 25 でない場合や前提ツールが未インストールの場合は中断し、ユーザーに「2-0 / 2-1 の前提ツール導入」を案内する。
3. 使用するポート（PostgreSQL: 5432、Spring Boot: 8080、Next.js: 3000）が他のプロセスに使われていないか `lsof -nP -iTCP:<ポート> -sTCP:LISTEN` で確認する。
   - 5432 を占有しているのが、このプロジェクト自身のコンテナ `budget-app-db`（`docker ps` で確認）である場合は、再実行とみなして中断せず、結果を報告して CP2-2 の指示を待つ。
   - それ以外でいずれかのポートがすでに使用されている場合は、既存のプロセスやコンテナを勝手に終了・削除せず、占有しているプロセス情報（PID・コマンド名・ポート番号）をユーザーに報告して処理を中断する。
   - ユーザー自身で該当プロセスを停止した後に、再度 `/setup-env` を実行するよう案内する。

## CP2-2 〔実装・動作確認〕PostgreSQL 18 の構築と起動
1. ルートディレクトリ直下に、配布された `.env.example` があることを確認する。`.env` が無ければ `cp .env.example .env` で作成する（`.env` が既にある場合は上書きしない）。`.env.example` が無い場合は中断し、ユーザーにファイルの配置を依頼する。DB のユーザー名・パスワード・DB 名はこの `.env` にだけ置き、`compose.yaml` や `application.yml` には書かない。
2. Git で管理している場合は、`.gitignore` に `.env` が含まれていることを確認し、無ければ追記する。
3. ルートディレクトリ直下に `compose.yaml` を以下の内容で作成する。

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

4. `docker compose up -d` を実行する。
5. `docker compose ps` を実行し、`budget-app-db` の状態が `healthy` になるまで待機・確認する。

## CP2-3 〔実装・動作確認〕バックエンド（Spring Boot 4 / Kotlin）のセットアップ
1. ルートディレクトリで以下のコマンドを実行し、`backend` ディレクトリを生成する（Spring Initializr から最新の Spring Boot 4.x を取得する）。

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

2. `backend/build.gradle.kts` の `dependencies` ブロックを確認し、Flyway の PostgreSQL モジュールとテスト用ライブラリが含まれるよう以下を追記する（`flyway-database-postgresql` がすでに記載されている場合は重複させない）。

```kotlin
	runtimeOnly("org.flywaydb:flyway-database-postgresql")
	testImplementation("io.mockk:mockk:1.14.11")
	testImplementation("com.lemonappdev:konsist:0.18.1")
```

3. `backend/src/main/resources/application.properties` を削除し、`backend/src/main/resources/application.yml` を作成する。

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

4. `backend/src/main/resources/db/migration/V1__create_tables.sql` を作成する。

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
6. サブシェル `(cd backend && ./gradlew bootRun)` でバックエンドを一時起動し、起動完了後にプロセスを停止する。その後、以下のコマンドで Flyway によるテーブル作成と初期データ投入を直接確認する。

```bash
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "\dt"'
docker compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT * FROM categories;"'
```

## CP2-4 〔実装・動作確認〕フロントエンド（Next.js 16 / pnpm 11）のセットアップ
1. ルートディレクトリの `mise.toml` に `node = "26"` と `pnpm = "11"` が定義されていることを確認する。
2. 非対話オプションを指定してルートディレクトリ直下に `frontend` プロジェクトを生成する。

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

   `create-next-app` は `frontend/.git` を生成するため、モノレポのルートで一括管理できるよう `rm -rf frontend/.git` で削除する。

3. カレントディレクトリをルートに保ったまま、テスト関連パッケージをインストールする。

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

## CP2-5 〔動作確認〕完了報告
すべての CP が完了したら、以下の項目を確認結果とともにユーザーへ報告する。
- Docker コンテナ `budget-app-db` の稼働状況
- Spring Boot プロジェクトの生成と `categories` / `transactions` テーブルおよび初期マスタデータの作成結果
- Next.js プロジェクトの生成とディレクトリ構成・ビルド完了結果
