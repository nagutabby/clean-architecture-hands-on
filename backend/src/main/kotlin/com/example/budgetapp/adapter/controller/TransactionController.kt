package com.example.budgetapp.adapter.controller

import com.example.budgetapp.adapter.controller.dto.*
import com.example.budgetapp.usecase.input.CreateTransactionUseCase
import com.example.budgetapp.usecase.input.DeleteTransactionUseCase
import com.example.budgetapp.usecase.input.GetTransactionsUseCase
import com.example.budgetapp.usecase.input.UpdateTransactionUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/transactions")
class TransactionController(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase
) {
    @GetMapping
    fun getAll(): TransactionListResponse {
        val output = getTransactionsUseCase.execute()
        return TransactionListResponse(
            transactions = output.transactions.map { TransactionItemResponse.fromDomain(it) },
            summary = BalanceSummaryResponse.fromDomain(output.summary)
        )
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: TransactionRequest): TransactionItemResponse {
        val created = createTransactionUseCase.execute(request.toUseCaseInput())
        return TransactionItemResponse.fromDomain(created)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: TransactionRequest): TransactionItemResponse {
        val updated = updateTransactionUseCase.execute(request.toUpdateInput(id))
        return TransactionItemResponse.fromDomain(updated)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        deleteTransactionUseCase.execute(id)
    }
}
