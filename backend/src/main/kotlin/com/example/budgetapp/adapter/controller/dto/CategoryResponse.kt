package com.example.budgetapp.adapter.controller.dto

import com.example.budgetapp.domain.model.Category

data class CategoryResponse(
    val id: Long,
    val name: String,
    val type: String
) {
    companion object {
        fun fromDomain(category: Category): CategoryResponse = CategoryResponse(
            id = category.id,
            name = category.name,
            type = category.type.name
        )
    }
}
