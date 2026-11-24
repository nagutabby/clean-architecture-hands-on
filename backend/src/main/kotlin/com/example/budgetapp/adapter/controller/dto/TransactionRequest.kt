package com.example.budgetapp.adapter.controller.dto

import com.example.budgetapp.usecase.input.CreateTransactionInput
import com.example.budgetapp.usecase.input.UpdateTransactionInput
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class TransactionRequest(
    @field:NotNull(message = "カテゴリIDは必須です。")
    val categoryId: Long?,

    @field:NotBlank(message = "タイトルは必須です。")
    @field:Size(max = 100, message = "タイトルは100文字以内で入力してください。")
    val title: String?,

    @field:NotNull(message = "金額は必須です。")
    @field:Positive(message = "金額は1円以上の正の整数で入力してください。")
    val amount: Int?,

    @field:NotNull(message = "決済日は必須です。")
    val transactionDate: LocalDate?,

    val memo: String?
) {
    fun toUseCaseInput(): CreateTransactionInput = CreateTransactionInput(
        categoryId = categoryId!!,
        title = title!!,
        amount = amount!!,
        transactionDate = transactionDate!!,
        memo = memo
    )

    fun toUpdateInput(id: Long): UpdateTransactionInput = UpdateTransactionInput(
        id = id,
        categoryId = categoryId!!,
        title = title!!,
        amount = amount!!,
        transactionDate = transactionDate!!,
        memo = memo
    )
}
