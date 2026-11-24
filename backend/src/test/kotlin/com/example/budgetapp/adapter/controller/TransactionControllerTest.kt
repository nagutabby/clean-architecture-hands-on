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
import com.example.budgetapp.usecase.input.UpdateTransactionUseCase
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
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
        fun updateTransactionUseCase(): UpdateTransactionUseCase = mockk()

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
    private lateinit var updateTransactionUseCase: UpdateTransactionUseCase

    @Autowired
    private lateinit var deleteTransactionUseCase: DeleteTransactionUseCase

    @BeforeEach
    fun setUp() {
        clearMocks(getTransactionsUseCase, createTransactionUseCase, updateTransactionUseCase, deleteTransactionUseCase)
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

    @Test
    fun `PUT _api_transactions_10 で更新すると200 OKと更新後の明細が返ること`() {
        val salary = Category(4L, "給与", TransactionType.INCOME)
        val updated = Transaction(10L, salary, "賞与", Amount(500000), LocalDate.of(2026, 11, 25), null)
        every { updateTransactionUseCase.execute(any()) } returns updated

        mockMvc.perform(
            put("/api/transactions/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"categoryId":4,"title":"賞与","amount":500000,"transactionDate":"2026-11-25"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.title").value("賞与"))
    }

    @Test
    fun `PUT _api_transactions_999 でDomainExceptionが発生すると400 Bad Requestが返ること`() {
        every { updateTransactionUseCase.execute(any()) } throws DomainException("指定された明細が存在しません (ID: 999)")

        mockMvc.perform(
            put("/api/transactions/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"categoryId":1,"title":"x","amount":100,"transactionDate":"2026-11-25"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("指定された明細が存在しません (ID: 999)"))
    }

    @Test
    fun `PUT で不正な値を送ると400 Bad Requestが返ること`() {
        mockMvc.perform(
            put("/api/transactions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"categoryId":1,"title":"","amount":0,"transactionDate":"2026-11-25"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.details").isArray)
    }
}
