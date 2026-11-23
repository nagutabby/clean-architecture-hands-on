import {
  Category,
  CreateTransactionInput,
  TransactionItem,
  TransactionListData,
} from '../domain/types';

export interface TransactionRepository {
  fetchCategories(): Promise<Category[]>;
  fetchTransactions(): Promise<TransactionListData>;
  createTransaction(input: CreateTransactionInput): Promise<TransactionItem>;
  deleteTransaction(id: number): Promise<void>;
}
