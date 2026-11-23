# Chapter 4: バックエンドのテスト実装（単体テスト・統合テスト・アーキテクチャテスト）

## このチャプターの目的

Chapter 3 で実装したバックエンドの各層に対して、性質の異なる4種類の自動テストを Claude Code（AI）に実装させます。

クリーンアーキテクチャを採用する利点は、ビジネスロジックと外部技術を分離したことで、データベースや Web サーバーを起動せずに高速かつ網羅的なテストを書ける点にあります。また、アーキテクチャテストを導入することで、今後の機能追加や AI によるコード生成時にも依存性ルール違反を自動で検知できる状態を作ります。

1. 4-1. ドメイン層の単体テスト（JUnit 5）：値オブジェクト、エンティティの不変条件、集計ロジックの検証
2. 4-2. ユースケース層の単体テスト（JUnit 5 + MockK）：Output Port をモック化した業務フロー検証
3. 4-3. コントローラー層の統合テスト（MockMvc）：HTTP ステータス、リクエストバリデーション、例外ハンドリングの検証
4. 4-4. アーキテクチャテスト（Konsist）：4層の依存方向（外側から内側への一方向）と層の飛び越え禁止の自動検証
5. 4-5. 全テストの実行と AI によるテストレビュー

---

## 4-1. ドメイン層の単体テスト（JUnit 5）の実装

第1層（`domain`）は外部ライブラリを一切使っていない純粋な Kotlin クラスで構成されているため、モックすら不要でそのままインスタンス化してテストできます。

### Claude Code への指示プロンプト

Claude Code のチャット欄に以下を入力して実行します。

```text
.claude/rules/backend.md に従い、第1層（domain）の単体テストを JUnit 5 で3ファイル実装してください。

1. backend/src/test/kotlin/com/example/budgetapp/domain/model/AmountTest.kt
   - 1円以上の正の整数で正常に生成できること
   - 0円または負の整数を指定した場合に DomainException がスローされること
2. backend/src/test/kotlin/com/example/budgetapp/domain/model/TransactionTest.kt
   - 有効な入力で生成でき、カテゴリの種別に応じて isIncome() と isExpense() が正しく判定されること
   - タイトルが空文字、空白のみ、または101文字以上の場合に DomainException がスローされること
3. backend/src/test/kotlin/com/example/budgetapp/domain/model/BalanceSummaryTest.kt
   - 収入と支出が混在する明細リストから、総収入・総支出・残高が正しく計算されること
   - 明細が空リストの場合にすべて 0 になること
```

### 実装コードと設計のポイント

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

---

## 4-2. ユースケース層の単体テスト（JUnit 5 + MockK）の実装

第2層（`usecase`）は Output Port（`TransactionRepository` / `CategoryRepository`）インターフェースのみに依存しています。そのため、PostgreSQL を起動せずに MockK でリポジトリをモック化するだけで、ユースケースの業務フローをミリ秒単位で検証できます。

### Claude Code への指示プロンプト

```text
MockK を使用して、ユースケース層（Interactor）の単体テストを実装してください。Spring コンテキストは起動せず、Repository インターフェースを mockk でモック化してください。

1. backend/src/test/kotlin/com/example/budgetapp/usecase/interactor/CreateTransactionInteractorTest.kt
   - 存在するカテゴリIDを指定した場合、明細が保存されて返却されること
   - 存在しないカテゴリIDを指定した場合、DomainException がスローされ save が呼ばれないこと
2. backend/src/test/kotlin/com/example/budgetapp/usecase/interactor/DeleteTransactionInteractorTest.kt
   - 存在する明細IDを指定した場合、deleteById が1回呼ばれること
   - 存在しない明細IDを指定した場合、DomainException がスローされ deleteById が呼ばれないこと
```

### 実装コードと設計のポイント

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

---

## 4-3. コントローラー層の統合テスト（MockMvc）の実装

第3層（`adapter.controller`）では、HTTP リクエストのデシリアライズ、Bean Validation（`@Valid`）、DTO への変換、および `GlobalExceptionHandler` によるエラーレスポンス整形を MockMvc で検証します。

Controller は Input Port（`*UseCase`）のみに依存しているため、テスト用の `@TestConfiguration` でユースケースのモックを Bean 登録するだけで Web 層のテストが完結します。

### Claude Code への指示プロンプト

```text
@WebMvcTest と MockMvc を使用して、TransactionController の統合テストを実装してください。
Spring Boot 4 に対応するため、@WebMvcTest は org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest を使用し、UseCase インターフェースは @TestConfiguration で MockK の mockk() を Bean 登録してください。

ファイル: backend/src/test/kotlin/com/example/budgetapp/adapter/controller/TransactionControllerTest.kt
検証ケース:
1. GET /api/transactions で 200 OK と明細一覧・サマリーJSONが返ること
2. POST /api/transactions に正常なJSONを送ると 201 Created と登録された明細JSONが返ること
3. POST /api/transactions に amount=0 や空の title を送ると、バリデーションエラーとなり 400 Bad Request と統一エラーJSON（message, details）が返ること
4. POST /api/transactions に形式不正な JSON（transactionDate が日付として解釈できない等）を送ると、GlobalExceptionHandler により 400 Bad Request と統一エラーJSON（message）が返ること
5. DELETE /api/transactions/999 で UseCase が DomainException をスローした場合、GlobalExceptionHandler により 400 Bad Request とエラーメッセージが返ること
```

### 実装コードと設計のポイント

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

---

## 4-4. アーキテクチャテスト（Konsist）の実装

人がレビューで気をつけるだけでなく、Kotlin のアーキテクチャ検証ライブラリである Konsist を使い、クリーンアーキテクチャの依存方向ルールを JUnit のテストとして自動化します。

JPA Entity と Spring Data JPA Repository を第3層（`adapter.repository`）に配置したことで、自作パッケージ間の依存方向が一方向（外側から内側へのみ依存）になります（外部ライブラリの import は検証対象外です）。

1. `domain`（第1層）：どの層にも依存しない（`dependsOnNothing()`）
2. `usecase`（第2層）：`domain` のみに依存する（`dependsOn(domain)`）
3. `adapter`（第3層）：`domain` と `usecase` のみに依存し、外側の `framework` には依存しない（`dependsOn(domain, usecase)`）
4. `framework`（第4層）：内側の層にのみ依存する（`dependsOn(domain, usecase, adapter)`）

Konsist の `dependsOn(...)` は「その層が依存してよい（依存している）層」の宣言であり、宣言していない外側の層への依存を禁止するものではありません（`dependsOnNothing()` だけが厳密な禁止になります）。そのため、外側への逆向き依存（`usecase` → `adapter` / `framework`、`adapter` → `framework`）は `doesNotDependOn(...)` で明示的に禁止します。

さらに個別ルールとして、第1層・第2層のフレームワーク非依存性と、`adapter.controller`（Web 側）から `usecase.output` や `adapter.repository`（DB 側）への層の飛び越え禁止を検証します。

### Claude Code への指示プロンプト

```text
Konsist を使用して、クリーンアーキテクチャの依存ルールを検証するアーキテクチャテストを実装してください。

ファイル: backend/src/test/kotlin/com/example/budgetapp/architecture/ArchitectureTest.kt
検証ルール:
1. assertArchitecture を使用し、4層（Domain, UseCase, Adapter, Framework）の依存方向が外側から内側へのみ向かっていること（domain.dependsOnNothing(), usecase.dependsOn(domain), usecase.doesNotDependOn(adapter, framework), adapter.dependsOn(domain, usecase), adapter.doesNotDependOn(framework), framework.dependsOn(domain, usecase, adapter)）
2. domain パッケージのファイルが org.springframework.* および jakarta.persistence.* を import していないこと
3. usecase パッケージのファイルが jakarta.persistence.* を import していないこと
4. adapter.controller パッケージのファイルが usecase.output.* および adapter.repository.* を直接 import していないこと（層の飛び越え禁止）
```

### 実装コードと設計のポイント

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

---

## 4-5. 全テストの実行と AI によるテストレビュー

すべてのテストコードが揃ったら、Claude Code にテスト実行と不足ケースのレビューを依頼します。

### Claude Code への指示プロンプト

```text
cd backend && ./gradlew test を実行し、すべての単体テスト・統合テスト・Konsist アーキテクチャテストがパスすることを確認してください。
あわせて、現在実装されているテストコードに境界値や異常系の検証漏れがないかレビューしてください。
```

`BUILD SUCCESSFUL` と表示され、全テストがパスすればバックエンドの実装とテストは完了です。

次の Chapter 5 では、フロントエンド（Next.js 16 / TypeScript）において、同じくクリーンアーキテクチャの4層構造と依存性逆転の原則を取り入れた実装を進めます。
