package com.querylens.workspace;

import java.nio.file.Path;

public record SavedConnection(long id, String displayName, Path databasePath) { }
