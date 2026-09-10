package com.querylens.workspace.model;

/**
 * A named SQL statement stored in the QueryLens workspace database.
 */
public record SavedQuery(long id, String title, String sql, String updatedAt) {
    @Override
    public String toString() {
        return title;
    }
}
