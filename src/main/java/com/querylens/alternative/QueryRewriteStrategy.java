package com.querylens.alternative;

import com.querylens.model.AnalysisResult;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface QueryRewriteStrategy {
    boolean supports(String sql, AnalysisResult analysis);

    List<QueryCandidate> generate(Connection connection, String sql, AnalysisResult analysis) throws SQLException;
}
