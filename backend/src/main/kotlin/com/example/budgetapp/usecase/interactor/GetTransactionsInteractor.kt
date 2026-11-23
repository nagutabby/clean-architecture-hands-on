package com.example.budgetapp.usecase.interactor

import com.example.budgetapp.domain.model.BalanceSummary
import com.example.budgetapp.usecase.input.GetTransactionsOutput
import com.example.budgetapp.usecase.input.GetTransactionsUseCase
import com.example.budgetapp.usecase.output.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetTransactionsInteractor(
    private val transactionRepository: TransactionRepository
) : GetTransactionsUseCase {

    @Transactional(readOnly = true)
    override fun execute(): GetTransactionsOutput {
        val transactions = transactionRepository.findAll()
        val summary = BalanceSummary.calculate(transactions)
        return GetTransactionsOutput(
            transactions = transactions,
            summary = summary
        )
    }
}
