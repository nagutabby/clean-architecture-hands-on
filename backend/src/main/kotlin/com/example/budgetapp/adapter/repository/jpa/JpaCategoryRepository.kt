package com.example.budgetapp.adapter.repository.jpa

import com.example.budgetapp.adapter.repository.entity.CategoryJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JpaCategoryRepository : JpaRepository<CategoryJpaEntity, Long>
