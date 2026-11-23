package com.example.budgetapp.adapter.repository

import com.example.budgetapp.adapter.repository.entity.CategoryJpaEntity
import com.example.budgetapp.adapter.repository.jpa.JpaCategoryRepository
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.TransactionType
import com.example.budgetapp.usecase.output.CategoryRepository
import org.springframework.stereotype.Component

@Component
class CategoryRepositoryImpl(
    private val jpaCategoryRepository: JpaCategoryRepository
) : CategoryRepository {

    override fun findAll(): List<Category> {
        return jpaCategoryRepository.findAll().map { it.toDomain() }
    }

    override fun findById(id: Long): Category? {
        return jpaCategoryRepository.findById(id).orElse(null)?.toDomain()
    }

    private fun CategoryJpaEntity.toDomain(): Category = Category(
        id = this.id ?: throw IllegalStateException("カテゴリIDが存在しません"),
        name = this.name,
        type = TransactionType.valueOf(this.type)
    )
}
