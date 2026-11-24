package com.example.budgetapp.domain.model

import com.example.budgetapp.domain.exception.DomainException

@JvmInline
value class Amount(val value: Int) {
    init {
        if (value <= 0) {
            throw DomainException("金額は1円以上に設定してください。")
        }
    }
}
