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
