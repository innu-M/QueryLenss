package com.querylens.persistence.core;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class AbstractWorkspaceRepository {
    private final Path workspaceDatabase;

    protected AbstractWorkspaceRepository(Path workspaceDatabase) {
        this.workspaceDatabase = workspaceDatabase;
    }

    protected Connection openConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + workspaceDatabase.toAbsolutePath());
    }
}


