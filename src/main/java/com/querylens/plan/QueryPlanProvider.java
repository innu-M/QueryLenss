package com.querylens.plan;

import java.nio.file.Path;
import java.util.List;

@FunctionalInterface
public interface QueryPlanProvider {
    List<QueryPlanRow> inspect(Path databasePath, String selectSql);
}
