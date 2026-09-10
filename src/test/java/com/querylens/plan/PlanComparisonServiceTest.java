package com.querylens.plan;

import com.querylens.plan.adapter.QueryPlanProvider;
import com.querylens.plan.model.QueryPlanRow;
import com.querylens.plan.service.PlanComparisonReport;
import com.querylens.plan.service.PlanComparisonService;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanComparisonServiceTest {
    @Test
    void identifiesAlternativeWithIndexedSearchAsStructurallyBetter() {
        QueryPlanProvider provider = (database, sql) -> sql.contains("INDEXED")
                ? List.of(new QueryPlanRow(1, 0, "SEARCH sailors USING INDEX idx_rating (rating=?)"))
                : List.of(
                        new QueryPlanRow(1, 0, "SCAN sailors"),
                        new QueryPlanRow(2, 1, "USE TEMP B-TREE FOR ORDER BY"));

        PlanComparisonReport report = new PlanComparisonService(provider).compare(
                Path.of("demo.db"), "SELECT original", "SELECT INDEXED");

        assertEquals(PlanComparisonReport.Verdict.ALTERNATIVE_LOOKS_BETTER, report.verdict());
        assertEquals(3, report.differences().size());
        assertTrue(report.differences().stream().allMatch(message -> message.startsWith("Alternative")));
        assertEquals(1, report.alternativeInsights().size());
    }
}
