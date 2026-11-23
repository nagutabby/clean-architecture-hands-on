package com.example.budgetapp.usecase.interactor

import com.example.budgetapp.domain.exception.DomainException
import com.example.budgetapp.usecase.input.DeleteTransactionUseCase
import com.example.budgetapp.usecase.output.TransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeleteTransactionInteractor(
    private val transactionRepository: TransactionRepository
) : DeleteTransactionUseCase {

    @Transactional
    override fun execute(transactionId: Long) {
        transactionRepository.findById(transactionId)
            ?: throw DomainException("指定された明細が存在しません (ID: $transactionId)")

        transactionRepository.deleteById(transactionId)
    }
}
