package com.querylens.workspace.model;

import java.nio.file.Path;

public record SavedConnection(long id, String displayName, Path databasePath) { }
