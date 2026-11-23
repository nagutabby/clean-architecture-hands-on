import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { TransactionItem } from '../domain/types';
import { formatCurrency } from './utils/formatCurrency';

interface TransactionListProps {
  transactions: TransactionItem[];
  onDelete: (id: number) => Promise<void>;
}

export function TransactionList({ transactions, onDelete }: TransactionListProps) {
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const handleDelete = async (id: number) => {
    setDeletingId(id);
    try {
      await onDelete(id);
    } finally {
      setDeletingId(null);
    }
  };

  if (transactions.length === 0) {
    return (
      <div className="bg-white rounded-lg shadow p-8 text-center text-gray-500">
        登録された明細がありません。
      </div>
    );
  }

  return (
    <div className="overflow-x-auto bg-white rounded-lg shadow">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              日付
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              カテゴリ
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              タイトル
            </th>
            <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">
              金額
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              メモ
            </th>
            <th className="px-4 py-3 text-center text-xs font-medium text-gray-500 uppercase">
              操作
            </th>
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {transactions.map((t) => {
            const isIncome = t.categoryType === 'INCOME';
            return (
              <tr key={t.id} className="border-b hover:bg-gray-50">
                <td className="px-4 py-3 text-sm text-gray-500">{t.transactionDate}</td>
                <td className="px-4 py-3 text-sm">
                  <span
                    className={`inline-flex px-2 py-0.5 text-xs font-semibold rounded-full ${
                      isIncome
                        ? 'bg-emerald-100 text-emerald-800'
                        : 'bg-rose-100 text-rose-800'
                    }`}
                  >
                    {t.categoryName}
                  </span>
                </td>
                <td className="px-4 py-3 text-sm text-gray-900">{t.title}</td>
                <td
                  className={`px-4 py-3 text-sm text-right font-semibold ${
                    isIncome ? 'text-emerald-600' : 'text-rose-600'
                  }`}
                >
                  {isIncome ? '+' : '-'}
                  {formatCurrency(t.amount)}
                </td>
                <td className="px-4 py-3 text-sm text-gray-500">{t.memo || '-'}</td>
                <td className="px-4 py-3 text-sm text-center">
                  <Button
                    className="px-2.5 py-1 text-xs"
                    type="button"
                    variant="danger"
                    disabled={deletingId === t.id}
                    onClick={() => handleDelete(t.id)}
                  >
                    {deletingId === t.id ? '削除中' : '削除'}
                  </Button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
