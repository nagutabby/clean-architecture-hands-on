# Chapter 1: データベース設計とプロジェクト全体構成

## このチャプターの目的

コードを書き始める前に、データベースのテーブル設計と、バックエンド・フロントエンド両方のディレクトリ構成を決めます。どのファイルがクリーンアーキテクチャのどの層に属するのか、そして依存の矢印が常に「外側から内側へ」向かう構造を理解することが、このチャプターのゴールです。

---

## 1. データベース設計: 2テーブル構成

収入・支出という収支の区別と、リレーションによるカテゴリとの紐付けを扱うために、2テーブル構成を採用します。アプリケーション層のドメインルールだけでなく、データベース側の制約である `NOT NULL`・`FOREIGN KEY`・`CHECK` でもデータの整合性を守ります。

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

### `categories`: カテゴリテーブル

食費、日用品、給与などの収支の分類を管理するマスタテーブルです。

| カラム名 | 物理名 | 型 | 制約 | 説明 |
| :--- | :--- | :--- | :--- | :--- |
| カテゴリID | `id` | `BIGSERIAL` | `PRIMARY KEY` | 自動採番ID |
| カテゴリ名 | `name` | `VARCHAR(50)` | `NOT NULL` | 例:「食費」「日用品」「給与」 |
| 収支タイプ | `type` | `VARCHAR(10)` | `NOT NULL`, `CHECK (type IN ('INCOME', 'EXPENSE'))` | `INCOME` は収入、`EXPENSE` は支出 |

### `transactions`: 家計簿明細テーブル

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

## 2. Spring Boot バックエンドのディレクトリ構成

パッケージ名 `com.example.budgetapp` の直下に、クリーンアーキテクチャの4つの円に対応するパッケージを置きます。

DB とのデータ変換である OR マッピングと、クエリインターフェースの定義は、「外側の技術と内側のドメインを仲介する」責務を持つため、第3層の `adapter.repository` に置きます。こうすると、第3層が第4層の `framework` を参照する必要がなくなり、依存の方向を一方向にそろえられます。

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

クリーンアーキテクチャで最も重要なのは、「実行時に処理が呼ばれる順番」と「ソースコード上の依存方向」が逆向きになる構造です。

```text
【実行時の処理の流れ（データの移動方向）】
Controller ──> Interactor ──> RepositoryImpl ──> JpaRepository ──> PostgreSQL

【ソースコードの依存方向（import の向き）】
Controller ──> InputPort <── Interactor ──> OutputPort <── RepositoryImpl ──> JpaRepository ──> Spring Data JPA（外部ライブラリ）
(第3層)        (第2層)        (第2層)        (第2層)         (第3層)            (第3層)          (ライブラリ。依存性のルールの対象外)
```

1. 層の飛び越えと逆向き依存の禁止
   - すべての Controller は必ず `GetCategoriesUseCase` などの Input Port を呼び出します。Output Port である `*Repository` や、JPA を使う `adapter.repository` を直接参照してはいけません。
   - 第3層の `adapter` は、第1層の `domain` と第2層の `usecase` だけを参照します。外側の第4層である `framework` には一切依存しません。
2. Presenter に関する設計上の簡略化
   本来のクリーンアーキテクチャには、Use Case から出力を受け取る Output Boundary、つまり Presenter があります。しかし、JSON を返す現代の REST API では、Output Data である Interactor の戻り値を Controller で DTO に変換する構成が一般的です。本ハンズオンでもこの実践的な構成を採用します。

---

## 3. Next.js フロントエンドのディレクトリ構成

フロントエンドでも同様に、内側のビジネスロジックが、外側の通信手段やルーティング基盤に依存しない構成にします。

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

- `presentation` と `adapter` がともに第3層の Interface Adapters に属する理由
  クリーンアーキテクチャの同心円では、「UI の入力受付・画面描画」は Controller / Presenter に相当し、「外部システムとの入出力変換」は Gateway / API クライアントに相当します。どちらも内側のドメイン・ユースケースを外側の世界とつなぐ役割を持ちます。そのため、同じ第3層の Interface Adapters に位置づけます。
- UI コンポーネントから `adapter` を直接参照しない、Composition Root の考え方
  `presentation` 内の純粋な表示コンポーネントである `BalanceSummaryCard`, `TransactionForm`, `TransactionList` は、`adapter/` を直接 import してはいけません。また、Next.js App Router では、`app/transactions/page.tsx` などの Server Component から Client Component へ、関数を含むオブジェクトを props で直接渡せません。そこで、機能のルートコンテナである `TransactionSection.tsx` だけが Composition Root、つまり依存性注入の組み立て役になります。`adapter/transactionApi` をデフォルト引数で受け取り、`useTransactions` に注入します。
- Domain 層と Presentation 層の分離
  通貨フォーマットの `formatCurrency.ts` は画面表示のためのロジックなので、`presentation/utils/` に置きます。一方、金額やタイトルの業務ルールを検証する `validateTransaction.ts` は純粋なドメインルールなので、`domain/` に置きます。

---

## 4. Claude Code で進める際の確認事項

この構成をもとに、次の Chapter 2 で Claude Code 用の設計ルールファイルである `CLAUDE.md` と `.claude/rules/` を作成します。

1. 新しいファイルを作るときは、先に上記のディレクトリ構成を見て、そのファイルがどの層に属するかを確認してから配置します。層の名前は、バックエンドでは `domain` / `usecase` / `adapter` / `framework`、フロントエンドでは `domain` / `usecase` / `adapter` / `presentation` / `app` です。
2. 内側の層のファイルを作成・編集した直後は、外側の層のパッケージやフレームワーク固有の `import` が紛れ込んでいないかを確認します。
