package com.example.budgetapp.usecase.input

interface DeleteTransactionUseCase {
    fun execute(transactionId: Long)
}
