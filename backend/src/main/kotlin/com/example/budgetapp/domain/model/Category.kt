package com.example.budgetapp.domain.model

data class Category(
    val id: Long,
    val name: String,
    val type: TransactionType
)
