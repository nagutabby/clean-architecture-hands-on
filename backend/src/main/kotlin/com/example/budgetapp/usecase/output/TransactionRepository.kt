package com.example.budgetapp.usecase.output

import com.example.budgetapp.domain.model.Transaction

interface TransactionRepository {
    fun findAll(): List<Transaction>
    fun findById(id: Long): Transaction?
    fun save(transaction: Transaction): Transaction
    fun deleteById(id: Long)
}
