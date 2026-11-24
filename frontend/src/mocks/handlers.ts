import { http, HttpResponse } from 'msw';
import {
  Category,
  CreateTransactionInput,
  TransactionItem,
} from '@/features/transaction/domain/types';

const MOCK_CATEGORIES: Category[] = [
  { id: 1, name: '食費', type: 'EXPENSE' },
  { id: 4, name: '給与', type: 'INCOME' },
];

const INITIAL_TRANSACTIONS: TransactionItem[] = [
  {
    id: 1,
    categoryId: 1,
    categoryName: '食費',
    categoryType: 'EXPENSE',
    title: 'テストランチ',
    amount: 1000,
    transactionDate: '2026-11-20',
    memo: 'テストメモ',
  },
];

let mockTransactions: TransactionItem[] = [...INITIAL_TRANSACTIONS];
let nextMockId = 100;

export function resetMockTransactions() {
  mockTransactions = [...INITIAL_TRANSACTIONS];
  nextMockId = 100;
}

function calculateSummary(items: TransactionItem[]) {
  const totalIncome = items
    .filter((t) => t.categoryType === 'INCOME')
    .reduce((sum, t) => sum + t.amount, 0);
  const totalExpense = items
    .filter((t) => t.categoryType === 'EXPENSE')
    .reduce((sum, t) => sum + t.amount, 0);

  return {
    totalIncome,
    totalExpense,
    balance: totalIncome - totalExpense,
  };
}

export const handlers = [
  http.get('http://localhost:8080/api/categories', () => {
    return HttpResponse.json(MOCK_CATEGORIES);
  }),

  http.get('http://localhost:8080/api/transactions', () => {
    return HttpResponse.json({
      transactions: mockTransactions,
      summary: calculateSummary(mockTransactions),
    });
  }),

  http.post('http://localhost:8080/api/transactions', async ({ request }) => {
    const body = (await request.json()) as CreateTransactionInput;
    const category = MOCK_CATEGORIES.find((c) => c.id === body.categoryId) ?? MOCK_CATEGORIES[0];

    const created: TransactionItem = {
      id: nextMockId++,
      categoryId: category.id,
      categoryName: category.name,
      categoryType: category.type,
      title: body.title,
      amount: body.amount,
      transactionDate: body.transactionDate,
      memo: body.memo,
    };

    mockTransactions = [created, ...mockTransactions];
    return HttpResponse.json(created, { status: 201 });
  }),

  http.put('http://localhost:8080/api/transactions/:id', async ({ params, request }) => {
    const id = Number(params.id);
    const body = (await request.json()) as CreateTransactionInput;
    const category = MOCK_CATEGORIES.find((c) => c.id === body.categoryId) ?? MOCK_CATEGORIES[0];
    const updated: TransactionItem = {
      id,
      categoryId: category.id,
      categoryName: category.name,
      categoryType: category.type,
      title: body.title,
      amount: body.amount,
      transactionDate: body.transactionDate,
      memo: body.memo,
    };
    mockTransactions = mockTransactions.map((t) => (t.id === id ? updated : t));
    return HttpResponse.json(updated);
  }),

  http.delete('http://localhost:8080/api/transactions/:id', ({ params }) => {
    const id = Number(params.id);
    mockTransactions = mockTransactions.filter((t) => t.id !== id);
    return new HttpResponse(null, { status: 204 });
  }),
];
