package com.example.budgetapp.usecase.input

import com.example.budgetapp.domain.model.BalanceSummary
import com.example.budgetapp.domain.model.Transaction

data class GetTransactionsOutput(
    val transactions: List<Transaction>,
    val summary: BalanceSummary
)

interface GetTransactionsUseCase {
    fun execute(): GetTransactionsOutput
}
