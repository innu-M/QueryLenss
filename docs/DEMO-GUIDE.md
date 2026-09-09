# Final Demonstration Guide

## Workflow 0: Alternative-plan competition

1. Open **Alternative Plans** and select a SQLite database containing an indexed single table.
2. Enter a single-table `SELECT` with a filter and click **Compare Safe Alternatives**.
3. Show the default, `NOT INDEXED`, and `INDEXED BY` candidates.
4. Explain that one warm-up and five measured executions are used, and that lower median time ranks higher.
5. Select each row to show its SQL, actual SQLite plan, generation rationale, and evidence-based explanation.
6. Run the comparison again to demonstrate the local “Beats X%” history percentile.
7. Try an `UPDATE` to demonstrate the validation chain rejecting automatic write benchmarking.

This workflow naturally demonstrates Strategy, Factory, Chain of Responsibility, Template Method, Command, Observer, Repository, and Facade.

## Workflow 1: Query execution and dynamic optimization

1. Open Query Analyzer.
2. Run `SELECT id, name FROM test WHERE cat = 7;`.
3. Show query results, the `SCAN test` plan, and the index recommendation.
4. Run `CREATE INDEX IF NOT EXISTS idx_cat ON test(cat);`.
5. Run the same select again.
6. Show `USING INDEX idx_cat` and explain why the index recommendation disappears.

## Workflow 2: Persistent history and recommendation lifecycle

1. Open History & Report and show saved execution records and timing.
2. Search a query and mark it reviewed.
3. Open Recommendations and mark one Pending recommendation as Applied or Dismissed.
4. Explain that State objects validate the lifecycle transition.

## Pattern Questions and Short Answers

### Why Strategy?

Optimization issues are independent. Adding a new optimization check requires one new strategy class rather than modifying a large conditional block.

### Why Adapter?

SQLite query plans and metadata use `EXPLAIN QUERY PLAN` and `PRAGMA`. The adapter keeps these details behind `QueryPlanProvider`, making future database support possible.

### Why Observer?

Execution should not know how data is stored. It publishes a result, while `PersistenceObserver` saves history, analysis, and recommendations.

### Why State?

A recommendation should move from Pending to Applied or Dismissed. State objects prevent invalid transitions.
