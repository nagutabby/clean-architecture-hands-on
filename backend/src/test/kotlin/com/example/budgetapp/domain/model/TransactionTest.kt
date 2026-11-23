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
