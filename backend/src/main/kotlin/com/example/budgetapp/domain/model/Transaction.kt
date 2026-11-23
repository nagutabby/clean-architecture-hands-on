package com.example.budgetapp.domain.model

import com.example.budgetapp.domain.exception.DomainException
import java.time.LocalDate

data class Transaction(
    val id: Long?,
    val category: Category,
    val title: String,
    val amount: Amount,
    val transactionDate: LocalDate,
    val memo: String?
) {
    init {
        if (title.isBlank()) {
            throw DomainException("タイトルは必須です。")
        }
        if (title.length > 100) {
            throw DomainException("タイトルは100文字以内で入力してください。")
        }
    }

    fun isIncome(): Boolean = category.type == TransactionType.INCOME
    fun isExpense(): Boolean = category.type == TransactionType.EXPENSE
}
