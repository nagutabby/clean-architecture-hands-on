# Chapter 4: Backend Testing (Unit Tests, Integration Tests, Architecture Tests)

## Purpose of This Chapter

For each layer of the backend implemented in Chapter 3, you have Claude Code (the AI) implement four kinds of automated tests, each with a different nature.

One benefit of Clean Architecture is that, because business logic is separated from external technology, you can write fast and thorough tests without starting a database or a web server. Introducing architecture tests also means that Dependency Rule violations are detected automatically when features are added later or when an AI generates code.

The checkpoints are divided by test type, and the last ones confirm that the tests actually detect violations.

| CP | Kind | Content |
| :--- | :--- | :--- |
| CP4-1 | Design | Have the test design (targets, types, test cases) presented, and approve it |
| CP4-2 | Test | Unit tests for `domain` (JUnit 5): value objects, entities, aggregation |
| CP4-3 | Test | Unit tests for `usecase` (JUnit 5 + MockK): mock the Output Ports |
| CP4-4 | Test | Integration tests for `adapter.controller` (MockMvc): HTTP, validation, exception handling |
| CP4-5 | Test | Architecture tests (Konsist): dependency direction and no layer skipping |
| CP4-6 | Verification | Confirm that the architecture tests actually detect violations |
| CP4-7 | Verification | Run all tests and review the tests |

## How to Work Through This Chapter

In this chapter you work through the checkpoints (CPs) in the table above **one at a time**. After finishing each CP, Claude Code reports the result, stops, and waits for the next instruction (this is defined in the "Checkpoint operation" section of `CLAUDE.md`).

1. Start Claude Code and enter the "Start prompt" below. Claude Code performs only the first CP and stops.
2. Compare the report with the "Points to check" of each CP to verify the result.
3. If there are no problems, type "Proceed to the next checkpoint". Claude Code performs just the next CP.
4. If there is a problem, do not move on. Instead, instruct it as in "Redo CPx-y. XX is wrong" and have it fixed within that CP.

### Start Prompt

Enter the following in the Claude Code chat box and run it. Claude Code performs only CP4-1 and stops.

```text
We will now work through Chapter 4 (backend testing) in the following 7 checkpoints (CPs).
Following the "Checkpoint operation" section of CLAUDE.md, perform only CP4-1 first, report, and stop.
After that, perform only one CP at a time, each time I type "Proceed to the next checkpoint".
Follow .claude/rules/backend.md when implementing the tests.

[CP4-1 Design] Do not create any test code or files. Present the backend test design as a table.
1. The test targets (layers and classes), the test types and tools (JUnit 5 / MockK / MockMvc / Konsist), and the list of cases to verify
2. Which tests can run without starting a database or a web server
3. What is verified as error cases and boundary values

[CP4-2 Test] Implement unit tests for layer 1 (domain) in 3 files using JUnit 5.
1. backend/src/test/kotlin/com/example/budgetapp/domain/model/AmountTest.kt
   - It can be created with a positive integer of 1 yen or more
   - DomainException is thrown when 0 yen or a negative integer is given
2. backend/src/test/kotlin/com/example/budgetapp/domain/model/TransactionTest.kt
   - It can be created with valid input, and isIncome() and isExpense() return the correct result according to the category type
   - DomainException is thrown when the title is an empty string, whitespace only, or 101 characters or more
3. backend/src/test/kotlin/com/example/budgetapp/domain/model/BalanceSummaryTest.kt
   - Total income, total expense, and balance are calculated correctly from a list of transactions mixing income and expense
   - All values are 0 when the list of transactions is empty
Completion condition: cd backend && ./gradlew test --tests 'com.example.budgetapp.domain.*' succeeds, and the number of executed tests is reported.

[CP4-3 Test] Using MockK, implement unit tests for the use case layer (Interactor). Do not start the Spring context; mock the Repository interfaces with mockk.
1. backend/src/test/kotlin/com/example/budgetapp/usecase/interactor/CreateTransactionInteractorTest.kt
   - When an existing category ID is given, the transaction is saved and returned
   - When a non-existent category ID is given, DomainException is thrown and save is not called
2. backend/src/test/kotlin/com/example/budgetapp/usecase/interactor/DeleteTransactionInteractorTest.kt
   - When an existing transaction ID is given, deleteById is called once
   - When a non-existent transaction ID is given, DomainException is thrown and deleteById is not called
Completion condition: cd backend && ./gradlew test --tests 'com.example.budgetapp.usecase.*' succeeds, and the number of executed tests is reported.

[CP4-4 Test] Using @WebMvcTest and MockMvc, implement integration tests for TransactionController.
To support Spring Boot 4, use org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest for @WebMvcTest, and register the UseCase interfaces as beans with MockK's mockk() in a @TestConfiguration.
File: backend/src/test/kotlin/com/example/budgetapp/adapter/controller/TransactionControllerTest.kt
Test cases:
1. GET /api/transactions returns 200 OK with the transaction list and summary JSON
2. POST /api/transactions with valid JSON returns 201 Created and the JSON of the registered transaction
3. POST /api/transactions with amount=0 or an empty title causes a validation error and returns 400 Bad Request with the unified error JSON (message, details)
4. POST /api/transactions with malformed JSON (for example, a transactionDate that cannot be parsed as a date) returns 400 Bad Request with the unified error JSON (message) through GlobalExceptionHandler
5. When the UseCase throws DomainException for DELETE /api/transactions/999, GlobalExceptionHandler returns 400 Bad Request with the error message
Completion condition: cd backend && ./gradlew test --tests 'com.example.budgetapp.adapter.*' succeeds, and the number of executed tests is reported.

[CP4-5 Test] Using Konsist, implement architecture tests that verify the Clean Architecture dependency rules.
File: backend/src/test/kotlin/com/example/budgetapp/architecture/ArchitectureTest.kt
Rules to verify:
1. Using assertArchitecture, the dependencies among the 4 layers (Domain, UseCase, Adapter, Framework) point only from the outside to the inside (domain.dependsOnNothing(), usecase.dependsOn(domain), usecase.doesNotDependOn(adapter, framework), adapter.dependsOn(domain, usecase), adapter.doesNotDependOn(framework), framework.dependsOn(domain, usecase, adapter))
2. Files in the domain package do not import org.springframework.* or jakarta.persistence.*
3. Files in the usecase package do not import jakarta.persistence.*
4. Files in the adapter.controller package do not directly import usecase.output.* or adapter.repository.* (no layer skipping)
Completion condition: cd backend && ./gradlew cleanTest test --tests '*ArchitectureTest' succeeds.

[CP4-6 Verification] Confirm that the architecture tests actually detect violations.
1. Temporarily add import com.example.budgetapp.usecase.output.CategoryRepository right after the package declaration in domain/model/Category.kt, and confirm that cd backend && ./gradlew cleanTest test --tests '*ArchitectureTest' fails.
2. After reverting step 1, temporarily add import com.example.budgetapp.adapter.repository.CategoryRepositoryImpl to usecase/output/CategoryRepository.kt, and confirm that the same command fails.
3. Revert all the added imports, and confirm that the same command succeeds.
Also report, from the test output at the time of failure, which rule detected the violation.

[CP4-7 Verification] Run cd backend && ./gradlew cleanTest test, and confirm that all unit tests, integration tests, and Konsist architecture tests pass.
Also review the currently implemented test code for missing verification of boundary values and error cases.
```

---

## CP4-1 [Design] Presenting and Approving the Test Design

Before any test code is written, have Claude Code lay out what is verified in which layer and with which tool. The purpose is to check that an appropriate testing approach has been chosen for each layer.

Claude Code creates no files in this CP.

### Reference Design

| Layer | Type / Tool | Main things verified | DB / Web server |
| :--- | :--- | :--- | :--- |
| `domain` | Unit test (JUnit 5) | Invariants of amount and title, aggregation logic | Not needed |
| `usecase` | Unit test (JUnit 5 + MockK) | Business flow with the Repository mocked | Not needed |
| `adapter.controller` | Integration test (MockMvc) | HTTP status, validation, exception handling | Not needed (`@WebMvcTest`) |
| All layers | Architecture test (Konsist) | Dependency direction, no layer skipping | Not needed |

### Points to check

- The targets and tools of the four kinds of tests do not differ significantly from the reference above
- Error cases and boundary values (0 yen, a 101-character title, a non-existent ID, malformed JSON, and so on) are listed

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. (Next: CP4-2)

---

## CP4-2 [Test] Unit Tests for the Domain Layer (JUnit 5)

Layer 1 (`domain`) consists of pure Kotlin classes that use no external libraries, so they can be instantiated and tested as they are, without even a mock.

### Implementation Code and Design Points

#### `AmountTest.kt`
`backend/src/test/kotlin/com/example/budgetapp/domain/model/AmountTest.kt`

```kotlin
package com.example.budgetapp.domain.model

import com.example.budgetapp.domain.exception.DomainException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AmountTest {

    @Test
    fun `1円以上の正の整数でAmountを生成できること`() {
        val amount = Amount(1)
        assertEquals(1, amount.value)
    }

    @Test
    fun `0円以下の値を指定した場合にDomainExceptionがスローされること`() {
        assertThrows(DomainException::class.java) {
            Amount(0)
        }
        assertThrows(DomainException::class.java) {
            Amount(-500)
        }
    }
}
```

#### `TransactionTest.kt`
`backend/src/test/kotlin/com/example/budgetapp/domain/model/TransactionTest.kt`

```kotlin
package com.example.budgetapp.domain.model

import com.example.budgetapp.domain.exception.DomainException
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TransactionTest {

    private val expenseCategory = Category(1L, "食費", TransactionType.EXPENSE)
    private val incomeCategory = Category(4L, "給与", TransactionType.INCOME)

    @Test
    fun `有効な値で生成でき収支タイプが正しく判定されること`() {
        val expenseTx = Transaction(
            id = 1L,
            category = expenseCategory,
            title = "a".repeat(100),
            amount = Amount(1200),
            transactionDate = LocalDate.of(2026, 11, 20),
            memo = null
        )
        assertTrue(expenseTx.isExpense())
        assertFalse(expenseTx.isIncome())

        val incomeTx = Transaction(
            id = 2L,
            category = incomeCategory,
            title = "月給",
            amount = Amount(300000),
            transactionDate = LocalDate.of(2026, 11, 25),
            memo = null
        )
        assertTrue(incomeTx.isIncome())
        assertFalse(incomeTx.isExpense())
    }

    @Test
    fun `タイトルが空文字または空白のみの場合はDomainExceptionがスローされること`() {
        assertThrows(DomainException::class.java) {
            Transaction(1L, expenseCategory, "", Amount(1000), LocalDate.of(2026, 11, 20), null)
        }
        assertThrows(DomainException::class.java) {
            Transaction(1L, expenseCategory, "   ", Amount(1000), LocalDate.of(2026, 11, 20), null)
        }
    }

    @Test
    fun `タイトルが100文字を超える場合はDomainExceptionがスローされること`() {
        assertThrows(DomainException::class.java) {
            Transaction(1L, expenseCategory, "a".repeat(101), Amount(1000), LocalDate.of(2026, 11, 20), null)
        }
    }
}
```

#### `BalanceSummaryTest.kt`
`backend/src/test/kotlin/com/example/budgetapp/domain/model/BalanceSummaryTest.kt`

```kotlin
package com.example.budgetapp.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BalanceSummaryTest {

    @Test
    fun `収入と支出の明細リストから総収入・総支出・残高が正しく計算されること`() {
        val food = Category(1L, "食費", TransactionType.EXPENSE)
        val salary = Category(4L, "給与", TransactionType.INCOME)

        val transactions = listOf(
            Transaction(1L, salary, "月給", Amount(300000), LocalDate.of(2026, 11, 1), null),
            Transaction(2L, food, "ランチ", Amount(1200), LocalDate.of(2026, 11, 2), null),
            Transaction(3L, food, "スーパー", Amount(3800), LocalDate.of(2026, 11, 3), null)
        )

        val summary = BalanceSummary.calculate(transactions)

        assertEquals(300000, summary.totalIncome)
        assertEquals(5000, summary.totalExpense)
        assertEquals(295000, summary.balance)
    }

    @Test
    fun `明細が空リストの場合はすべて0になること`() {
        val summary = BalanceSummary.calculate(emptyList())

        assertEquals(0, summary.totalIncome)
        assertEquals(0, summary.totalExpense)
        assertEquals(0, summary.balance)
    }
}
```

### Points to check

- `AmountTest.kt`, `TransactionTest.kt`, and `BalanceSummaryTest.kt` are created under `domain/model/`
- Claude Code's report states that the `domain` tests passed and how many tests were run
- The tests instantiate the classes directly, without using mocks

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. (Next: CP4-3)

---

## CP4-3 [Test] Unit Tests for the Use Case Layer (JUnit 5 + MockK)

Layer 2 (`usecase`) depends only on the Output Port interfaces (`TransactionRepository` / `CategoryRepository`). Because of this, simply mocking the repositories with MockK, without starting PostgreSQL, is enough to verify the business flow of the use cases in milliseconds.

### Implementation Code and Design Points

#### `CreateTransactionInteractorTest.kt`
`backend/src/test/kotlin/com/example/budgetapp/usecase/interactor/CreateTransactionInteractorTest.kt`

```kotlin
package com.example.budgetapp.usecase.interactor

import com.example.budgetapp.domain.exception.DomainException
import com.example.budgetapp.domain.model.Amount
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.Transaction
import com.example.budgetapp.domain.model.TransactionType
import com.example.budgetapp.usecase.input.CreateTransactionInput
import com.example.budgetapp.usecase.output.CategoryRepository
import com.example.budgetapp.usecase.output.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CreateTransactionInteractorTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val interactor = CreateTransactionInteractor(transactionRepository, categoryRepository)

    @Test
    fun `存在するカテゴリを指定した場合に明細が正常に保存されること`() {
        val category = Category(1L, "食費", TransactionType.EXPENSE)
        val input = CreateTransactionInput(
            categoryId = 1L,
            title = "カフェ",
            amount = 650,
            transactionDate = LocalDate.of(2026, 11, 20),
            memo = "打ち合わせ"
        )

        every { categoryRepository.findById(1L) } returns category
        every { transactionRepository.save(any()) } answers {
            firstArg<Transaction>().copy(id = 10L)
        }

        val result = interactor.execute(input)

        assertEquals(10L, result.id)
        assertEquals("カフェ", result.title)
        assertEquals(Amount(650), result.amount)
        verify(exactly = 1) { transactionRepository.save(any()) }
    }

    @Test
    fun `存在しないカテゴリIDを指定した場合にDomainExceptionがスローされること`() {
        val input = CreateTransactionInput(
            categoryId = 999L,
            title = "不明な支出",
            amount = 1000,
            transactionDate = LocalDate.of(2026, 11, 20),
            memo = null
        )

        every { categoryRepository.findById(999L) } returns null

        assertThrows(DomainException::class.java) {
            interactor.execute(input)
        }
        verify(exactly = 0) { transactionRepository.save(any()) }
    }
}
```

#### `DeleteTransactionInteractorTest.kt`
`backend/src/test/kotlin/com/example/budgetapp/usecase/interactor/DeleteTransactionInteractorTest.kt`

```kotlin
package com.example.budgetapp.usecase.interactor

import com.example.budgetapp.domain.exception.DomainException
import com.example.budgetapp.domain.model.Amount
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.Transaction
import com.example.budgetapp.domain.model.TransactionType
import com.example.budgetapp.usecase.output.TransactionRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DeleteTransactionInteractorTest {

    private val transactionRepository = mockk<TransactionRepository>()
    private val interactor = DeleteTransactionInteractor(transactionRepository)

    @Test
    fun `存在する明細IDを指定した場合に削除が実行されること`() {
        val category = Category(1L, "食費", TransactionType.EXPENSE)
        val existing = Transaction(1L, category, "ランチ", Amount(1000), LocalDate.of(2026, 11, 20), null)

        every { transactionRepository.findById(1L) } returns existing
        every { transactionRepository.deleteById(1L) } just runs

        interactor.execute(1L)

        verify(exactly = 1) { transactionRepository.deleteById(1L) }
    }

    @Test
    fun `存在しない明細IDを指定した場合にDomainExceptionがスローされること`() {
        every { transactionRepository.findById(999L) } returns null

        assertThrows(DomainException::class.java) {
            interactor.execute(999L)
        }
        verify(exactly = 0) { transactionRepository.deleteById(any()) }
    }
}
```

### Points to check

- `CreateTransactionInteractorTest.kt` and `DeleteTransactionInteractorTest.kt` are created under `usecase/interactor/`
- The tests verify that `save` / `deleteById` are not called for a non-existent ID (`verify(exactly = 0)`)
- Claude Code's report states that the `usecase` tests passed and how many tests were run

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. (Next: CP4-4)

---

## CP4-4 [Test] Integration Tests for the Controller Layer (MockMvc)

In layer 3 (`adapter.controller`), MockMvc verifies HTTP request deserialization, Bean Validation (`@Valid`), conversion to DTOs, and the error response formatting done by `GlobalExceptionHandler`.

The Controller depends only on the Input Ports (`*UseCase`), so registering mocks of the use cases as beans in a test `@TestConfiguration` is all that is needed to complete the web layer tests.

### Implementation Code and Design Points

`backend/src/test/kotlin/com/example/budgetapp/adapter/controller/TransactionControllerTest.kt`

```kotlin
package com.example.budgetapp.adapter.controller

import com.example.budgetapp.domain.exception.DomainException
import com.example.budgetapp.domain.model.Amount
import com.example.budgetapp.domain.model.BalanceSummary
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.Transaction
import com.example.budgetapp.domain.model.TransactionType
import com.example.budgetapp.usecase.input.CreateTransactionUseCase
import com.example.budgetapp.usecase.input.DeleteTransactionUseCase
import com.example.budgetapp.usecase.input.GetTransactionsOutput
import com.example.budgetapp.usecase.input.GetTransactionsUseCase
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(TransactionController::class)
@Import(TransactionControllerTest.TestConfig::class)
class TransactionControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun getTransactionsUseCase(): GetTransactionsUseCase = mockk()

        @Bean
        fun createTransactionUseCase(): CreateTransactionUseCase = mockk()

        @Bean
        fun deleteTransactionUseCase(): DeleteTransactionUseCase = mockk()
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var getTransactionsUseCase: GetTransactionsUseCase

    @Autowired
    private lateinit var createTransactionUseCase: CreateTransactionUseCase

    @Autowired
    private lateinit var deleteTransactionUseCase: DeleteTransactionUseCase

    @BeforeEach
    fun setUp() {
        clearMocks(getTransactionsUseCase, createTransactionUseCase, deleteTransactionUseCase)
    }

    @Test
    fun `GET _api_transactions で明細一覧と収支サマリーが返却されること`() {
        val food = Category(1L, "食費", TransactionType.EXPENSE)
        val tx = Transaction(1L, food, "ランチ", Amount(1000), LocalDate.of(2026, 11, 20), "定食")
        every { getTransactionsUseCase.execute() } returns GetTransactionsOutput(
            transactions = listOf(tx),
            summary = BalanceSummary(totalIncome = 0, totalExpense = 1000, balance = -1000)
        )

        mockMvc.perform(get("/api/transactions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.transactions[0].title").value("ランチ"))
            .andExpect(jsonPath("$.transactions[0].amount").value(1000))
            .andExpect(jsonPath("$.summary.totalExpense").value(1000))
            .andExpect(jsonPath("$.summary.balance").value(-1000))
    }

    @Test
    fun `POST _api_transactions に正常なリクエストを送ると201 Createdが返ること`() {
        val food = Category(1L, "食費", TransactionType.EXPENSE)
        val created = Transaction(10L, food, "コーヒー", Amount(500), LocalDate.of(2026, 11, 20), null)
        every { createTransactionUseCase.execute(any()) } returns created

        val requestJson = """
            {
              "categoryId": 1,
              "title": "コーヒー",
              "amount": 500,
              "transactionDate": "2026-11-20"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.title").value("コーヒー"))
    }

    @Test
    fun `POST _api_transactions に不正な値を送ると400 Bad Requestとエラー詳細が返ること`() {
        val invalidJson = """
            {
              "categoryId": 1,
              "title": "",
              "amount": 0,
              "transactionDate": "2026-11-20"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("入力内容に誤りがあります。"))
            .andExpect(jsonPath("$.details").isArray)
    }

    @Test
    fun `POST _api_transactions に形式不正なJSONを送ると統一エラーJSONで400 Bad Requestが返ること`() {
        mockMvc.perform(
            post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"categoryId": 1, "title": "x", "amount": 1, "transactionDate": "bad"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("リクエストの形式が不正です。"))
    }

    @Test
    fun `DELETE _api_transactions_999 でDomainExceptionが発生すると400 Bad Requestが返ること`() {
        every { deleteTransactionUseCase.execute(999L) } throws DomainException("指定された明細が存在しません (ID: 999)")

        mockMvc.perform(delete("/api/transactions/999"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("指定された明細が存在しません (ID: 999)"))
    }
}
```

### Points to check

- `adapter/controller/TransactionControllerTest.kt` is created and contains the 5 kinds of test cases (list, registration, validation error, malformed JSON, and `DomainException` on delete)
- `@WebMvcTest` is imported from `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` (the location in Spring Boot 4)
- Claude Code's report states that the `adapter` tests passed and how many tests were run

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. (Next: CP4-5)

---

## CP4-5 [Test] Architecture Tests (Konsist)

Rather than relying only on people being careful during review, you use Konsist, an architecture verification library for Kotlin, to automate the Clean Architecture dependency direction rules as JUnit tests.

By placing the JPA Entities and Spring Data JPA Repositories in layer 3 (`adapter.repository`), the dependencies between the project's own packages run in one direction only (from the outside to the inside). Imports of external libraries are not covered by this verification.

1. `domain` (layer 1): depends on no layer (`dependsOnNothing()`)
2. `usecase` (layer 2): depends only on `domain` (`dependsOn(domain)`)
3. `adapter` (layer 3): depends only on `domain` and `usecase`, and not on the outer `framework` (`dependsOn(domain, usecase)`)
4. `framework` (layer 4): depends only on the inner layers (`dependsOn(domain, usecase, adapter)`)

Konsist's `dependsOn(...)` declares the layers that a layer may depend on (or does depend on). It does not forbid dependencies on outer layers that are not declared (only `dependsOnNothing()` is a strict prohibition). For this reason, reverse dependencies toward the outside (`usecase` -> `adapter` / `framework`, `adapter` -> `framework`) are explicitly forbidden with `doesNotDependOn(...)`.

As individual rules, the tests also verify that layers 1 and 2 are independent of frameworks, and that `adapter.controller` (the web side) does not skip layers to reach `usecase.output` or `adapter.repository` (the DB side).

### Implementation Code and Design Points

`backend/src/test/kotlin/com/example/budgetapp/architecture/ArchitectureTest.kt`

```kotlin
package com.example.budgetapp.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.Test

class ArchitectureTest {

    @Test
    fun `クリーンアーキテクチャの4層の依存方向が外側から内側へのみ向かっていること`() {
        Konsist
            .scopeFromProject()
            .assertArchitecture {
                val domain = Layer("Domain", "com.example.budgetapp.domain..")
                val usecase = Layer("UseCase", "com.example.budgetapp.usecase..")
                val adapter = Layer("Adapter", "com.example.budgetapp.adapter..")
                val framework = Layer("Framework", "com.example.budgetapp.framework..")

                domain.dependsOnNothing()
                usecase.dependsOn(domain)
                usecase.doesNotDependOn(adapter, framework)
                adapter.dependsOn(domain, usecase)
                adapter.doesNotDependOn(framework)
                framework.dependsOn(domain, usecase, adapter)
            }
    }

    @Test
    fun `domain層はSpringやJPAのフレームワークに依存していないこと`() {
        Konsist
            .scopeFromPackage("com.example.budgetapp.domain..")
            .files
            .assertFalse { file ->
                file.hasImport { import ->
                    import.name.startsWith("org.springframework.") ||
                        import.name.startsWith("jakarta.persistence.")
                }
            }
    }

    @Test
    fun `usecase層はJPAに依存していないこと`() {
        Konsist
            .scopeFromPackage("com.example.budgetapp.usecase..")
            .files
            .assertFalse { file ->
                file.hasImport { import ->
                    import.name.startsWith("jakarta.persistence.")
                }
            }
    }

    @Test
    fun `Controller層はOutputPortとDBアダプターに直接依存していないこと`() {
        Konsist
            .scopeFromPackage("com.example.budgetapp.adapter.controller..")
            .files
            .assertFalse { file ->
                file.hasImport { import ->
                    import.name.startsWith("com.example.budgetapp.usecase.output.") ||
                        import.name.startsWith("com.example.budgetapp.adapter.repository.")
                }
            }
    }
}
```

### Points to check

- `architecture/ArchitectureTest.kt` is created and includes the prohibition of reverse dependencies with `doesNotDependOn`
- `./gradlew cleanTest test --tests '*ArchitectureTest'` succeeds

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. (Next: CP4-6)

---

## CP4-6 [Verification] Confirming That the Architecture Tests Actually Detect Violations

A test can be trusted only after you confirm not just that it passes but also that it does not miss violations. Here you deliberately add an `import` that violates the dependency direction, confirm that the test fails, and then revert it.

`./gradlew test` is not re-run and is reported as `UP-TO-DATE` when its inputs have not changed, so it is run with `cleanTest` every time (the hook in Chapter 7 uses `cleanTest` for the same reason).

### Points to check

- The test fails when a `domain` -> `usecase` dependency (from the inside to the outside) is added
- The test fails when a `usecase` -> `adapter` dependency (forbidden with `doesNotDependOn`) is added
- All the added `import` lines are reverted and the test passes again (no diff remains in the source, as checked with `git diff` or similar)

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. (Next: CP4-7)

---

## CP4-7 [Verification] Running All Tests and Reviewing the Tests

Once all the test code is in place, ask Claude Code to run all the tests and to review them for missing cases.

### Points to check

- `./gradlew cleanTest test` ends with `BUILD SUCCESSFUL` and all tests pass
- Read the proposals for additional cases raised in the review and choose the ones you need (they are not implemented automatically)

When all tests pass, the backend implementation and tests are complete.

In the next chapter, Chapter 5, you implement the frontend (Next.js 16 / TypeScript) in the same way, adopting the four-layer structure of Clean Architecture and the Dependency Inversion Principle.
