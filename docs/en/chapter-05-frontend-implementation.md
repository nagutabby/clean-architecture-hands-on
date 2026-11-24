# Chapter 5: Frontend Implementation (Next.js 16 / TypeScript)

## Goal of this chapter

Based on the `.claude/rules/frontend.md` created in Chapter 2, you have Claude Code (the AI) implement the four layers of the frontend.

As in the backend, implementation proceeds from the inner layers outward. The Use Cases layer (custom hooks), which holds the screen logic, must not depend directly on the concrete implementation of external communication (`fetch`), so the frontend also applies Dependency Inversion (DIP) through a Repository interface.

As in the backend, you implement the inner layers first, then verify each screen feature in the browser.

| CP | Kind | Content |
| :--- | :--- | :--- |
| CP5-1 | Design | Have Claude Code present the frontend design proposal (screen structure, files for the four layers, dependency direction) and approve it |
| CP5-2 | Implementation | Shared UI parts (`components/ui`): `Button`, `Input` |
| CP5-3 | Implementation | Layer 1 `domain`: type definitions and input validation |
| CP5-4 | Implementation | Layer 2 `usecase`: Repository interface and the `useTransactions` hook |
| CP5-5 | Implementation | Layer 3 `adapter`: API client using `fetch` |
| CP5-6 | Implementation | Layer 3 `presentation` (parts): amount formatting, summary, entry form, transaction list |
| CP5-7 | Implementation | Layer 3 `presentation` (screen coordination): `TransactionSection` (Composition Root) |
| CP5-8 | Implementation | Layer 4 `app`: wiring into the routing page and build check |
| CP5-9 | Test | Architecture rule check and build check |
| CP5-10 | Verification | Starting the servers and the initial display of the list and summary |
| CP5-11 | Verification | Registering transactions, input errors, deletion |
| CP5-12 | Verification | Error display when the backend is stopped |

## How to work through this chapter

This chapter proceeds through the checkpoints (CPs) in the table above **one at a time**. After finishing each CP, Claude Code reports the result, stops, and waits for the next instruction (this is defined in the "Checkpoint operation" section of `CLAUDE.md`).

1. Start Claude Code and enter the "Start prompt" below. Claude Code performs only the first CP and then stops.
2. Compare the report with the "Points to check" of each CP to confirm the result.
3. If there is no problem, type "Proceed to the next checkpoint". Claude Code performs only the next CP.
4. If there is a problem, do not move on. Instead, instruct Claude Code, for example "Redo CPx-y. XX is wrong", and have it fix the issue within that CP.

### Start prompt

Enter the following in the Claude Code chat box and run it. Claude Code performs only CP5-1 and then stops.

```text
We will now work through Chapter 5 (frontend implementation) in the following 12 checkpoints (CPs).
Following the "Checkpoint operation" section of CLAUDE.md, first perform only CP5-1, report, and stop.
After that, perform only the next single CP each time I type "Proceed to the next checkpoint".
Follow .claude/rules/frontend.md for all implementation.

Frontend features to build: display of the income/expense summary (total income, total expense, current balance), display of the transaction list, a transaction registration form, and transaction deletion. The backend API is http://localhost:8080/api.

【CP5-1 Design】Do not create any code or files. Present the following four items.
1. The list of components that make up the screen, their parent-child relationships, and their responsibilities
2. The file list for each layer (domain / usecase / adapter / presentation / app), as paths under frontend/src
3. A table of dependency directions (how usecase avoids importing adapter, where the Repository interface lives, where the Composition Root is)
4. The screen state transitions (initial display, registration, deletion, errors) and any design concerns in light of .claude/rules/frontend.md (write "None" if there are none)

【CP5-2 Implementation】Using Tailwind CSS v4, create two generic UI components.
1. frontend/src/components/ui/Button.tsx
   - Extend ButtonHTMLAttributes<HTMLButtonElement> and accept variant ('primary' | 'secondary' | 'danger')
2. frontend/src/components/ui/Input.tsx
   - Extend InputHTMLAttributes<HTMLInputElement> and accept an optional label
   - Use React's useId() so that the label's htmlFor and the input's id are always linked even when id is not specified
Completion condition: cd frontend && pnpm exec tsc --noEmit succeeds.

【CP5-3 Implementation】Implement layer 1 (frontend/src/features/transaction/domain). Do not include any React, fetch, or display formatting code.
1. domain/types.ts
   - TransactionType ('INCOME' | 'EXPENSE')
   - Category (id, name, type)
   - TransactionItem (id, categoryId, categoryName, categoryType, title, amount, transactionDate, memo)
   - BalanceSummary (totalIncome, totalExpense, balance)
   - TransactionListData (transactions, summary)
   - CreateTransactionInput (categoryId, title, amount, transactionDate, memo)
2. domain/validateTransaction.ts
   - Define validateTransactionInput, a pure function that validates the values of CreateTransactionInput and returns an array of error message strings (string[])
   - Validation rules: the category ID is 1 or greater; the title is not empty or whitespace only (at most 100 characters); the amount is an integer of 1 or greater; the transaction date is not empty
Completion condition: confirm with grep that there is no react import or fetch under domain, and pnpm exec tsc --noEmit succeeds.

【CP5-4 Implementation】Implement layer 2 (frontend/src/features/transaction/usecase). Do not import any file under adapter.
1. usecase/TransactionRepository.ts
   - An interface that defines fetchCategories(), fetchTransactions(), createTransaction(input), and deleteTransaction(id)
2. usecase/useTransactions.ts
   - A custom hook that takes repository: TransactionRepository as its argument
   - Keep the latest repository reference in a useRef so that useEffect does not loop infinitely even if a new object is passed on re-render
   - Fetch the category list and the transaction list on initial display, and re-fetch the transaction list and summary after registration or deletion
   - Update the error state appropriately when an error occurs during registration, deletion, or re-fetching
Completion condition: confirm with grep that there is no adapter import under usecase, and pnpm exec tsc --noEmit succeeds.

【CP5-5 Implementation】Implement layer 3 (frontend/src/features/transaction/adapter/transactionApi.ts).
Export transactionApi, an object that implements the TransactionRepository interface from usecase/TransactionRepository.ts.
Use http://localhost:8080/api as the backend URL, and when POST or DELETE returns an error, extract message from the response JSON and throw an Error.
Completion condition: pnpm exec tsc --noEmit succeeds.

【CP5-6 Implementation】Implement the display parts of layer 3 (frontend/src/features/transaction/presentation). Pure display components must not import adapter directly.
1. presentation/utils/formatCurrency.ts
   - formatCurrency, a function that formats a number in Japanese yen notation (e.g. ￥1,280)
2. presentation/BalanceSummaryCard.tsx
   - A component that displays summary (totalIncome, totalExpense, balance) as three cards
3. presentation/TransactionForm.tsx
   - An input form with category selection, title, amount, transaction date, and memo. The initial value of the transaction date uses YYYY-MM-DD based on the local time zone, not UTC (toISOString)
   - On submit, validate in advance with domain/validateTransaction.ts and display the errors as a list if there are any
4. presentation/TransactionList.tsx
   - A component that displays the transaction list table (date, category badge, title, amount, memo, delete button)
Completion condition: confirm with grep that the four files do not import adapter, and pnpm exec tsc --noEmit succeeds.

【CP5-7 Implementation】Implement presentation/TransactionSection.tsx.
- A Client Component that takes an optional repository?: TransactionRepository (default value: adapter/transactionApi) and coordinates the whole screen using useTransactions(repository)
- This Composition Root is the only place allowed to import adapter
- When an error occurs, show it as an error banner at the top of the screen without hiding the whole screen
Completion condition: pnpm exec tsc --noEmit succeeds.

【CP5-8 Implementation】Create frontend/src/app/transactions/page.tsx and place TransactionSection in it.
Also change frontend/src/app/page.tsx so that it redirects to /transactions.
Completion condition: cd frontend && pnpm build succeeds.

【CP5-9 Test】For all files under frontend/src/features/transaction, do the following two things.
1. Check for and report any violations of the four-layer dependency constraints described in .claude/rules/frontend.md (domain contains no React or display formatting, usecase does not import adapter directly, and pure display components in presentation do not import adapter directly).
2. Run cd frontend && pnpm build and confirm there are no type errors or build errors.

【CP5-10 Verification】After confirming that the PostgreSQL container (budget-app-db) is running, start the backend (cd backend && ./gradlew bootRun) and the frontend (cd frontend && pnpm dev) each in the background.
Confirm that startup has completed, and report that curl returns 200 for http://localhost:8080/api/categories and 200 for http://localhost:3000/transactions. Leave the servers running.
I will do the browser checks myself.

【CP5-11 Verification】If the servers are not running, start them. I will do the browser checks myself, so wait in case an error occurs during the checks. When I report an anomaly, investigate the cause from the logs and the relevant code.

【CP5-12 Verification】Stop only the backend process and leave the frontend (3000) running. Report that it has stopped, then wait. I will do the browser checks myself.
```

---

## CP5-1 [Design] Presenting and approving the frontend design proposal

Before any code is written, have Claude Code present a design proposal covering the screen structure, the assignment of the four layers, and the dependency direction. In the frontend as well, it is important to decide up front how `usecase` (the hook) avoids depending directly on `adapter` (`fetch`).

Claude Code creates no files in this CP.

### Guideline for the design proposal

| Layer | Files | Content |
| :--- | :--- | :--- |
| Shared UI | 2 | `components/ui/Button.tsx`, `Input.tsx` |
| `domain` | 2 | `types.ts`, `validateTransaction.ts` |
| `usecase` | 2 | `TransactionRepository.ts` (interface), `useTransactions.ts` |
| `adapter` | 1 | `transactionApi.ts` |
| `presentation` | 5 | `formatCurrency.ts`, `BalanceSummaryCard`, `TransactionForm`, `TransactionList`, `TransactionSection` |
| `app` | 2 | `transactions/page.tsx`, `page.tsx` (redirects to `/transactions`) |

### Points to check

- `useTransactions` takes `TransactionRepository` (the interface) as an argument and is designed not to import `adapter`
- The Composition Root that injects `transactionApi` is `TransactionSection` only, a single place
- The state transitions include initial display, registration, deletion, and error display

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. (Next: CP5-2)

---

## CP5-2 [Implementation] Creating shared UI parts (`components/ui`)

First create the generic button and input form parts that every feature module shares.

### Implementation code and design points

#### `Button.tsx`
`frontend/src/components/ui/Button.tsx`

```tsx
import { ButtonHTMLAttributes } from 'react';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger';
}

export function Button({
  variant = 'primary',
  className = '',
  children,
  ...props
}: ButtonProps) {
  const baseStyle =
    'px-4 py-2 rounded-md font-medium transition-colors focus:outline-none focus:ring-2 disabled:opacity-50 cursor-pointer disabled:cursor-not-allowed';
  const variants = {
    primary: 'bg-blue-600 hover:bg-blue-700 text-white focus:ring-blue-500',
    secondary: 'bg-gray-200 hover:bg-gray-300 text-gray-800 focus:ring-gray-400',
    danger: 'bg-rose-600 hover:bg-rose-700 text-white focus:ring-rose-500',
  };

  return (
    <button className={`${baseStyle} ${variants[variant]} ${className}`} {...props}>
      {children}
    </button>
  );
}
```

#### `Input.tsx`
Setting `useId()` as a fallback means that focus moving on label click and `getByLabelText` in tests work correctly even when the caller does not specify `id` explicitly.

`frontend/src/components/ui/Input.tsx`

```tsx
import { InputHTMLAttributes, useId } from 'react';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
}

export function Input({ id, label, className = '', ...props }: InputProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;

  return (
    <div className="w-full">
      {label && (
        <label htmlFor={inputId} className="block text-sm font-medium text-gray-700 mb-1">
          {label}
        </label>
      )}
      <input
        id={inputId}
        className={`text-gray-900 w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${className}`}
        {...props}
      />
    </div>
  );
}
```

### Points to check

- `Button.tsx` and `Input.tsx` are created in `components/ui/`
- `Input` uses `useId()` and the `label` and `input` are linked
- Claude Code's report includes a successful `tsc --noEmit`

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. (Next: CP5-3)

---

## CP5-3 [Implementation] Layer 1: Domain (type definitions and pure business rules)

`features/transaction/domain/` holds pure TypeScript type definitions and business rule functions that depend on none of React, `fetch`, or DOM operations. Currency formatting for display is the responsibility of the UI side, so it is not included in this layer.

### Implementation code and design points

#### `types.ts`
`frontend/src/features/transaction/domain/types.ts`

```ts
export type TransactionType = 'INCOME' | 'EXPENSE';

export interface Category {
  id: number;
  name: string;
  type: TransactionType;
}

export interface TransactionItem {
  id: number;
  categoryId: number;
  categoryName: string;
  categoryType: TransactionType;
  title: string;
  amount: number;
  transactionDate: string;
  memo?: string;
}

export interface BalanceSummary {
  totalIncome: number;
  totalExpense: number;
  balance: number;
}

export interface TransactionListData {
  transactions: TransactionItem[];
  summary: BalanceSummary;
}

export interface CreateTransactionInput {
  categoryId: number;
  title: string;
  amount: number;
  transactionDate: string;
  memo?: string;
}
```

#### `validateTransaction.ts`
The same business rules as the backend `Amount` and `Transaction` are also defined as a frontend domain function. Because it is a pure function with no framework dependency, it can be unit tested without starting React.

`frontend/src/features/transaction/domain/validateTransaction.ts`

```ts
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
```

### Points to check

- Two files, `types.ts` and `validateTransaction.ts`, are created in `domain/`
- Claude Code's report states that there were 0 imports of React / `fetch` and that `tsc --noEmit` succeeded
- `validateTransactionInput` has the same business rules as the backend `Amount` / `Transaction` (amount of at least 1 yen, title of at most 100 characters)

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. (Next: CP5-4)

---

## CP5-4 [Implementation] Layer 2: Use Cases (Repository interface and custom hook)

In layer 2 (`usecase/`), you implement the `TransactionRepository` interface (Output Port), which represents the communication contract with the external API, and the custom hook `useTransactions`, which controls the screen state transitions and the use case steps.

The key point is that `useTransactions.ts` never directly `import`s the concrete implementation under `adapter/`. By receiving the interface as an argument, the frontend also realizes the Dependency Inversion Principle.

### Implementation code and design points

#### `TransactionRepository.ts` (Output Port)
`frontend/src/features/transaction/usecase/TransactionRepository.ts`

```ts
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
```

#### `useTransactions.ts` (screen use case hook)
`frontend/src/features/transaction/usecase/useTransactions.ts`

```ts
import { useState, useEffect, useCallback, useRef } from 'react';
import {
  BalanceSummary,
  Category,
  CreateTransactionInput,
  TransactionItem,
} from '../domain/types';
import { TransactionRepository } from './TransactionRepository';

const INITIAL_SUMMARY: BalanceSummary = {
  totalIncome: 0,
  totalExpense: 0,
  balance: 0,
};

export function useTransactions(repository: TransactionRepository) {
  const [transactions, setTransactions] = useState<TransactionItem[]>([]);
  const [summary, setSummary] = useState<BalanceSummary>(INITIAL_SUMMARY);
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const repositoryRef = useRef(repository);
  useEffect(() => {
    repositoryRef.current = repository;
  }, [repository]);

  const refreshTransactions = useCallback(async () => {
    const updated = await repositoryRef.current.fetchTransactions();
    setTransactions(updated.transactions);
    setSummary(updated.summary);
  }, []);

  useEffect(() => {
    let active = true;

    async function loadInitialData() {
      try {
        setError(null);
        const [listData, categoryData] = await Promise.all([
          repositoryRef.current.fetchTransactions(),
          repositoryRef.current.fetchCategories(),
        ]);
        if (!active) return;
        setTransactions(listData.transactions);
        setSummary(listData.summary);
        setCategories(categoryData);
      } catch (err) {
        if (!active) return;
        setError(err instanceof Error ? err.message : 'データの取得に失敗しました');
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    }

    loadInitialData();
    return () => {
      active = false;
    };
  }, []);

  const addTransaction = async (input: CreateTransactionInput) => {
    await repositoryRef.current.createTransaction(input);
    try {
      setError(null);
      await refreshTransactions();
    } catch (err) {
      setError(err instanceof Error ? err.message : '一覧の再取得に失敗しました');
    }
  };

  const removeTransaction = async (id: number) => {
    try {
      setError(null);
      await repositoryRef.current.deleteTransaction(id);
      await refreshTransactions();
    } catch (err) {
      setError(err instanceof Error ? err.message : '明細の削除に失敗しました');
    }
  };

  return {
    transactions,
    summary,
    categories,
    isLoading,
    error,
    addTransaction,
    removeTransaction,
  };
}
```

### Points to check

- `TransactionRepository.ts` and `useTransactions.ts` are created in `usecase/`
- `useTransactions.ts` does not import `adapter` (Claude Code's report includes the grep result)
- `repository` is held in a `useRef`, so that the implementation does not make `useEffect` loop infinitely

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. (Next: CP5-5)

---

## CP5-5 [Implementation] Layer 3: Adapter (API client)

In `adapter/` of layer 3, you implement the `TransactionRepository` interface defined in layer 2, and encapsulate the HTTP communication (`fetch`) with the Spring Boot backend and the parsing of error responses.

### Implementation code and design points

#### `transactionApi.ts`
`frontend/src/features/transaction/adapter/transactionApi.ts`

```ts
import {
  Category,
  CreateTransactionInput,
  TransactionItem,
  TransactionListData,
} from '../domain/types';
import { TransactionRepository } from '../usecase/TransactionRepository';

const API_BASE_URL = 'http://localhost:8080/api';

export const transactionApi: TransactionRepository = {
  async fetchCategories(): Promise<Category[]> {
    const res = await fetch(`${API_BASE_URL}/categories`);
    if (!res.ok) {
      throw new Error('カテゴリの取得に失敗しました');
    }
    return res.json();
  },

  async fetchTransactions(): Promise<TransactionListData> {
    const res = await fetch(`${API_BASE_URL}/transactions`);
    if (!res.ok) {
      throw new Error('明細一覧の取得に失敗しました');
    }
    return res.json();
  },

  async createTransaction(input: CreateTransactionInput): Promise<TransactionItem> {
    const res = await fetch(`${API_BASE_URL}/transactions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    });

    if (!res.ok) {
      const errorData = await res.json().catch(() => null);
      throw new Error(errorData?.message || '明細の登録に失敗しました');
    }
    return res.json();
  },

  async deleteTransaction(id: number): Promise<void> {
    const res = await fetch(`${API_BASE_URL}/transactions/${id}`, {
      method: 'DELETE',
    });

    if (!res.ok) {
      const errorData = await res.json().catch(() => null);
      throw new Error(errorData?.message || '明細の削除に失敗しました');
    }
  },
};
```

### Points to check

- `adapter/transactionApi.ts` is created and implements `TransactionRepository`
- On a POST / DELETE error, it extracts `message` from the response JSON and throws an `Error`

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. (Next: CP5-6)

---

## CP5-6 [Implementation] Layer 3: Presentation (display formatting and UI parts)

`presentation/` of layer 3 holds the display formatting function and the React UI components. No `fetch` is written inside the components; they only bind the data and handler functions received from the `useTransactions` hook to the screen.

### Implementation code and design points

#### `formatCurrency.ts`
`frontend/src/features/transaction/presentation/utils/formatCurrency.ts`

```ts
export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('ja-JP', {
    style: 'currency',
    currency: 'JPY',
  }).format(amount);
}
```

#### `BalanceSummaryCard.tsx`
`frontend/src/features/transaction/presentation/BalanceSummaryCard.tsx`

```tsx
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
```

#### `TransactionForm.tsx`
`new Date().toISOString()` is based on UTC, so running it between 00:00 and 09:00 Japan time (UTC+9) returns the previous day's date. Use `getTodayLocalDate()` to set today's date in the local time zone as the initial value.

`frontend/src/features/transaction/presentation/TransactionForm.tsx`

```tsx
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
```

#### `TransactionList.tsx`
`frontend/src/features/transaction/presentation/TransactionList.tsx`

```tsx
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
```

### Points to check

- `BalanceSummaryCard`, `TransactionForm`, `TransactionList`, and `utils/formatCurrency.ts` are created in `presentation/`
- The three display components do not import `adapter` (Claude Code's report includes the grep result)
- The initial value of the transaction date in `TransactionForm` is based on the local time zone, not on `toISOString()` (UTC)

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. (Next: CP5-7)

---

## CP5-7 [Implementation] Layer 3: Presentation (`TransactionSection`: Composition Root)

Have Claude Code implement the screen coordination component that ties the display parts together and injects `transactionApi` into the `useTransactions` hook. `TransactionSection` is the only place allowed to import `adapter`.

### Implementation code and design points

#### `TransactionSection.tsx` (Composition Root and screen coordination)
Only this component injects `transactionApi` (the concrete implementation) into `useTransactions` (the Use Case). When a communication error occurs, it also shows an error banner at the top while keeping the screen structure of the form and the list.

`frontend/src/features/transaction/presentation/TransactionSection.tsx`

```tsx
'use client';

import { TransactionRepository } from '../usecase/TransactionRepository';
import { useTransactions } from '../usecase/useTransactions';
import { transactionApi } from '../adapter/transactionApi';
import { BalanceSummaryCard } from './BalanceSummaryCard';
import { TransactionForm } from './TransactionForm';
import { TransactionList } from './TransactionList';

interface TransactionSectionProps {
  repository?: TransactionRepository;
}

export function TransactionSection({ repository = transactionApi }: TransactionSectionProps) {
  const {
    transactions,
    summary,
    categories,
    isLoading,
    error,
    addTransaction,
    removeTransaction,
  } = useTransactions(repository);

  if (isLoading) {
    return <p className="text-gray-500 text-center py-8">読み込み中...</p>;
  }

  return (
    <div>
      {error && (
        <div role="alert" className="mb-6 p-4 bg-red-50 text-red-700 rounded-md">
          エラーが発生しました: {error}
        </div>
      )}

      <BalanceSummaryCard summary={summary} />
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="md:col-span-1">
          <TransactionForm categories={categories} onSubmit={addTransaction} />
        </div>
        <div className="md:col-span-2">
          <h2 className="text-xl font-bold text-gray-900 mb-4">家計簿明細一覧</h2>
          <TransactionList transactions={transactions} onDelete={removeTransaction} />
        </div>
      </div>
    </div>
  );
}
```

### Points to check

- `presentation/TransactionSection.tsx` is created
- The default argument of `repository` is `transactionApi`
- On error, the whole screen does not disappear and an error banner is displayed at the top of the screen

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. (Next: CP5-8)

---

## CP5-8 [Implementation] Layer 4: Wiring into the App Router page

Finally, place `TransactionSection` in the routing page of the Next.js App Router, which is layer 4.

### Implementation code

#### `frontend/src/app/transactions/page.tsx`

```tsx
import { TransactionSection } from '@/features/transaction/presentation/TransactionSection';

export const metadata = {
  title: 'シンプル家計簿 | クリーンアーキテクチャ実践ハンズオン',
};

export default function TransactionsPage() {
  return (
    <main className="min-h-screen bg-gray-100 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        <h1 className="text-3xl font-extrabold text-gray-900 mb-8 border-b pb-4">
          Simple Budget App
        </h1>
        <TransactionSection/>
      </div>
    </main>
  );
}
```

#### `frontend/src/app/page.tsx`

```tsx
import { redirect } from 'next/navigation';

export default function HomePage() {
  redirect('/transactions');
}
```

### Points to check

- `app/transactions/page.tsx` is created and `app/page.tsx` redirects to `/transactions`
- `pnpm build` succeeds

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. (Next: CP5-9)

---

## CP5-9 [Test] Architecture rule check and build check

Once the implementation is complete, have Claude Code review the frontend for dependency rule violations, and confirm through the build that there are no type errors.

### Points to check

- Claude Code's report shows 0 violations of the four-layer dependency constraints
- `pnpm build` succeeds

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. (Next: CP5-10)

---

## CP5-10 [Verification] Starting the servers and the initial display of the list and summary

From here on, you verify each screen feature in the browser. You do the checks in the browser yourself, and Claude Code is responsible for starting the servers and keeping them running.

Open `http://localhost:3000/transactions` in the browser (`http://localhost:3000` also redirects to `/transactions`).

### Points to check

- Claude Code's report shows that PostgreSQL, the backend (8080), and the frontend (3000) are all running
- The three cards "総収入" (Total income), "総支出" (Total expense), and "現在の残高" (Current balance) all display `￥0`
- The transaction list shows "登録された明細がありません。" (No transactions registered.)
- The category select in the registration form shows the five categories fetched from the backend
- The initial value of the transaction date in the registration form is today's date (local date)

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. (Next: CP5-11)

---

## CP5-11 [Verification] Registering transactions, input errors, deletion

Perform the following operations in the browser in order and confirm that the screen updates as expected. Claude Code waits with the servers still running.

### Points to check

- **Registration (income)**: With the category "給与（収入）" (Salary (income)), the title "月給" (Monthly salary), and the amount "300000", press "登録する" (Register). One row is added to the list, and total income and current balance become `￥300,000`
- **Registration (expense)**: Register with the category "食費（支出）" (Food (expense)), the title "ランチ" (Lunch), and the amount "1500". Total expense is updated to `￥1,500` and current balance to `￥298,500`
- **Input errors**: If you try to register with the title and amount left empty, validation errors are displayed and the list does not change
- **Deletion**: Pressing the "削除" (Delete) button in the list removes that row and recalculates the summary amounts (after deleting both entries, the screen returns to `￥0` and "登録された明細がありません。" (No transactions registered.))

When the checks pass, type "Proceed to the next checkpoint" in Claude Code. (Next: CP5-12)

---

## CP5-12 [Verification] Error display when the backend is stopped

Have Claude Code stop only the backend, and check how the screen behaves on a communication error.

### Points to check

- When you reload the browser, an error banner "エラーが発生しました: …" (An error occurred: …) is displayed at the top of the screen
- Even while the error banner is shown, the screen structure such as the form and the list does not disappear

When the checks are finished, instruct Claude Code with "起動中のサーバーをすべて停止して" (Stop all running servers).

Once you have confirmed the list, registration, input error, deletion, and communication error behavior on the frontend screen, the frontend implementation is complete. In the next chapter, Chapter 6, you will implement frontend domain unit tests and screen integration tests with Vitest + MSW.
