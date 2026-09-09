package com.querylens.command;

import com.querylens.model.QueryExecutionResult;
import com.querylens.service.QueryExecutionService;

public class ExecuteQueryCommand implements Command<QueryExecutionResult> {
    private final QueryExecutionService service;
    private final String databasePath;
    private final String sql;

    public ExecuteQueryCommand(QueryExecutionService service, String databasePath, String sql) {
        this.service = service;
        this.databasePath = databasePath;
        this.sql = sql;
    }

    @Override
    public QueryExecutionResult execute() throws Exception {
        return service.execute(databasePath, sql);
    }
}
