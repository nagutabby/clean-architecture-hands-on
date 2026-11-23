export type TransactionType = 'INCOME' | 'EXPENSE';

export interface Category {
  id: number;
  name: string;
  type: TransactionType;
}

export interface TransactionItem {
  id: number;
  categoryId: number;
  categoryName: string;
  categoryType: TransactionType;
  title: string;
  amount: number;
  transactionDate: string;
  memo?: string;
}

export interface BalanceSummary {
  totalIncome: number;
  totalExpense: number;
  balance: number;
}

export interface TransactionListData {
  transactions: TransactionItem[];
  summary: BalanceSummary;
}

export interface CreateTransactionInput {
  categoryId: number;
  title: string;
  amount: number;
  transactionDate: string;
  memo?: string;
}
