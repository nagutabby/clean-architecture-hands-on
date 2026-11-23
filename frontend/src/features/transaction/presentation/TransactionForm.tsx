import { useState, FormEvent } from 'react';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Category, CreateTransactionInput } from '../domain/types';
import { validateTransactionInput } from '../domain/validateTransaction';

interface TransactionFormProps {
  categories: Category[];
  onSubmit: (data: CreateTransactionInput) => Promise<void>;
}

function getTodayLocalDate(): string {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function TransactionForm({ categories, onSubmit }: TransactionFormProps) {
  const [selectedCategoryId, setSelectedCategoryId] = useState<string>('');
  const [title, setTitle] = useState('');
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState(getTodayLocalDate);
  const [memo, setMemo] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formErrors, setFormErrors] = useState<string[]>([]);

  const effectiveCategoryId =
    selectedCategoryId || (categories.length > 0 ? String(categories[0].id) : '');

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    const input: CreateTransactionInput = {
      categoryId: Number(effectiveCategoryId),
      title,
      amount: Number(amount),
      transactionDate: date,
      memo: memo || undefined,
    };

    const validationErrors = validateTransactionInput(input);
    if (validationErrors.length > 0) {
      setFormErrors(validationErrors);
      return;
    }

    setSubmitting(true);
    setFormErrors([]);
    try {
      await onSubmit(input);
      setTitle('');
      setAmount('');
      setMemo('');
    } catch (err) {
      setFormErrors([err instanceof Error ? err.message : '登録に失敗しました']);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="bg-white p-6 rounded-lg shadow space-y-4" noValidate>
      <h2 className="text-lg font-bold text-gray-900">明細の追加</h2>

      {formErrors.length > 0 && (
        <ul className="p-3 text-sm bg-red-50 text-red-700 rounded-md list-disc list-inside space-y-1">
          {formErrors.map((errMsg, index) => (
            <li key={index}>{errMsg}</li>
          ))}
        </ul>
      )}

      <div>
        <label htmlFor="category" className="block text-sm font-medium text-gray-700 mb-1">
          カテゴリ
        </label>
        <select
          id="category"
          value={effectiveCategoryId}
          onChange={(e) => setSelectedCategoryId(e.target.value)}
          className="text-gray-900 w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          {categories.map((cat) => (
            <option key={cat.id} value={cat.id}>
              {cat.name}（{cat.type === 'INCOME' ? '収入' : '支出'}）
            </option>
          ))}
        </select>
      </div>

      <Input
        id="title"
        label="タイトル"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        placeholder="例: スーパーで買い物"
      />
      <Input
        id="amount"
        label="金額（円）"
        type="number"
        value={amount}
        onChange={(e) => setAmount(e.target.value)}
        placeholder="例: 1280"
      />
      <Input
        id="transactionDate"
        label="決済日"
        type="date"
        value={date}
        onChange={(e) => setDate(e.target.value)}
      />
      <Input
        id="memo"
        label="メモ（任意）"
        value={memo}
        onChange={(e) => setMemo(e.target.value)}
        placeholder="備考など"
      />
      <Button className="w-full" type="submit" disabled={submitting}>
        {submitting ? '登録中...' : '登録する'}
      </Button>
    </form>
  );
}
