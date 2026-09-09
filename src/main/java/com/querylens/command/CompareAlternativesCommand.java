package com.querylens.command;

import com.querylens.comparison.ComparisonReport;
import com.querylens.service.AlternativeComparisonService;

public class CompareAlternativesCommand implements Command<ComparisonReport> {
    private final AlternativeComparisonService service;
    private final String databasePath;
    private final String sql;

    public CompareAlternativesCommand(AlternativeComparisonService service, String databasePath, String sql) {
        this.service = service;
        this.databasePath = databasePath;
        this.sql = sql;
    }

    @Override
    public ComparisonReport execute() throws Exception {
        return service.compare(databasePath, sql);
    }
}
