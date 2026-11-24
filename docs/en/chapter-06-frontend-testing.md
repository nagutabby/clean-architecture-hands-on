# Chapter 6: Frontend Test Implementation: Unit Tests and Integration Tests

## Purpose of this chapter

You have Claude Code, the AI, implement automated tests for each layer of the frontend built in Chapter 5, using Vitest, React Testing Library, and Mock Service Worker. Mock Service Worker is called MSW from here on.

Clean Architecture separates the layers. As a result, the business rules in `domain` can be unit-tested quickly without React or a browser environment. The whole screen in `presentation` can be integration-tested in a way that is close to real user operations, by mocking HTTP communication with MSW.

First set up the test foundation, that is, the mock server and the configuration. Then add tests target by target.

| CP | Type | Content |
| :--- | :--- | :--- |
| CP6-1 | Design | Have the AI present the frontend test strategy, covering targets, means, and cases to verify, and approve it |
| CP6-2 | Implementation | Build the mock server with MSW: `handlers.ts` and `server.ts` |
| CP6-3 | Implementation | Vitest configuration files: `vitest.config.ts` and `vitest.setup.ts` |
| CP6-4 | Test | Unit tests for `domain`: input validation |
| CP6-5 | Test | Unit tests for `presentation/utils`: currency formatting |
| CP6-6 | Test | Integration tests for the whole screen with Testing Library and MSW: list, registration, input errors, deletion, communication errors |
| CP6-7 | Verification | Run all tests, confirm the build, and review the tests |

## How to proceed in this chapter

In this chapter you work through the checkpoints, or CPs, in the table above **one at a time**. After finishing each CP, Claude Code reports the result, stops, and waits for the next instruction. This is defined in "Checkpoint operation" in `CLAUDE.md`.

1. Start Claude Code and enter the "Start prompt" below. Claude Code performs only the first CP and stops.
2. Compare the report with the "Points to check" of each CP.
3. If there are no problems, type "Proceed to the next checkpoint". Claude Code performs exactly one next CP.
4. If there are problems, do not move on. Instead, give an instruction such as "Redo CPx-y. The XX is wrong" so that it is fixed within that CP.

### Start prompt

Enter the following in the Claude Code chat field and run it. Claude Code performs only CP6-1 and stops.

```text
We will now work through Chapter 6 (frontend test implementation) in the following seven checkpoints (CP).
Following "Checkpoint operation" in CLAUDE.md, perform only CP6-1 first, report, and stop.
After that, perform exactly one next CP each time I type "Proceed to the next checkpoint".
Follow .claude/rules/frontend.md for the test implementation.

[CP6-1 Design] Do not create any test code or files. Present the frontend test strategy in a table.
1. The test targets (layer and file), the type of test and tool (Vitest / Testing Library / MSW), and the list of cases to verify
2. How to test the whole screen without starting the backend
3. What to verify as error cases and boundary values

[CP6-2 Implementation] Using MSW (msw and msw/node), create the API mock handlers and the server definition.
1. frontend/src/mocks/handlers.ts
   - GET http://localhost:8080/api/categories: return 食費 (EXPENSE, id: 1) and 給与 (INCOME, id: 4)
   - GET http://localhost:8080/api/transactions: return an in-memory list of transactions and a summary (totalIncome, totalExpense, balance) calculated from it
   - POST http://localhost:8080/api/transactions: receive the request body, add it to the list, and return it with 201 Created
   - DELETE http://localhost:8080/api/transactions/:id: remove the transaction with the given ID from the list and return 204 No Content
   - Also export a resetMockTransactions function to reset the state between tests
2. frontend/src/mocks/server.ts
   - Export the server instance with setupServer(...handlers)
Completion condition: cd frontend && pnpm exec tsc --noEmit succeeds.

[CP6-3 Implementation] Create two Vitest configuration files under frontend.
1. frontend/vitest.config.ts
   - Enable @vitejs/plugin-react, and set environment to 'jsdom', setupFiles to ['./vitest.setup.ts'], and globals to true
   - To work in an ESM environment, do not use __dirname; set the '@' alias with fileURLToPath(new URL('./src', import.meta.url))
2. frontend/vitest.setup.ts
   - Import '@testing-library/jest-dom/vitest'
   - Import server from src/mocks/server, and call listen in beforeAll, resetHandlers in afterEach, and close in afterAll
   - Pass onUnhandledFrame: 'error' to server.listen (the option name in MSW v3)
Completion condition: cd frontend && pnpm exec tsc --noEmit and pnpm vitest run --passWithNoTests succeed.

[CP6-4 Test] Create frontend/src/features/transaction/domain/validateTransaction.test.ts and implement unit tests for the validateTransactionInput function.
Cases to verify:
1. An empty array is returned when all fields are valid
2. An error message is returned when the category ID is 0 or less
3. An error message is returned for each of: an empty title, a whitespace-only title, and a title of 101 or more characters
4. An error message is returned when the amount is 0, negative, or a decimal
5. An error message is returned when the transaction date is empty
Completion condition: cd frontend && pnpm vitest run succeeds, and report the number of tests that ran.

[CP6-5 Test] Create frontend/src/features/transaction/presentation/utils/formatCurrency.test.ts and verify that 0 and numbers that need thousands separators are formatted in Japanese yen notation. The yen sign can be either half-width (¥) or full-width (￥) depending on the runtime environment, so verify with a regular expression.
Completion condition: cd frontend && pnpm vitest run succeeds, and report the number of tests that ran.

[CP6-6 Test] Using React Testing Library (render, screen, fireEvent, waitFor) and MSW, implement integration tests for TransactionSection.
File: frontend/src/features/transaction/presentation/TransactionSection.test.tsx
Cases to verify:
1. On initial display, the transaction fetched from the API (「テストランチ」) and the income/expense summary are displayed
2. When you enter category 「給与（4）」, title 「月給」, and amount 「300000」 in the form and click 「登録する」, 「月給」 is added to the list and the current balance is updated to 299,000 yen
3. When you click 「登録する」 with the title and amount left empty, validation error messages are displayed
4. When you click the 「削除」 button in the list, the transaction is deleted and 「登録された明細がありません。」 is displayed
5. When the API returns a 500 error, an error message is displayed
Completion condition: cd frontend && pnpm vitest run succeeds, and report the number of tests that ran.

[CP6-7 Verification] Run cd frontend && pnpm vitest run and confirm that all unit tests and integration tests pass. Also confirm that cd frontend && pnpm exec tsc --noEmit and pnpm build succeed.
Then propose any test cases that should be added, such as the form display when the API returns a 400 error (a server-side validation error).
```

---

## CP6-1 [Design] Present and approve the test strategy

Before writing tests, have Claude Code organize which layer is verified by which means. On the frontend, the key point is to use each approach where it fits: business rules get unit tests without React, and the whole screen gets integration tests with HTTP mocked by MSW.

Claude Code creates no files in this CP.

### Guideline for the design proposal

| Target | Type / tool | Main things to verify |
| :--- | :--- | :--- |
| `domain/validateTransaction.ts` | Unit test with Vitest | Validation rules for category, title, amount, and transaction date |
| `presentation/utils/formatCurrency.ts` | Unit test with Vitest | Japanese yen notation, thousands separators |
| `presentation/TransactionSection` | Integration test with Testing Library and MSW | Initial display, registration, input errors, deletion, communication errors |

### Points to check

- The whole-screen tests mock HTTP with MSW without starting the backend
- Error cases are included in the cases to verify: input errors and API 500 errors

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. Next is CP6-2.

---

## CP6-2 [Implementation] Build the mock server with MSW

In the screen integration tests, you do not start the backend server, Spring Boot. Instead, MSW intercepts HTTP requests at the network level.

The handlers hold in-memory state that can be reset for each test. This way, the latest state is returned even when the list fetch, a GET, runs again after a registration, a POST, or a deletion, a DELETE.

### Implementation code

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

### Points to check

- `src/mocks/handlers.ts` and `src/mocks/server.ts` have been created
- The four endpoints, which are get categories, get transactions, register, and delete, are defined as handlers, and `resetMockTransactions` is exported
- Claude Code's report states that `tsc --noEmit` succeeded

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. Next is CP6-3.

---

## CP6-3 [Implementation] Create the Vitest configuration files

First, create the configuration files so that Vitest can handle React components and the `@/` path alias in a jsdom environment. To resolve paths safely in an ESM environment, use `fileURLToPath` instead of `__dirname`.

`vitest.setup.ts` imports the `server` created in CP6-2, so the mock server is prepared first.

### Implementation code and design points

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

If you specify `onUnhandledFrame: 'error'`, you can detect immediately when a test sends a request to a URL that is not defined in MSW. Note that MSW v3 renamed this option from the former `onUnhandledRequest`.

### Points to check

- `vitest.config.ts` and `vitest.setup.ts` have been created
- `vitest.setup.ts` calls `server.listen({ onUnhandledFrame: 'error' })`, not the former `onUnhandledRequest`
- Claude Code's report states that `tsc --noEmit` and `pnpm vitest run --passWithNoTests` succeeded

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. Next is CP6-4.

---

## CP6-4 [Test] Unit tests for layer 1, `domain`

`domain/validateTransaction.ts` is a pure TypeScript function with no dependency on React. Call it directly to verify the input rules, without rendering any component.

### Implementation code

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

### Points to check

- `domain/validateTransaction.test.ts` has been created and contains five kinds of verification cases
- Claude Code's report states that `pnpm vitest run` succeeded and gives the number of tests. This is the first time `vitest.setup.ts` is actually loaded

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. Next is CP6-5.

---

## CP6-5 [Test] Unit tests for layer 3, `presentation/utils`

Also create a test for `formatCurrency`, the display formatting utility. The yen sign produced by `Intl.NumberFormat` can be either half-width, `¥`, or full-width, `￥`, depending on the ICU data of the runtime environment. So use a regular expression to accept either form.

### Implementation code

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

### Points to check

- `presentation/utils/formatCurrency.test.ts` has been created
- It verifies with a regular expression so that it passes with either the half-width or full-width yen sign
- Claude Code's report states that `pnpm vitest run` succeeded and gives the number of tests

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. Next is CP6-6.

---

## CP6-6 [Test] Integration tests for the whole screen

Render `TransactionSection` and verify the actual user operations end to end. These are initial display, form input and registration, display of validation errors, deletion of a transaction, and display when an API error occurs. For amount assertions, use a regular expression that absorbs the yen-sign differences between environments, together with `getAllByText`, throughout.

### Implementation code

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

    // Verify the row amount in the list and the total expense and balance in the summary cards
    expect(screen.getByText('総支出').nextElementSibling).toHaveTextContent(/^[¥￥]1,000$/);
    expect(screen.getByText('現在の残高').nextElementSibling).toHaveTextContent(/^[-−][¥￥]1,000$/);
    // Shown in two places: the list row (-￥1,000) and the balance card (-￥1,000)
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

### Points to check

- `presentation/TransactionSection.test.tsx` has been created and contains five kinds of verification cases: initial display, registration, input errors, deletion, and API error
- Amount assertions use a regular expression that absorbs the yen-sign differences between environments, together with `getAllByText`
- Claude Code's report states that `pnpm vitest run` succeeded and gives the number of tests

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. Next is CP6-7.

---

## CP6-7 [Verification] Run all tests, confirm the build, and review the tests

Once all test files are in place, ask Claude Code to run all the tests and review which cases are missing.

### Points to check

- All tests pass with `pnpm vitest run`
- `pnpm exec tsc --noEmit` and `pnpm build` succeed. That is, adding test code causes no type errors or build errors
- Read the proposals for additional cases raised in the review and choose the ones you need. They are not implemented automatically

When all tests pass, the frontend implementation and tests are complete.

In the next and final chapter, Chapter 7, you will do an advanced exercise that uses Claude Code's Agent Skills, which are in `.claude/skills/`, and Hooks, which are in `.claude/settings.json`, to implement and verify a new use case across all four layers of Clean Architecture without omissions.
