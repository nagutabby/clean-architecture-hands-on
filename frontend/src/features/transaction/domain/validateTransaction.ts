import { CreateTransactionInput } from './types';

export function validateTransactionInput(input: Partial<CreateTransactionInput>): string[] {
  const errors: string[] = [];

  if (!input.categoryId || input.categoryId <= 0) {
    errors.push('カテゴリを選択してください。');
  }

  if (!input.title || input.title.trim().length === 0) {
    errors.push('タイトルを入力してください。');
  } else if (input.title.length > 100) {
    errors.push('タイトルは100文字以内で入力してください。');
  }

  if (
    input.amount === undefined ||
    Number.isNaN(input.amount) ||
    !Number.isInteger(input.amount) ||
    input.amount <= 0
  ) {
    errors.push('金額は1円以上の整数で入力してください。');
  }

  if (!input.transactionDate || input.transactionDate.trim().length === 0) {
    errors.push('決済日を入力してください。');
  }

  return errors;
}
