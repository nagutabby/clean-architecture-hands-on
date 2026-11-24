import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, beforeEach } from 'vitest';
import { http, HttpResponse } from 'msw';
import { server } from '@/mocks/server';
import { resetMockTransactions } from '@/mocks/handlers';
import { TransactionSection } from './TransactionSection';

describe('TransactionSection', () => {
  beforeEach(() => {
    resetMockTransactions();
  });

  it('初期表示時にAPIから取得した明細とサマリーが表示されること', async () => {
    render(<TransactionSection/>);

    await waitFor(() => {
      expect(screen.getByText('テストランチ')).toBeInTheDocument();
    });

    // 一覧の行の金額と、サマリーカードの総支出・残高を検証する
    expect(screen.getByText('総支出').nextElementSibling).toHaveTextContent(/^[¥￥]1,000$/);
    expect(screen.getByText('現在の残高').nextElementSibling).toHaveTextContent(/^[-−][¥￥]1,000$/);
    // 一覧の行（-￥1,000）と残高カード（-￥1,000）の2箇所に表示される
    expect(screen.getAllByText(/^[-−][¥￥]1,000$/)).toHaveLength(2);
  });

  it('フォームから新規明細を登録すると一覧と残高サマリーが更新されること', async () => {
    render(<TransactionSection/>);

    await waitFor(() => {
      expect(screen.getByText('テストランチ')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText('カテゴリ'), { target: { value: '4' } });
    fireEvent.change(screen.getByLabelText('タイトル'), { target: { value: '月給' } });
    fireEvent.change(screen.getByLabelText('金額（円）'), { target: { value: '300000' } });
    fireEvent.click(screen.getByRole('button', { name: '登録する' }));

    await waitFor(() => {
      expect(screen.getByText('月給')).toBeInTheDocument();
    });

    expect(screen.getAllByText(/^[¥￥]299,000$/).length).toBeGreaterThan(0);
  });

  it('不正な入力値で送信した場合はバリデーションエラーを表示すること', async () => {
    render(<TransactionSection/>);

    await waitFor(() => {
      expect(screen.getByText('テストランチ')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: '登録する' }));

    expect(await screen.findByText('タイトルを入力してください。')).toBeInTheDocument();
    expect(screen.getByText('金額は1円以上の整数で入力してください。')).toBeInTheDocument();
  });

  it('削除ボタンをクリックすると明細が削除されること', async () => {
    render(<TransactionSection/>);

    await waitFor(() => {
      expect(screen.getByText('テストランチ')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: '削除' }));

    await waitFor(() => {
      expect(screen.getByText('登録された明細がありません。')).toBeInTheDocument();
    });
  });

  it('APIエラー発生時にエラーメッセージが表示されること', async () => {
    server.use(
      http.get('http://localhost:8080/api/transactions', () => {
        return new HttpResponse(null, { status: 500 });
      })
    );

    render(<TransactionSection/>);

    expect(await screen.findByRole('alert')).toHaveTextContent(/エラーが発生しました/);
  });
});
