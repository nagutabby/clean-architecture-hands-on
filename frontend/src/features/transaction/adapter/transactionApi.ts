import {
  Category,
  CreateTransactionInput,
  TransactionItem,
  TransactionListData,
} from '../domain/types';
import { TransactionRepository } from '../usecase/TransactionRepository';

const API_BASE_URL = 'http://localhost:8080/api';

export const transactionApi: TransactionRepository = {
  async fetchCategories(): Promise<Category[]> {
    const res = await fetch(`${API_BASE_URL}/categories`);
    if (!res.ok) {
      throw new Error('カテゴリの取得に失敗しました');
    }
    return res.json();
  },

  async fetchTransactions(): Promise<TransactionListData> {
    const res = await fetch(`${API_BASE_URL}/transactions`);
    if (!res.ok) {
      throw new Error('明細一覧の取得に失敗しました');
    }
    return res.json();
  },

  async createTransaction(input: CreateTransactionInput): Promise<TransactionItem> {
    const res = await fetch(`${API_BASE_URL}/transactions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    });

    if (!res.ok) {
      const errorData = await res.json().catch(() => null);
      throw new Error(errorData?.message || '明細の登録に失敗しました');
    }
    return res.json();
  },

  async deleteTransaction(id: number): Promise<void> {
    const res = await fetch(`${API_BASE_URL}/transactions/${id}`, {
      method: 'DELETE',
    });

    if (!res.ok) {
      const errorData = await res.json().catch(() => null);
      throw new Error(errorData?.message || '明細の削除に失敗しました');
    }
  },
};
