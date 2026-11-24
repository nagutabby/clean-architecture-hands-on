# Chapter 1: データベース設計とプロジェクト全体構成

## このチャプターの目的

コードを書き始める前に、データベースのテーブル設計と、バックエンド・フロントエンド双方のディレクトリ構成を確定させます。どのファイルがクリーンアーキテクチャのどの層に属するのか、そして依存の矢印が常に「外側から内側へ」向かう構造を把握することがこのチャプターのゴールです。

---

## 1. データベース設計（2テーブル構成）

収支の区別（収入・支出）とカテゴリ紐付けのリレーションを扱う2テーブル構成を採用します。アプリケーション層のドメインルールだけでなく、データベース側の制約（`NOT NULL`・`FOREIGN KEY`・`CHECK`）でもデータの整合性を担保します。

```text
+------------------+          +------------------------+
|    categories    | 1      N |      transactions      |
+------------------+----------+------------------------+
| id (PK)          |          | id (PK)                |
| name             |          | category_id (FK)       |
| type             |          | title                  |
+------------------+          | amount                 |
                              | transaction_date       |
                              | memo                   |
                              +------------------------+
```

### `categories`（カテゴリテーブル）

収支の分類（食費、日用品、給与など）を管理するマスタテーブルです。

| カラム名 | 物理名 | 型 | 制約 | 説明 |
| :--- | :--- | :--- | :--- | :--- |
| カテゴリID | `id` | `BIGSERIAL` | `PRIMARY KEY` | 自動採番ID |
| カテゴリ名 | `name` | `VARCHAR(50)` | `NOT NULL` | 例:「食費」「日用品」「給与」 |
| 収支タイプ | `type` | `VARCHAR(10)` | `NOT NULL`, `CHECK (type IN ('INCOME', 'EXPENSE'))` | `INCOME`（収入） / `EXPENSE`（支出） |

### `transactions`（家計簿明細テーブル）

日々の収支明細を記録するトランザクションテーブルです。

| カラム名 | 物理名 | 型 | 制約 | 説明 |
| :--- | :--- | :--- | :--- | :--- |
| 明細ID | `id` | `BIGSERIAL` | `PRIMARY KEY` | 自動採番ID |
| カテゴリID | `category_id` | `BIGINT` | `NOT NULL`, `FOREIGN KEY` | `categories.id` への外部キー |
| タイトル | `title` | `VARCHAR(100)` | `NOT NULL` | 例:「スーパーで買い物」 |
| 金額 | `amount` | `INT` | `NOT NULL`, `CHECK (amount >= 1)` | 1円以上の正の整数 |
| 決済日 | `transaction_date` | `DATE` | `NOT NULL` | 収支が発生した日付 |
| メモ | `memo` | `TEXT` | `NULL`許容 | 自由記述 |

---

## 2. バックエンド（Spring Boot）のディレクトリ構成

パッケージ名 `com.example.budgetapp` の直下に、クリーンアーキテクチャの4つの円に対応するパッケージを配置します。

DB とのデータ変換（OR マッピング）およびクエリインターフェース定義は「外側の技術と内側のドメインを仲介する責務」であるため、第3層（`adapter.repository`）に配置します。これにより、第3層が第4層（`framework`）を参照する必要がなくなり、依存方向が一方向に統一されます。

```text
backend/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── kotlin/com/example/budgetapp/
    │   │   ├── BudgetappApplication.kt
    │   │   │
    │   │   ├── domain/                              # 【第1層：Entities】最内層・純粋なビジネスルール
    │   │   │   ├── exception/
    │   │   │   │   └── DomainException.kt           # ドメインルール違反を表す独自例外
    │   │   │   └── model/
    │   │   │       ├── Amount.kt                    # 値オブジェクト（「1円以上」のルール）
    │   │   │       ├── BalanceSummary.kt            # 収支集計ロジック（総収入・総支出・残高）
    │   │   │       ├── Category.kt                  # カテゴリモデル
    │   │   │       ├── Transaction.kt               # 明細エンティティ
    │   │   │       └── TransactionType.kt           # 収支タイプ（INCOME / EXPENSE）
    │   │   │
    │   │   ├── usecase/                             # 【第2層：Use Cases】アプリケーション固有のルール
    │   │   │   ├── input/                           # Input Port（外側から呼ばれる入り口の契約）
    │   │   │   │   ├── CreateTransactionUseCase.kt
    │   │   │   │   ├── DeleteTransactionUseCase.kt
    │   │   │   │   ├── GetCategoriesUseCase.kt
    │   │   │   │   └── GetTransactionsUseCase.kt
    │   │   │   ├── output/                          # Output Port（外側へ依頼する出口の契約）
    │   │   │   │   ├── CategoryRepository.kt
    │   │   │   │   └── TransactionRepository.kt
    │   │   │   └── interactor/                      # ユースケースの具体的な処理手順
    │   │   │       ├── CreateTransactionInteractor.kt
    │   │   │       ├── DeleteTransactionInteractor.kt
    │   │   │       ├── GetCategoriesInteractor.kt
    │   │   │       └── GetTransactionsInteractor.kt
    │   │   │
    │   │   ├── adapter/                             # 【第3層：Interface Adapters】外側と内側の翻訳機
    │   │   │   ├── controller/                      # Web側アダプター（HTTP・JSON変換）
    │   │   │   │   ├── advice/
    │   │   │   │   │   └── GlobalExceptionHandler.kt
    │   │   │   │   ├── dto/
    │   │   │   │   │   ├── CategoryResponse.kt
    │   │   │   │   │   ├── TransactionListResponse.kt
    │   │   │   │   │   └── TransactionRequest.kt
    │   │   │   │   ├── CategoryController.kt
    │   │   │   │   └── TransactionController.kt
    │   │   │   └── repository/                      # DB側アダプター（永続化・ORマッピング）
    │   │   │       ├── entity/
    │   │   │       │   ├── CategoryJpaEntity.kt
    │   │   │       │   └── TransactionJpaEntity.kt
    │   │   │       ├── jpa/
    │   │   │       │   ├── JpaCategoryRepository.kt
    │   │   │       │   └── JpaTransactionRepository.kt
    │   │   │       ├── CategoryRepositoryImpl.kt
    │   │   │       └── TransactionRepositoryImpl.kt
    │   │   │
    │   │   └── framework/                           # 【第4層：Frameworks & Drivers】最外層・基盤設定
    │   │       └── config/
    │   │           └── CorsConfig.kt                # CORSなどのフレームワーク全体設定
    │   │
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/
    │           └── V1__create_tables.sql
    │
    └── test/kotlin/com/example/budgetapp/
        ├── architecture/
        │   └── ArchitectureTest.kt                  # Konsistによる4層の依存方向自動検証
        ├── domain/model/
        │   ├── AmountTest.kt                        # 値オブジェクトの単体テスト
        │   ├── BalanceSummaryTest.kt                # 収支集計ロジックの単体テスト
        │   └── TransactionTest.kt                   # エンティティ不変条件の単体テスト
        ├── usecase/interactor/
        │   ├── CreateTransactionInteractorTest.kt   # MockKを使った新規登録ユースケーステスト
        │   └── DeleteTransactionInteractorTest.kt   # MockKを使った削除ユースケーステスト
        └── adapter/controller/
            └── TransactionControllerTest.kt         # MockMvcを使ったAPI統合テスト
```

### 実行時の処理の流れとソースコードの依存方向

クリーンアーキテクチャで最も重要なのは、「実行時に処理が呼ばれる順番」と「ソースコード上の依存方向」が逆転する構造です。

```text
【実行時の処理の流れ（データの移動方向）】
Controller ──> Interactor ──> RepositoryImpl ──> JpaRepository ──> PostgreSQL

【ソースコードの依存方向（import の向き）】
Controller ──> InputPort <── Interactor ──> OutputPort <── RepositoryImpl ──> JpaRepository ──> Spring Data JPA（外部ライブラリ）
(第3層)        (第2層)        (第2層)        (第2層)         (第3層)            (第3層)          (ライブラリ。依存性のルールの対象外)
```

1. 層の飛び越えと逆向き依存の禁止
   - すべての Controller は必ず Input Port（`GetCategoriesUseCase` など）を呼び出し、Output Port（`*Repository`）や `adapter.repository`（JPA）を直接参照しません。
   - 第3層（`adapter`）は第1層（`domain`）と第2層（`usecase`）のみを参照し、外側の第4層（`framework`）には一切依存しません。
2. Presenter に関する設計上の簡略化
   本来のクリーンアーキテクチャには Use Case から出力を受け取る Output Boundary（Presenter）が存在しますが、JSON を返す現代の REST API では、Interactor の戻り値（Output Data）を Controller で DTO へ変換する構成が一般的です。本ハンズオンでもこの実践的な構成を採用します。

---

## 3. フロントエンド（Next.js）のディレクトリ構成

フロントエンドにも同じく、内側のビジネスロジックが外側の通信手段やルーティング基盤に依存しない構成をとります。

```text
frontend/
├── package.json
├── vitest.config.ts
├── vitest.setup.ts
└── src/
    ├── app/                                         # 【第4層：Frameworks】Next.js App Router
    │   ├── page.tsx                                 # `/` から `/transactions` へのリダイレクト
    │   └── transactions/
    │       └── page.tsx                             # `/transactions` のページエントリ
    │
    ├── components/ui/                               # 汎用UIパーツ
    │   ├── Button.tsx
    │   └── Input.tsx
    │
    ├── features/transaction/
    │   ├── domain/                                  # 【第1層：Entities】型定義・純粋なビジネスルール
    │   │   ├── types.ts                             # TransactionItem, Category, BalanceSummary 等の型
    │   │   ├── validateTransaction.ts               # 登録入力の事前バリデーションルール
    │   │   └── validateTransaction.test.ts          # ドメインルールの単体テスト
    │   │
    │   ├── usecase/                                 # 【第2層：Use Cases】状態管理・画面ユースケース
    │   │   ├── TransactionRepository.ts             # Output Port（API通信を抽象化するインターフェース）
    │   │   └── useTransactions.ts                   # 一覧取得・登録・削除のユースケースフック
    │   │
    │   ├── adapter/                                 # 【第3層：Interface Adapters】外部通信の実装
    │   │   └── transactionApi.ts                    # TransactionRepository の fetch 実装
    │   │
    │   └── presentation/                            # 【第3層：Interface Adapters】UI表示と整形
    │       ├── utils/
    │       │   ├── formatCurrency.ts                # 日本円表記（￥1,280）への表示整形
    │       │   └── formatCurrency.test.ts           # 表示整形関数の単体テスト
    │       ├── BalanceSummaryCard.tsx
    │       ├── TransactionForm.tsx
    │       ├── TransactionList.tsx
    │       ├── TransactionSection.tsx               # 機能コンテナ（Composition Root兼務）
    │       └── TransactionSection.test.tsx          # MSWを使った画面結合テスト
    │
    └── mocks/                                       # テスト用モックサーバー（MSW）
        ├── handlers.ts
        └── server.ts
```

### フロントエンドにおける責務分離のポイント

- `presentation` と `adapter` がともに第3層（Interface Adapters）に属する理由
  クリーンアーキテクチャの同心円では、「UI の入力受付・画面描画（Controller / Presenter に相当）」と「外部システムとの入出力変換（Gateway / API クライアントに相当）」は、どちらも内側のドメイン・ユースケースを外側の世界とつなぐ役割を持つため、同じ第3層（Interface Adapters）に位置づけられます。
- UI コンポーネントからの `adapter` 直接参照の制限（Composition Root）
  `presentation` 内の純粋な表示コンポーネント（`BalanceSummaryCard`, `TransactionForm`, `TransactionList`）は `adapter/` を直接 import してはなりません。また、Next.js App Router では Server Component（`app/transactions/page.tsx`）から Client Component へ関数を含むオブジェクトを props で直接渡せないため、機能のルートコンテナである `TransactionSection.tsx` のみが Composition Root（依存性注入の組み立て役）として `adapter/transactionApi` をデフォルト引数で受け取り、`useTransactions` へ注入します。
- Domain 層と Presentation 層の分離
  通貨フォーマット（`formatCurrency.ts`）は画面表示のためのロジックであるため `presentation/utils/` に配置します。一方、金額やタイトルの業務ルール検証（`validateTransaction.ts`）は純粋なドメインルールとして `domain/` に配置します。

---

## 4. Claude Code で進める際の確認事項

この構成をベースに、次の Chapter 2 で Claude Code 用の設計ルールファイル（`CLAUDE.md` および `.claude/rules/`）を作成します。

1. 新しいファイルを作成する際は、必ず上記のディレクトリ構成のどの層（`domain` / `usecase` / `adapter` / `framework`、または `domain` / `usecase` / `adapter` / `presentation` / `app`）に属するかを確認してから配置します。
2. 内側の層のファイルを作成・編集した直後は、外側の層のパッケージやフレームワーク固有の `import` が混入していないかを確認します。
