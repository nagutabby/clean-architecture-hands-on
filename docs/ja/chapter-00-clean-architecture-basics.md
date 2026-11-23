# Chapter 0: クリーンアーキテクチャの基礎

## このハンズオンの概要とゴール

このハンズオンは、シンプル家計簿アプリ Budget App を作りながら、クリーンアーキテクチャ、つまり Clean Architecture の設計原則と実装方法を身につけるための実践資料です。

フレームワークやデータベースといった外部の技術的な詳細からビジネスロジックを切り離し、変更に強くテストしやすいアプリケーションを作る方法を学びます。また、各チャプターで Claude Code を使い、設計ルールを AI に守らせながら、実装とテストを段階的に進める流れを実践します。

### 技術スタック

| 領域 | 技術・ツール | バージョン / 備考 |
| :--- | :--- | :--- |
| バックエンド | Kotlin / Java | Kotlin v2 / Java 25 (LTS) |
| | Spring Boot | v4 / 依存は Spring Web, Spring Data JPA, Validation, Flyway, PostgreSQL |
| | データベース / マイグレーション | PostgreSQL v18 / Flyway |
| | テスト / ビルド | JUnit 5, MockK, MockMvc, Konsist / Gradle (Kotlin DSL) |
| フロントエンド | Node.js / パッケージマネージャ | Node.js v26 (LTS) / pnpm v11 |
| | フレームワーク / 言語 | Next.js v16 (App Router) / React 19 / TypeScript |
| | スタイリング | Tailwind CSS v4 |
| | テスト | Vitest, React Testing Library, MSW |
| インフラ / 環境管理 | コンテナ / バージョン管理 | Rancher Desktop, Docker 互換 / mise |
| AI 開発支援 | Claude Code | CLAUDE.md, .claude/rules/, Agent Skills, Hooks |

---

## 1. なぜクリーンアーキテクチャを採用するのか

Web アプリケーション開発では、ビジネスロジックとフレームワークやデータベースのコードが混ざると、次のような問題が起きます。

1. データベースやライブラリのバージョンアップ・乗り換えのたびに、ビジネスロジックまで修正が必要になる。
2. 単体テストを実行するたびに Web サーバーやデータベースを起動しなければならず、テストが遅くなる。
3. 処理の責務があいまいになり、1つのクラスやコンポーネントが大きくなりすぎる。

クリーンアーキテクチャでは、ソフトウェアを「変わりにくいもの」であるビジネスルールと、「変わりやすいもの」である DB・画面・フレームワークにはっきり分けることで、これらの問題を解決します。

---

## 2. クリーンアーキテクチャの「4つの同心円」

クリーンアーキテクチャは、関心事の分離を4つの同心円で表します。中心に近いほどビジネスの本質的なルールで、外側に向かうほど具体的な技術の詳細になります。

```text
+-------------------------------------------------------------------+
|  【第4層】Frameworks & Drivers（フレームワークとドライバー）           |
|   Spring Boot設定(CorsConfig) / PostgreSQL / Next.js App Router   |
|  +-------------------------------------------------------------+  |
|  |  【第3層】Interface Adapters（インターフェースアダプター）        |  |
|  |   Controller / DTO / Repository実装・JPA / API通信 / UI表示  |  |
|  |  +-------------------------------------------------------+  |  |
|  |  |  【第2層】Use Cases（ユースケース）                      |  |  |
|  |  |   アプリケーション固有の処理手順（一覧取得・新規登録など）   |  |  |
|  |  |  +-------------------------------------------------+  |  |  |
|  |  |  |  【第1層】Entities（エンティティ / ドメイン）       |  |  |  |
|  |  |  |   純粋なビジネスルール（金額は1円以上、残高計算など） |  |  |  |
|  |  |  +-------------------------------------------------+  |  |  |
|  |  +-------------------------------------------------------+  |  |
|  +-------------------------------------------------------------+  |
+-------------------------------------------------------------------+
          =====>  依存の方向は「常に外側から内側へ」のみ  =====>
```

### 各層の役割

| 層 | 名称 | 役割と具体例 |
| :--- | :--- | :--- |
| 第1層 | Entities / ドメイン | アプリケーション全体で共有される、純粋なビジネスルールとデータ構造です。家計簿アプリでは、「金額は1円以上の正の整数である」「総収入から総支出を引いて残高を求める」といったルールが当てはまります。 |
| 第2層 | Use Cases / ユースケース | ユーザーがシステムで実現したい具体的な操作の手順を定義します。例えば一覧取得、新規登録、削除などです。第1層のオブジェクトを組み合わせて、処理の流れを制御します。 |
| 第3層 | Interface Adapters | 内側の層である Entities と Use Cases が扱うデータ形式と、外側の技術である Web・DB・ブラウザが扱うデータ形式を相互に変換します。バックエンドの Controller・DTO・Repository 実装・JPA Entity / JPA Repository、フロントエンドの API クライアント・UI コンポーネントがここに属します。 |
| 第4層 | Frameworks & Drivers | Spring Boot の起動基盤と全体設定である `CorsConfig` など、PostgreSQL データベース、`app/` にある Next.js App Router のルーティングなど、最も外側にある基盤や外部ツールです。自作コードでは `framework` パッケージと `app/` が該当します。 |

---

## 3. 依存性のルール / Dependency Rule

クリーンアーキテクチャで最も重要な制約が「依存性のルール」です。

> ソースコードの依存関係、つまり import 文や参照は、必ず「外側の円」から「内側の円」に向かっていなければなりません。

- 第1層の Entities は、第2層〜第4層のクラスやパッケージ、フレームワークを一切参照しません。`@Entity` や `@RestController` のような Spring / JPA のアノテーションをドメインモデルに書いてはいけません。
- 第2層の Use Cases は、第1層の Entities にだけ依存します。第3層の Interface Adapters と、第4層であるデータベースやルーティングには一切依存しません。ただし本ハンズオンでは、実装のしやすさを優先し、第2層でフレームワークの基本機能を使うというトレードオフを採用しています。基本機能とは、バックエンドの `@Service` / `@Transactional` と、フロントエンドの React Hooks です。
- 第3層の Interface Adapters は、第1層・第2層にだけ依存します。外側の第4層である `framework` パッケージや `app` パッケージには一切依存しません。
- 第4層の Frameworks & Drivers は、アプリケーションの起動・横断設定・ルーティングを担う最も外側の層です。内側の第1層〜第3層を参照して、全体を組み立てます。

なお、この依存性のルールが対象とするのは「自分たちが書いたパッケージ間の依存」です。対象のパッケージは、バックエンドでは `domain` / `usecase` / `adapter` / `framework`、フロントエンドでは `domain` / `usecase` / `adapter` / `presentation` / `app` です。第3層が Spring MVC・Spring Data JPA・`jakarta.persistence` などの外部ライブラリを使うことは許容されます。第2層で限定的に許す `@Service` / `@Transactional` / React Hooks も同じ扱いです。第1層だけは、外部ライブラリのフレームワークにも依存しません。

このルールにより、自作パッケージ間の依存は「外側から内側へ」の一方向に保たれ、内側の層は外側の変更の影響を受けません。

---

## 4. 依存性逆転の原則 / DIP と、処理の流れの対比

実際のアプリケーションでは、第2層の Use Cases の処理の中でデータベースにデータを保存する必要があります。しかし、Use Cases からデータベース操作の実装クラスを直接呼び出すと、「内側から外側への依存」になりルール違反です。

この問題を解決するために、依存性逆転の原則、英語では Dependency Inversion Principle を適用します。

1. 内側の Use Case 層に、データ永続化の契約となるインターフェースを定義します。これが `TransactionRepository`、つまり Output Port です。
2. 外側の Interface Adapters 層に、そのインターフェースを実装するクラスの `TransactionRepositoryImpl` と、Spring Data JPA のインターフェースの `JpaTransactionRepository`、JPA エンティティの `TransactionJpaEntity` を置きます。
3. 実行時には、第4層の Spring Boot / Spring Data JPA / PostgreSQL といったフレームワークが実際のインスタンスを生成し、DI、つまり依存性の注入で Use Case に渡します。

その結果、実行時の処理の流れとソースコード上の依存方向は、次のように逆向きになります。

```text
【実行時の処理の流れ（データの移動方向）】
Controller ──> Interactor ──> RepositoryImpl ──> JpaRepository ──> PostgreSQL

【ソースコードの依存方向（import の向き）】
Controller ──> InputPort <── Interactor ──> OutputPort <── RepositoryImpl ──> JpaRepository ──> Spring Data JPA（外部ライブラリ）
(第3層)        (第2層)        (第2層)        (第2層)         (第3層)            (第3層)          (ライブラリ。依存性のルールの対象外)
```

第4層である `framework` パッケージと Spring Boot の起動基盤は、DI でこれらの実装クラスを組み立てて Use Case に渡す役割を持ちます。`adapter` から import されることはありません。

この構造には次の利点があります。

- Use Case の単体テストでは、データベースを起動せず、`TransactionRepository` インターフェースをMockK のモックに差し替えるだけで、高速にテストできます。
- データの保存先を PostgreSQL から外部 API やインメモリ保存に変える場合も、`RepositoryImpl` を差し替えるだけで済みます。Use Case 層と Domain 層は修正不要です。

---

## 5. フロントエンドへの適用と Claude Code の活用方針

### フロントエンドにおけるクリーンアーキテクチャ

フロントエンドの Next.js / React でも、UI コンポーネントの中に `fetch` 通信やビジネスルールを直接書くと、画面仕様の変更やテストが難しくなります。本ハンズオンでは、機能単位の `features/transaction` とルーティングの `app` を次の4層に分けます。

- 第1層の domain: 型定義と、入力バリデーションなどの純粋なビジネスルール
- 第2層の usecase: 画面の状態管理と操作手順をまとめたカスタムフック、および通信インターフェースである Repository
- 第3層の adapter / presentation: `fetch` による API 通信の実装である adapter と、表示の整形・UI 描画コンポーネントである presentation
- 第4層の app: Next.js App Router のルーティングページ

### Claude Code を用いたハンズオンの進め方

AI コーディングエージェントの Claude Code で開発を進めるとき、指示があいまいだと、「ドメインモデルに JPA アノテーションを付ける」「Controller から直接 Repository を呼び出す」といったアーキテクチャ違反が起きやすくなります。

本ハンズオンでは、次の順序で Claude Code の機能を段階的に使います。

1. Chapter 2: スキルと設計ルールの定義、および AI による環境構築
   前提ツールを導入したあと、人が環境構築スキルの `.claude/skills/setup-env/SKILL.md` と、設計ルールの `CLAUDE.md` / `.claude/rules/` を作成します。次に Claude Code に `/setup-env` を実行させ、開発環境を構築します。
2. Chapter 3〜6: 層ごとの段階的な実装と、テストによる自動検証
   内側の層から順に実装します。Konsist, MockK, MockMvc, Vitest, MSW によるテストコードで、ルール違反や不具合を自動的に検出します。
3. Chapter 7: Agent Skills と Hooks による機能追加の演習
   ユースケースを追加する手順を `.claude/skills/add-feature/SKILL.md` に定義します。さらに、ファイル編集時に自動で検証を実行する `.claude/settings.json` に書く `PostToolUse` フックの設定例も組み合わせて、機能追加を試します。

### チェックポイント方式での進行

Chapter 2 以降の Claude Code への指示は、作業を「チェックポイント」、略して CP に区切って進めます。CP には「設計 → 実装 → テスト → 動作確認」の種別があり、Claude Code は CP を1つ終えるたびに結果を報告して停止します。人は結果を確認し、「次のチェックポイントに進む」と入力して次の CP へ進めます。この運用ルールは、Chapter 2 で `CLAUDE.md` の「チェックポイント運用」に記載します。

---

## 6. チャプター構成一覧

- Chapter 0: クリーンアーキテクチャの基礎。本ファイルが該当します
- Chapter 1: データベース設計とプロジェクト全体構成
- Chapter 2: Claude Code ルール設定と環境構築
- Chapter 3: Spring Boot / Kotlin によるバックエンド実装
- Chapter 4: バックエンドのテスト実装：単体テスト・統合テスト・アーキテクチャテスト
- Chapter 5: フロントエンド実装：Next.js 16 / TypeScript
- Chapter 6: フロントエンドのテスト実装：単体テスト・結合テスト
- Chapter 7: Claude Code の Skills と Hooks を活用した機能追加演習
