package com.querylens.service;

import com.querylens.model.AnalysisResult;

import java.sql.Connection;

public interface QueryPlanProvider {
    AnalysisResult inspect(Connection connection, String sql, AnalysisResult analysis);
}
