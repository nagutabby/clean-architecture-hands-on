package com.example.budgetapp.usecase.interactor

import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.usecase.input.GetCategoriesUseCase
import com.example.budgetapp.usecase.output.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetCategoriesInteractor(
    private val categoryRepository: CategoryRepository
) : GetCategoriesUseCase {

    @Transactional(readOnly = true)
    override fun execute(): List<Category> {
        return categoryRepository.findAll()
    }
}
