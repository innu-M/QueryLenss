package com.querylens.alternative;

import java.nio.file.Path;
import java.util.List;

@FunctionalInterface
public interface IndexCatalogProvider {
    List<String> findIndexes(Path databasePath, String tableName);
}
