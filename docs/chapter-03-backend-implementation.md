# Chapter 3: バックエンド実装（Spring Boot / Kotlin）

## このチャプターの目的

Chapter 2 で作成した `CLAUDE.md` と `.claude/rules/backend.md` を前提として、Claude Code（AI）にバックエンドの4層を実装させます。

クリーンアーキテクチャの依存性ルールを体感するため、最も中心にある第1層（Entities）から最外層の第4層（Frameworks & Drivers）まで、内側から外側へ向かって順番に段階実装を進めます。各ステップでは Claude Code への指示プロンプトと、AI が生成すべき実装コードおよび設計上のポイントを記載しています。

1. 3-1. 第1層（domain）：純粋なビジネスルール、値オブジェクト、エンティティ
2. 3-2. 第2層（usecase）：Input Port、Output Port、Interactor によるユースケース制御
3. 3-3. 第3層（adapter）：DB アダプター（JPA Entity・JPA Repository・Repository 実装）と Web アダプター（DTO・Controller・例外ハンドラー）
4. 3-4. 第4層（framework）：CORS などのフレームワーク全体設定
5. 3-5. AI によるアーキテクチャ検証と API 疎通確認

---

## 3-1. 第1層：Entities（`domain` パッケージ）の実装

最初に、システムの中核となるドメイン層を実装させます。この層には Spring Framework や JPA の `import` 文を一切含めないことがルールです。

### Claude Code への指示プロンプト

Claude Code のチャット欄に以下を入力して実行します。

```text
.claude/rules/backend.md に従い、第1層（domain パッケージ）の以下のクラスを実装してください。
Spring や JPA のアノテーション・import は一切使わないでください。

1. domain/exception/DomainException.kt: RuntimeException を継承した独自例外
2. domain/model/TransactionType.kt: INCOME と EXPENSE を持つ enum
3. domain/model/Amount.kt: 1円以上の正の整数であることを init で検証する @JvmInline value class（違反時は DomainException）
4. domain/model/Category.kt: id, name, type を持つ data class
5. domain/model/Transaction.kt: id, category, title, amount, transactionDate, memo を持つ data class（title が空文字・空白のみ、または100文字を超える場合は DomainException、isIncome() と isExpense() メソッドを持つ）
6. domain/model/BalanceSummary.kt: totalIncome, totalExpense, balance を持ち、companion object の calculate(transactions) で集計を行う data class
```

### 実装コードと設計のポイント

Claude Code によって以下の6ファイルが作成されたことを確認します。

#### `DomainException.kt`
`backend/src/main/kotlin/com/example/budgetapp/domain/exception/DomainException.kt`

```kotlin
package com.example.budgetapp.domain.exception

class DomainException(message: String) : RuntimeException(message)
```

#### `TransactionType.kt`
`backend/src/main/kotlin/com/example/budgetapp/domain/model/TransactionType.kt`

```kotlin
package com.example.budgetapp.domain.model

enum class TransactionType {
    INCOME,  // 収入
    EXPENSE  // 支出
}
```

#### `Amount.kt`（値オブジェクト）
「金額は1円以上の正の整数でなければならない」という業務ルールをプリミティブ型（`Int`）のまま放置せず、値オブジェクトに閉じ込めます。`value class` を使うことで実行時のメモリ負荷なく型安全性を確保できます。

`backend/src/main/kotlin/com/example/budgetapp/domain/model/Amount.kt`

```kotlin
package com.example.budgetapp.domain.model

import com.example.budgetapp.domain.exception.DomainException

@JvmInline
value class Amount(val value: Int) {
    init {
        if (value <= 0) {
            throw DomainException("金額は1円以上に設定してください。")
        }
    }
}
```

#### `Category.kt`
`backend/src/main/kotlin/com/example/budgetapp/domain/model/Category.kt`

```kotlin
package com.example.budgetapp.domain.model

data class Category(
    val id: Long,
    val name: String,
    val type: TransactionType
)
```

#### `Transaction.kt`（エンティティ）
データベースのテーブル定義ではなく、アプリケーションの業務概念を表現するモデルです。`categoryId: Long` ではなく `Category` オブジェクトそのものを保持することで、自身が収入か支出かを判定する振る舞い（`isIncome()` / `isExpense()`）を持てます。

また、`init` ブロックでタイトルの必須チェックおよび100文字以内の上限チェックを検証することで、どの経路からインスタンスが生成されても不正な状態が存在しない（モデルの不変条件）ことを保証します。

`backend/src/main/kotlin/com/example/budgetapp/domain/model/Transaction.kt`

```kotlin
package com.example.budgetapp.domain.model

import com.example.budgetapp.domain.exception.DomainException
import java.time.LocalDate

data class Transaction(
    val id: Long?,
    val category: Category,
    val title: String,
    val amount: Amount,
    val transactionDate: LocalDate,
    val memo: String?
) {
    init {
        if (title.isBlank()) {
            throw DomainException("タイトルは必須です。")
        }
        if (title.length > 100) {
            throw DomainException("タイトルは100文字以内で入力してください。")
        }
    }

    fun isIncome(): Boolean = category.type == TransactionType.INCOME
    fun isExpense(): Boolean = category.type == TransactionType.EXPENSE
}
```

#### `BalanceSummary.kt`（収支集計のドメインモデル）
`backend/src/main/kotlin/com/example/budgetapp/domain/model/BalanceSummary.kt`

```kotlin
package com.example.budgetapp.domain.model

data class BalanceSummary(
    val totalIncome: Int,
    val totalExpense: Int,
    val balance: Int
) {
    companion object {
        fun calculate(transactions: List<Transaction>): BalanceSummary {
            val income = transactions
                .filter { it.isIncome() }
                .sumOf { it.amount.value }

            val expense = transactions
                .filter { it.isExpense() }
                .sumOf { it.amount.value }

            return BalanceSummary(
                totalIncome = income,
                totalExpense = expense,
                balance = income - expense
            )
        }
    }
}
```

---

## 3-2. 第2層：Use Cases（`usecase` パッケージ）の実装

次に、アプリケーションがユーザーに提供する操作手順（ユースケース）を実装させます。この層は第1層（`domain`）のみを参照し、外側の第3層（`adapter` / JPA）や第4層（`framework`）には一切依存しません。なお、Chapter 0 で説明した通り、本ハンズオンでは実用上のトレードオフとして Spring の `@Service` と `@Transactional` のみをアノテーションとして利用しています。

### Claude Code への指示プロンプト

```text
.claude/rules/backend.md に従い、第2層（usecase パッケージ）を実装してください。
adapter や framework パッケージ、jakarta.persistence は import しないでください。

1. usecase/output/CategoryRepository.kt: findAll(), findById(id) を定義するインターフェース
2. usecase/output/TransactionRepository.kt: findAll(), findById(id), save(transaction), deleteById(id) を定義するインターフェース
3. usecase/input/:
   - GetCategoriesUseCase.kt（execute(): List<Category>）
   - GetTransactionsUseCase.kt（GetTransactionsOutput と execute(): GetTransactionsOutput）
   - CreateTransactionUseCase.kt（CreateTransactionInput と execute(input): Transaction）
   - DeleteTransactionUseCase.kt（execute(transactionId: Long)）
4. usecase/interactor/: 上記4つの UseCase インターフェースを実装する Interactor クラス（@Service と @Transactional を付与し、存在しないカテゴリや明細IDの場合は DomainException をスローする）
```

### 実装コードと設計のポイント

#### Output Port（出口の契約インターフェース）

ユースケースが外部（データベース等）にデータ永続化を依頼するためのインターフェースです。実装クラスは第3層（`adapter.repository`）に配置するため、依存性逆転の原則が成立します。

`backend/src/main/kotlin/com/example/budgetapp/usecase/output/CategoryRepository.kt`

```kotlin
package com.example.budgetapp.usecase.output

import com.example.budgetapp.domain.model.Category

interface CategoryRepository {
    fun findAll(): List<Category>
    fun findById(id: Long): Category?
}
```

`backend/src/main/kotlin/com/example/budgetapp/usecase/output/TransactionRepository.kt`

```kotlin
package com.example.budgetapp.usecase.output

import com.example.budgetapp.domain.model.Transaction

interface TransactionRepository {
    fun findAll(): List<Transaction>
    fun findById(id: Long): Transaction?
    fun save(transaction: Transaction): Transaction
    fun deleteById(id: Long)
}
```

#### Input Port（入り口の契約インターフェース）

外側の Controller がユースケースを呼び出すための契約です。1つの操作につき1つのインターフェースを定義することで、責務の肥大化を防ぎます。

`backend/src/main/kotlin/com/example/budgetapp/usecase/input/GetCategoriesUseCase.kt`

```kotlin
package com.example.budgetapp.usecase.input

import com.example.budgetapp.domain.model.Category

interface GetCategoriesUseCase {
    fun execute(): List<Category>
}
```

`backend/src/main/kotlin/com/example/budgetapp/usecase/input/GetTransactionsUseCase.kt`

```kotlin
package com.example.budgetapp.usecase.input

import com.example.budgetapp.domain.model.BalanceSummary
import com.example.budgetapp.domain.model.Transaction

data class GetTransactionsOutput(
    val transactions: List<Transaction>,
    val summary: BalanceSummary
)

interface GetTransactionsUseCase {
    fun execute(): GetTransactionsOutput
}
```

`backend/src/main/kotlin/com/example/budgetapp/usecase/input/CreateTransactionUseCase.kt`

```kotlin
package com.example.budgetapp.usecase.input

import com.example.budgetapp.domain.model.Transaction
import java.time.LocalDate

data class CreateTransactionInput(
    val categoryId: Long,
    val title: String,
    val amount: Int,
    val transactionDate: LocalDate,
    val memo: String?
)

interface CreateTransactionUseCase {
    fun execute(input: CreateTransactionInput): Transaction
}
```

`backend/src/main/kotlin/com/example/budgetapp/usecase/input/DeleteTransactionUseCase.kt`

```kotlin
package com.example.budgetapp.usecase.input

interface DeleteTransactionUseCase {
    fun execute(transactionId: Long)
}
```

#### Interactor（ユースケースの実装）

`backend/src/main/kotlin/com/example/budgetapp/usecase/interactor/GetCategoriesInteractor.kt`

```kotlin
package com.example.budgetapp.usecase.interactor

import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.usecase.input.GetCategoriesUseCase
import com.example.budgetapp.usecase.output.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetCategoriesInteractor(
    private val categoryRepository: CategoryRepository
) : GetCategoriesUseCase {

    @Transactional(readOnly = true)
    override fun execute(): List<Category> {
        return categoryRepository.findAll()
    }
}
```

`backend/src/main/kotlin/com/example/budgetapp/usecase/interactor/GetTransactionsInteractor.kt`

```kotlin
package com.example.budgetapp.usecase.interactor

import com.example.budgetapp.domain.model.BalanceSummary
import com.example.budgetapp.usecase.input.GetTransactionsOutput
import com.example.budgetapp.usecase.input.GetTransactionsUseCase
import com.example.budgetapp.usecase.output.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetTransactionsInteractor(
    private val transactionRepository: TransactionRepository
) : GetTransactionsUseCase {

    @Transactional(readOnly = true)
    override fun execute(): GetTransactionsOutput {
        val transactions = transactionRepository.findAll()
        val summary = BalanceSummary.calculate(transactions)
        return GetTransactionsOutput(
            transactions = transactions,
            summary = summary
        )
    }
}
```

`backend/src/main/kotlin/com/example/budgetapp/usecase/interactor/CreateTransactionInteractor.kt`

```kotlin
package com.example.budgetapp.usecase.interactor

import com.example.budgetapp.domain.exception.DomainException
import com.example.budgetapp.domain.model.Amount
import com.example.budgetapp.domain.model.Transaction
import com.example.budgetapp.usecase.input.CreateTransactionInput
import com.example.budgetapp.usecase.input.CreateTransactionUseCase
import com.example.budgetapp.usecase.output.CategoryRepository
import com.example.budgetapp.usecase.output.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateTransactionInteractor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : CreateTransactionUseCase {

    @Transactional
    override fun execute(input: CreateTransactionInput): Transaction {
        val category = categoryRepository.findById(input.categoryId)
            ?: throw DomainException("指定されたカテゴリが存在しません (ID: ${input.categoryId})")

        val transaction = Transaction(
            id = null,
            category = category,
            title = input.title,
            amount = Amount(input.amount),
            transactionDate = input.transactionDate,
            memo = input.memo
        )

        return transactionRepository.save(transaction)
    }
}
```

`backend/src/main/kotlin/com/example/budgetapp/usecase/interactor/DeleteTransactionInteractor.kt`

```kotlin
package com.example.budgetapp.usecase.interactor

import com.example.budgetapp.domain.exception.DomainException
import com.example.budgetapp.usecase.input.DeleteTransactionUseCase
import com.example.budgetapp.usecase.output.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteTransactionInteractor(
    private val transactionRepository: TransactionRepository
) : DeleteTransactionUseCase {

    @Transactional
    override fun execute(transactionId: Long) {
        transactionRepository.findById(transactionId)
            ?: throw DomainException("指定された明細が存在しません (ID: $transactionId)")

        transactionRepository.deleteById(transactionId)
    }
}
```

---

## 3-3. 第3層：Interface Adapters（`adapter` パッケージ）の実装

続いて、第2層（Use Cases）と外部（DB・HTTP）の間でデータ形式を変換するアダプター層を実装させます。

この層は2つのサブパッケージに分かれます。
- `adapter.repository`（DB アダプター）：JPA Entity、Spring Data JPA インターフェース、および Output Port を実装する `*RepositoryImpl`
- `adapter.controller`（Web アダプター）：REST Controller、DTO、および共通例外ハンドラー

### Claude Code への指示プロンプト

```text
.claude/rules/backend.md に従い、第3層（adapter パッケージ）を実装してください。
外側の framework パッケージは import しないでください。また、Controller は必ず usecase/input の *UseCase インターフェースのみに依存し、usecase/output や adapter/repository を直接呼び出さないでください。

1. adapter/repository/entity/:
   - CategoryJpaEntity.kt（categories テーブルにマッピングする @Entity）
   - TransactionJpaEntity.kt（transactions テーブルにマッピングする @Entity）
2. adapter/repository/jpa/:
   - JpaCategoryRepository.kt（JpaRepository<CategoryJpaEntity, Long>）
   - JpaTransactionRepository.kt（JpaRepository<TransactionJpaEntity, Long> と findAllByOrderByTransactionDateDescIdDesc()）
3. adapter/repository/:
   - CategoryRepositoryImpl.kt（CategoryRepository を実装）
   - TransactionRepositoryImpl.kt（TransactionRepository を実装し、CategoryJpaEntity から Category への変換はプライベート拡張関数で共通化）
4. adapter/controller/dto/:
   - CategoryResponse.kt
   - TransactionRequest.kt（jakarta.validation の @NotNull, @NotBlank, @Size(max = 100), @Positive を付与し、toUseCaseInput() を持つ）
   - TransactionListResponse.kt（TransactionItemResponse, BalanceSummaryResponse, TransactionListResponse を定義）
5. adapter/controller/:
   - CategoryController.kt（GET /api/categories）
   - TransactionController.kt（GET /api/transactions, POST /api/transactions [201 Created], DELETE /api/transactions/{id} [204 No Content]）
6. adapter/controller/advice/GlobalExceptionHandler.kt:
   - MethodArgumentNotValidException、HttpMessageNotReadableException（JSON の形式不正・日付形式の誤りなど）、DomainException を捕捉し、400 Bad Request と ErrorResponse(message, details) を返す @RestControllerAdvice
```

### 実装コードと設計のポイント

#### JPA Entity（DB テーブルとのマッピング）
JPA 用のエンティティをドメインモデルと分離することで、RDB のテーブル構造（外部キーの `categoryId: Long` やデフォルト引数コンストラクタの制約）がドメイン層の設計に影響を与えるのを防ぎます。

`backend/src/main/kotlin/com/example/budgetapp/adapter/repository/entity/CategoryJpaEntity.kt`

```kotlin
package com.example.budgetapp.adapter.repository.entity

import jakarta.persistence.*

@Entity
@Table(name = "categories")
class CategoryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 50)
    var name: String = "",

    @Column(nullable = false, length = 10)
    var type: String = ""
)
```

`backend/src/main/kotlin/com/example/budgetapp/adapter/repository/entity/TransactionJpaEntity.kt`

```kotlin
package com.example.budgetapp.adapter.repository.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "transactions")
class TransactionJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "category_id", nullable = false)
    var categoryId: Long = 0,

    @Column(nullable = false, length = 100)
    var title: String = "",

    @Column(nullable = false)
    var amount: Int = 0,

    @Column(name = "transaction_date", nullable = false)
    var transactionDate: LocalDate = LocalDate.now(),

    @Column(columnDefinition = "TEXT")
    var memo: String? = null
)
```

#### Spring Data JPA Repository

`backend/src/main/kotlin/com/example/budgetapp/adapter/repository/jpa/JpaCategoryRepository.kt`

```kotlin
package com.example.budgetapp.adapter.repository.jpa

import com.example.budgetapp.adapter.repository.entity.CategoryJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaCategoryRepository : JpaRepository<CategoryJpaEntity, Long>
```

`backend/src/main/kotlin/com/example/budgetapp/adapter/repository/jpa/JpaTransactionRepository.kt`

```kotlin
package com.example.budgetapp.adapter.repository.jpa

import com.example.budgetapp.adapter.repository.entity.TransactionJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaTransactionRepository : JpaRepository<TransactionJpaEntity, Long> {
    fun findAllByOrderByTransactionDateDescIdDesc(): List<TransactionJpaEntity>
}
```

#### Repository 実装（DB とドメインモデルの変換）

`backend/src/main/kotlin/com/example/budgetapp/adapter/repository/CategoryRepositoryImpl.kt`

```kotlin
package com.example.budgetapp.adapter.repository

import com.example.budgetapp.adapter.repository.entity.CategoryJpaEntity
import com.example.budgetapp.adapter.repository.jpa.JpaCategoryRepository
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.TransactionType
import com.example.budgetapp.usecase.output.CategoryRepository
import org.springframework.stereotype.Component

@Component
class CategoryRepositoryImpl(
    private val jpaCategoryRepository: JpaCategoryRepository
) : CategoryRepository {

    override fun findAll(): List<Category> {
        return jpaCategoryRepository.findAll().map { it.toDomain() }
    }

    override fun findById(id: Long): Category? {
        return jpaCategoryRepository.findById(id).orElse(null)?.toDomain()
    }

    private fun CategoryJpaEntity.toDomain(): Category = Category(
        id = this.id ?: throw IllegalStateException("カテゴリIDが存在しません"),
        name = this.name,
        type = TransactionType.valueOf(this.type)
    )
}
```

`backend/src/main/kotlin/com/example/budgetapp/adapter/repository/TransactionRepositoryImpl.kt`

```kotlin
package com.example.budgetapp.adapter.repository

import com.example.budgetapp.adapter.repository.entity.CategoryJpaEntity
import com.example.budgetapp.adapter.repository.entity.TransactionJpaEntity
import com.example.budgetapp.adapter.repository.jpa.JpaCategoryRepository
import com.example.budgetapp.adapter.repository.jpa.JpaTransactionRepository
import com.example.budgetapp.domain.model.Amount
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.Transaction
import com.example.budgetapp.domain.model.TransactionType
import com.example.budgetapp.usecase.output.TransactionRepository
import org.springframework.stereotype.Component

@Component
class TransactionRepositoryImpl(
    private val jpaTransactionRepository: JpaTransactionRepository,
    private val jpaCategoryRepository: JpaCategoryRepository
) : TransactionRepository {

    override fun findAll(): List<Transaction> {
        val categoryMap = jpaCategoryRepository.findAll().associateBy { it.id }
        return jpaTransactionRepository.findAllByOrderByTransactionDateDescIdDesc().map { entity ->
            val categoryEntity = categoryMap[entity.categoryId]
                ?: throw IllegalStateException("カテゴリが見つかりません (ID: ${entity.categoryId})")
            entity.toDomain(categoryEntity.toDomain())
        }
    }

    override fun findById(id: Long): Transaction? {
        val entity = jpaTransactionRepository.findById(id).orElse(null) ?: return null
        val categoryEntity = jpaCategoryRepository.findById(entity.categoryId).orElse(null)
            ?: throw IllegalStateException("カテゴリが見つかりません (ID: ${entity.categoryId})")

        return entity.toDomain(categoryEntity.toDomain())
    }

    override fun save(transaction: Transaction): Transaction {
        val saved = jpaTransactionRepository.save(transaction.toEntity())
        return saved.toDomain(transaction.category)
    }

    override fun deleteById(id: Long) {
        jpaTransactionRepository.deleteById(id)
    }

    private fun CategoryJpaEntity.toDomain(): Category = Category(
        id = this.id ?: throw IllegalStateException("カテゴリIDが存在しません"),
        name = this.name,
        type = TransactionType.valueOf(this.type)
    )

    private fun TransactionJpaEntity.toDomain(category: Category): Transaction = Transaction(
        id = this.id,
        category = category,
        title = this.title,
        amount = Amount(this.amount),
        transactionDate = this.transactionDate,
        memo = this.memo
    )

    private fun Transaction.toEntity(): TransactionJpaEntity = TransactionJpaEntity(
        id = this.id,
        categoryId = this.category.id,
        title = this.title,
        amount = this.amount.value,
        transactionDate = this.transactionDate,
        memo = this.memo
    )
}
```

#### DTO（HTTP リクエスト・レスポンスの型定義）

ドメインモデルを直接 JSON として返却せず、API 専用の DTO に変換することで、API のレスポンス形式変更とドメインロジックの変更を分離します。

なお、`TransactionRequest` の `@NotBlank` / `@Size(max = 100)` / `@Positive` は「HTTP リクエスト境界での入力検証」を担い、`Transaction` / `Amount` の検証は「ドメインモデル自身の不変条件の保証」を担うため、両方に検証を記述して責務を分担しています。

`backend/src/main/kotlin/com/example/budgetapp/adapter/controller/dto/CategoryResponse.kt`

```kotlin
package com.example.budgetapp.adapter.controller.dto

import com.example.budgetapp.domain.model.Category

data class CategoryResponse(
    val id: Long,
    val name: String,
    val type: String
) {
    companion object {
        fun fromDomain(category: Category): CategoryResponse = CategoryResponse(
            id = category.id,
            name = category.name,
            type = category.type.name
        )
    }
}
```

`backend/src/main/kotlin/com/example/budgetapp/adapter/controller/dto/TransactionRequest.kt`

```kotlin
package com.example.budgetapp.adapter.controller.dto

import com.example.budgetapp.usecase.input.CreateTransactionInput
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class TransactionRequest(
    @field:NotNull(message = "カテゴリIDは必須です。")
    val categoryId: Long?,

    @field:NotBlank(message = "タイトルは必須です。")
    @field:Size(max = 100, message = "タイトルは100文字以内で入力してください。")
    val title: String?,

    @field:NotNull(message = "金額は必須です。")
    @field:Positive(message = "金額は1円以上の正の整数で入力してください。")
    val amount: Int?,

    @field:NotNull(message = "決済日は必須です。")
    val transactionDate: LocalDate?,

    val memo: String?
) {
    fun toUseCaseInput(): CreateTransactionInput = CreateTransactionInput(
        categoryId = categoryId!!,
        title = title!!,
        amount = amount!!,
        transactionDate = transactionDate!!,
        memo = memo
    )
}
```

`backend/src/main/kotlin/com/example/budgetapp/adapter/controller/dto/TransactionListResponse.kt`

```kotlin
package com.example.budgetapp.adapter.controller.dto

import com.example.budgetapp.domain.model.BalanceSummary
import com.example.budgetapp.domain.model.Transaction
import java.time.LocalDate

data class TransactionItemResponse(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryType: String,
    val title: String,
    val amount: Int,
    val transactionDate: LocalDate,
    val memo: String?
) {
    companion object {
        fun fromDomain(domain: Transaction): TransactionItemResponse = TransactionItemResponse(
            id = domain.id ?: throw IllegalStateException("IDが確定していません"),
            categoryId = domain.category.id,
            categoryName = domain.category.name,
            categoryType = domain.category.type.name,
            title = domain.title,
            amount = domain.amount.value,
            transactionDate = domain.transactionDate,
            memo = domain.memo
        )
    }
}

data class BalanceSummaryResponse(
    val totalIncome: Int,
    val totalExpense: Int,
    val balance: Int
) {
    companion object {
        fun fromDomain(summary: BalanceSummary): BalanceSummaryResponse = BalanceSummaryResponse(
            totalIncome = summary.totalIncome,
            totalExpense = summary.totalExpense,
            balance = summary.balance
        )
    }
}

data class TransactionListResponse(
    val transactions: List<TransactionItemResponse>,
    val summary: BalanceSummaryResponse
)
```

#### Controller（REST API エンドポイント）

`backend/src/main/kotlin/com/example/budgetapp/adapter/controller/CategoryController.kt`

```kotlin
package com.example.budgetapp.adapter.controller

import com.example.budgetapp.adapter.controller.dto.CategoryResponse
import com.example.budgetapp.usecase.input.GetCategoriesUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val getCategoriesUseCase: GetCategoriesUseCase
) {
    @GetMapping
    fun getAll(): List<CategoryResponse> {
        return getCategoriesUseCase.execute().map { CategoryResponse.fromDomain(it) }
    }
}
```

`backend/src/main/kotlin/com/example/budgetapp/adapter/controller/TransactionController.kt`

```kotlin
package com.example.budgetapp.adapter.controller

import com.example.budgetapp.adapter.controller.dto.*
import com.example.budgetapp.usecase.input.CreateTransactionUseCase
import com.example.budgetapp.usecase.input.DeleteTransactionUseCase
import com.example.budgetapp.usecase.input.GetTransactionsUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/transactions")
class TransactionController(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase
) {
    @GetMapping
    fun getAll(): TransactionListResponse {
        val output = getTransactionsUseCase.execute()
        return TransactionListResponse(
            transactions = output.transactions.map { TransactionItemResponse.fromDomain(it) },
            summary = BalanceSummaryResponse.fromDomain(output.summary)
        )
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: TransactionRequest): TransactionItemResponse {
        val created = createTransactionUseCase.execute(request.toUseCaseInput())
        return TransactionItemResponse.fromDomain(created)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        deleteTransactionUseCase.execute(id)
    }
}
```

#### GlobalExceptionHandler（例外ハンドリングの共通化）

`backend/src/main/kotlin/com/example/budgetapp/adapter/controller/advice/GlobalExceptionHandler.kt`

```kotlin
package com.example.budgetapp.adapter.controller.advice

import com.example.budgetapp.domain.exception.DomainException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorResponse(
    val message: String,
    val details: List<String> = emptyList()
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = "入力内容に誤りがあります。", details = errors))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = "リクエストの形式が不正です。"))
    }

    @ExceptionHandler(DomainException::class)
    fun handleDomainException(ex: DomainException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = ex.message ?: "ビジネスルール違反が発生しました。"))
    }
}
```

---

## 3-4. 第4層：Frameworks & Drivers（`framework` パッケージ）の実装

最外層である第4層（`framework`）には、フロントエンド（`http://localhost:3000`）からの通信を許可するための CORS 設定など、フレームワーク全体の横断設定を配置します。

### Claude Code への指示プロンプト

```text
.claude/rules/backend.md に従い、第4層（framework パッケージ）を実装してください。

1. framework/config/CorsConfig.kt: /api/** に対して http://localhost:3000 からの GET, POST, PUT, DELETE, OPTIONS を許可する WebMvcConfigurer 実装
```

### 実装コードと設計のポイント

#### CORS 設定

`backend/src/main/kotlin/com/example/budgetapp/framework/config/CorsConfig.kt`

```kotlin
package com.example.budgetapp.framework.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
    }
}
```

---

## 3-5. AI によるアーキテクチャ検証と API 疎通確認

実装が完了したら、Claude Code にアーキテクチャ規約のセルフレビューと API の疎通確認を実行させます。

### Claude Code への指示プロンプト

```text
backend/src/main/kotlin/com/example/budgetapp 配下の全ファイルについて、以下の2点を実施してください。

1. .claude/rules/backend.md に記載された4層の依存方向ルール（domain → なし、usecase → domain のみ、adapter → domain / usecase のみ、framework → 内側のみ）と層の飛び越え禁止ルールに違反している箇所がないか検査して報告する。
2. cd backend && ./gradlew compileKotlin を実行してコンパイルエラーがないことを確認する。
```

レビューで違反がなくコンパイルが通ったら、API の動作を Claude Code に確認させます。

```text
Spring Boot アプリケーションを起動し、curl を使って以下の4パターンの API 疎通テストを実行して結果を報告してください。テスト後はサーバーを停止してください。

1. GET /api/categories で初期マスタデータが返ること
2. POST /api/transactions で新規明細を登録し 201 Created が返ること
3. POST /api/transactions で amount に 0 を指定し、GlobalExceptionHandler により 400 Bad Request と統一エラー JSON が返ること
4. DELETE /api/transactions/{id} で登録した明細を削除し 204 No Content が返ること
```

4パターンすべての動作が確認できれば、バックエンドの実装は完了です。次の Chapter 4 では、今回実装したコードに対して自動テスト（ドメイン単体テスト、MockK によるユースケーステスト、MockMvc 統合テスト、Konsist アーキテクチャテスト）を構築します。
