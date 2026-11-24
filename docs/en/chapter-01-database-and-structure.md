# Chapter 1: Database Design and Overall Project Structure

## Purpose of This Chapter

Before writing any code, you finalize the database table design and the directory structure of both the backend and the frontend. The goal of this chapter is to understand which Clean Architecture layer each file belongs to, and the structure in which dependency arrows always point "from the outside in".

---

## 1. Database Design (Two-Table Structure)

We adopt a two-table structure that handles the distinction between income and expense and the relationship linking transactions to categories. Data integrity is guaranteed not only by domain rules in the application layer but also by database constraints (`NOT NULL`, `FOREIGN KEY`, `CHECK`).

```text
+------------------+          +------------------------+
|    categories    | 1      N |      transactions      |
+------------------+----------+------------------------+
| id (PK)          |          | id (PK)                |
| name             |          | category_id (FK)       |
| type             |          | title                  |
+------------------+          | amount                 |
                              | transaction_date       |
                              | memo                   |
                              +------------------------+
```

### `categories` (Category table)

A master table that manages the classification of income and expenses (food, daily necessities, salary, etc.).

| Column | Physical name | Type | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| Category ID | `id` | `BIGSERIAL` | `PRIMARY KEY` | Auto-generated ID |
| Category name | `name` | `VARCHAR(50)` | `NOT NULL` | Examples: "食費" (food), "日用品" (daily necessities), "給与" (salary) |
| Transaction type | `type` | `VARCHAR(10)` | `NOT NULL`, `CHECK (type IN ('INCOME', 'EXPENSE'))` | `INCOME` (income) / `EXPENSE` (expense) |

### `transactions` (Transaction table)

A transaction table that records daily income and expense entries.

| Column | Physical name | Type | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| Transaction ID | `id` | `BIGSERIAL` | `PRIMARY KEY` | Auto-generated ID |
| Category ID | `category_id` | `BIGINT` | `NOT NULL`, `FOREIGN KEY` | Foreign key to `categories.id` |
| Title | `title` | `VARCHAR(100)` | `NOT NULL` | Example: "スーパーで買い物" (shopping at the supermarket) |
| Amount | `amount` | `INT` | `NOT NULL`, `CHECK (amount >= 1)` | A positive integer of at least 1 yen |
| Transaction date | `transaction_date` | `DATE` | `NOT NULL` | The date on which the income or expense occurred |
| Memo | `memo` | `TEXT` | `NULL` allowed | Free text |

---

## 2. Backend (Spring Boot) Directory Structure

Directly under the package name `com.example.budgetapp`, place packages corresponding to the four circles of Clean Architecture.

Data conversion with the DB (OR mapping) and query interface definitions have the responsibility of "mediating between outer technology and the inner domain", so they are placed in Layer 3 (`adapter.repository`). This removes the need for Layer 3 to reference Layer 4 (`framework`), and unifies the dependency direction into one way.

```text
backend/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── kotlin/com/example/budgetapp/
    │   │   ├── BudgetappApplication.kt
    │   │   │
    │   │   ├── domain/                              # [Layer 1: Entities] Innermost layer, pure business rules
    │   │   │   ├── exception/
    │   │   │   │   └── DomainException.kt           # Custom exception representing a domain rule violation
    │   │   │   └── model/
    │   │   │       ├── Amount.kt                    # Value object (the "at least 1 yen" rule)
    │   │   │       ├── BalanceSummary.kt            # Balance aggregation logic (total income, total expenses, balance)
    │   │   │       ├── Category.kt                  # Category model
    │   │   │       ├── Transaction.kt               # Transaction entity
    │   │   │       └── TransactionType.kt           # Transaction type (INCOME / EXPENSE)
    │   │   │
    │   │   ├── usecase/                             # [Layer 2: Use Cases] Application-specific rules
    │   │   │   ├── input/                           # Input Port (contract for the entry points called from the outside)
    │   │   │   │   ├── CreateTransactionUseCase.kt
    │   │   │   │   ├── DeleteTransactionUseCase.kt
    │   │   │   │   ├── GetCategoriesUseCase.kt
    │   │   │   │   └── GetTransactionsUseCase.kt
    │   │   │   ├── output/                          # Output Port (contract for the exits that request work from the outside)
    │   │   │   │   ├── CategoryRepository.kt
    │   │   │   │   └── TransactionRepository.kt
    │   │   │   └── interactor/                      # Concrete processing steps of each use case
    │   │   │       ├── CreateTransactionInteractor.kt
    │   │   │       ├── DeleteTransactionInteractor.kt
    │   │   │       ├── GetCategoriesInteractor.kt
    │   │   │       └── GetTransactionsInteractor.kt
    │   │   │
    │   │   ├── adapter/                             # [Layer 3: Interface Adapters] Translator between outside and inside
    │   │   │   ├── controller/                      # Web-side adapter (HTTP / JSON conversion)
    │   │   │   │   ├── advice/
    │   │   │   │   │   └── GlobalExceptionHandler.kt
    │   │   │   │   ├── dto/
    │   │   │   │   │   ├── CategoryResponse.kt
    │   │   │   │   │   ├── TransactionListResponse.kt
    │   │   │   │   │   └── TransactionRequest.kt
    │   │   │   │   ├── CategoryController.kt
    │   │   │   │   └── TransactionController.kt
    │   │   │   └── repository/                      # DB-side adapter (persistence / OR mapping)
    │   │   │       ├── entity/
    │   │   │       │   ├── CategoryJpaEntity.kt
    │   │   │       │   └── TransactionJpaEntity.kt
    │   │   │       ├── jpa/
    │   │   │       │   ├── JpaCategoryRepository.kt
    │   │   │       │   └── JpaTransactionRepository.kt
    │   │   │       ├── CategoryRepositoryImpl.kt
    │   │   │       └── TransactionRepositoryImpl.kt
    │   │   │
    │   │   └── framework/                           # [Layer 4: Frameworks & Drivers] Outermost layer, infrastructure configuration
    │   │       └── config/
    │   │           └── CorsConfig.kt                # Framework-wide configuration such as CORS
    │   │
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/
    │           └── V1__create_tables.sql
    │
    └── test/kotlin/com/example/budgetapp/
        ├── architecture/
        │   └── ArchitectureTest.kt                  # Automated verification of the four-layer dependency direction with Konsist
        ├── domain/model/
        │   ├── AmountTest.kt                        # Unit test for the value object
        │   ├── BalanceSummaryTest.kt                # Unit test for the balance aggregation logic
        │   └── TransactionTest.kt                   # Unit test for the entity invariants
        ├── usecase/interactor/
        │   ├── CreateTransactionInteractorTest.kt   # Create-transaction use case test using MockK
        │   └── DeleteTransactionInteractorTest.kt   # Delete-transaction use case test using MockK
        └── adapter/controller/
            └── TransactionControllerTest.kt         # API integration test using MockMvc
```

### Runtime Flow of Processing and Source Code Dependency Direction

The most important point of Clean Architecture is the structure in which "the order in which processing is called at runtime" and "the dependency direction in the source code" are reversed.

```text
[Runtime flow of processing (direction of data movement)]
Controller ──> Interactor ──> RepositoryImpl ──> JpaRepository ──> PostgreSQL

[Source code dependency direction (direction of imports)]
Controller ──> InputPort <── Interactor ──> OutputPort <── RepositoryImpl ──> JpaRepository ──> Spring Data JPA (external library)
(Layer 3)      (Layer 2)      (Layer 2)      (Layer 2)       (Layer 3)           (Layer 3)         (library; outside the scope of the Dependency Rule)
```

1. Prohibition of layer skipping and reverse dependencies
   - Every Controller always calls an Input Port (such as `GetCategoriesUseCase`), and never directly references an Output Port (`*Repository`) or `adapter.repository` (JPA).
   - Layer 3 (`adapter`) references only Layer 1 (`domain`) and Layer 2 (`usecase`), and never depends on the outer Layer 4 (`framework`).
2. Design simplification regarding the Presenter
   The original Clean Architecture has an Output Boundary (Presenter) that receives output from the Use Case. However, in modern REST APIs that return JSON, it is common to convert the Interactor's return value (Output Data) into a DTO in the Controller. This hands-on also adopts this practical structure.

---

## 3. Frontend (Next.js) Directory Structure

The frontend likewise uses a structure in which the inner business logic does not depend on the outer communication means or routing infrastructure.

```text
frontend/
├── package.json
├── vitest.config.ts
├── vitest.setup.ts
└── src/
    ├── app/                                         # [Layer 4: Frameworks] Next.js App Router
    │   ├── page.tsx                                 # Redirect from `/` to `/transactions`
    │   └── transactions/
    │       └── page.tsx                             # Page entry for `/transactions`
    │
    ├── components/ui/                               # Generic UI parts
    │   ├── Button.tsx
    │   └── Input.tsx
    │
    ├── features/transaction/
    │   ├── domain/                                  # [Layer 1: Entities] Type definitions, pure business rules
    │   │   ├── types.ts                             # Types such as TransactionItem, Category, BalanceSummary
    │   │   ├── validateTransaction.ts               # Pre-validation rules for registration input
    │   │   └── validateTransaction.test.ts          # Unit test for the domain rules
    │   │
    │   ├── usecase/                                 # [Layer 2: Use Cases] State management, screen use cases
    │   │   ├── TransactionRepository.ts             # Output Port (interface that abstracts API communication)
    │   │   └── useTransactions.ts                   # Use case hook for listing, creating, and deleting
    │   │
    │   ├── adapter/                                 # [Layer 3: Interface Adapters] Implementation of external communication
    │   │   └── transactionApi.ts                    # fetch implementation of TransactionRepository
    │   │
    │   └── presentation/                            # [Layer 3: Interface Adapters] UI display and formatting
    │       ├── utils/
    │       │   ├── formatCurrency.ts                # Display formatting to Japanese yen notation (￥1,280)
    │       │   └── formatCurrency.test.ts           # Unit test for the display formatting function
    │       ├── BalanceSummaryCard.tsx
    │       ├── TransactionForm.tsx
    │       ├── TransactionList.tsx
    │       ├── TransactionSection.tsx               # Feature container (also serves as the Composition Root)
    │       └── TransactionSection.test.tsx          # Screen integration test using MSW
    │
    └── mocks/                                       # Mock server for tests (MSW)
        ├── handlers.ts
        └── server.ts
```

### Key Points of Responsibility Separation on the Frontend

- Why `presentation` and `adapter` both belong to Layer 3 (Interface Adapters)
  In the concentric circles of Clean Architecture, "accepting UI input and rendering screens (equivalent to Controller / Presenter)" and "converting input and output with external systems (equivalent to Gateway / API client)" both play the role of connecting the inner domain and use cases with the outside world, so both are placed in the same Layer 3 (Interface Adapters).
- Restriction on direct references to `adapter` from UI components (Composition Root)
  The pure display components in `presentation` (`BalanceSummaryCard`, `TransactionForm`, `TransactionList`) must not import `adapter/` directly. In addition, in the Next.js App Router a Server Component (`app/transactions/page.tsx`) cannot pass an object containing functions directly to a Client Component via props, so only `TransactionSection.tsx`, the root container of the feature, acts as the Composition Root (the assembler of dependency injection): it receives `adapter/transactionApi` as a default argument and injects it into `useTransactions`.
- Separation of the Domain layer and the Presentation layer
  Currency formatting (`formatCurrency.ts`) is logic for screen display, so it is placed in `presentation/utils/`. On the other hand, validation of business rules for amount and title (`validateTransaction.ts`) is a pure domain rule, so it is placed in `domain/`.

---

## 4. Points to Check When Proceeding with Claude Code

Based on this structure, the next Chapter 2 creates the design rule files for Claude Code (`CLAUDE.md` and `.claude/rules/`).

1. When creating a new file, always check which layer of the directory structure above it belongs to (`domain` / `usecase` / `adapter` / `framework`, or `domain` / `usecase` / `adapter` / `presentation` / `app`) before placing it.
2. Immediately after creating or editing a file in an inner layer, check that no outer-layer packages or framework-specific `import` statements have slipped in.
