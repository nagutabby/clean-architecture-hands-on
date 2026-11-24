package com.example.budgetapp.adapter.controller

import com.example.budgetapp.adapter.controller.dto.CategoryResponse
import com.example.budgetapp.usecase.input.GetCategoriesUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val getCategoriesUseCase: GetCategoriesUseCase
) {
    @GetMapping
    fun getAll(): List<CategoryResponse> {
        return getCategoriesUseCase.execute().map { CategoryResponse.fromDomain(it) }
    }
}
