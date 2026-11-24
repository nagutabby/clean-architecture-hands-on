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
