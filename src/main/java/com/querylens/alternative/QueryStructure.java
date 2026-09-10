package com.querylens.alternative;

public record QueryStructure(String sql, String tableName, int tableTokenEnd) {
    public String addTableDirective(String directive) {
        return sql.substring(0, tableTokenEnd) + " " + directive + sql.substring(tableTokenEnd);
    }
}
