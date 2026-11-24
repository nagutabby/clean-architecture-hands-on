# Chapter 7: Feature Addition Exercise Using Claude Code Skills and Hooks

## Purpose of this chapter

With Clean Architecture, adding even one feature requires creating or changing files in several layers in order, such as "Input Port -> Interactor -> Repository -> Controller -> tests". With manual work or simple instructions alone, skipping layers and forgetting tests tend to happen.

In this final chapter, you build Claude Code's extension features, Agent Skills (`.claude/skills/`) and Hooks (`.claude/settings.json`), and use them to add the new feature "edit (update) a budget transaction" across the full stack while the design rules and tests are enforced automatically.

7-1 and 7-2 are preparation done by a person. When you run the `/add-usecase` skill in 7-3, Claude Code works through CP7-1 to CP7-7 one at a time.

| CP | Type | Content |
| :--- | :--- | :--- |
| 7-1 | Preparation (manual) | Create the `add-usecase` skill |
| 7-2 | Preparation (manual) | Configure the hook (`verify-changes.mjs` and `settings.json`) |
| CP7-1 | Design | Have the AI present the list of files to change across all layers (implementation plan) and approve it |
| CP7-2 | Implementation | Backend: domain -> usecase -> adapter (update API) |
| CP7-3 | Test | Backend tests: Interactor unit tests, Controller integration tests, all tests |
| CP7-4 | Implementation | Frontend: domain -> usecase -> adapter -> presentation (edit UI) |
| CP7-5 | Test | Frontend tests: MSW handler, integration tests, build |
| CP7-6 | Verification | Check the update API (curl) |
| CP7-7 | Verification | Final check on the screen (register -> edit -> delete) and run all tests |

---

## 7-1. Preparation (done by a person): Create the use case addition skill (`SKILL.md`)

As with the `setup-env` skill created in Chapter 2, you define as a skill the procedure for implementing a new use case across all four layers of Clean Architecture without omissions.

### 1. Create the skill directory

Run the following command in the project root directory (`budget-app-handson`).

```bash
mkdir -p .claude/skills/add-usecase
```

### 2. Create `.claude/skills/add-usecase/SKILL.md`

Create `.claude/skills/add-usecase/SKILL.md` and write the following content.

```markdown
---
name: add-usecase
description: A skill that adds a new use case following the four-layer structure of Clean Architecture (backend, frontend, and automated tests)
---

# Procedure for implementing a new use case

When the user invokes it with `/add-usecase <feature overview>`, strictly follow the dependency constraints in `.claude/rules/backend.md` and `.claude/rules/frontend.md`, and proceed through the following checkpoints (CP) in order.

Following "Checkpoint operation" in CLAUDE.md, report the result and stop each time you finish one CP, and do not start the next CP until the user types "Proceed to the next checkpoint". Perform only CP7-1 at first.

## CP7-1 [Design] Present the implementation plan
Before writing code, present the user with the list of files to create or change in the following four layers, and stop.
1. Backend layer 1 (domain): add the necessary domain rules or methods
2. Backend layer 2 (usecase): `usecase/input/*UseCase.kt`, `usecase/interactor/*Interactor.kt`, and, if needed, method additions to `usecase/output/*Repository.kt`
3. Backend layer 3 (adapter): `*RepositoryImpl.kt`, DTOs, `*Controller.kt`
4. Frontend layers 1 to 3: `domain/types.ts`, `usecase/TransactionRepository.ts`, `usecase/useTransactions.ts`, `adapter/transactionApi.ts`, and the `presentation/` components

## CP7-2 [Implementation] Implement the backend
1. Implement in order from the inner layers (domain -> usecase -> adapter).
2. In the Interactor of an update or delete use case, always verify that the target record exists with `findById` before calling a persistence method, and throw `DomainException` if it does not exist.
3. The Controller must receive only the interfaces in `usecase/input`, and must not reference `usecase/output` or `adapter/repository` (JPA) directly. Also, `adapter` must not import from `framework`.
4. Confirm that `cd backend && ./gradlew compileKotlin` succeeds.

## CP7-3 [Test] Backend tests
1. Create or update the following tests.
   - Unit tests for the Interactor (using MockK)
   - Integration tests for the Controller (using MockMvc). If you add a UseCase to the Controller's constructor, also add the corresponding Bean to the `TestConfig` of the existing `TransactionControllerTest` (otherwise the existing tests fail with `NoSuchBeanDefinitionException`)
2. Run `cd backend && ./gradlew test` and confirm that all tests, including the Konsist architecture tests, pass.

## CP7-4 [Implementation] Implement the frontend
1. Implement in order from the inner layers (domain -> usecase -> adapter -> presentation).
2. `usecase/useTransactions.ts` must not import `adapter` directly, and communicates through the `TransactionRepository` interface.
3. Confirm that `cd frontend && pnpm exec tsc --noEmit` succeeds.

## CP7-5 [Test] Frontend tests
1. Add test cases for the new feature to the MSW handlers (`src/mocks/handlers.ts`) and the integration tests (`TransactionSection.test.tsx`).
2. Run `cd frontend && pnpm vitest run` and `cd frontend && pnpm build`, and confirm that the tests and the type check pass.

## CP7-6 [Verification] Verify the API
1. Confirm that PostgreSQL (`budget-app-db`) is running, and start the backend in the background with `bootRun`.
2. Use curl to check the new feature's API for both normal and error cases (non-existent ID, category, and so on), and report the results. Delete all data registered for the check after the check.
3. Leave the backend running for CP7-7.

## CP7-7 [Verification] Final check on the screen and completion report
1. If the backend is not running, start it, and start the frontend (`cd frontend && pnpm dev`) in the background.
2. Run `cd backend && ./gradlew test` and `cd frontend && pnpm vitest run`, and report the results of all tests.
3. Compile the list of added and changed files and the test results from step 2 into a completion report, ask the user to check in the browser (`http://localhost:3000/transactions`), and wait.
```

This skill embeds the practice of stopping at each checkpoint into the procedure itself. Together with "Checkpoint operation" in `CLAUDE.md`, it lets you proceed one step at a time with checks, even when adding a feature.

---

## 7-2. Preparation (done by a person): Configure automatic verification with Hooks (`.claude/settings.json`)

With Claude Code's Hooks feature, you can automatically run any command before or after a tool runs (`PreToolUse` / `PostToolUse`) or when a response completes (`Stop`).

Here you configure a hook that runs a verification script chosen by the file's extension immediately after Claude Code creates (`Write`) or edits (`Edit`) a file.

- Immediately after a Kotlin file (`.kt`) is edited: run `./gradlew cleanTest test --tests "*ArchitectureTest"` to check for violations of the dependency rules between packages
- Immediately after a TypeScript file (`.ts` / `.tsx`) is edited: run `pnpm exec tsc --noEmit` to detect type errors

### 1. Create the verification script (`.claude/hooks/verify-changes.mjs`)

Embedding a complex one-liner inside JSON makes the escaping hard to maintain, so it is split out as a standalone Node.js script.

Create the directory in the root directory (`budget-app-handson`).

```bash
mkdir -p .claude/hooks
```

Create `.claude/hooks/verify-changes.mjs` and write the following content.

```javascript
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

const input = JSON.parse(fs.readFileSync(0, 'utf8'));
const filePath = input.tool_input?.file_path ?? '';
const projectDir = process.env.CLAUDE_PROJECT_DIR || process.cwd();

function runCheck(cmd, args, cwd) {
  try {
    execFileSync(cmd, args, {
      cwd,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
    });
  } catch (err) {
    const output = [err.stdout, err.stderr].filter(Boolean).join('\n');
    process.stderr.write(`検証エラー (${cmd} ${args.join(' ')}):\n${output}\n`);
    process.exit(2);
  }
}

if (filePath.endsWith('.kt')) {
  const gradlew = process.platform === 'win32' ? 'gradlew.bat' : './gradlew';
  runCheck(gradlew, ['cleanTest', 'test', '--tests', '*ArchitectureTest'], path.join(projectDir, 'backend'));
} else if (filePath.endsWith('.ts') || filePath.endsWith('.tsx')) {
  runCheck('pnpm', ['exec', 'tsc', '--noEmit'], path.join(projectDir, 'frontend'));
}
```

### 2. Create `.claude/settings.json`

Create `.claude/settings.json` in the project root and write the configuration that calls the script above using the environment variable `$CLAUDE_PROJECT_DIR`.

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "node \"$CLAUDE_PROJECT_DIR/.claude/hooks/verify-changes.mjs\""
          }
        ]
      }
    ]
  }
}
```

### Configuration points

- Use of `$CLAUDE_PROJECT_DIR`: Even if the current directory has moved during a Claude Code session, the paths to `backend/` and `frontend/` are always resolved correctly relative to the project root.
- Error output to `stderr` and exit code `2`: In Claude Code's hook specification, when the exit code is `2`, what was written to the **standard error output (`stderr`)** is fed back to the AI. The script combines both `stdout` and `stderr` and writes them with `process.stderr.write`, so that the details of Gradle or `tsc` errors reliably reach Claude Code and prompt it to fix them on its own.
- Why `cleanTest` is added: Konsist reads Kotlin source files directly, but Gradle does not treat the source files as inputs of the `test` task. If only an `import` statement is added and the bytecode does not change, `test` is judged `UP-TO-DATE`, Konsist does not run, and a violation is missed. `cleanTest` forces a re-run every time.
- Note on execution time: The tests are narrowed to `ArchitectureTest` only, but starting Gradle (the JVM) takes several seconds to over ten seconds.
- Note on failures in the middle of implementation: Right after a method is added to an Output Port, for example, the hook fails with a compile error while the implementation class has not been updated yet. This is expected behavior. The AI goes on to fix the implementation class, so it is fine as long as the hook finally succeeds.
- Changing to `Stop`: If you change it to `Stop` (which runs only once when a response completes), the input of the `Stop` event does not include `tool_input.file_path`, so nothing would be verified with this script as it is. In that case, rewrite the script to drop the branching by extension and run the verification of both `backend` and `frontend` unconditionally.

---

## 7-3. Hands-on exercise: Implement the "transaction update feature" with the `/add-usecase` skill

You now have the design rules (`CLAUDE.md` / `.claude/rules/`), the implementation procedure skill (`add-usecase`), and the automatic verification hook (`PostToolUse`) all in place.

Start Claude Code and add, as a new use case, "a feature to edit and update a registered budget transaction". The way of proceeding is the same as in Chapters 3 to 6: Claude Code stops each time it finishes one CP, so check the result and type "Proceed to the next checkpoint".

### 1. Start Claude Code (or reload the configuration)

Start Claude Code in the root directory (`budget-app-handson`). If it is already running, exit once with `/exit` and restart it, so that the new skill and hook are reliably loaded.

```bash
claude
```

### 2. Run the `/add-usecase` skill

Enter the following command in the chat field and run it. Claude Code performs only CP7-1 (design) and stops.

```text
/add-usecase Add a feature to edit (update) a budget transaction. On the backend, add a PUT /api/transactions/{id} endpoint (UpdateTransactionUseCase / UpdateTransactionInteractor), and throw DomainException when the category ID or the transaction ID does not exist. On the frontend, add an "Edit" button to each row of the transaction list, and implement a UI where clicking it lets you edit the content of that transaction and save it.
```

### 3. Checks at each checkpoint

When Claude Code stops at each CP, check the following and type "Proceed to the next checkpoint". In the implementation CPs (CP7-2 and CP7-4), the `PostToolUse` hook starts each time a file is created or edited, and `ArchitectureTest` (`.kt`) and `tsc --noEmit` (`.ts` / `.tsx`) run automatically.

#### CP7-1 [Design] Present the implementation plan

Claude Code presents the list of files to change across all layers and stops. No code has been written yet.

- The list includes all of the backend `domain` / `usecase` / `adapter` and the frontend `domain` / `usecase` / `adapter` / `presentation`
- The plan includes `UpdateTransactionUseCase` (Input Port), `UpdateTransactionInteractor`, and the use of `TransactionRepository` (update through `save`)
- The plan has the Controller depend only on `usecase/input`

#### CP7-2 [Implementation] Implement the backend

- Files are created and edited in order from the inner layers, and `UpdateTransactionInteractor` first verifies that the targets exist with `transactionRepository.findById(input.id)` and `categoryRepository.findById(input.categoryId)`, and throws `DomainException` if they do not exist
- Only when existence is confirmed, it builds a `Transaction` that keeps the existing `id` and calls `transactionRepository.save()`
- If the hook fails, Claude Code fixes the problem automatically and re-runs it (temporary failures in the middle are expected)
- `compileKotlin` succeeds

#### CP7-3 [Test] Backend tests

- `UpdateTransactionInteractorTest` (MockK) and a MockMvc test for `PUT /api/transactions/{id}` have been added
- The Bean for `UpdateTransactionUseCase` has been added to the `TestConfig` of the existing `TransactionControllerTest`
- `./gradlew test` passes entirely, including `ArchitectureTest`

#### CP7-4 [Implementation] Implement the frontend

- In order from the inner layers, the types, `TransactionRepository.updateTransaction`, `useTransactions`, `transactionApi`, and the edit UI have been implemented
- `useTransactions.ts` does not import `adapter`
- `tsc --noEmit` succeeds

#### CP7-5 [Test] Frontend tests

- `PUT` has been added to the MSW handlers, and a test case for editing has been added to `TransactionSection.test.tsx`
- `pnpm vitest run` and `pnpm build` succeed

#### CP7-6 [Verification] Verify the update API

- `PUT /api/transactions/{id}` succeeds, and the update is reflected in the content of `GET /api/transactions` and in `summary`
- Updating with a non-existent transaction ID or a non-existent category ID returns 400 and an error message

#### CP7-7 [Verification] Final check on the screen and completion report

Claude Code starts the backend and the frontend, reports the results of all tests, and waits. Open `http://localhost:3000/transactions` in a browser and check the following operations.

- Register a new transaction -> change the amount or category with "編集" (Edit) and save -> the list and the "総収入・総支出・現在の残高" (total income, total expense, current balance) follow the updated content
- When you "削除" (Delete) the edited transaction, it disappears from the list and the summary
- Claude Code's report shows that all tests of `./gradlew test` and `pnpm vitest run` pass

When the checks are done, tell Claude Code "Stop all running servers".

---

## 7-4. Summary of the hands-on

Through building the simple budget app, this hands-on practiced the following design principles and development workflow.

1. Separation of concerns and the four-layer structure
   Business rules (`domain`), application procedures (`usecase`), input/output conversion (`adapter`), and technical details (`framework` / `app`) are clearly separated.
2. Dependency Inversion Principle (DIP)
   By defining the `TransactionRepository` interface in the Use Case layer and injecting outer implementation classes and API clients, the inner code is structured so that it does not depend on external technology.
3. Layered automated tests and architecture verification
   In addition to JUnit 5, MockK, MockMvc, Vitest, and MSW, architecture tests with Konsist were introduced, building a mechanism that automatically prevents Dependency Rule violations.
4. Guardrail design for AI development
   By combining `CLAUDE.md`, `.claude/rules/`, Agent Skills (`SKILL.md`), and Hooks (`settings.json`), a development environment was set up in which an AI agent can implement and verify on its own while following the design conventions.
