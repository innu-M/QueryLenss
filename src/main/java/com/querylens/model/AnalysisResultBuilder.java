package com.querylens.model;

import java.util.List;

public class AnalysisResultBuilder {
    private String queryType = "Unknown";
    private List<String> tables = List.of();
    private int joinCount;
    private boolean hasWhereClause;
    private List<String> whereColumns = List.of();
    private int complexityScore;
    private String riskLevel = "Low";
    private boolean usesSelectStar;
    private List<String> planSteps = List.of();
    private List<String> indexedWhereColumns = List.of();

    public static AnalysisResultBuilder from(AnalysisResult result) {
        AnalysisResultBuilder builder = new AnalysisResultBuilder();
        builder.queryType = result.queryType();
        builder.tables = result.tables();
        builder.joinCount = result.joinCount();
        builder.hasWhereClause = result.hasWhereClause();
        builder.whereColumns = result.whereColumns();
        builder.complexityScore = result.complexityScore();
        builder.riskLevel = result.riskLevel();
        builder.usesSelectStar = result.usesSelectStar();
        builder.planSteps = result.planSteps();
        builder.indexedWhereColumns = result.indexedWhereColumns();
        return builder;
    }

    public AnalysisResultBuilder queryType(String value) { queryType = value; return this; }
    public AnalysisResultBuilder tables(List<String> value) { tables = value; return this; }
    public AnalysisResultBuilder joinCount(int value) { joinCount = value; return this; }
    public AnalysisResultBuilder hasWhereClause(boolean value) { hasWhereClause = value; return this; }
    public AnalysisResultBuilder whereColumns(List<String> value) { whereColumns = value; return this; }
    public AnalysisResultBuilder complexityScore(int value) { complexityScore = value; return this; }
    public AnalysisResultBuilder riskLevel(String value) { riskLevel = value; return this; }
    public AnalysisResultBuilder usesSelectStar(boolean value) { usesSelectStar = value; return this; }
    public AnalysisResultBuilder planSteps(List<String> value) { planSteps = value; return this; }
    public AnalysisResultBuilder indexedWhereColumns(List<String> value) { indexedWhereColumns = value; return this; }

    public AnalysisResult build() {
        return new AnalysisResult(queryType, tables, joinCount, hasWhereClause, whereColumns,
                complexityScore, riskLevel, usesSelectStar, planSteps, indexedWhereColumns);
    }
}
