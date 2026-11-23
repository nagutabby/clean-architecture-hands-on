package com.example.budgetapp.usecase.output

import com.example.budgetapp.domain.model.Category

interface CategoryRepository {
    fun findAll(): List<Category>
    fun findById(id: Long): Category?
}
