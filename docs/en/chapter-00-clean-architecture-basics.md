# Chapter 0: Clean Architecture Basics

## Overview and Goals of This Hands-On

This hands-on is a practical guide for learning the design principles and implementation methods of Clean Architecture while building Budget App, a simple household budget app.

You will learn how to separate business logic from external technical details such as frameworks and databases, and how to build an application that is resilient to change and easy to test. In each chapter you will also use Claude Code, having the AI follow the design rules while you work through implementation and testing step by step.

### Tech Stack

| Area | Technology / Tool | Version / Notes |
| :--- | :--- | :--- |
| Backend | Kotlin / Java | Kotlin v2 / Java 25 LTS |
| | Spring Boot | v4 / Dependencies: Spring Web, Spring Data JPA, Validation, Flyway, PostgreSQL |
| | Database / Migration | PostgreSQL v18 / Flyway |
| | Testing / Build | JUnit 5, MockK, MockMvc, Konsist / Gradle with Kotlin DSL |
| Frontend | Node.js / Package manager | Node.js v26 LTS / pnpm v11 |
| | Framework / Language | Next.js v16 with App Router / React 19 / TypeScript |
| | Styling | Tailwind CSS v4 |
| | Testing | Vitest, React Testing Library, MSW |
| Infrastructure / Environment management | Container / Version management | Rancher Desktop, Docker compatible / mise |
| AI development support | Claude Code | CLAUDE.md, .claude/rules/, Agent Skills, Hooks |

---

## 1. Why Adopt Clean Architecture

In web application development, mixing business logic with framework and database code causes the following problems.

1. Every time you upgrade or replace a database or library, the business logic has to be modified as well.
2. Every unit test run requires starting a web server or database, which makes the tests slow.
3. Responsibilities become vague, and a single class or component grows too large.

Clean Architecture solves these problems by clearly separating software into "things that rarely change", which are the business rules, and "things that change often", which are the DB, the UI, and frameworks.

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
| Layer 1 | Entities / Domain | Pure business rules and data structures shared across the whole application. In a household budget app, rules such as "an amount is a positive integer of at least 1 yen" and "the balance is total income minus total expenses" belong here. |
| Layer 2 | Use Cases | Define the steps of the concrete operations a user wants to accomplish with the system, for example listing, creating, and deleting. They combine Layer 1 objects to control the flow of processing. |
| Layer 3 | Interface Adapters | Convert in both directions between the data formats handled by the inner layers, Entities and Use Cases, and the data formats handled by outer technologies such as the Web, the DB, and the browser. The backend Controller, DTO, Repository implementation, and JPA Entity / JPA Repository, and the frontend API client and UI components belong here. |
| Layer 4 | Frameworks & Drivers | The outermost foundations and external tools: the Spring Boot startup infrastructure and global configuration such as `CorsConfig`, the PostgreSQL database, and the Next.js App Router routing in `app/`. In the code we write, the `framework` package and `app/` correspond to this layer. |

---

## 3. The Dependency Rule

The most important constraint in Clean Architecture is the Dependency Rule.

> Source code dependencies, that is, import statements and references, must always point from an outer circle to an inner circle.

- Layer 1, Entities, never references classes or packages of Layers 2 to 4, or any framework. Writing Spring / JPA annotations such as `@Entity` or `@RestController` on a domain model is prohibited.
- Layer 2, Use Cases, depends only on Layer 1, Entities, and never on Layer 3, Interface Adapters, or Layer 4, which covers the database and routing. Note that this hands-on prioritizes ease of implementation and accepts a design trade-off: Layer 2 uses basic framework features. These are `@Service` / `@Transactional` on the backend and React Hooks on the frontend.
- Layer 3, Interface Adapters, depends only on Layers 1 and 2, and never on the outer Layer 4, which is the `framework` and `app` packages.
- Layer 4, Frameworks & Drivers, is the outermost layer responsible for application startup, cross-cutting configuration, and routing. It references the inner Layers 1 to 3 to assemble the whole application.

Note that the Dependency Rule applies to "dependencies between the packages we write ourselves". These packages are `domain` / `usecase` / `adapter` / `framework` on the backend, and `domain` / `usecase` / `adapter` / `presentation` / `app` on the frontend. It is acceptable for Layer 3 to use external libraries such as Spring MVC, Spring Data JPA, and `jakarta.persistence`. `@Service` / `@Transactional` / React Hooks, which are allowed in a limited way in Layer 2, are treated the same way. Only Layer 1 does not depend on the frameworks of external libraries either.

With this rule, the dependency direction between our own packages stays one-way, "from the outside in", and the inner layers are protected from changes in the outer layers.

---

## 4. Dependency Inversion Principle / DIP and the Flow of Processing

In a real application, the processing of Layer 2, Use Cases, needs to save data to the database. However, if a Use Case directly calls an implementation class that operates on the database, that is a dependency from the inside to the outside and violates the rule.

To solve this problem, apply the Dependency Inversion Principle, DIP for short.

1. On the inside, in the Use Case layer, define an interface that acts as the contract for data persistence. This is `TransactionRepository`, an Output Port.
2. On the outside, in the Interface Adapters layer, place `TransactionRepositoryImpl`, the class that implements that interface, `JpaTransactionRepository`, the Spring Data JPA interface, and `TransactionJpaEntity`, the JPA entity.
3. At runtime, the Layer 4 frameworks such as Spring Boot, Spring Data JPA, and PostgreSQL create the actual instances and pass them to the Use Case through DI, that is, dependency injection.

As a result, the runtime flow of processing and the source code dependency direction run in opposite directions, as follows.

```text
[Runtime flow of processing: direction of data movement]
Controller ──> Interactor ──> RepositoryImpl ──> JpaRepository ──> PostgreSQL

[Source code dependency direction: direction of imports]
Controller ──> InputPort <── Interactor ──> OutputPort <── RepositoryImpl ──> JpaRepository ──> Spring Data JPA (external library)
(Layer 3)      (Layer 2)      (Layer 2)      (Layer 2)       (Layer 3)           (Layer 3)         (library; outside the scope of the Dependency Rule)
```

Layer 4, which consists of the `framework` package and the Spring Boot startup infrastructure, is responsible for assembling these implementation classes through DI and passing them to the Use Case. It is never imported from `adapter`.

This structure has the following advantages.

- In a Use Case unit test, you do not need to start a database. Simply replace the `TransactionRepository` interface with a MockK mock, and the test runs quickly.
- Even when you change the data store from PostgreSQL to an external API or in-memory storage, you only need to replace `RepositoryImpl`. The Use Case and Domain layers do not need to be modified.

---

## 5. Applying It to the Frontend and the Approach to Using Claude Code

### Clean Architecture on the Frontend

In the frontend, Next.js / React, as well, writing `fetch` calls or business rules directly inside UI components makes screen specification changes and testing difficult. In this hands-on, `features/transaction`, the feature unit, and `app`, the routing, are divided into the following four layers.

- Layer 1, domain: Type definitions and pure business rules such as input validation
- Layer 2, usecase: Custom hooks that bundle screen state management and operation steps, and the Repository, which is the communication interface
- Layer 3, adapter / presentation: The adapter, which implements API communication with `fetch`, and the presentation, which holds the display formatting and UI rendering components
- Layer 4, app: Next.js App Router routing pages

### How to Proceed with the Hands-On Using Claude Code

When developing with an AI coding agent, Claude Code, vague instructions make architecture violations more likely, such as "adding JPA annotations to the domain model" or "calling a Repository directly from a Controller".

In this hands-on, you will use Claude Code features step by step in the following order.

1. Chapter 2: Defining skills and design rules, and environment setup by AI
   After installing the prerequisite tools, you write the environment setup skill `.claude/skills/setup-env/SKILL.md` and the design rules `CLAUDE.md` / `.claude/rules/` yourself. Then you have Claude Code run `/setup-env` to build the development environment.
2. Chapters 3 to 6: Step-by-step implementation per layer and automated verification through tests
   Implement from the innermost layer outward, and automatically detect rule violations and defects with test code using Konsist, MockK, MockMvc, Vitest, and MSW.
3. Chapter 7: Feature-addition exercise with Agent Skills and Hooks
   Define the steps for adding a use case in `.claude/skills/add-feature/SKILL.md`. Then try adding a feature, combining it with an example configuration of a `PostToolUse` hook, written in `.claude/settings.json`, that runs automated verification when files are edited.

### Proceeding with Checkpoints

From Chapter 2 onward, instructions to Claude Code divide the work into "checkpoints", or CPs for short. Each CP has a kind in the sequence "Design -> Implementation -> Test -> Verification", and Claude Code reports its result and stops each time it finishes one CP. You review the result and type "Proceed to the next checkpoint" to move on to the next CP. This operating rule is written in the "Checkpoint operation" section of `CLAUDE.md`, which you create in Chapter 2.

---

## 6. Chapter List

- Chapter 0: Clean Architecture Basics. This file covers it
- Chapter 1: Database Design and Overall Project Structure
- Chapter 2: Claude Code Rule Setup and Environment Setup
- Chapter 3: Backend Implementation with Spring Boot and Kotlin
- Chapter 4: Backend Test Implementation: Unit Tests, Integration Tests, Architecture Tests
- Chapter 5: Frontend Implementation: Next.js 16 / TypeScript
- Chapter 6: Frontend Test Implementation: Unit Tests, Integration Tests
- Chapter 7: Feature-Addition Exercise Using Claude Code Skills and Hooks
