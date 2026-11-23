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
