package com.querylens.plan;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryPlanTreeBuilderTest {
    private final QueryPlanTreeBuilder builder = new QueryPlanTreeBuilder();

    @Test
    void buildsParentChildHierarchyAndClassifiesOperations() {
        QueryPlanTree tree = builder.build(List.of(
                new QueryPlanRow(1, 0, "COMPOUND QUERY"),
                new QueryPlanRow(2, 1, "SCAN sailors"),
                new QueryPlanRow(3, 1, "SEARCH reserves USING INDEX idx_sid (sid=?)"),
                new QueryPlanRow(4, 3, "USE TEMP B-TREE FOR ORDER BY")));

        assertEquals(1, tree.roots().size());
        QueryPlanNode root = tree.roots().getFirst();
        assertEquals(PlanOperationType.COMPOUND, root.operationType());
        assertEquals(2, root.children().size());
        assertEquals(PlanOperationType.SCAN, root.children().getFirst().operationType());
        assertEquals(PlanOperationType.SEARCH, root.children().get(1).operationType());
        assertEquals(PlanOperationType.TEMPORARY_BTREE,
                root.children().get(1).children().getFirst().operationType());
        assertEquals(1, tree.count(PlanOperationType.SCAN));
    }

    @Test
    void keepsOrphanRowsAsRoots() {
        QueryPlanTree tree = builder.build(List.of(new QueryPlanRow(7, 99, "SCAN boats")));

        assertEquals(1, tree.roots().size());
        assertEquals(7, tree.roots().getFirst().id());
    }

    @Test
    void rejectsDuplicateNodeIds() {
        assertThrows(IllegalArgumentException.class, () -> builder.build(List.of(
                new QueryPlanRow(1, 0, "SCAN sailors"),
                new QueryPlanRow(1, 0, "SCAN boats"))));
    }

    @Test
    void visitorExplainsEveryNode() {
        QueryPlanTree tree = builder.build(List.of(
                new QueryPlanRow(1, 0, "SCAN sailors"),
                new QueryPlanRow(2, 1, "USE TEMP B-TREE FOR ORDER BY")));

        List<PlanInsight> insights = tree.roots().getFirst().accept(new PlanExplanationVisitor());

        assertEquals(2, insights.size());
        assertTrue(insights.getFirst().message().contains("Full scan"));
        assertEquals(PlanInsight.Severity.EXPENSIVE, insights.get(1).severity());
    }
}
