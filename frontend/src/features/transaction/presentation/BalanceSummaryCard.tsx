import { BalanceSummary } from '../domain/types';
import { formatCurrency } from './utils/formatCurrency';

interface BalanceSummaryCardProps {
  summary: BalanceSummary;
}

export function BalanceSummaryCard({ summary }: BalanceSummaryCardProps) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
      <div className="bg-white p-5 rounded-lg shadow border-l-4 border-emerald-500">
        <p className="text-xs font-medium text-gray-500 uppercase">総収入</p>
        <p className="mt-1 text-2xl font-bold text-emerald-600">
          {formatCurrency(summary.totalIncome)}
        </p>
      </div>
      <div className="bg-white p-5 rounded-lg shadow border-l-4 border-rose-500">
        <p className="text-xs font-medium text-gray-500 uppercase">総支出</p>
        <p className="mt-1 text-2xl font-bold text-rose-600">
          {formatCurrency(summary.totalExpense)}
        </p>
      </div>
      <div className="bg-white p-5 rounded-lg shadow border-l-4 border-blue-600">
        <p className="text-xs font-medium text-gray-500 uppercase">現在の残高</p>
        <p className="mt-1 text-2xl font-bold text-gray-900">
          {formatCurrency(summary.balance)}
        </p>
      </div>
    </div>
  );
}
