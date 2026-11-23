package com.example.budgetapp.adapter.repository

import com.example.budgetapp.adapter.repository.entity.CategoryJpaEntity
import com.example.budgetapp.adapter.repository.entity.TransactionJpaEntity
import com.example.budgetapp.adapter.repository.jpa.JpaCategoryRepository
import com.example.budgetapp.adapter.repository.jpa.JpaTransactionRepository
import com.example.budgetapp.domain.model.Amount
import com.example.budgetapp.domain.model.Category
import com.example.budgetapp.domain.model.Transaction
import com.example.budgetapp.domain.model.TransactionType
import com.example.budgetapp.usecase.output.TransactionRepository
import org.springframework.stereotype.Component

@Component
class TransactionRepositoryImpl(
    private val jpaTransactionRepository: JpaTransactionRepository,
    private val jpaCategoryRepository: JpaCategoryRepository
) : TransactionRepository {

    override fun findAll(): List<Transaction> {
        val categoryMap = jpaCategoryRepository.findAll().associateBy { it.id }
        return jpaTransactionRepository.findAllByOrderByTransactionDateDescIdDesc().map { entity ->
            val categoryEntity = categoryMap[entity.categoryId]
                ?: throw IllegalStateException("カテゴリが見つかりません (ID: ${entity.categoryId})")
            entity.toDomain(categoryEntity.toDomain())
        }
    }

    override fun findById(id: Long): Transaction? {
        val entity = jpaTransactionRepository.findById(id).orElse(null) ?: return null
        val categoryEntity = jpaCategoryRepository.findById(entity.categoryId).orElse(null)
            ?: throw IllegalStateException("カテゴリが見つかりません (ID: ${entity.categoryId})")

        return entity.toDomain(categoryEntity.toDomain())
    }

    override fun save(transaction: Transaction): Transaction {
        val saved = jpaTransactionRepository.save(transaction.toEntity())
        return saved.toDomain(transaction.category)
    }

    override fun deleteById(id: Long) {
        jpaTransactionRepository.deleteById(id)
    }

    private fun CategoryJpaEntity.toDomain(): Category = Category(
        id = this.id ?: throw IllegalStateException("カテゴリIDが存在しません"),
        name = this.name,
        type = TransactionType.valueOf(this.type)
    )

    private fun TransactionJpaEntity.toDomain(category: Category): Transaction = Transaction(
        id = this.id,
        category = category,
        title = this.title,
        amount = Amount(this.amount),
        transactionDate = this.transactionDate,
        memo = this.memo
    )

    private fun Transaction.toEntity(): TransactionJpaEntity = TransactionJpaEntity(
        id = this.id,
        categoryId = this.category.id,
        title = this.title,
        amount = this.amount.value,
        transactionDate = this.transactionDate,
        memo = this.memo
    )
}
