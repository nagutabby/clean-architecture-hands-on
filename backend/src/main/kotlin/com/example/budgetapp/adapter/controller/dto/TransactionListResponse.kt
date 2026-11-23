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
