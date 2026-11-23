# Chapter 5: フロントエンド実装：Next.js 16 / TypeScript

## このチャプターの目的

Chapter 2 で作成した `.claude/rules/frontend.md` に従って、AI である Claude Code にフロントエンドの4層を実装させます。

バックエンドと同様に、内側の層から外側へ向かって実装を進めます。また、画面ロジックを担う Use Case 層のカスタムフックが、外部通信の具体実装である `fetch` に直接依存しないよう、Repository インターフェースによる依存性逆転、つまり DIP をフロントエンドにも適用します。

実装を終えたら、画面の機能ごとに動作を確認します。

| CP | 種別 | 内容 |
| :--- | :--- | :--- |
| CP5-1 | 設計 | フロントエンドの設計案として画面構成、4層のファイル、依存方向を提示させ、承認する |
| CP5-2 | 実装 | 共通 UI パーツ `components/ui` の `Button`・`Input` |
| CP5-3 | 実装 | 第1層 `domain`：型定義と入力バリデーション |
| CP5-4 | 実装 | 第2層 `usecase`：Repository インターフェースと `useTransactions` フック |
| CP5-5 | 実装 | 第3層 `adapter`：`fetch` による API 通信クライアント |
| CP5-6 | 実装 | 第3層 `presentation` の部品：金額整形・サマリー・登録フォーム・明細一覧 |
| CP5-7 | 実装 | 第3層 `presentation` の画面統括：`TransactionSection`、Composition Root |
| CP5-8 | 実装 | 第4層 `app`：ルーティングページへの組み込みとビルド確認 |
| CP5-9 | テスト | アーキテクチャ規約の検査とビルド確認 |
| CP5-10 | 動作確認 | サーバー起動と、一覧・サマリーの初期表示 |
| CP5-11 | 動作確認 | 明細の登録・入力エラー・削除 |
| CP5-12 | 動作確認 | バックエンド停止時のエラー表示 |

## このチャプターの進め方

このチャプターは、上の表のチェックポイントである CP を **1つずつ** 進めます。Claude Code は CP を1つ終えるたびに結果を報告して停止し、次の指示を待ちます。この運用は `CLAUDE.md` の「チェックポイント運用」で定めています。

1. Claude Code を起動し、下の「開始プロンプト」を入力します。Claude Code は最初の CP だけを実施して停止します。
2. Claude Code の報告を、各 CP の「確認ポイント」と見比べて確認します。
3. 問題がなければ「次のチェックポイントに進む」と入力します。Claude Code が次の CP を1つだけ実施します。
4. 問題があれば、次へ進まずに「CPx-y をやり直して。〇〇が違う」のように指示し、その CP の中で直させます。

### 開始プロンプト

Claude Code のチャット欄に以下を入力して実行します。Claude Code は CP5-1 だけを実施して停止します。

```text
これから Chapter 5（フロントエンド実装）を、以下の12個のチェックポイント（CP）に分けて進めます。
CLAUDE.md の「チェックポイント運用」に従い、まず CP5-1 だけを実施して報告し、停止してください。
以降は、私が「次のチェックポイントに進む」と入力したときに、次の CP を1つだけ実施してください。
すべての実装は .claude/rules/frontend.md に従ってください。

作るフロントエンドの機能: 収支サマリー（総収入・総支出・現在の残高）の表示、明細一覧の表示、明細の登録フォーム、明細の削除。バックエンドの API は http://localhost:8080/api です。

【CP5-1 設計】コードやファイルは作成せず、次の4点を提示してください。
1. 画面を構成するコンポーネントの一覧と親子関係、責務
2. 4層（domain / usecase / adapter / presentation / app）ごとのファイル一覧（frontend/src 配下のパス）
3. 依存方向の表（usecase が adapter を import しない仕組み、Repository インターフェースの置き場所、Composition Root の場所）
4. 画面の状態遷移（初期表示、登録、削除、エラー）と、.claude/rules/frontend.md に照らした設計上の懸念点（なければ「なし」）

【CP5-2 実装】Tailwind CSS v4 を使用して、汎用 UI コンポーネントを2つ作成してください。
1. frontend/src/components/ui/Button.tsx
   - ButtonHTMLAttributes<HTMLButtonElement> を継承し、variant ('primary' | 'secondary' | 'danger') を受け取る
2. frontend/src/components/ui/Input.tsx
   - InputHTMLAttributes<HTMLInputElement> を継承し、オプショナルな label を受け取る
   - React の useId() を使用し、id が未指定の場合でも label の htmlFor と input の id が確実に紐付くようにする
完了条件: cd frontend && pnpm exec tsc --noEmit が成功すること。

【CP5-3 実装】第1層（frontend/src/features/transaction/domain）を実装してください。React や fetch、表示用フォーマット処理は一切含めないでください。
1. domain/types.ts
   - TransactionType ('INCOME' | 'EXPENSE')
   - Category (id, name, type)
   - TransactionItem (id, categoryId, categoryName, categoryType, title, amount, transactionDate, memo)
   - BalanceSummary (totalIncome, totalExpense, balance)
   - TransactionListData (transactions, summary)
   - CreateTransactionInput (categoryId, title, amount, transactionDate, memo)
2. domain/validateTransaction.ts
   - CreateTransactionInput の入力値を検証し、エラーメッセージの文字列配列（string[]）を返す純粋関数 validateTransactionInput を定義する
   - 検証ルール: カテゴリIDが1以上であること、タイトルが空文字・空白のみでないこと（100文字以内）、金額が1以上の整数であること、決済日が空でないこと
完了条件: domain 配下に react の import や fetch がないことを grep で確認し、pnpm exec tsc --noEmit が成功すること。

【CP5-4 実装】第2層（frontend/src/features/transaction/usecase）を実装してください。adapter 配下のファイルは一切 import しないでください。
1. usecase/TransactionRepository.ts
   - fetchCategories(), fetchTransactions(), createTransaction(input), deleteTransaction(id) を定義するインターフェース
2. usecase/useTransactions.ts
   - 引数に repository: TransactionRepository を受け取るカスタムフック
   - repository は useRef で最新の参照を保持し、再描画時に新しいオブジェクトが渡されても useEffect が無限ループしないようにする
   - 初期表示時にカテゴリ一覧と明細一覧を取得し、新規登録・削除後には明細一覧とサマリーを再取得する
   - 登録・削除や再取得でエラーが発生した場合にも error 状態を適切に更新する
完了条件: usecase 配下に adapter の import がないことを grep で確認し、pnpm exec tsc --noEmit が成功すること。

【CP5-5 実装】第3層（frontend/src/features/transaction/adapter/transactionApi.ts）を実装してください。
usecase/TransactionRepository.ts の TransactionRepository インターフェースを実装するオブジェクト transactionApi をエクスポートしてください。
バックエンドの URL は http://localhost:8080/api とし、POST や DELETE でエラーが返った場合はレスポンス JSON の message を取り出して Error をスローしてください。
完了条件: pnpm exec tsc --noEmit が成功すること。

【CP5-6 実装】第3層の表示部品（frontend/src/features/transaction/presentation）を実装してください。純粋な表示コンポーネントからは adapter を直接 import しないでください。
1. presentation/utils/formatCurrency.ts
   - 数値を日本円表記（例: ￥1,280）に整形する関数 formatCurrency
2. presentation/BalanceSummaryCard.tsx
   - summary (totalIncome, totalExpense, balance) を3枚のカードで表示するコンポーネント
3. presentation/TransactionForm.tsx
   - カテゴリ選択、タイトル、金額、決済日、メモの入力フォーム。決済日の初期値は UTC（toISOString）ではなくローカルタイムゾーン基準の YYYY-MM-DD を使用する
   - 送信時に domain/validateTransaction.ts で事前検証を行い、エラーがあれば一覧表示する
4. presentation/TransactionList.tsx
   - 明細一覧テーブル（日付、カテゴリバッジ、タイトル、金額、メモ、削除ボタン）を表示するコンポーネント
完了条件: 4ファイルが adapter を import していないことを grep で確認し、pnpm exec tsc --noEmit が成功すること。

【CP5-7 実装】presentation/TransactionSection.tsx を実装してください。
- オプショナルな repository?: TransactionRepository（デフォルト値は adapter/transactionApi）を受け取り、useTransactions(repository) を使って全体を統括する Client Component
- adapter を import してよいのは、この Composition Root のみ
- error 発生時は画面全体を消さずに画面上部のエラーバナーとして表示する
完了条件: pnpm exec tsc --noEmit が成功すること。

【CP5-8 実装】frontend/src/app/transactions/page.tsx を作成し、TransactionSection を配置してください。
また、frontend/src/app/page.tsx から /transactions へリダイレクトするように変更してください。
完了条件: cd frontend && pnpm build が成功すること。

【CP5-9 テスト】frontend/src/features/transaction 配下の全ファイルについて、次の2点を実施してください。
1. .claude/rules/frontend.md に記載された4層の依存制約（domain が React や表示整形を含まないこと、usecase が adapter を直接 import していないこと、presentation の純粋な表示コンポーネントが adapter を直接 import していないこと）に違反がないか検査して報告する。
2. cd frontend && pnpm build を実行して型エラーやビルドエラーがないことを確認する。

【CP5-10 動作確認】PostgreSQL コンテナ（budget-app-db）の稼働を確認したうえで、バックエンド（cd backend && ./gradlew bootRun）とフロントエンド（cd frontend && pnpm dev）をそれぞれバックグラウンドで起動してください。
起動完了を確認し、curl で http://localhost:8080/api/categories が 200、http://localhost:3000/transactions が 200 を返すことを報告してください。サーバーは起動したままにしてください。
ブラウザでの確認は私が行います。

【CP5-11 動作確認】サーバーが起動していなければ起動してください。ブラウザでの確認は私が行うので、確認中にエラーが出た場合に備えて待機してください。異常を報告したら、ログと該当コードから原因を調べてください。

【CP5-12 動作確認】バックエンドのプロセスだけを停止し、フロントエンド（3000）は起動したままにしてください。停止したことを報告したら待機してください。ブラウザでの確認は私が行います。
```

---

## CP5-1 〔設計〕フロントエンドの設計案の提示と承認

コードを書く前に、画面の構成、4層への割り当て、依存方向を Claude Code に設計案として出させます。フロントエンドでも、`usecase` のフックが `adapter` の `fetch` に直接依存しない仕組みを先に決めておくことが重要です。

Claude Code が作成するファイルはありません。

### 設計案の目安

| 層 | ファイル | 内容 |
| :--- | :--- | :--- |
| 共通 UI | 2 | `components/ui/Button.tsx`、`Input.tsx` |
| `domain` | 2 | `types.ts`、`validateTransaction.ts` |
| `usecase` | 2 | インターフェースの `TransactionRepository.ts`、`useTransactions.ts` |
| `adapter` | 1 | `transactionApi.ts` |
| `presentation` | 5 | `formatCurrency.ts`、`BalanceSummaryCard`、`TransactionForm`、`TransactionList`、`TransactionSection` |
| `app` | 2 | `transactions/page.tsx`、`/transactions` へリダイレクトする `page.tsx` |

### 確認ポイント

- `useTransactions` が インターフェースである `TransactionRepository` を引数で受け取り、`adapter` を import しない設計になっていること
- `transactionApi` を注入する Composition Root が `TransactionSection` の1か所だけになっていること
- 状態遷移に、初期表示・登録・削除・エラー表示が含まれていること

確認できたら、Claude Code に「次のチェックポイントに進む」と入力します。次は CP5-2 です。

---

## CP5-2 〔実装〕共通 UI パーツの作成: `components/ui`

各機能モジュールで共通して使う、汎用のボタンと入力フォームのパーツを先に作成します。

### 実装コードと設計のポイント

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
`useId()` をフォールバックとして設定しておくことで、呼び出し側が `id` を指定しない場合でも、ラベルをクリックしたときのフォーカス移動やテストでの `getByLabelText` が正しく動作します。

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

### 確認ポイント

- `components/ui/` に `Button.tsx` と `Input.tsx` が作成されていること
- `Input` が `useId()` を使い、`label` と `input` が紐付いていること
- Claude Code の報告に `tsc --noEmit` の成功が含まれていること

確認できたら、Claude Code に「次のチェックポイントに進む」と入力します。次は CP5-3 です。

---

## CP5-3 〔実装〕第1層：Domain、型定義と純粋なビジネスルール

`features/transaction/domain/` には、React・`fetch`・DOM 操作に一切依存しない、純粋な TypeScript の型定義と業務ルール関数を置きます。通貨の表示形式は UI 側の責務なので、この層には含めません。

### 実装コードと設計のポイント

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
バックエンドの `Amount` や `Transaction` と同じ業務ルールを、フロントエンド側でもドメイン関数として定義します。フレームワークに依存しない純粋関数なので、React を起動せずに単体テストできます。

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

### 確認ポイント

- `domain/` に `types.ts` と `validateTransaction.ts` の2ファイルが作成されていること
- Claude Code の報告に、React / `fetch` の import が 0 件だったことと、`tsc --noEmit` の成功が含まれていること
- `validateTransactionInput` がバックエンドの `Amount` / `Transaction` と同じ業務ルール、つまり金額 1 円以上とタイトル 100 文字以内を持っていること

確認できたら、Claude Code に「次のチェックポイントに進む」と入力します。次は CP5-4 です。

---

## CP5-4 〔実装〕第2層：Use Case、Repository インターフェースとカスタムフック

第2層の `usecase/` では、外部 API との通信の取り決めを表す `TransactionRepository` インターフェースである Output Port と、画面の状態遷移やユースケースの手順を制御するカスタムフック `useTransactions` を実装します。

重要なのは、`useTransactions.ts` が `adapter/` 配下の具体実装を直接 `import` しないことです。インターフェースを引数で受け取ることで、依存性逆転の原則をフロントエンドでも実現します。

### 実装コードと設計のポイント

#### `TransactionRepository.ts`: Output Port
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

#### `useTransactions.ts`: 画面ユースケースフック
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

### 確認ポイント

- `usecase/` に `TransactionRepository.ts` と `useTransactions.ts` が作成されていること
- `useTransactions.ts` が `adapter` を import していないこと。Claude Code の報告に grep 結果が含まれていること
- `repository` を `useRef` で保持し、`useEffect` が無限ループしない実装になっていること

確認できたら、Claude Code に「次のチェックポイントに進む」と入力します。次は CP5-5 です。

---

## CP5-5 〔実装〕第3層：Adapter、API 通信クライアント

第3層の `adapter/` では、第2層で定義した `TransactionRepository` インターフェースを実装します。Spring Boot バックエンドとの HTTP 通信である `fetch` とエラーレスポンスの解析は、このクラスの中に閉じ込めます。

### 実装コードと設計のポイント

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

### 確認ポイント

- `adapter/transactionApi.ts` が作成され、`TransactionRepository` を実装していること
- POST / DELETE のエラー時に、レスポンス JSON の `message` を取り出して `Error` をスローしていること

確認できたら、Claude Code に「次のチェックポイントに進む」と入力します。次は CP5-6 です。

---

## CP5-6 〔実装〕第3層：Presentation、表示整形と UI 部品

第3層の `presentation/` には、表示用のフォーマット関数と React の UI コンポーネントを置きます。コンポーネント内には `fetch` を一切書かず、`useTransactions` フックから受け取ったデータとハンドラー関数を画面に結びつけます。

### 実装コードと設計のポイント

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
`new Date().toISOString()` は UTC 基準なので、日本時間つまり UTC+9 の午前0時〜午前9時に実行すると前日の日付になります。そこで `getTodayLocalDate()` を使い、ローカルタイムゾーン基準の当日の日付を初期値にします。

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

### 確認ポイント

- `presentation/` に `BalanceSummaryCard`、`TransactionForm`、`TransactionList`、`utils/formatCurrency.ts` が作成されていること
- 3つの表示コンポーネントが `adapter` を import していないこと。Claude Code の報告に grep 結果が含まれていること
- `TransactionForm` の決済日の初期値が、UTC 基準の `toISOString()` ではなくローカルタイムゾーン基準になっていること

確認できたら、Claude Code に「次のチェックポイントに進む」と入力します。次は CP5-7 です。

---

## CP5-7 〔実装〕第3層：Presentation、`TransactionSection` と Composition Root

表示部品をまとめ、`useTransactions` フックに `transactionApi` を注入する、画面全体を統括するコンポーネントを実装させます。`adapter` を import してよいのは、この `TransactionSection` だけです。

### 実装コードと設計のポイント

#### `TransactionSection.tsx`: Composition Root と画面統括
具体実装の `transactionApi` を、Use Case の `useTransactions` へ注入するのは、このコンポーネントだけです。また、通信エラーが起きても、フォームや一覧の構造は残したまま、上部にエラーバナーを表示します。

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

### 確認ポイント

- `presentation/TransactionSection.tsx` が作成されていること
- `repository` のデフォルト引数が `transactionApi` になっていること
- エラー時に画面全体が消えず、画面上部にエラーバナーが表示される実装になっていること

確認できたら、Claude Code に「次のチェックポイントに進む」と入力します。次は CP5-8 です。

---

## CP5-8 〔実装〕第4層：App Router ページへの組み込み

最後に、第4層である Next.js App Router のルーティングページに `TransactionSection` を配置します。

### 実装コード

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

### 確認ポイント

- `app/transactions/page.tsx` が作成され、`app/page.tsx` が `/transactions` へリダイレクトしていること
- `pnpm build` が成功していること

確認できたら、Claude Code に「次のチェックポイントに進む」と入力します。次は CP5-9 です。

---

## CP5-9 〔テスト〕アーキテクチャ規約の検査とビルド確認

実装が終わったら、フロントエンドに依存ルール違反がないか Claude Code にレビューさせ、ビルドして型エラーがないことを確認します。

### 確認ポイント

- Claude Code の報告で、4層の依存制約の違反が 0 件であること
- `pnpm build` が成功していること

確認できたら、Claude Code に「次のチェックポイントに進む」と入力します。次は CP5-10 です。

---

## CP5-10 〔動作確認〕サーバー起動と、一覧・サマリーの初期表示

ここからは、画面の機能ごとにブラウザで動作を確認します。確認は人がブラウザで行い、Claude Code はサーバーの起動と稼働の維持を担当します。

ブラウザで `http://localhost:3000/transactions` を開きます。`http://localhost:3000` でも `/transactions` へ転送されます。

### 確認ポイント

- Claude Code の報告で、PostgreSQL・バックエンドの 8080 番ポート・フロントエンドの 3000 番ポートがすべて起動していること
- 「総収入」「総支出」「現在の残高」の3枚のカードが、いずれも `￥0` で表示されること
- 明細一覧に「登録された明細がありません。」と表示されること
- 登録フォームのカテゴリ選択に、バックエンドから取得した5件のカテゴリが表示されること
- 登録フォームの決済日の初期値が、ローカル日付の今日の日付になっていること

確認できたら、Claude Code に「次のチェックポイントに進む」と入力します。次は CP5-11 です。

---

## CP5-11 〔動作確認〕明細の登録・入力エラー・削除

ブラウザで次の順に操作し、画面が期待どおりに更新されることを確認します。Claude Code はサーバーを起動したまま待機します。

### 確認ポイント

- **登録: 収入**：収入カテゴリの「給与」、タイトル「月給」、金額「300000」で「登録する」を押すと、一覧に1行追加され、総収入と現在の残高が `￥300,000` になること
- **登録: 支出**：支出カテゴリの「食費」、タイトル「ランチ」、金額「1500」で登録すると、総支出が `￥1,500`、現在の残高が `￥298,500` に更新されること
- **入力エラー**：タイトルと金額を空にして登録しようとすると、バリデーションエラーが表示され、一覧が変わらないこと
- **削除**：一覧の「削除」ボタンを押すと、その行が消え、サマリーの金額が再計算されること。2件とも削除すると、`￥0` と「登録された明細がありません。」に戻ること

確認できたら、Claude Code に「次のチェックポイントに進む」と入力します。次は CP5-12 です。

---

## CP5-12 〔動作確認〕バックエンド停止時のエラー表示

Claude Code にバックエンドだけを停止させ、通信エラー時の画面の動きを確認します。

### 確認ポイント

- ブラウザを再読み込みすると、画面上部に「エラーが発生しました: …」というエラーバナーが表示されること
- エラーバナーが出ていても、フォームや一覧などの画面構造が消えないこと

確認が終わったら、Claude Code に「起動中のサーバーをすべて停止して」と指示します。

フロントエンドの画面で、一覧・登録・入力エラー・削除・通信エラーの動作が確認できれば、フロントエンドの実装は完了です。次の Chapter 6 では、フロントエンドのドメイン単体テストと Vitest + MSW による画面結合テストを実装します。
