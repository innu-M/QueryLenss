# Query Workspace

The Query Workspace is the main starting point for QueryLens. A user saves a SQLite database connection, chooses it in the workspace, runs one supported SQL statement, and sees the result, timing, simple analysis, and recent execution history.

## Supported statements

The workspace accepts `SELECT`, `INSERT`, `UPDATE`, and `DELETE`. It blocks multiple statements and schema-changing statements such as `DROP` or `ALTER`. Queries that change rows require an explicit confirmation in the interface.

`SELECT` results show at most 100 rows so the desktop application remains responsive. Insert, update, and delete statements show the affected-row count instead.

## Design choices

- **Repository:** `ConnectionRepository` and `QueryHistoryRepository` keep workspace SQL isolated from the UI and service layer.
- **Facade:** `QueryWorkspaceService` gives the UI one entry point for saving connections, running a query, loading history, and applying validation.
- **Template Method:** `AbstractQueryExecutor` owns the shared timing and JDBC workflow. `SQLiteQueryExecutor` supplies the SQLite-specific connection.
- **Chain of Responsibility:** `SqlValidationChain` runs focused validation rules before execution.

The view classes only handle controls and presentation. SQL execution, validation, analysis, and persistence remain outside the `ui` package.

The workspace code is grouped by responsibility: `model`, `analysis`, `execution`, `validation/chain`, and `facade`.
