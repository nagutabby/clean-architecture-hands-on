package com.example.budgetapp.adapter.repository.jpa

import com.example.budgetapp.adapter.repository.entity.TransactionJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaTransactionRepository : JpaRepository<TransactionJpaEntity, Long> {
    fun findAllByOrderByTransactionDateDescIdDesc(): List<TransactionJpaEntity>
}
