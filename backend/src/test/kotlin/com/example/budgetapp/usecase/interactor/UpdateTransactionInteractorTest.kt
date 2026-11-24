package com.example.budgetapp.usecase.interactor

import com.example.budgetapp.domain.exception.DomainException
import com.example.budgetapp.domain.model.Amount
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.Transaction
import com.example.budgetapp.domain.model.TransactionType
import com.example.budgetapp.usecase.input.UpdateTransactionInput
import com.example.budgetapp.usecase.output.CategoryRepository
import com.example.budgetapp.usecase.output.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UpdateTransactionInteractorTest {
    private val transactionRepository = mockk<TransactionRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val interactor = UpdateTransactionInteractor(transactionRepository, categoryRepository)

    private val food = Category(1L, "食費", TransactionType.EXPENSE)
    private val salary = Category(4L, "給与", TransactionType.INCOME)
    private val existing = Transaction(5L, food, "ランチ", Amount(1000), LocalDate.of(2026, 11, 20), null)
    private val input = UpdateTransactionInput(5L, 4L, "月給", 300000, LocalDate.of(2026, 11, 25), "メモ")

    @Test
    fun `存在する明細とカテゴリを指定した場合に更新内容が保存されること`() {
        every { transactionRepository.findById(5L) } returns existing
        every { categoryRepository.findById(4L) } returns salary
        every { transactionRepository.save(any()) } answers { firstArg() }

        val result = interactor.execute(input)

        assertEquals(5L, result.id)
        assertEquals(salary, result.category)
        assertEquals(Amount(300000), result.amount)
        verify(exactly = 1) { transactionRepository.save(any()) }
    }

    @Test
    fun `存在しない明細IDの場合はDomainExceptionでsaveされないこと`() {
        every { transactionRepository.findById(5L) } returns null
        assertThrows(DomainException::class.java) { interactor.execute(input) }
        verify(exactly = 0) { transactionRepository.save(any()) }
    }

    @Test
    fun `存在しないカテゴリIDの場合はDomainExceptionでsaveされないこと`() {
        every { transactionRepository.findById(5L) } returns existing
        every { categoryRepository.findById(4L) } returns null
        assertThrows(DomainException::class.java) { interactor.execute(input) }
        verify(exactly = 0) { transactionRepository.save(any()) }
    }
}
