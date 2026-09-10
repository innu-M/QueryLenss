package com.querylens.workspace.execution.template;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

public final class SQLiteQueryExecutor extends QueryExecutionTemplate {
    @Override
    protected Connection openConnection(Path databasePath) throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
    }
}



