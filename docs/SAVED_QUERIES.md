# Saved Queries

Saved Queries is a small reusable SQL library for QueryLens users. It is deliberately separate from query history: history records what was executed, while saved queries contain statements the user wants to keep and reuse.

## User flow

1. Write SQL in **Query Workspace** and choose **Save query**, or add it directly in **Saved Queries**.
2. Give the query a meaningful title.
3. Select it later and choose **Load in workspace** to place its SQL back into the editor.
4. Update or delete it from the Saved Queries tab when it is no longer useful.

## Design

`SavedQueriesView` owns the JavaFX controls only. `QueryWorkspaceService` validates titles and SQL, then delegates persistence to `SavedQueryRepository`. The repository is the only class that contains SQL statements for the `saved_queries` table.

This follows the same repository-and-facade separation used by the rest of QueryLens. It prevents UI code from opening SQLite connections and keeps reusable-query storage independent from execution history.
