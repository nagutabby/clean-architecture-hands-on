package com.example.budgetapp.adapter.repository.entity

import jakarta.persistence.*

@Entity
@Table(name = "categories")
class CategoryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 50)
    var name: String = "",

    @Column(nullable = false, length = 10)
    var type: String = ""
)
