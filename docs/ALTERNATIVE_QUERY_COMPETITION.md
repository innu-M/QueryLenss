# Alternative SQL Competition

QueryLens can generate safe alternatives for a single-table SQLite `SELECT`, verify that every candidate returns the same result, benchmark verified candidates, rank them, explain measured differences, and save the complete competition to Comparison History.

## Generated candidates

- Original SQL using SQLite's default optimizer choice
- `NOT INDEXED` table-scan alternative
- One `INDEXED BY` alternative for each existing index
- A conservative same-column equality rewrite such as `rating = 8 OR rating = 10` to `rating IN (8, 10)`

The generator rejects modifying statements, multiple statements, joins, unions, subqueries, aliases, and complex `FROM` clauses. Unsupported SQL remains executable elsewhere in QueryLens but is not automatically rewritten.

## Competition workflow

1. Generate candidates through registered strategies.
2. Execute each query in SQLite `query_only` mode and calculate a SHA-256 result fingerprint.
3. Reject candidates whose columns, types, values, duplicates, or required ordering differ from the original.
4. Run the configured warm-ups and measured executions for verified candidates.
5. Rank using the selected median, average, P95, or stability strategy.
6. Capture `EXPLAIN QUERY PLAN` evidence and explain the measured result.
7. Save the session, candidates, samples, ranks, plans, and explanations to Comparison History.

## Understanding “Beats X%”

This is a local competition percentile. It means that a candidate ranked faster than X% of the other verified candidates generated for this database and query during this run. It does not compare the user with other people or claim a global LeetCode-style percentile.

## Design patterns

- **Strategy:** each `AlternativeQueryStrategy` owns one generation rule.
- **Factory:** `AlternativeQueryStrategyFactory` creates the standard strategy set.
- **Adapter:** `SQLiteIndexCatalogProvider`, `SQLiteReadOnlyQueryExecutor`, and `SQLiteQueryPlanInspector` isolate SQLite APIs.
- **Facade/Service:** `AlternativeQueryCompetitionService` coordinates generation, verification, benchmarking, ranking, plans, explanations, and persistence.
- **Observer:** `BenchmarkProgressListener` publishes warm-up, measurement, cancellation, timeout, failure, and completion events to JavaFX.
- **Repository:** `ComparisonHistoryRepository` saves the completed competition without exposing schema details to the service.

## Running and verification

Start the JavaFX application with:

```shell
mvn javafx:run
```

Open **Alternative Competition**, choose a SQLite database, enter a supported query, adjust benchmark settings, and select **Generate and benchmark alternatives**.

Run automated tests with:

```shell
mvn test
```

The tests cover safe generation, candidate limits, unsupported SQL rejection, real index discovery through competition, fingerprints, equivalence rejection, benchmarks, ranking, plan capture, and history persistence.
