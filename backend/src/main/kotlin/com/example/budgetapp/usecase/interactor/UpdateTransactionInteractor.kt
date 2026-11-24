package com.example.budgetapp.usecase.interactor

import com.example.budgetapp.domain.exception.DomainException
import com.example.budgetapp.domain.model.Amount
import com.example.budgetapp.domain.model.Transaction
import com.example.budgetapp.usecase.input.UpdateTransactionInput
import com.example.budgetapp.usecase.input.UpdateTransactionUseCase
import com.example.budgetapp.usecase.output.CategoryRepository
import com.example.budgetapp.usecase.output.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateTransactionInteractor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : UpdateTransactionUseCase {

    @Transactional
    override fun execute(input: UpdateTransactionInput): Transaction {
        transactionRepository.findById(input.id)
            ?: throw DomainException("指定された明細が存在しません (ID: ${input.id})")
        val category = categoryRepository.findById(input.categoryId)
            ?: throw DomainException("指定されたカテゴリが存在しません (ID: ${input.categoryId})")

        val updated = Transaction(
            id = input.id,
            category = category,
            title = input.title,
            amount = Amount(input.amount),
            transactionDate = input.transactionDate,
            memo = input.memo
        )
        return transactionRepository.save(updated)
    }
}
