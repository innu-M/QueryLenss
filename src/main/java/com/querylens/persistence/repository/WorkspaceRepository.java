package com.querylens.persistence.repository;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

abstract class WorkspaceRepository {
    private final Path workspaceDatabase;

    WorkspaceRepository(Path workspaceDatabase) {
        this.workspaceDatabase = workspaceDatabase;
    }

    protected Connection openConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + workspaceDatabase.toAbsolutePath());
    }
}



