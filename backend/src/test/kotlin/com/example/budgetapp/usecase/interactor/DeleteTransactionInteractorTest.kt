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
