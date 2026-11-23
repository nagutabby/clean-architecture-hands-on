# Chapter 6: フロントエンドのテスト実装（単体テスト・結合テスト）

## このチャプターの目的

Chapter 5 で実装したフロントエンドの各層に対して、Vitest、React Testing Library、MSW（Mock Service Worker）を用いた自動テストを Claude Code（AI）に実装させます。

クリーンアーキテクチャの層分離により、ビジネスルール（`domain`）は React やブラウザ環境なしで高速に単体テストでき、画面全体（`presentation`）は MSW で HTTP 通信をモック化することで、実際のユーザー操作に近い形で結合テストを実行できます。

1. 6-1. Vitest の設定ファイル作成（`vitest.config.ts`, `vitest.setup.ts`）
2. 6-2. 第1層（domain）の単体テスト（`validateTransaction.test.ts`）
3. 6-3. 第3層（presentation/utils）の単体テスト（`formatCurrency.test.ts`）
4. 6-4. MSW によるモックサーバーの構築（`handlers.ts`, `server.ts`）
5. 6-5. 画面全体の結合テスト（`TransactionSection.test.tsx`）
6. 6-6. 全テストの実行と AI によるテストレビュー

---

## 6-1. Vitest の設定ファイル作成

最初に、Vitest が jsdom 環境で React コンポーネントと `@/` パスエイリアスを扱えるように設定ファイルを作成します。ESM 環境でも安全にパスを解決できるよう、`__dirname` ではなく `fileURLToPath` を使用します。

### Claude Code への指示プロンプト

Claude Code のチャット欄に以下を入力して実行します。

```text
frontend 配下に Vitest の設定ファイルを2つ作成してください。

1. frontend/vitest.config.ts
   - @vitejs/plugin-react を有効化し、environment を 'jsdom'、setupFiles を ['./vitest.setup.ts']、globals を true に設定する
   - ESM 環境で動作するよう、__dirname は使わず fileURLToPath(new URL('./src', import.meta.url)) で '@' エイリアスを設定する
2. frontend/vitest.setup.ts
   - '@testing-library/jest-dom/vitest' をインポートする
   - src/mocks/server から server をインポートし、beforeAll で listen、afterEach で resetHandlers、afterAll で close を実行する
```

### 実装コードと設計のポイント

#### `vitest.config.ts`
`frontend/vitest.config.ts`

```ts
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
    globals: true,
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
});
```

#### `vitest.setup.ts`
`frontend/vitest.setup.ts`

```ts
import '@testing-library/jest-dom/vitest';
import { beforeAll, afterEach, afterAll } from 'vitest';
import { server } from './src/mocks/server';

beforeAll(() => server.listen({ onUnhandledFrame: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

`onUnhandledFrame: 'error'` を指定しておくことで（MSW v3 では旧 `onUnhandledRequest` から名称が変更されています）、テスト中に MSW で定義していない予期しない URL へリクエストが飛んだ場合に即座に検知できます。

---

## 6-2. 第1層（domain）の単体テスト実装

`domain/validateTransaction.ts` は React に一切依存しない純粋な TypeScript 関数であるため、コンポーネントを描画せずにそのまま呼び出して入力ルールを検証します。

### Claude Code への指示プロンプト

```text
frontend/src/features/transaction/domain/validateTransaction.test.ts を作成し、validateTransactionInput 関数の単体テストを実装してください。

検証ケース:
1. すべての項目が正しい場合に空配列が返ること
2. カテゴリIDが 0 以下の場合にエラーメッセージが返ること
3. タイトルが空文字・空白のみ・101文字以上の場合にそれぞれエラーメッセージが返ること
4. 金額が 0・負数・小数の場合にエラーメッセージが返ること
5. 決済日が空の場合にエラーメッセージが返ること
```

### 実装コード

#### `validateTransaction.test.ts`
`frontend/src/features/transaction/domain/validateTransaction.test.ts`

```ts
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
```

---

## 6-3. 第3層（presentation/utils）の単体テスト実装

表示整形ユーティリティである `formatCurrency` のテストもあわせて作成します。`Intl.NumberFormat` の円記号は実行環境の ICU データによって半角（`¥`）または全角（`￥`）になる場合があるため、正規表現でどちらの表記でも検証できるようにします。

### Claude Code への指示プロンプト

```text
frontend/src/features/transaction/presentation/utils/formatCurrency.test.ts を作成し、0 や 3桁区切りが必要な数値が日本円表記に整形されることを検証してください。円記号は実行環境によって半角（¥）と全角（￥）のどちらも取り得るため、正規表現で検証してください。
```

### 実装コード

#### `formatCurrency.test.ts`
`frontend/src/features/transaction/presentation/utils/formatCurrency.test.ts`

```ts
import { describe, it, expect } from 'vitest';
import { formatCurrency } from './formatCurrency';

describe('formatCurrency', () => {
  it('数値を3桁カンマ区切りの日本円表記に整形すること', () => {
    expect(formatCurrency(1280)).toMatch(/^[¥￥]1,280$/);
    expect(formatCurrency(300000)).toMatch(/^[¥￥]300,000$/);
  });

  it('0円を正しく整形すること', () => {
    expect(formatCurrency(0)).toMatch(/^[¥￥]0$/);
  });
});
```

---

## 6-4. MSW によるモックサーバーの構築

画面結合テストでは、バックエンドサーバー（Spring Boot）を起動する代わりに、MSW を使用してネットワークレベルで HTTP リクエストをインターセプトします。

登録（POST）や削除（DELETE）のあとに一覧取得（GET）が再実行された際にも最新状態が返るように、テストごとにリセット可能なインメモリ状態を持つハンドラーを定義します。

### Claude Code への指示プロンプト

```text
MSW（msw と msw/node）を使用して、API モックハンドラーとサーバー定義を作成してください。

1. frontend/src/mocks/handlers.ts
   - GET http://localhost:8080/api/categories: 食費（EXPENSE, id: 1）と給与（INCOME, id: 4）を返す
   - GET http://localhost:8080/api/transactions: インメモリの明細リストと、そこから計算した summary（totalIncome, totalExpense, balance）を返す
   - POST http://localhost:8080/api/transactions: リクエストボディを受け取ってリストに追加し、201 Created で返却する
   - DELETE http://localhost:8080/api/transactions/:id: 指定IDの明細をリストから削除し、204 No Content を返す
   - テスト間で状態を初期化するための resetMockTransactions 関数もエクスポートする
2. frontend/src/mocks/server.ts
   - setupServer(...handlers) でサーバーインスタンスをエクスポートする
```

### 実装コード

#### `handlers.ts`
`frontend/src/mocks/handlers.ts`

```ts
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

  http.delete('http://localhost:8080/api/transactions/:id', ({ params }) => {
    const id = Number(params.id);
    mockTransactions = mockTransactions.filter((t) => t.id !== id);
    return new HttpResponse(null, { status: 204 });
  }),
];
```

#### `server.ts`
`frontend/src/mocks/server.ts`

```ts
import { setupServer } from 'msw/node';
import { handlers } from './handlers';

export const server = setupServer(...handlers);
```

---

## 6-5. 画面全体の結合テスト実装

`TransactionSection` をレンダリングし、ユーザーの実際の操作（初期表示、フォーム入力と登録、バリデーションエラー表示、明細の削除、API エラー発生時の表示）を一通り検証します。金額の検証は円記号の環境差を吸収する正規表現と `getAllByText` に統一します。

### Claude Code への指示プロンプト

```text
React Testing Library（render, screen, fireEvent, waitFor）と MSW を使用して、TransactionSection の結合テストを実装してください。

ファイル: frontend/src/features/transaction/presentation/TransactionSection.test.tsx
検証ケース:
1. 初期表示時に API から取得した明細（「テストランチ」）と収支サマリーが表示されること
2. フォームにカテゴリ「給与（4）」、タイトル「月給」、金額「300000」を入力して「登録する」をクリックすると、一覧に「月給」が追加され、現在の残高が 299,000 円に更新されること
3. タイトルと金額を空のまま「登録する」をクリックすると、バリデーションエラーメッセージが表示されること
4. 一覧の「削除」ボタンをクリックすると明細が削除され、「登録された明細がありません。」が表示されること
5. API が 500 エラーを返した場合にエラーメッセージが表示されること
```

### 実装コード

#### `TransactionSection.test.tsx`
`frontend/src/features/transaction/presentation/TransactionSection.test.tsx`

```tsx
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
```

---

## 6-6. 全テストの実行と AI によるテストレビュー

すべてのテストファイルを配置したら、Claude Code にテスト実行とレビューを依頼します。

### Claude Code への指示プロンプト

```text
cd frontend && pnpm vitest run を実行し、すべての単体テストと結合テストがパスすることを確認してください。
あわせて、API が 400 エラー（サーバー側バリデーションエラー）を返した際のフォーム表示など、追加すべきテストケースがあれば提案してください。
```

すべてのテストがパスすれば、フロントエンドの実装とテストは完了です。

次の最終章 Chapter 7 では、Claude Code の Agent Skills（`.claude/skills/`）と Hooks（`.claude/settings.json`）を活用し、新しいユースケースをクリーンアーキテクチャの4層すべてに漏れなく実装・検証する発展演習を行います。
