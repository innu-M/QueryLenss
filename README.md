<div align="center">

# QueryLens

**A JavaFX desktop tool for analyzing, benchmarking, and improving SQLite query performance**

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blue?logo=java)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven)
![SQLite](https://img.shields.io/badge/Database-SQLite-003B57?logo=sqlite)
![Tests](https://img.shields.io/badge/Tests-Passing-brightgreen)

</div>

---

## 📋 Submission

| | |
|---|---|
| **Submitted by** | **Fahmida Munni** (Roll: BSSE1604) · **Irin Sultana** (Roll: BSSE1642) |
| **Program** | Bachelor of Science in Software Engineering (BSSE), Institute of Information Technology (IIT), University of Dhaka |
| **Submitted to** | **Mridha Md. Nafis Fuad**, Lecturer, Institute of Information Technology (IIT), University of Dhaka |
| **Course** | Design Patterns Lab — Mini SPL2 |

---

## 📖 Overview

QueryLens lets you connect to a SQLite database, run and analyze queries, and get concrete recommendations for speeding them up. Its core differentiator: it automatically generates and benchmarks **alternative versions** of a query to see which one actually performs best — with results you can revisit later.

---

## 🖥️ Screens

| Screen | What it does |
|---|---|
| **Connections** | Manage saved SQLite database connections |
| **Query Workspace** | Run `SELECT`/`INSERT`/`UPDATE`/`DELETE` queries, view results, and get automatic complexity/risk analysis |
| **Recommendations** | Actionable suggestions (missing indexes, `SELECT *` usage, sort/group support) generated from query analysis |
| **Benchmark Controls** | Configure warm-up runs, measured runs, timeouts, and ranking strategy before benchmarking |
| **Alternative Competition** | Generates safe alternative `SELECT` queries, verifies equivalent results, and benchmarks them against the original |
| **Plan Trees** | Visualizes `EXPLAIN QUERY PLAN` output as a real parent/child tree and highlights scans vs. indexed searches |
| **Comparison History** | Browse, search, and re-run past query comparisons, with summary stats across all sessions |

---

## 🚀 Getting Started

**Requirements:** Java 21, Maven

```bash
mvn test          # run the test suite
mvn javafx:run    # launch the application
```

The application creates its own workspace database at `data/querylens.db` on first launch — no setup required.

---

## 🏗️ Architecture

The codebase is organized by feature area, with a consistent separation between persistence, business logic/services, and JavaFX views:

```
src/main/java/com/querylens/
├── workspace/       — connections, query execution, SQL analysis
├── recommendation/  — recommendation strategies and lifecycle
├── alternative/      — alternative query generation and competition
├── benchmark/        — configurable benchmarking and ranking
├── plan/              — EXPLAIN QUERY PLAN parsing, tree building, visitors
├── history/           — comparison history data model
├── sandbox/           — safe proposed-index testing
├── persistence/       — repositories (SQLite/JDBC access)
└── ui/                — JavaFX views, one per screen
```

Each feature area has a persistence layer (`*Repository`), a service/facade layer that the UI depends on, and pattern-based building blocks chosen for that area's specific problem — not applied uniformly for their own sake. See [`docs/`](docs/) for a full per-feature writeup.

---

## 🧩 Design Patterns

| Pattern | Where | Problem It Solves |
|---|---|---|
| **Strategy** | `alternative/*Strategy`, `recommendation/*Strategy`, `benchmark/RankingStrategy` | Swappable algorithms for generating candidates, recommending fixes, and ranking results |
| **Factory** | `AlternativeQueryStrategyFactory`, `RecommendationStrategyFactory`, `RecommendationStateFactory` | Centralizes which strategy/state runs for a given input, without callers knowing concrete classes |
| **State** | `recommendation/RecommendationState` (Pending → Applied → Dismissed) | Only valid status transitions are possible |
| **Chain of Responsibility** | `workspace/SqlValidationChain`, `SqlValidationRule` | Independent, composable safety checks before a query executes |
| **Template Method** | `workspace/QueryExecutionTemplate` | Fixed execute → analyze → record workflow with pluggable steps per query type |
| **Composite** | `plan/QueryPlanComponent`, `QueryPlanNode` | `EXPLAIN QUERY PLAN` output is naturally a parent/child tree |
| **Visitor** | `plan/QueryPlanVisitor`, `PlanExplanationVisitor` | Per-operator explanations without a giant switch statement |
| **Builder** | `plan/QueryPlanTreeBuilder` | Assembles a plan tree from raw, unordered plan rows |
| **Repository** | `persistence/*Repository` | Keeps all SQLite/JDBC code out of services and UI |
| **Facade / Service** | `QueryWorkspaceService`, `AlternativeQueryCompetitionService`, `PlanComparisonService` | UI talks to one coordinating object instead of orchestrating persistence + logic itself |
| **Adapter** | `SQLiteQueryExecutor`, `SQLiteQueryPlanInspector`, `SQLiteIndexCatalogProvider` | Hides SQLite-specific quirks behind a stable interface |
| **Observer** | `benchmark/BenchmarkProgressListener` | UI reacts to in-progress benchmark runs without the benchmark logic knowing about JavaFX |

Full reasoning for each pattern — problem, alternatives considered, and future extensibility — is documented per feature:

- 📄 [`docs/QUERY_WORKSPACE.md`](docs/QUERY_WORKSPACE.md)
- 📄 [`docs/RECOMMENDATIONS_ENGINE.md`](docs/RECOMMENDATIONS_ENGINE.md)
- 📄 [`docs/ALTERNATIVE_QUERY_COMPETITION.md`](docs/ALTERNATIVE_QUERY_COMPETITION.md)
- 📄 [`docs/PLAN_TREE_VISUALIZATION.md`](docs/PLAN_TREE_VISUALIZATION.md)
- 📄 [`docs/COMPARISON_HISTORY.md`](docs/COMPARISON_HISTORY.md)

---

## 🗄️ Database

SQLite, initialized automatically on first run (`persistence/DatabaseInitializer`). Schema: [`src/main/resources/database/schema.sql`](src/main/resources/database/schema.sql).

- **Core workflow:** `database_connections`, `query_history`, `query_analyses`, `recommendations`
- **Comparison workflow:** `comparison_sessions`, `comparison_candidates`, `benchmark_runs`

All linked by foreign keys with cascading deletes.

---

## ✅ Testing

```bash
mvn test
```

Coverage spans query execution and validation, recommendation strategies and state transitions, alternative query generation and equivalence verification, benchmark cancellation/timeout handling, plan tree construction, and comparison history persistence (including cascading deletes).

---

## 🔀 Development Process

Built with feature branches off `develop`, merged via reviewed pull requests, with CI running the full test suite on every PR (`.github/workflows/maven-pr.yml`). `master` reflects the current submission-ready state.
