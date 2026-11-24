package com.example.budgetapp.usecase.interactor

import com.example.budgetapp.domain.exception.DomainException
import com.example.budgetapp.domain.model.Amount
import com.example.budgetapp.domain.model.Transaction
import com.example.budgetapp.usecase.input.CreateTransactionInput
import com.example.budgetapp.usecase.input.CreateTransactionUseCase
import com.example.budgetapp.usecase.output.CategoryRepository
import com.example.budgetapp.usecase.output.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateTransactionInteractor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : CreateTransactionUseCase {

    @Transactional
    override fun execute(input: CreateTransactionInput): Transaction {
        val category = categoryRepository.findById(input.categoryId)
            ?: throw DomainException("指定されたカテゴリが存在しません (ID: ${input.categoryId})")

        val transaction = Transaction(
            id = null,
            category = category,
            title = input.title,
            amount = Amount(input.amount),
            transactionDate = input.transactionDate,
            memo = input.memo
        )

        return transactionRepository.save(transaction)
    }
}
