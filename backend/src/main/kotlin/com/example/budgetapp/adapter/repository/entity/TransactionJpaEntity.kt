package com.example.budgetapp.adapter.repository.entity

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "transactions")
class TransactionJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "category_id", nullable = false)
    var categoryId: Long = 0,

    @Column(nullable = false, length = 100)
    var title: String = "",

    @Column(nullable = false)
    var amount: Int = 0,

    @Column(name = "transaction_date", nullable = false)
    var transactionDate: LocalDate = LocalDate.now(),

    @Column(columnDefinition = "TEXT")
    var memo: String? = null
)
