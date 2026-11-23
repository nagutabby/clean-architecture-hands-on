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
