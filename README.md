# QueryLens — Database Query Performance Analyzer

QueryLens is a JavaFX desktop application that helps a user execute SQLite queries, inspect their performance, understand their query plans, compare safe alternative plans, and receive practical optimization advice. It is a database-learning and developer-support tool, not a replacement for a database engine.

## What problem does it solve?

Writing a correct SQL query does not always mean it is efficient. A query may scan an entire table, use `SELECT *`, build a temporary sorting structure, or repeatedly filter on an unindexed column. QueryLens makes these issues visible in one place.

It executes the user's query against the selected SQLite database, measures the execution time, reads SQLite's real `EXPLAIN QUERY PLAN` output, stores the result in history, and generates recommendations from the actual query and plan.

## Main features

- Run SQLite `SELECT`, `INSERT`, `UPDATE`, and other supported SQL statements.
- Display up to 100 result rows for `SELECT` queries.
- Identify query type, tables, joins, `WHERE` clauses, filtered columns, and a complexity score.
- Measure execution time and mark slow queries at 500 ms or more.
- Use SQLite `EXPLAIN QUERY PLAN` to identify scans, automatic indexes, and temporary B-trees.
- Check the target database's existing indexes before suggesting a new index.
- Recommend improvements for `SELECT *`, expensive joins, full scans on filtered columns, automatic indexes, and temporary B-trees.
- Generate safe single-table `SELECT` alternatives using SQLite's default plan, `NOT INDEXED`, and available `INDEXED BY` choices.
- Benchmark each candidate with one warm-up and five measured executions, then rank verified-equivalent results by median time.
- Show a comparison chart, real plan differences, evidence-based explanations, and a local-history performance percentile.
- Store connections, query history, analysis results, recommendations, and index metadata in a separate SQLite history database.
- Show six screens: Query Analyzer, Alternative Plans, Connections, History & Report, Recommendations, and Index Catalog.

## Simple workflow

```text
Choose SQLite database
        ↓
Write and execute query
        ↓
Measure time + read EXPLAIN QUERY PLAN
        ↓
Analyze SQL structure
        ↓
Generate recommendations
        ↓
Save history and review results
```

The Alternative Plans workflow is:

```text
Validate one read-only SELECT
        ↓
Generate default/index/table-scan candidates
        ↓
Warm up + measure each candidate five times
        ↓
Reject candidates whose result fingerprint differs
        ↓
Rank by median time + explain plan differences
        ↓
Save samples and calculate a comparable local percentile
```

The percentile means “beats this percentage of previously saved, verified candidates for the same database path and normalized query family.” It is not a claim about other QueryLens users.

## Technology

- Java 21
- JavaFX 21
- Maven
- SQLite and JDBC
- JUnit 5

## Run the application

1. Open this folder as a Maven project in IntelliJ IDEA.
2. Set the Project SDK to Java 21.
3. Allow Maven to import the dependencies from `pom.xml`.
4. Run this Maven goal from IntelliJ's Maven panel or a terminal:

```bash
mvn javafx:run
```

To verify the business-logic tests:

```bash
mvn test
```

## Database setup

The application accepts the path of any SQLite database file. For a ready demo database, create a new SQLite file and run these scripts in order:

1. `src/main/resources/database/schema.sql`
2. `src/main/resources/database/sample-data.sql`

The sample database contains the familiar `Sailors`, `Boats`, and `Reserves` tables. The application's own history database is kept separate, so it does not modify the user's analysed database except when the user explicitly executes a modifying SQL statement.

## Implemented design patterns

The patterns below are in the application/business-logic layers; they are not merely UI decoration.

| Pattern | Implementation | Problem solved |
| --- | --- | --- |
| Strategy | `RecommendationStrategy` and its optimization-rule classes | Adds a recommendation rule without changing query execution. |
| Strategy | `QueryRewriteStrategy`, `IndexPlanStrategy`, and `RankingStrategy` | Adds candidate-generation or ranking algorithms without changing orchestration. |
| Factory | Recommendation and query-rewrite strategy factories | Centralizes selection of extensible algorithm families. |
| Chain of Responsibility | Candidate validators under `validation` | Applies independent SELECT-only, single-statement, and read-only safety rules. |
| Observer | Query and benchmark publishers with persistence observers | Persists completed work and supports additional progress consumers without changing execution. |
| Command | `ExecuteQueryCommand` and `CompareAlternativesCommand` | Encapsulates distinct user operations as request objects. |
| Repository | Repository classes under `repository` | Keeps SQLite persistence code out of services and the UI. |
| Template Method | `QueryAnalysisTemplate` and `QueryBenchmarkTemplate` | Reuses fixed analysis and benchmark workflows while allowing engine-specific steps. |
| Builder | `AnalysisResultBuilder` | Builds analysis results safely from many optional values. |
| State | `RecommendationState` classes | Validates recommendation status changes such as Pending to Applied. |
| Adapter | `SQLiteQueryPlanInspector` through `QueryPlanProvider` | Hides SQLite-specific plan and index APIs behind a stable interface. |
| Service / Facade | Query execution and alternative-comparison services | Coordinate validation, generation, execution, ranking, explanation, percentile calculation, and publishing. |

## Important documents

- [Architecture and design-pattern explanation](docs/ARCHITECTURE.md)
- [Editable UML class diagram in Lucidchart](https://lucid.app/lucidchart/ccb6834d-bdaa-4e59-8a80-f4f87d2f175d/edit)
- [UML diagram note](docs/UML-CLASS-DIAGRAM.md)
- [Database ER diagram](docs/ER-DIAGRAM.md)
- [Final demonstration guide](docs/DEMO-GUIDE.md)
- [Database schema](src/main/resources/database/schema.sql)
- [Sample data](src/main/resources/database/sample-data.sql)

## Test coverage

The project includes focused tests for SQL analysis, candidate generation and validation, benchmarking, ranking, normalization, plan explanations, index inspection, recommendations, and state transitions. Run `mvn test` before submission.

### Submitted to
## **Mridha MD. Nafis Fuad**
## Lecturer,IIT ,University of Dhaka

### Submitted by
### **Fahmida Munni** 
### (Roll: **BSSE1604**)
### and
### **Irin Sultana** 
### (Roll: **BSSE1642**)
