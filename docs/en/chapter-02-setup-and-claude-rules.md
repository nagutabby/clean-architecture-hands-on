# Chapter 2: Claude Code Rules and Environment Setup

## Purpose of This Chapter

In this chapter, you build the development environment with a clear division of roles between the person and the AI (Claude Code).

- 2-0 (done by a person): Install Homebrew, Rancher Desktop (Docker-compatible), and mise on the system.
- 2-1 (done by a person): Create the working directory, install languages and runtimes with mise, create the environment setup skill (`SKILL.md`), and place the Clean Architecture design rules (`CLAUDE.md` / `.claude/rules/`).
- 2-2 (done by the AI): Have Claude Code load the skill and rules the person created, then have the AI set up PostgreSQL 18, Spring Boot 4, and Next.js 16, through the initial build and migration check, stopping at each checkpoint.

---

## 2-0. Install Prerequisite Tools (done by a person)

This hands-on uses Rancher Desktop (Docker-compatible) as the container runtime and mise to manage the versions of Java, Node.js, and pnpm.

### 1. Check the Homebrew Installation

The following installations use Homebrew. Check whether it is already installed with the following command.

```bash
brew --version
```

If `command not found` is displayed, install it by following the instructions on the official Homebrew site (https://brew.sh/).

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

Run the "Next steps" commands shown when the installation finishes (such as `eval "$(/opt/homebrew/bin/brew shellenv)"`, which adds `brew` to your PATH), then run `brew --version` again and confirm that the version is displayed.

### 2. Install Rancher Desktop

Rancher Desktop bundles the Docker CLI (`docker` / `docker compose`). If you select `dockerd (moby)` as the container engine, you can operate it with the same commands as Docker Desktop.

```bash
brew install --cask rancher
```

After installation, start Rancher Desktop and select the following in the initial setup.

- Container Engine: `dockerd (moby)`
- Configure PATH: `Automatic`

After startup completes, verify that it works with the following commands.

```bash
docker --version
docker compose version
```

### 3. Install mise

```bash
brew install mise
echo 'eval "$(mise activate zsh)"' >> ~/.zshrc  # For bash / fish, adapt this to your shell
exec $SHELL -l
```

---

## 2-1. Project Initialization and Claude Code Configuration (done by a person)

The steps from here are carried out by the learner in a terminal and editor, from top to bottom.

### 1. Create the Root Working Directory

Open a terminal, create the root directory that holds the whole project and the configuration directories under `.claude`, then move into it.

```bash
mkdir -p budget-app-handson/.claude/skills/setup-env
mkdir -p budget-app-handson/.claude/rules
cd budget-app-handson
```

### 2. Install Java 25 / Node.js 26 / pnpm 11 (`mise.toml`)

Create `mise.toml` directly under the root directory (`budget-app-handson`), then pin and install the tool versions used by the project.

```bash
cat > mise.toml <<'EOF'
[tools]
java = "temurin-25"
node = "26"
pnpm = "11"
EOF
mise trust
mise install
```

Verify that Java 25, Node.js 26, and pnpm 11 are active with the following commands.

```bash
java -version   # 25.x
node -v         # v26.x
pnpm -v         # 11.x
```

### 3. Create the Environment Setup Skill (`.claude/skills/setup-env/SKILL.md`)

Create the skill file that lets Claude Code perform the environment setup in "2-2" below.

Create `.claude/skills/setup-env/SKILL.md` and write the following content.

````markdown
---
name: setup-env
description: Skill that builds the local development environment for Budget App (PostgreSQL 18, Spring Boot 4, Next.js 16) and runs it through startup verification
disable-model-invocation: true
---

# Budget App Environment Setup Procedure

When invoked by the user with `/setup-env`, run every command relative to the project root (`budget-app-handson`) and proceed through the following checkpoints (CP) in order to build the development environment.

Follow "Checkpoint operation" in CLAUDE.md: after finishing each CP, report the result and stop, and do not start the next CP until the user types "Proceed to the next checkpoint". Perform only CP2-1 at first.

## CP2-1 [Design] Preliminary checks
1. Confirm that the current working directory is the project root (`budget-app-handson`).
2. Check that `docker --version` (dockerd from Rancher Desktop), `java -version` (Java 25 required), `node -v` (v26), and `pnpm -v` (v11) are available. If Java is not 25 or a prerequisite tool is not installed, stop and direct the user to "install the prerequisite tools in 2-0 / 2-1".
3. Check with `lsof -nP -iTCP:<port> -sTCP:LISTEN` that the ports to be used (PostgreSQL: 5432, Spring Boot: 8080, Next.js: 3000) are not in use by other processes.
   - If the process occupying 5432 is this project's own container `budget-app-db` (check with `docker ps`), treat it as a re-run: do not stop, report the result, and wait for the CP2-2 instruction.
   - If any other port is already in use, do not terminate or delete the existing process or container on your own. Report the occupying process information (PID, command name, port number) to the user and stop.
   - Tell the user to stop the process themselves and then run `/setup-env` again.

## CP2-2 [Implementation + Verification] Build and start PostgreSQL 18
1. Create `compose.yaml` directly under the root directory with the following content.

```yaml
services:
  db:
    image: postgres:18
    container_name: budget-app-db
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: budget_user
      POSTGRES_PASSWORD: budget_password
      POSTGRES_DB: budget_db
    volumes:
      - postgres_data:/var/lib/postgresql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U budget_user -d budget_db"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

2. Run `docker compose up -d`.
3. Run `docker compose ps` and wait until the status of `budget-app-db` becomes `healthy`, then confirm it.

## CP2-3 [Implementation + Verification] Set up the backend (Spring Boot 4 / Kotlin)
1. Run the following command in the root directory to generate the `backend` directory (this fetches the latest Spring Boot 4.x from Spring Initializr).

```bash
curl -fsSL https://start.spring.io/starter.tgz \
  -d type=gradle-project-kotlin \
  -d language=kotlin \
  -d javaVersion=25 \
  -d dependencies=web,data-jpa,validation,flyway,postgresql \
  -d groupId=com.example \
  -d artifactId=budgetapp \
  -d packageName=com.example.budgetapp \
  -d baseDir=backend \
  | tar -xzf -
```

2. Check the `dependencies` block of `backend/build.gradle.kts` and add the following so that the Flyway PostgreSQL module and the test libraries are included (do not duplicate `flyway-database-postgresql` if it is already listed).

```kotlin
	runtimeOnly("org.flywaydb:flyway-database-postgresql")
	testImplementation("io.mockk:mockk:1.14.11")
	testImplementation("com.lemonappdev:konsist:0.18.1")
```

3. Delete `backend/src/main/resources/application.properties` and create `backend/src/main/resources/application.yml`.

```yaml
spring:
  application:
    name: budgetapp
  datasource:
    url: jdbc:postgresql://localhost:5432/budget_db
    username: budget_user
    password: budget_password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
```

4. Create `backend/src/main/resources/db/migration/V1__create_tables.sql`.

```sql
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE'))
);

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    amount INT NOT NULL CHECK (amount >= 1),
    transaction_date DATE NOT NULL,
    memo TEXT,
    CONSTRAINT fk_categories FOREIGN KEY (category_id) REFERENCES categories(id)
);

INSERT INTO categories (name, type) VALUES ('食費', 'EXPENSE');
INSERT INTO categories (name, type) VALUES ('日用品', 'EXPENSE');
INSERT INTO categories (name, type) VALUES ('交通費', 'EXPENSE');
INSERT INTO categories (name, type) VALUES ('給与', 'INCOME');
INSERT INTO categories (name, type) VALUES ('副業', 'INCOME');
```

5. Run the subshell `(cd backend && ./gradlew test)` and confirm that the build and the initial tests succeed.
6. Start the backend temporarily with the subshell `(cd backend && ./gradlew bootRun)`, and stop the process once startup completes. Then directly verify the table creation and initial data insertion by Flyway with the following commands.

```bash
docker compose exec -T db psql -U budget_user -d budget_db -c "\dt"
docker compose exec -T db psql -U budget_user -d budget_db -c "SELECT * FROM categories;"
```

## CP2-4 [Implementation + Verification] Set up the frontend (Next.js 16 / pnpm 11)
1. Confirm that `node = "26"` and `pnpm = "11"` are defined in `mise.toml` in the root directory.
2. Generate the `frontend` project directly under the root directory with non-interactive options.

```bash
pnpm create next-app@latest frontend \
  --ts \
  --tailwind \
  --eslint \
  --app \
  --src-dir \
  --import-alias "@/*" \
  --use-pnpm \
  --yes
```

   `create-next-app` generates `frontend/.git`, so delete it with `rm -rf frontend/.git` to allow the monorepo to be managed as a whole from the root.

3. Install the test-related packages while keeping the current directory at the root.

```bash
pnpm --dir frontend add -D vitest @vitejs/plugin-react jsdom @testing-library/react @testing-library/dom @testing-library/jest-dom msw
```

4. Create the directories for the Clean Architecture structure.

```bash
mkdir -p frontend/src/components/ui \
  frontend/src/features/transaction/domain \
  frontend/src/features/transaction/usecase \
  frontend/src/features/transaction/adapter \
  frontend/src/features/transaction/presentation/utils \
  frontend/src/mocks
```

5. Run `pnpm --dir frontend build` and confirm that the Next.js build completes without errors.

## CP2-5 [Verification] Completion report
When all CPs are complete, report the following items to the user together with the verification results.
- Running status of the Docker container `budget-app-db`
- Result of generating the Spring Boot project and creating the `categories` / `transactions` tables and the initial master data
- Result of generating the Next.js project, its directory structure, and the build completion
````

### 4. Create the Project-Wide Guidelines (`CLAUDE.md`)

Create the common rules file that Claude Code loads automatically at the start of a session.
Create `CLAUDE.md` directly under the root directory and write the following content.

```markdown
# Budget App Development Guidelines

This is a monorepo for a simple household budget app (Spring Boot + Next.js).
In every implementation, strictly follow the Clean Architecture Dependency Rule (dependencies point only from outer layers to inner layers).

## Tech Stack
- Backend: Kotlin v2, Java 25 (LTS), Spring Boot v4, PostgreSQL v18, Flyway, JUnit 5, MockK, Konsist
- Frontend: TypeScript, Next.js v16 (App Router), React 19, Tailwind CSS v4, Vitest, Testing Library, MSW, pnpm v11
- Infra: Rancher Desktop / Docker Compose (PostgreSQL 18)

## Main Commands
- Start DB: `docker compose up -d`
- Backend tests: `cd backend && ./gradlew test`
- Start backend: `cd backend && ./gradlew bootRun`
- Frontend tests: `cd frontend && pnpm vitest run`
- Frontend dev server: `cd frontend && pnpm dev`

## Common Design Principles
1. Dependency Rule: Source code dependencies must always point from the outer circles to the inner circles. Inner layers must never import outer layers.
2. No layer skipping: The presentation layer (Controller / UI) must always go through the use case layer (UseCase / custom hook) and must not call repository implementations or external communication directly.
3. Keep tests in place: When adding a new class or use case, always write the corresponding unit or integration test and confirm that the tests pass.

## Checkpoint operation
Work proceeds in units of checkpoints (CP).
1. Perform only one CP per instruction. Do not run multiple CPs in succession.
2. When a CP is complete, declare "CPx-y complete", report the files created or changed, the commands run and their results, and the name of the next CP, and then always stop.
3. Do not start the next CP until the user types "Proceed to the next checkpoint".
4. If a verification command fails, fix the cause within that CP, re-run it, and report completion only after it succeeds. Do not carry a failure over to the next CP.
5. Do not create files that are not listed in the CP.
```

### 5. Create the Area-Specific Architecture Rules (`.claude/rules/`)

Create detailed rules that are applied automatically only when editing files in the target directories.

#### Create `.claude/rules/backend.md`

```markdown
---
paths:
  - "backend/**/*.kt"
---

# Backend (Spring Boot / Kotlin) Architecture Rules

Packages under `com.example.budgetapp` follow the four-layer structure below.

## Four-Layer Package Structure and Dependency Constraints

1. `domain` (Layer 1: Entities)
   - Place value objects (`Amount`), entities (`Transaction`, `Category`), domain services (`BalanceSummary`), and the custom exception (`DomainException`).
   - Prohibited: Importing `org.springframework.*`, `jakarta.persistence.*`, or the `usecase`, `adapter`, and `framework` packages is strictly forbidden.

2. `usecase` (Layer 2: Use Cases)
   - `usecase.input`: Place Input Port interfaces (`*UseCase`) and input data classes.
   - `usecase.output`: Place Output Port interfaces (`*Repository`).
   - `usecase.interactor`: Place `*Interactor` classes that implement `*UseCase`. The transaction boundary (`@Transactional`) is applied to the Interactor.
   - Prohibited: Importing the `adapter` and `framework` packages or `jakarta.persistence.*` is forbidden.

3. `adapter` (Layer 3: Interface Adapters)
   - `adapter.controller`: Place the REST Controller, request/response DTOs, and `GlobalExceptionHandler`.
   - `adapter.repository`: Place `*RepositoryImpl`, which implements `usecase.output.*Repository`, the JPA Entities (`adapter.repository.entity`), and the Spring Data JPA interfaces (`adapter.repository.jpa`), and perform the conversion between JPA Entities and domain models.
   - Prohibited: Importing the outer Layer 4 (`framework` package) is strictly forbidden. In addition, `adapter.controller` must receive only `usecase.input` and must not call `usecase.output` or `adapter.repository` (JPA) directly.

4. `framework` (Layer 4: Frameworks & Drivers)
   - `framework.config`: Place framework-wide configuration classes such as CORS.
```

#### Create `.claude/rules/frontend.md`

```markdown
---
paths:
  - "frontend/**/*.{ts,tsx}"
---

# Frontend (Next.js / TypeScript) Architecture Rules

Under the feature module `src/features/<feature>/`, follow the four-layer structure below.

## Four-Layer Directory Structure and Dependency Constraints

1. `domain/` (Layer 1: Entities)
   - Place pure TypeScript type definitions (`types.ts`) and framework-independent business rule functions (such as input validation).
   - Prohibited: Do not include React imports, `fetch` calls, DOM manipulation, or display formatting.

2. `usecase/` (Layer 2: Use Cases)
   - Place the interfaces that abstract external communication (`*Repository.ts`) and the custom hooks (`use*.ts`) that manage screen use cases and state.
   - Prohibited: Do not import concrete implementations under `adapter/` directly; communicate through the `*Repository` interfaces (dependency inversion).

3. `adapter/` (Layer 3: Interface Adapters - external communication)
   - Place the API clients (`*Api.ts`) that implement the `*Repository` interfaces, and encapsulate `fetch` and error handling.

4. `presentation/` (Layer 3: Interface Adapters - UI display)
   - Place UI components (`*.tsx`) and display utilities (such as `presentation/utils/formatCurrency.ts`).
   - Do not write `fetch` directly inside components.
   - Pure display components (`BalanceSummaryCard`, `TransactionForm`, `TransactionList`, etc.) must not import `adapter/` directly. Injection of the `adapter/` implementation (Composition Root) is done only through the default arguments of the feature root container (`TransactionSection.tsx`).
```

That completes the preparation by the person.

---

## 2-2. Build and Verify the Development Environment (done by the AI)

From here, start Claude Code in the root directory (`budget-app-handson`) and have the AI perform the environment setup.

### 1. Start Claude Code and Confirm the Rules Are Loaded

Start Claude Code in the terminal.

```bash
claude
```

After startup, enter the following prompt in the chat to confirm that the rules and skill created in `2-1` are recognized.

```text
Summarize the four-layer structure and the dependency prohibition rules for the backend and frontend defined in CLAUDE.md and .claude/rules/. Also check the available skills.
```

If the response shows that the four-layer rules and the `setup-env` skill are recognized, you are ready.

### 2. Run the Environment Setup with the `/setup-env` Skill

Next, enter the following skill command in the Claude Code chat to have the AI perform the environment setup.

```text
/setup-env
```

Claude Code follows the procedure in `.claude/skills/setup-env/SKILL.md`, performs the following checkpoints (CP) one at a time, and reports and stops after each one. If it asks for permission to run a command, review the content and approve it.

| CP | Kind | Content |
| :--- | :--- | :--- |
| CP2-1 | Design | Check the prerequisite tool versions and the availability of ports (5432, 8080, 3000) |
| CP2-2 | Implementation + Verification | Generate `compose.yaml`, start PostgreSQL 18 with Docker, and run the health check |
| CP2-3 | Implementation + Verification | Generate `backend` from Spring Initializr, add dependency libraries, place `application.yml` and the Flyway SQL, and verify the tables and initial master data with `psql` |
| CP2-4 | Implementation + Verification | Check `mise.toml`, generate `frontend`, install the test libraries, create the four-layer directories, and verify the build |
| CP2-5 | Verification | Completion report |

When Claude Code stops and shows its report, review the content and enter "Proceed to the next checkpoint".

### 3. Checkpoints After the AI Completes the Setup

When the CP2-5 completion report is output, confirm that the following state is in place.

- `compose.yaml`, `backend/`, and `frontend/` have been created directly under the root directory.
- The PostgreSQL 18 container (`budget-app-db`) is running, and the `categories` table (5 initial rows) and the `transactions` table have been created.
- The backend's `./gradlew test` and the frontend's `pnpm --dir frontend build` have completed without errors.

This completes the setup of the development environment and the AI development foundation. From Chapter 3 onward, you continue with implementation and testing while having Claude Code follow the architecture rules.
