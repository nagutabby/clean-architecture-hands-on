# Chapter 3: Backend Implementation with Spring Boot / Kotlin

## Purpose of This Chapter

Based on the `CLAUDE.md` and `.claude/rules/backend.md` created in Chapter 2, you have Claude Code, an AI, implement the four backend layers.

To experience the Dependency Rule of Clean Architecture, you implement from the innermost layer 1, Entities, to the outermost layer 4, Frameworks & Drivers, working from the inside out. Each checkpoint gives the instruction for Claude Code, the implementation code the AI should produce, and the design points to note.

Implement the layers from the inside out, then verify the behavior feature by feature.

| CP | Kind | Content |
| :--- | :--- | :--- |
| CP3-1 | Design | Have the AI present a backend design that includes the class list, dependency directions, and API specification, and approve it |
| CP3-2 | Implementation | Layer 1 `domain`: value objects, entities, aggregation logic |
| CP3-3 | Implementation | Layer 2 `usecase`: Output Port, Input Port, Interactor |
| CP3-4 | Implementation | Layer 3 `adapter`, DB side: JPA Entity, JPA Repository, Repository implementation |
| CP3-5 | Implementation | Layer 3 `adapter`, Web side: DTO, Controller, exception handler |
| CP3-6 | Implementation | Layer 4 `framework`: CORS configuration |
| CP3-7 | Test | Dependency direction rule check and compile check |
| CP3-8 | Verification | Start the server, category list, and transaction list in the empty state |
| CP3-9 | Verification | Create transactions, check the list and totals, and input errors returning 400 |
| CP3-10 | Verification | Delete transactions, delete a non-existent ID, stop the server |

## How to Proceed in This Chapter

In this chapter, you proceed through the checkpoints, or CPs, in the table above **one at a time**. After finishing each CP, Claude Code reports the result, stops, and waits for the next instruction. This behavior is defined in the "Checkpoint operation" section of `CLAUDE.md`.

1. Start Claude Code and enter the "Start prompt" below. Claude Code carries out only the first CP and stops.
2. Compare the report with the "Points to check" of each CP to verify the result.
3. If there are no problems, type "Proceed to the next checkpoint". Claude Code carries out exactly one next CP.
4. If there are problems, do not move on. Instead, instruct it like "Redo CPx-y. XX is wrong", and have it fix the issue within that CP.

### Start Prompt

Enter the following in the Claude Code chat field and run it. Claude Code carries out only CP3-1 and stops.

```text
We will now proceed through Chapter 3 (backend implementation) in the following 10 checkpoints (CPs).
Following "Checkpoint operation" in CLAUDE.md, first carry out only CP3-1, report, and stop.
After that, each time I type "Proceed to the next checkpoint", carry out exactly one next CP.
Follow .claude/rules/backend.md for all implementation.

Backend features to build: get the category list, get the transaction list (with totals for total income, total expense, and balance), create a transaction, delete a transaction.
Assume the DB tables defined in backend/src/main/resources/db/migration/V1__create_tables.sql.

[CP3-1 Design] Do not create any code or files. Present the following four items.
1. A list of classes to place in each of the four layers (package, class name, responsibility)
2. A dependency table (including the correspondence between Output Ports and implementation classes, and the UseCase each Controller depends on)
3. A list of REST APIs (method, path, request, success status code)
4. Design concerns in light of .claude/rules/backend.md (write "None" if there are none)

[CP3-2 Implementation] Implement layer 1 (domain package). Do not use any Spring or JPA annotations or imports.
1. domain/exception/DomainException.kt: a custom exception extending RuntimeException
2. domain/model/TransactionType.kt: an enum with INCOME and EXPENSE
3. domain/model/Amount.kt: a @JvmInline value class that verifies in init that the value is a positive integer of 1 yen or more (throw DomainException on violation)
4. domain/model/Category.kt: a data class with id, name, type
5. domain/model/Transaction.kt: a data class with id, category, title, amount, transactionDate, memo (throw DomainException if title is empty, blank only, or longer than 100 characters; has isIncome() and isExpense() methods)
6. domain/model/BalanceSummary.kt: a data class with totalIncome, totalExpense, balance, which aggregates in calculate(transactions) of the companion object
Completion condition: confirm with grep that there are no org.springframework / jakarta imports under domain, and that cd backend && ./gradlew compileKotlin succeeds.

[CP3-3 Implementation] Implement layer 2 (usecase package). Do not import the adapter or framework packages, or jakarta.persistence.
1. usecase/output/CategoryRepository.kt: an interface defining findAll(), findById(id)
2. usecase/output/TransactionRepository.kt: an interface defining findAll(), findById(id), save(transaction), deleteById(id)
3. usecase/input/:
   - GetCategoriesUseCase.kt (execute(): List<Category>)
   - GetTransactionsUseCase.kt (GetTransactionsOutput and execute(): GetTransactionsOutput)
   - CreateTransactionUseCase.kt (CreateTransactionInput and execute(input): Transaction)
   - DeleteTransactionUseCase.kt (execute(transactionId: Long))
4. usecase/interactor/: Interactor classes implementing the four UseCase interfaces above (annotate with @Service and @Transactional, and throw DomainException when the category or transaction ID does not exist)
Completion condition: confirm with grep that there are no adapter / framework / jakarta.persistence imports under usecase, and that compileKotlin succeeds.

[CP3-4 Implementation] Implement the DB side (adapter.repository) of layer 3. Do not import the outer framework package.
1. adapter/repository/entity/:
   - CategoryJpaEntity.kt (an @Entity mapped to the categories table)
   - TransactionJpaEntity.kt (an @Entity mapped to the transactions table)
2. adapter/repository/jpa/:
   - JpaCategoryRepository.kt (JpaRepository<CategoryJpaEntity, Long>)
   - JpaTransactionRepository.kt (JpaRepository<TransactionJpaEntity, Long> and findAllByOrderByTransactionDateDescIdDesc())
3. adapter/repository/:
   - CategoryRepositoryImpl.kt (implements CategoryRepository)
   - TransactionRepositoryImpl.kt (implements TransactionRepository, and shares the conversion from CategoryJpaEntity to Category through a private extension function)
Completion condition: compileKotlin succeeds.

[CP3-5 Implementation] Implement the Web side (adapter.controller) of layer 3. Do not import the outer framework package. A Controller must depend only on the *UseCase interfaces in usecase/input, and must never call usecase/output or adapter/repository directly.
1. adapter/controller/dto/:
   - CategoryResponse.kt
   - TransactionRequest.kt (annotated with jakarta.validation @NotNull, @NotBlank, @Size(max = 100), @Positive, and has toUseCaseInput())
   - TransactionListResponse.kt (defines TransactionItemResponse, BalanceSummaryResponse, TransactionListResponse)
2. adapter/controller/:
   - CategoryController.kt (GET /api/categories)
   - TransactionController.kt (GET /api/transactions, POST /api/transactions [201 Created], DELETE /api/transactions/{id} [204 No Content])
3. adapter/controller/advice/GlobalExceptionHandler.kt:
   - A @RestControllerAdvice that catches MethodArgumentNotValidException, HttpMessageNotReadableException (malformed JSON, incorrect date format, etc.), and DomainException, and returns 400 Bad Request with ErrorResponse(message, details)
Completion condition: compileKotlin succeeds.

[CP3-6 Implementation] Implement layer 4 (framework package).
1. framework/config/CorsConfig.kt: a WebMvcConfigurer implementation that allows GET, POST, PUT, DELETE, OPTIONS from http://localhost:3000 for /api/**
Completion condition: compileKotlin succeeds.

[CP3-7 Test] For all files under backend/src/main/kotlin/com/example/budgetapp, do the following two things.
1. Check whether anything violates the four-layer dependency direction rules described in .claude/rules/backend.md (domain -> none, usecase -> domain only, adapter -> domain / usecase only, framework -> inner layers only) and the rule prohibiting skipping layers, and report the result.
2. Run cd backend && ./gradlew compileKotlin and confirm there are no compile errors.

[CP3-8 Verification] Start the Spring Boot application in the background with bootRun, confirm that startup has completed (the "Started ..." log), and then confirm the following two points with curl. Leave the server running for the next CP.
1. GET /api/categories returns 200 with the 5 initial master data records
2. GET /api/transactions returns 200 with no transactions, and totalIncome / totalExpense / balance in summary are all 0

[CP3-9 Verification] If the server is not running, start it, and confirm the following four points with curl. Leave the server running.
1. Create a salary (categoryId=4) transaction of 300000 yen and a food (categoryId=1) transaction of 1500 yen with POST /api/transactions, and both return 201 Created
2. GET /api/transactions reflects the 2 transactions, and summary is totalIncome=300000, totalExpense=1500, balance=298500
3. POSTing with amount set to 0 returns 400 Bad Request and the unified error JSON (message, details) from GlobalExceptionHandler
4. POSTing with a string that cannot be interpreted as a date in transactionDate returns 400 Bad Request and the unified error JSON (message)

[CP3-10 Verification] If the server is not running, start it, and confirm the following three points with curl.
1. Delete the 2 transactions created in CP3-9 with DELETE /api/transactions/{id}, and both return 204 No Content
2. GET /api/transactions shows the transactions empty again, and summary is all 0
3. DELETE of a non-existent ID (99999) returns 400 Bad Request with an error message
After verifying, stop the started server and confirm that port 8080 has been released.
```

---

## CP3-1 [Design] Present and Approve the Backend Design

Before writing code, have Claude Code produce only a design. In Clean Architecture, deciding in advance which classes go in which layer and who depends on whom makes it less likely that later implementation skips a layer.

Claude Code creates no files. Check that the presented design matches `.claude/rules/backend.md` and the directory structure from Chapter 1.

### Guideline for the Design

If the following is presented, you can proceed to the later CPs.

| Layer | Classes | Content |
| :--- | :--- | :--- |
| `domain` | 6 | `DomainException`, `TransactionType`, `Amount`, `Category`, `Transaction`, `BalanceSummary` |
| `usecase` | 10 | 2 Output Ports, 4 Input Ports for getting Categories, getting Transactions, Create, and Delete, and 4 Interactors |
| `adapter` | 12 | 2 JPA Entities, 2 Spring Data Repositories, 2 Repository implementations, 3 DTOs, 2 Controllers, 1 `GlobalExceptionHandler` |
| `framework` | 1 | `CorsConfig` |

There are four REST APIs: `GET /api/categories`, `GET /api/transactions`, `POST /api/transactions`, and `DELETE /api/transactions/{id}`. A successful POST returns 201 and a successful DELETE returns 204.

### Points to Check

- All dependency directions are "outer -> inner". `domain` depends on nothing
- `TransactionRepository`, the Output Port, is placed in `usecase`, and its implementation class is placed in `adapter`
- Controllers depend only on `*UseCase`, the Input Port

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. Next is CP3-2.

---

## CP3-2 [Implementation] Layer 1 Entities: `domain` Package

First, have the AI implement the domain layer at the core of the system. The rule is that this layer contains no `import` statements from Spring Framework or JPA at all.

### Implementation Code and Design Points

Confirm that Claude Code created the following 6 files.

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
    INCOME,  // income
    EXPENSE  // expense
}
```

#### `Amount.kt`: Value Object
Instead of leaving the business rule "an amount must be a positive integer of 1 yen or more" in a primitive `Int`, enclose it in a value object. A `value class` gives you type safety without runtime memory overhead.

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

#### `Transaction.kt`: Entity
This model expresses the business concepts of the application rather than the database table definition. By holding the `Category` object itself instead of `categoryId: Long`, it can have behavior that determines whether it is income or expense, namely `isIncome()` and `isExpense()`.

In addition, the `init` block checks that the title is present and at most 100 characters. This guarantees that no instance can be in an invalid state, however it is created. This is the model invariant.

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

#### `BalanceSummary.kt`: Domain Model for Income and Expense Totals
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

### Points to Check

- 6 files are created under `domain/`
- Claude Code's report includes that there were 0 `org.springframework` / `jakarta` imports and that `compileKotlin` succeeded
- `Amount` rejects values below 1 yen, and `Transaction` rejects an empty title and a title of 101 or more characters, with `DomainException`

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. Next is CP3-3.

---

## CP3-3 [Implementation] Layer 2 Use Cases: `usecase` Package

Next, have the AI implement the use cases, which are the operations the application offers to users. This layer references only layer 1, `domain`, and does not depend at all on the outer layer 3, `adapter` and JPA, or layer 4, `framework`. As explained in Chapter 0, this hands-on uses only Spring's `@Service` and `@Transactional` as annotations, as a practical trade-off.

### Implementation Code and Design Points

#### Output Port: The Outgoing Contract Interface

These are the interfaces through which a use case asks the outside, such as the database, to save data. Because the implementation classes are placed in layer 3, `adapter.repository`, the Dependency Inversion Principle holds.

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

#### Input Port: The Incoming Contract Interface

This is the contract through which the outer Controller calls a use case. Defining one interface per operation prevents responsibilities from growing too large.

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

#### Interactor: Implementation of the Use Cases

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

### Points to Check

- Under `usecase/`, 2 files in `output`, 4 files in `input`, and 4 files in `interactor` are created
- The `Interactor` classes do not import classes from `adapter` and depend only on the `*Repository` interfaces
- Claude Code's report includes that there were 0 `adapter` / `framework` / `jakarta.persistence` imports and that `compileKotlin` succeeded

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. Next is CP3-4.

---

## CP3-4 [Implementation] Layer 3 Interface Adapters: DB Side `adapter.repository`

Next, have the AI implement the adapter layer, which converts data formats between layer 2, Use Cases, and the outside, such as the DB and HTTP. This layer is divided into two subpackages: the DB side is implemented in CP3-4 and the Web side in CP3-5.

- `adapter.repository`, the DB adapter: JPA Entities, Spring Data JPA interfaces, and `*RepositoryImpl` classes that implement the Output Ports
- `adapter.controller`, the Web adapter: REST Controllers, DTOs, and the common exception handler

### Implementation Code and Design Points

#### JPA Entity: Mapping to DB Tables
By keeping JPA entities separate from domain models, the RDB table structure does not affect the design of the domain layer. Examples of that structure are the foreign key `categoryId: Long` and the default-argument constructor constraint.

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

#### Repository Implementation: Conversion Between the DB and Domain Models

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

### Points to Check

- Under `adapter/repository/`, 2 files in `entity`, 2 files in `jpa`, and 2 `*RepositoryImpl` files are created
- JPA annotations such as `@Entity` exist only in `adapter/repository/entity` and are not attached to `domain` classes
- `*RepositoryImpl` implements the `usecase.output` interfaces and converts between JPA Entities and domain models in both directions
- Claude Code's report includes that `compileKotlin` succeeded

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. Next is CP3-5.

---

## CP3-5 [Implementation] Layer 3 Interface Adapters: Web Side `adapter.controller`

After the DB side, have the AI implement the Web side, which handles HTTP input and output. The important points are that Controllers depend only on the `usecase.input` interfaces and never call a Repository directly.

### Implementation Code and Design Points

#### DTO: Type Definitions for HTTP Requests and Responses

Instead of returning domain models directly as JSON, converting them to API-specific DTOs separates changes to the API response format from changes to domain logic.

Note that `@NotBlank` / `@Size(max = 100)` / `@Positive` on `TransactionRequest` handle "input validation at the HTTP request boundary", while the validation in `Transaction` / `Amount` handles "guaranteeing the invariants of the domain model itself". Validation is therefore written in both places to divide the responsibilities.

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

#### Controller: REST API Endpoints

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

#### GlobalExceptionHandler: Common Exception Handling

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

### Points to Check

- Under `adapter/controller/`, 3 files in `dto`, `CategoryController`, `TransactionController`, and `advice/GlobalExceptionHandler` are created
- The Controller imports do not include `usecase.output` or `adapter.repository`
- `GlobalExceptionHandler` handles three kinds: `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, and `DomainException`
- Claude Code's report includes that `compileKotlin` succeeded

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. Next is CP3-6.

---

## CP3-6 [Implementation] Layer 4 Frameworks & Drivers: `framework` Package

In the outermost layer 4, `framework`, place settings that apply to the framework as a whole, such as the CORS configuration that allows communication from the frontend running at `http://localhost:3000`.

### Implementation Code and Design Points

#### CORS Configuration

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

### Points to Check

- Only `framework/config/CorsConfig.kt` is created
- The allowed origin is `http://localhost:3000`, and it is limited to `/api/**`

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. Next is CP3-7.

---

## CP3-7 [Test] Architecture Rule Check and Compile Check

Once the implementation is complete, have Claude Code run a self-review of the architecture rules and a compile check. Automated tests with Konsist are introduced in Chapter 4. The check here has the AI do what a person would do in a visual review.

### Points to Check

- Claude Code's report shows 0 violations of the four-layer dependency direction rules and the rule prohibiting skipping layers
- `./gradlew compileKotlin` succeeds

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. Next is CP3-8.

---

## CP3-8 [Verification] Start the Server, Category List, and Transaction List in the Empty State

From here on, you verify the API behavior feature by feature. Leave the server running until CP3-10. Before starting it, confirm that the Docker PostgreSQL, `budget-app-db`, is running.

### Points to Check

- `GET /api/categories` returns 200 with the 5 initial master data records
- `GET /api/transactions` returns 200, `transactions` is an empty array, and all three `summary` fields are 0
- The Flyway migration was applied at startup. Check the startup log

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. Next is CP3-9.

---

## CP3-9 [Verification] Create Transactions, Check the List and Totals, and Input Errors Returning 400

### Points to Check

- Creating the salary of 300000 yen and the food expense of 1500 yen both return 201 Created
- `summary` in `GET /api/transactions` is `totalIncome` 300000, `totalExpense` 1500, `balance` 298500
- When `amount` is 0, 400 and an error JSON of the form `{"message": ..., "details": [...]}` are returned
- When `transactionDate` is invalid, 400 and `{"message": "リクエストの形式が不正です。"}`, which means "The request format is invalid.", are returned. Confirm that the response is not in Spring's default error format.

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. Next is CP3-10.

---

## CP3-10 [Verification] Delete Transactions, Delete a Non-existent ID, Stop the Server

### Points to Check

- `DELETE` of both created transactions returns 204 No Content, the list becomes empty, and `summary` returns to 0
- Deleting a non-existent ID returns 400 and an error message
- The server is stopped and port 8080 is released

Once you have confirmed the API behavior of the four features, namely category list, transaction list, create, and delete, the backend implementation is complete. In Chapter 4, you will build automated tests for the code implemented here. Specifically, these are domain unit tests, use case tests with MockK, MockMvc integration tests, and Konsist architecture tests.
