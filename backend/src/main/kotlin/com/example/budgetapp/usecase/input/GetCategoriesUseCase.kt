package com.example.budgetapp.usecase.input

import com.example.budgetapp.domain.model.Category

interface GetCategoriesUseCase {
    fun execute(): List<Category>
}
