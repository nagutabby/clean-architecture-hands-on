# Chapter 0: Clean Architecture Basics

## Overview and Goals of This Hands-On

This hands-on is a practical guide to learning the design principles and implementation techniques of Clean Architecture by building a simple household budget app (Budget App).

You will learn how to separate business logic from external technical details such as frameworks and databases, and build an application that is resilient to change and easy to test. In each chapter you will also use Claude Code, making the AI follow the design conventions while you implement and test step by step.

### Tech Stack

| Area | Technology / Tool | Version / Notes |
| :--- | :--- | :--- |
| Backend | Kotlin / Java | Kotlin v2 / Java 25 (LTS) |
| | Spring Boot | v4 (Spring Web, Spring Data JPA, Validation, Flyway, PostgreSQL) |
| | Database / Migration | PostgreSQL v18 / Flyway |
| | Testing / Build | JUnit 5, MockK, MockMvc, Konsist / Gradle (Kotlin DSL) |
| Frontend | Node.js / Package manager | Node.js v26 (LTS) / pnpm v11 |
| | Framework / Language | Next.js v16 (App Router) / React 19 / TypeScript |
| | Styling | Tailwind CSS v4 |
| | Testing | Vitest, React Testing Library, MSW |
| Infrastructure / Environment management | Container / Version management | Rancher Desktop (Docker compatible) / mise |
| AI development support | Claude Code | CLAUDE.md, .claude/rules/, Agent Skills, Hooks |

---

## 1. Why Adopt Clean Architecture

In web application development, mixing business logic with framework and database code causes the following problems.

1. When upgrading or replacing a database or library, the business logic has to be modified as well.
2. Every unit test run requires starting a web server or database, which slows down test execution.
3. Responsibilities become unclear, and a single class or component grows too large.

Clean Architecture solves these problems by clearly separating software into "things that rarely change (business rules)" and "things that change often (DB, UI, frameworks)".

---

## 2. The "Four Concentric Circles" of Clean Architecture

Clean Architecture expresses the separation of concerns as four concentric circles. The closer to the center, the more essential the business rules; the further out, the more concrete the technical details.

```text
+-------------------------------------------------------------------+
|  [Layer 4] Frameworks & Drivers                                   |
|   Spring Boot config / PostgreSQL / Next.js App Router            |
|  +-------------------------------------------------------------+  |
|  |  [Layer 3] Interface Adapters                               |  |
|  |   Controller / DTO / Repository impl, JPA / API client / UI |  |
|  |  +-------------------------------------------------------+  |  |
|  |  |  [Layer 2] Use Cases                                  |  |  |
|  |  |   App-specific procedures (list, create)              |  |  |
|  |  |  +-------------------------------------------------+  |  |  |
|  |  |  |  [Layer 1] Entities (Domain)                    |  |  |  |
|  |  |  |   Pure rules (amount >= 1 yen, balance)         |  |  |  |
|  |  |  +-------------------------------------------------+  |  |  |
|  |  +-------------------------------------------------------+  |  |
|  +-------------------------------------------------------------+  |
+-------------------------------------------------------------------+
          =====>  Dependencies always point from the outside in, and only that way  =====>
```

### Role of Each Layer

| Layer | Name | Role and examples |
| :--- | :--- | :--- |
| Layer 1 | Entities (Domain) | Pure business rules and data structures shared across the whole application. In a household budget app, rules such as "an amount is a positive integer of at least 1 yen" and "the balance is calculated by subtracting total expenses from total income" belong here. |
| Layer 2 | Use Cases | Define the concrete operations a user wants to accomplish with the system (list, create, delete, etc.). They combine Layer 1 objects to control the flow of processing. |
| Layer 3 | Interface Adapters | Convert between the data formats handled by the inner layers (Entities / Use Cases) and the data formats handled by outer technologies (Web / DB / browser). The backend Controller, DTO, Repository implementation, and JPA Entity / JPA Repository, and the frontend API client and UI components belong here. |
| Layer 4 | Frameworks & Drivers | The outermost foundations and external tools: the Spring Boot startup infrastructure and global configuration (`CorsConfig`, etc.), the PostgreSQL database, and Next.js App Router routing (`app/`). In the code we write, the `framework` package and `app/` correspond to this layer. |

---

## 3. The Dependency Rule

The most important constraint in Clean Architecture is the Dependency Rule.

> Source code dependencies (import statements and references) must always point from an outer circle to an inner circle.

- Layer 1 (Entities) never references classes or packages of Layers 2 to 4, or any framework. Writing Spring / JPA annotations such as `@Entity` or `@RestController` on a domain model is prohibited.
- Layer 2 (Use Cases) depends only on Layer 1 (Entities), and never on Layer 3 (Interface Adapters) or Layer 4 (database and routing). Note that this hands-on prioritizes practicality in implementation and accepts a design trade-off: Layer 2 uses basic framework features (`@Service` / `@Transactional` on the backend, React Hooks on the frontend).
- Layer 3 (Interface Adapters) depends only on Layers 1 and 2, and never on the outer Layer 4 (the `framework` and `app` packages).
- Layer 4 (Frameworks & Drivers) is the outermost layer responsible for application startup, cross-cutting configuration, and routing. It references the inner layers (Layers 1 to 3) to assemble the whole application.

Note that the Dependency Rule applies to "dependencies between the packages we write ourselves" (`domain` / `usecase` / `adapter` / `framework`, and on the frontend `domain` / `usecase` / `adapter` / `presentation` / `app`). It is acceptable for Layer 3 to use external libraries such as Spring MVC, Spring Data JPA, and `jakarta.persistence` (the same applies to `@Service` / `@Transactional` / React Hooks, whose use is allowed in a limited way in Layer 2). Only Layer 1 has no framework dependency on external libraries either.

With this rule, the dependency direction between our own packages stays one-way, "from the outside in", and the inner layers are protected from changes in the outer layers.

---

## 4. Dependency Inversion Principle (DIP) and the Flow of Processing

In a real application, the processing of Layer 2 (Use Cases) needs to save data to the database. However, if a Use Case directly calls an implementation class that operates on the database, that is a dependency from the inside to the outside and violates the rule.

To solve this problem, apply the Dependency Inversion Principle (DIP).

1. On the inside (Use Case layer), define an interface (`TransactionRepository` = Output Port) that acts as the contract for data persistence.
2. On the outside (Interface Adapters layer), place the class that implements that interface (`TransactionRepositoryImpl`), the Spring Data JPA interface (`JpaTransactionRepository`), and the JPA entity (`TransactionJpaEntity`).
3. At runtime, the Layer 4 framework (Spring Boot / Spring Data JPA / PostgreSQL) creates the actual instances and passes them to the Use Case through DI (dependency injection).

As a result, the runtime flow of processing and the source code dependency direction are contrasted as follows.

```text
[Runtime flow of processing (direction of data movement)]
Controller ──> Interactor ──> RepositoryImpl ──> JpaRepository ──> PostgreSQL

[Source code dependency direction (direction of imports)]
Controller ──> InputPort <── Interactor ──> OutputPort <── RepositoryImpl ──> JpaRepository ──> Spring Data JPA (external library)
(Layer 3)      (Layer 2)      (Layer 2)      (Layer 2)       (Layer 3)           (Layer 3)         (library; outside the scope of the Dependency Rule)
```

Layer 4 (the `framework` package and the Spring Boot startup infrastructure) is responsible for assembling these implementation classes through DI and passing them to the Use Case, and it is never imported from `adapter`.

This structure has the following advantages.

- In a Use Case unit test, you can test quickly by simply replacing the `TransactionRepository` interface with a mock (MockK) instead of starting a database.
- Even when changing the data store from PostgreSQL to an external API or in-memory storage, you only need to replace `RepositoryImpl`, and the Use Case and Domain layers do not need to be modified.

---

## 5. Applying It to the Frontend and the Approach to Using Claude Code

### Clean Architecture on the Frontend

In the frontend (Next.js / React) as well, writing `fetch` calls or business rules directly inside UI components makes screen specification changes and testing difficult. In this hands-on, the feature unit (`features/transaction`) and routing (`app`) are divided into the following four layers.

- Layer 1 (domain): Type definitions and pure business rules such as input validation
- Layer 2 (usecase): Custom hooks that bundle screen state management and operation procedures, and the communication interface (Repository)
- Layer 3 (adapter / presentation): The API communication implementation using `fetch` (adapter), and display formatting and UI rendering components (presentation)
- Layer 4 (app): Next.js App Router routing pages

### How to Proceed with the Hands-On Using Claude Code

When developing with an AI coding agent (Claude Code), vague instructions tend to cause architecture violations such as "adding JPA annotations to the domain model" or "calling a Repository directly from a Controller".

In this hands-on, you will use Claude Code features step by step as follows.

1. Defining skills and design rules, and AI-driven environment setup (Chapter 2)
   After installing the prerequisite tools, you create the environment setup skill (`.claude/skills/setup-env/SKILL.md`) and the design rules (`CLAUDE.md` / `.claude/rules/`) yourself, then have Claude Code run `/setup-env` to build the development environment.
2. Step-by-step implementation per layer and automated verification through tests (Chapters 3 to 6)
   Implement from the innermost layer outward, and automatically detect rule violations and defects with test code (Konsist, MockK, MockMvc, Vitest, MSW).
3. Feature-addition exercise with Agent Skills and Hooks (Chapter 7)
   Define the procedure for adding a use case in `.claude/skills/add-usecase/SKILL.md`, and try adding a feature in combination with an example configuration of a `PostToolUse` hook (`.claude/settings.json`) that runs automated verification when files are edited.

### Proceeding with Checkpoints

From Chapter 2 onward, instructions to Claude Code divide the work into "checkpoints (CP)". Each CP has a kind in the sequence "Design -> Implementation -> Test -> Verification", and Claude Code reports its result and stops each time it finishes one CP. You review the result and type "Proceed to the next checkpoint" to move on to the next CP. This operating rule is written in the "Checkpoint operation" section of `CLAUDE.md` (Chapter 2).

---

## 6. Chapter List

- Chapter 0: Clean Architecture Basics (this file)
- Chapter 1: Database Design and Overall Project Structure
- Chapter 2: Claude Code Rule Setup and Environment Setup
- Chapter 3: Backend Implementation (Spring Boot / Kotlin)
- Chapter 4: Backend Test Implementation (Unit Tests, Integration Tests, Architecture Tests)
- Chapter 5: Frontend Implementation (Next.js 16 / TypeScript)
- Chapter 6: Frontend Test Implementation (Unit Tests, Integration Tests)
- Chapter 7: Feature-Addition Exercise Using Claude Code Skills and Hooks
