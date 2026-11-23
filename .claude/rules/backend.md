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
