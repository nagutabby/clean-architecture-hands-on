import { useState, useEffect, useCallback, useRef } from 'react';
import {
  BalanceSummary,
  Category,
  CreateTransactionInput,
  TransactionItem,
} from '../domain/types';
import { TransactionRepository } from './TransactionRepository';

const INITIAL_SUMMARY: BalanceSummary = {
  totalIncome: 0,
  totalExpense: 0,
  balance: 0,
};

export function useTransactions(repository: TransactionRepository) {
  const [transactions, setTransactions] = useState<TransactionItem[]>([]);
  const [summary, setSummary] = useState<BalanceSummary>(INITIAL_SUMMARY);
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const repositoryRef = useRef(repository);
  useEffect(() => {
    repositoryRef.current = repository;
  }, [repository]);

  const refreshTransactions = useCallback(async () => {
    const updated = await repositoryRef.current.fetchTransactions();
    setTransactions(updated.transactions);
    setSummary(updated.summary);
  }, []);

  useEffect(() => {
    let active = true;

    async function loadInitialData() {
      try {
        setError(null);
        const [listData, categoryData] = await Promise.all([
          repositoryRef.current.fetchTransactions(),
          repositoryRef.current.fetchCategories(),
        ]);
        if (!active) return;
        setTransactions(listData.transactions);
        setSummary(listData.summary);
        setCategories(categoryData);
      } catch (err) {
        if (!active) return;
        setError(err instanceof Error ? err.message : 'データの取得に失敗しました');
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    }

    loadInitialData();
    return () => {
      active = false;
    };
  }, []);

  const addTransaction = async (input: CreateTransactionInput) => {
    await repositoryRef.current.createTransaction(input);
    try {
      setError(null);
      await refreshTransactions();
    } catch (err) {
      setError(err instanceof Error ? err.message : '一覧の再取得に失敗しました');
    }
  };

  const editTransaction = async (id: number, input: CreateTransactionInput) => {
    await repositoryRef.current.updateTransaction(id, input);
    try {
      setError(null);
      await refreshTransactions();
    } catch (err) {
      setError(err instanceof Error ? err.message : '一覧の再取得に失敗しました');
    }
  };

  const removeTransaction = async (id: number) => {
    try {
      setError(null);
      await repositoryRef.current.deleteTransaction(id);
      await refreshTransactions();
    } catch (err) {
      setError(err instanceof Error ? err.message : '明細の削除に失敗しました');
    }
  };

  return {
    transactions,
    summary,
    categories,
    isLoading,
    error,
    addTransaction,
    editTransaction,
    removeTransaction,
  };
}
