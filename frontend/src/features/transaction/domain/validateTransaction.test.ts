import { describe, it, expect } from 'vitest';
import { validateTransactionInput } from './validateTransaction';

describe('validateTransactionInput', () => {
  const validInput = {
    categoryId: 1,
    title: 'スーパーで買い物',
    amount: 1280,
    transactionDate: '2026-11-20',
    memo: '夕食の材料',
  };

  it('すべての入力値が正しい場合は空配列を返すこと', () => {
    expect(validateTransactionInput(validInput)).toEqual([]);
  });

  it('カテゴリIDが0以下の場合はエラーを返すこと', () => {
    const errors = validateTransactionInput({ ...validInput, categoryId: 0 });
    expect(errors).toContain('カテゴリを選択してください。');
  });

  it('タイトルが空文字または空白のみの場合はエラーを返すこと', () => {
    expect(validateTransactionInput({ ...validInput, title: '' })).toContain(
      'タイトルを入力してください。'
    );
    expect(validateTransactionInput({ ...validInput, title: '   ' })).toContain(
      'タイトルを入力してください。'
    );
  });

  it('タイトルが100文字を超える場合はエラーを返すこと', () => {
    const longTitle = 'あ'.repeat(101);
    expect(validateTransactionInput({ ...validInput, title: longTitle })).toContain(
      'タイトルは100文字以内で入力してください。'
    );
  });

  it('金額が0以下または小数の場合はエラーを返すこと', () => {
    expect(validateTransactionInput({ ...validInput, amount: 0 })).toContain(
      '金額は1円以上の整数で入力してください。'
    );
    expect(validateTransactionInput({ ...validInput, amount: -100 })).toContain(
      '金額は1円以上の整数で入力してください。'
    );
    expect(validateTransactionInput({ ...validInput, amount: 12.5 })).toContain(
      '金額は1円以上の整数で入力してください。'
    );
  });

  it('決済日が空の場合はエラーを返すこと', () => {
    expect(validateTransactionInput({ ...validInput, transactionDate: '' })).toContain(
      '決済日を入力してください。'
    );
  });
});
