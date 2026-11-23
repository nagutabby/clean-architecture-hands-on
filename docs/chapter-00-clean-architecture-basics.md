# Chapter 0: クリーンアーキテクチャの基礎

## このハンズオンの概要とゴール

このハンズオンは、クリーンアーキテクチャ（Clean Architecture）の設計原則と実装手法を、シンプル家計簿アプリ（Budget App）の開発を通じて習得するための実践資料です。

フレームワークやデータベースといった外部の技術詳細からビジネスロジックを切り離し、変更に強くテストしやすいアプリケーションを構築する手法を学びます。また、各チャプターで Claude Code を活用し、設計規約を AI に遵守させながら段階的に実装とテストを進めるワークフローを実践します。

### 技術スタック

| 領域 | 技術・ツール | バージョン / 備考 |
| :--- | :--- | :--- |
| バックエンド | Kotlin / Java | Kotlin v2 / Java 25 (LTS) |
| | Spring Boot | v4（Spring Web, Spring Data JPA, Validation, Flyway, PostgreSQL） |
| | データベース / マイグレーション | PostgreSQL v18 / Flyway |
| | テスト / ビルド | JUnit 5, MockK, MockMvc, Konsist / Gradle (Kotlin DSL) |
| フロントエンド | Node.js / パッケージマネージャ | Node.js v26 (LTS) / pnpm v11 |
| | フレームワーク / 言語 | Next.js v16 (App Router) / React 19 / TypeScript |
| | スタイリング | Tailwind CSS v4 |
| | テスト | Vitest, React Testing Library, MSW |
| インフラ / 環境管理 | コンテナ / バージョン管理 | Rancher Desktop（Docker 互換）/ mise |
| AI 開発支援 | Claude Code | CLAUDE.md, .claude/rules/, Agent Skills, Hooks |

---

## 1. なぜクリーンアーキテクチャを採用するのか

Web アプリケーション開発では、ビジネスロジックとフレームワークやデータベースのコードが混在すると、以下の問題が発生します。

1. データベースやライブラリのバージョンアップ・乗り換え時に、ビジネスロジックまで修正が必要になる。
2. 単体テストを実行するたびに Web サーバーやデータベースを起動しなければならず、テストの実行速度が低下する。
3. 処理の責務が曖昧になり、1つのクラスやコンポーネントが肥大化する。

クリーンアーキテクチャでは、ソフトウェアを「変わりにくいもの（ビジネスルール）」と「変わりやすいもの（DB・画面・フレームワーク）」に明確に分離することで、これらの問題を解決します。

---

## 2. クリーンアーキテクチャの「4つの同心円」

クリーンアーキテクチャは、関心事の分離を4つの同心円として表現します。中心に近いほどビジネス上の本質的なルールであり、外側に向かうほど具体的な技術詳細になります。

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
| 第1層 | Entities（ドメイン） | アプリケーション全体で共有される純粋なビジネスルールとデータ構造です。家計簿アプリにおける「金額は1円以上の正の整数である」「総収入から総支出を引いて残高を算出する」といったルールが該当します。 |
| 第2層 | Use Cases（ユースケース） | ユーザーがシステムを使って実現したい具体的な操作手順（一覧取得、新規登録、削除など）を定義します。第1層のオブジェクトを組み合わせて処理の流れを制御します。 |
| 第3層 | Interface Adapters | 内側の層（Entities / Use Cases）が扱うデータ形式と、外側の技術（Web / DB / ブラウザ）が扱うデータ形式を相互に変換します。バックエンドの Controller・DTO・Repository 実装・JPA Entity / JPA Repository、フロントエンドの API クライアント・UI コンポーネントがここに属します。 |
| 第4層 | Frameworks & Drivers | Spring Boot の起動基盤と全体設定（`CorsConfig` 等）、PostgreSQL データベース、Next.js App Router のルーティング（`app/`）といった最外層の基盤・外部ツールです。自作コードとしては `framework` パッケージと `app/` が該当します。 |

---

## 3. 依存性のルール（Dependency Rule）

クリーンアーキテクチャにおいて最も重要な制約が「依存性のルール」です。

> ソースコードの依存関係（import 文や参照）は、必ず「外側の円」から「内側の円」に向かっていなければなりません。

- 第1層（Entities）は、第2層〜第4層のクラスやパッケージ、フレームワークを一切参照しません。`@Entity` や `@RestController` のような Spring / JPA のアノテーションをドメインモデルに記述することは禁止されます。
- 第2層（Use Cases）は、第1層（Entities）にのみ依存し、第3層（Interface Adapters）および第4層（データベースやルーティング）には一切依存しません。なお、本ハンズオンでは実装上の実用性を優先し、第2層でフレームワークの基本機能（バックエンドの `@Service` / `@Transactional`、フロントエンドの React Hooks）を利用する設計上のトレードオフを採用しています。
- 第3層（Interface Adapters）は、第1層・第2層にのみ依存し、外側の第4層（`framework` パッケージや `app` パッケージ）には一切依存しません。
- 第4層（Frameworks & Drivers）は、アプリケーションの起動・横断設定・ルーティングを担う最外層であり、内側の層（第1層〜第3層）を参照して全体を組み立てます。

なお、この依存性のルールが対象とするのは「自分たちが書いたパッケージ（`domain` / `usecase` / `adapter` / `framework`、フロントエンドでは `domain` / `usecase` / `adapter` / `presentation` / `app`）間の依存」です。第3層が Spring MVC・Spring Data JPA・`jakarta.persistence` などの外部ライブラリを利用することは許容されます（第2層で利用を限定的に許す `@Service` / `@Transactional` / React Hooks もこの扱いです）。第1層だけは外部ライブラリのフレームワーク依存も持ちません。

このルールにより、自作パッケージ間では「外側から内側へ」の依存方向が一方向に保たれ、内側の層は外側の変更から保護されます。

---

## 4. 依存性逆転の原則（DIP）と処理の流れの対比

実際のアプリケーションでは、第2層（Use Cases）の処理の中でデータベースへデータを保存する必要があります。しかし、Use Cases からデータベース操作の実装クラスを直接呼び出すと「内側から外側への依存」となりルール違反になります。

この問題を解決するために、依存性逆転の原則（Dependency Inversion Principle）を適用します。

1. 内側（Use Case 層）に、データ永続化の契約となるインターフェース（`TransactionRepository` ＝ Output Port）を定義します。
2. 外側（Interface Adapters 層）に、そのインターフェースを実装するクラス（`TransactionRepositoryImpl`）と、Spring Data JPA のインターフェース（`JpaTransactionRepository`）・JPA エンティティ（`TransactionJpaEntity`）を配置します。
3. 実行時には、第4層のフレームワーク（Spring Boot / Spring Data JPA / PostgreSQL）が実行実体を生成し、DI（依存性の注入）によって Use Case に渡します。

これにより、実行時の処理の流れとソースコード上の依存方向が次のように対比されます。

```text
【実行時の処理の流れ（データの移動方向）】
Controller ──> Interactor ──> RepositoryImpl ──> JpaRepository ──> PostgreSQL

【ソースコードの依存方向（import の向き）】
Controller ──> InputPort <── Interactor ──> OutputPort <── RepositoryImpl ──> JpaRepository ──> Spring Data JPA（外部ライブラリ）
(第3層)        (第2層)        (第2層)        (第2層)         (第3層)            (第3層)          (ライブラリ。依存性のルールの対象外)
```

第4層（`framework` パッケージ・Spring Boot の起動基盤）は、DI によってこれらの実装クラスを組み立てて Use Case に渡す役割を担い、`adapter` から import されることはありません。

この構造には以下の利点があります。

- Use Case の単体テストでは、データベースを起動する代わりに `TransactionRepository` インターフェースをモック（MockK）に差し替えるだけで高速にテストできます。
- データの保存先を PostgreSQL から外部 API やインメモリ保存に変更する場合でも、`RepositoryImpl` を差し替えるだけで済み、Use Case 層と Domain 層を修正する必要がありません。

---

## 5. フロントエンドへの適用と Claude Code の活用方針

### フロントエンドにおけるクリーンアーキテクチャ

フロントエンド（Next.js / React）においても、UI コンポーネントの中に `fetch` 通信やビジネスルールを直接書くと、画面仕様の変更やテストが困難になります。本ハンズオンでは、機能単位（`features/transaction`）とルーティング（`app`）を次の4層に分割します。

- 第1層（domain）: 型定義および入力バリデーションなどの純粋なビジネスルール
- 第2層（usecase）: 画面の状態管理と操作手順をまとめたカスタムフック、および通信インターフェース（Repository）
- 第3層（adapter / presentation）: `fetch` による API 通信実装（adapter）と、表示整形・UI 描画コンポーネント（presentation）
- 第4層（app）: Next.js App Router のルーティングページ

### Claude Code を用いたハンズオンの進め方

AI コーディングエージェント（Claude Code）を使って開発を進める際、指示が曖昧だと「ドメインモデルに JPA アノテーションを付けてしまう」「Controller から直接 Repository を呼び出してしまう」といったアーキテクチャ違反が起きやすくなります。

本ハンズオンでは、以下の手順で Claude Code の機能を段階的に活用します。

1. スキルと設計ルールの定義、および AI による環境構築（Chapter 2）
   前提ツールを導入したうえで、人が環境構築スキル（`.claude/skills/setup-env/SKILL.md`）と設計ルール（`CLAUDE.md` / `.claude/rules/`）を作成し、Claude Code に `/setup-env` を実行させて開発環境を構築します。
2. 層ごとの段階的実装とテストによる自動検証（Chapter 3〜6）
   内側の層から順番に実装を進め、テストコード（Konsist, MockK, MockMvc, Vitest, MSW）によってルール違反や不具合を自動検知します。
3. Agent Skills と Hooks による機能追加の演習（Chapter 7）
   ユースケース追加の手順を `.claude/skills/add-usecase/SKILL.md` に定義し、ファイル編集時に自動検証を実行する `PostToolUse` フック（`.claude/settings.json`）の設定例を組み合わせて機能追加を試します。

---

## 6. チャプター構成一覧

- Chapter 0: クリーンアーキテクチャの基礎（本ファイル）
- Chapter 1: データベース設計とプロジェクト全体構成
- Chapter 2: Claude Code ルール設定と環境構築
- Chapter 3: バックエンド実装（Spring Boot / Kotlin）
- Chapter 4: バックエンドのテスト実装（単体テスト・統合テスト・アーキテクチャテスト）
- Chapter 5: フロントエンド実装（Next.js 16 / TypeScript）
- Chapter 6: フロントエンドのテスト実装（単体テスト・結合テスト）
- Chapter 7: Claude Code の Skills と Hooks を活用した機能追加演習
