package com.example.budgetapp.usecase.input

import com.example.budgetapp.domain.model.Transaction
import java.time.LocalDate

data class CreateTransactionInput(
    val categoryId: Long,
    val title: String,
    val amount: Int,
    val transactionDate: LocalDate,
    val memo: String?
)

interface CreateTransactionUseCase {
    fun execute(input: CreateTransactionInput): Transaction
}
