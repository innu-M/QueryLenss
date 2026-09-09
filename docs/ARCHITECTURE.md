# QueryLens Architecture and Design Decisions

## Purpose

QueryLens is a JavaFX developer tool that executes SQLite queries, measures their execution time, reads SQLite's actual `EXPLAIN QUERY PLAN` output, stores query history, creates optimization recommendations, and safely compares alternative plans for eligible `SELECT` queries.

## Architecture

```text
ui → command → service facade → analyzer / plan provider → repositories → SQLite
```

The alternative-comparison flow is:

```text
UI → CompareAlternativesCommand → AlternativeComparisonService
   → validation chain → rewrite strategies → benchmark template
   → ranking strategy → explanation service → benchmark publisher
   → persistence observer → comparison repository
```

The JavaFX package contains controls and event handlers only. Query analysis, recommendations, state transitions, execution, and persistence are implemented outside the UI.

## Applied Patterns

| Pattern | Problem | Implementation | Future benefit |
| --- | --- | --- | --- |
| Strategy | Each optimization issue has different detection logic. | `RecommendationStrategy` with select-star, join, full-scan, automatic-index, and temporary-B-tree strategies. | New optimization rules can be added without changing the engine. |
| Strategy | Candidate generation and ranking can use different algorithms. | `QueryRewriteStrategy`, `IndexPlanStrategy`, `RankingStrategy`, and `MedianTimeRankingStrategy`. | New rewrite and scoring algorithms can be added independently. |
| Chain of Responsibility | Unsafe candidates must fail independent checks with a precise reason. | Validators for SELECT-only, single-statement, and read-only rules. | New safety policies can be inserted without one large conditional method. |
| Repository | SQL in UI or services would mix responsibilities. | Connection, history, analysis, recommendation, and index-catalog repositories. | Persistence can be changed or tested independently. |
| Facade / Service Layer | Execution and comparison require many coordinated steps. | `QueryExecutionService` and `AlternativeComparisonService`. | UI gets simple operations without knowing every subsystem. |
| Factory | Creating all strategies in an engine causes tight coupling. | Recommendation and query-rewrite strategy factories. | Different strategy sets can be supplied later. |
| Builder | `AnalysisResult` has many optional fields. | `AnalysisResultBuilder`. | New analysis fields can be added without long constructors. |
| Adapter | SQLite uses database-specific `EXPLAIN` and `PRAGMA` APIs. | `QueryPlanProvider` and `SQLiteQueryPlanInspector`. | MySQL or PostgreSQL providers can be introduced later. |
| Template Method | Analysis and benchmarking have fixed workflows with variable steps. | `QueryAnalysisTemplate`, `QueryBenchmarkTemplate`, and `SQLiteSelectBenchmark`. | Other engines or benchmark policies can reuse the workflows. |
| Command | User operations should be encapsulated as request objects. | `ExecuteQueryCommand` and `CompareAlternativesCommand`. | An invoker can later add cancellation, queuing, or retry behavior. |
| Observer | Completed work should not know how every consumer handles it. | Query and benchmark publishers notify persistence observers. | UI progress, auditing, or telemetry observers can be added independently. |
| State | Recommendations have valid lifecycle transitions. | Pending, Applied, and Dismissed state classes. | Future states such as `REJECTED` can be added with transition rules. |

## Real SQLite Optimization Logic

QueryLens does not invent numeric database costs. SQLite does not expose a PostgreSQL-style cost value. Instead, QueryLens uses SQLite's actual optimizer decisions:

- `SCAN table` without `USING ...` → possible full table scan.
- `USING INDEX ...` → the plan is using an existing index.
- `AUTOMATIC INDEX` → SQLite built a temporary automatic index.
- `USE TEMP B-TREE` → SQLite created a temporary sorting or grouping structure.

The application also checks primary keys and indexes with `PRAGMA table_info`, `PRAGMA index_list`, and `PRAGMA index_info` before suggesting a new index.

## Error Handling

- Empty database paths and SQL are rejected before execution.
- SQL/JDBC failures are shown in the UI instead of crashing the application.
- Result display is limited to 100 rows.
- Recommendation status changes are validated by State objects.
- History deletion uses a transaction to remove related analysis and recommendation records safely.
