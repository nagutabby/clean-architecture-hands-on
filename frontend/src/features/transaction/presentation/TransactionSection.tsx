'use client';

import { TransactionRepository } from '../usecase/TransactionRepository';
import { useTransactions } from '../usecase/useTransactions';
import { transactionApi } from '../adapter/transactionApi';
import { BalanceSummaryCard } from './BalanceSummaryCard';
import { TransactionForm } from './TransactionForm';
import { TransactionList } from './TransactionList';

interface TransactionSectionProps {
  repository?: TransactionRepository;
}

export function TransactionSection({ repository = transactionApi }: TransactionSectionProps) {
  const {
    transactions,
    summary,
    categories,
    isLoading,
    error,
    addTransaction,
    removeTransaction,
  } = useTransactions(repository);

  if (isLoading) {
    return <p className="text-gray-500 text-center py-8">読み込み中...</p>;
  }

  return (
    <div>
      {error && (
        <div role="alert" className="mb-6 p-4 bg-red-50 text-red-700 rounded-md">
          エラーが発生しました: {error}
        </div>
      )}

      <BalanceSummaryCard summary={summary} />
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="md:col-span-1">
          <TransactionForm categories={categories} onSubmit={addTransaction} />
        </div>
        <div className="md:col-span-2">
          <h2 className="text-xl font-bold text-gray-900 mb-4">家計簿明細一覧</h2>
          <TransactionList transactions={transactions} onDelete={removeTransaction} />
        </div>
      </div>
    </div>
  );
}
