package com.example.budgetapp.domain.model

import com.example.budgetapp.domain.exception.DomainException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AmountTest {

    @Test
    fun `1円以上の正の整数でAmountを生成できること`() {
        val amount = Amount(1)
        assertEquals(1, amount.value)
    }

    @Test
    fun `0円以下の値を指定した場合にDomainExceptionがスローされること`() {
        assertThrows(DomainException::class.java) {
            Amount(0)
        }
        assertThrows(DomainException::class.java) {
            Amount(-500)
        }
    }
}
