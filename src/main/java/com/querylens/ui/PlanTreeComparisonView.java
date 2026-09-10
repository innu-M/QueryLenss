package com.querylens.ui;

import com.querylens.plan.PlanComparisonReport;
import com.querylens.plan.PlanComparisonService;
import com.querylens.plan.PlanInsight;
import com.querylens.plan.PlanOperationType;
import com.querylens.plan.QueryPlanNode;
import com.querylens.plan.QueryPlanTree;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PlanTreeComparisonView extends BorderPane {
    private final PlanComparisonService comparisonService;
    private final TextField databasePath = new TextField();
    private final TextArea originalSql = sqlInput("Original SELECT query");
    private final TextArea alternativeSql = sqlInput("Alternative SELECT query");
    private final TreeView<PlanNodeDisplay> originalTree = createTree();
    private final TreeView<PlanNodeDisplay> alternativeTree = createTree();
    private final Label verdict = new Label("Choose a database and compare two SELECT queries.");
    private final TextArea explanation = new TextArea();
    private Map<Integer, PlanInsight> originalInsights = Map.of();
    private Map<Integer, PlanInsight> alternativeInsights = Map.of();

    public PlanTreeComparisonView(PlanComparisonService comparisonService) {
        this.comparisonService = comparisonService;
        setPadding(new Insets(16));
        setTop(createInputs());
        setCenter(createTrees());
        setBottom(createExplanationPanel());
        originalTree.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> showInsight(selected, originalInsights));
        alternativeTree.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> showInsight(selected, alternativeInsights));
    }

    private VBox createInputs() {
        Label heading = new Label("SQLite Plan-Tree Comparison");
        heading.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        Label note = new Label("Plan structure suggests why a query may be better; measured benchmarks make the final decision.");

        databasePath.setPromptText("Path to SQLite database");
        HBox.setHgrow(databasePath, Priority.ALWAYS);
        Button browse = new Button("Browse…");
        browse.setOnAction(event -> chooseDatabase());
        HBox databaseRow = new HBox(8, new Label("Database"), databasePath, browse);

        GridPane sqlGrid = new GridPane();
        sqlGrid.setHgap(12);
        sqlGrid.setVgap(6);
        sqlGrid.add(new Label("Original SQL"), 0, 0);
        sqlGrid.add(new Label("Alternative SQL"), 1, 0);
        sqlGrid.add(originalSql, 0, 1);
        sqlGrid.add(alternativeSql, 1, 1);
        GridPane.setHgrow(originalSql, Priority.ALWAYS);
        GridPane.setHgrow(alternativeSql, Priority.ALWAYS);

        Button compare = new Button("Compare plan trees");
        compare.setDefaultButton(true);
        compare.setOnAction(event -> comparePlans());
        return new VBox(8, heading, note, databaseRow, sqlGrid, compare);
    }

    private SplitPane createTrees() {
        VBox originalPanel = treePanel("Original plan", originalTree);
        VBox alternativePanel = treePanel("Alternative plan", alternativeTree);
        SplitPane split = new SplitPane(originalPanel, alternativePanel);
        split.setDividerPositions(0.5);
        BorderPane.setMargin(split, new Insets(14, 0, 14, 0));
        return split;
    }

    private VBox treePanel(String title, TreeView<PlanNodeDisplay> tree) {
        Label label = new Label(title);
        label.setStyle("-fx-font-weight: bold;");
        VBox panel = new VBox(6, label, tree);
        VBox.setVgrow(tree, Priority.ALWAYS);
        return panel;
    }

    private VBox createExplanationPanel() {
        verdict.setStyle("-fx-font-weight: bold;");
        explanation.setEditable(false);
        explanation.setWrapText(true);
        explanation.setPrefRowCount(5);
        explanation.setPromptText("Select a plan node to see what it means.");
        return new VBox(6, verdict, explanation);
    }

    private void chooseDatabase() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose SQLite database");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("SQLite databases", "*.db", "*.sqlite", "*.sqlite3"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        File selected = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (selected != null) databasePath.setText(selected.getAbsolutePath());
    }

    private void comparePlans() {
        try {
            PlanComparisonReport report = comparisonService.compare(
                    Path.of(databasePath.getText().strip()), originalSql.getText(), alternativeSql.getText());
            originalInsights = index(report.originalInsights());
            alternativeInsights = index(report.alternativeInsights());
            populate(originalTree, report.originalPlan());
            populate(alternativeTree, report.alternativePlan());
            verdict.setText(verdictText(report.verdict()));
            explanation.setText(String.join("\n", report.differences()));
        } catch (RuntimeException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR, exception.getMessage(), ButtonType.OK);
            alert.setHeaderText("Could not compare query plans");
            alert.showAndWait();
        }
    }

    private void populate(TreeView<PlanNodeDisplay> tree, QueryPlanTree plan) {
        TreeItem<PlanNodeDisplay> root = new TreeItem<>(new PlanNodeDisplay(-1, PlanOperationType.OTHER, "Query plan"));
        for (QueryPlanNode node : plan.roots()) root.getChildren().add(toTreeItem(node));
        root.setExpanded(true);
        tree.setRoot(root);
        expand(root);
    }

    private TreeItem<PlanNodeDisplay> toTreeItem(QueryPlanNode node) {
        TreeItem<PlanNodeDisplay> item = new TreeItem<>(
                new PlanNodeDisplay(node.id(), node.operationType(), node.detail()));
        for (QueryPlanNode child : node.children()) item.getChildren().add(toTreeItem(child));
        return item;
    }

    private void expand(TreeItem<PlanNodeDisplay> item) {
        item.setExpanded(true);
        item.getChildren().forEach(this::expand);
    }

    private void showInsight(TreeItem<PlanNodeDisplay> selected, Map<Integer, PlanInsight> insights) {
        if (selected == null || selected.getValue().id() < 0) return;
        PlanInsight insight = insights.get(selected.getValue().id());
        if (insight != null) explanation.setText(insight.severity() + "\n" + insight.message());
    }

    private Map<Integer, PlanInsight> index(List<PlanInsight> insights) {
        Map<Integer, PlanInsight> byNode = new HashMap<>();
        insights.forEach(insight -> byNode.put(insight.nodeId(), insight));
        return Map.copyOf(byNode);
    }

    private String verdictText(PlanComparisonReport.Verdict result) {
        return switch (result) {
            case ALTERNATIVE_LOOKS_BETTER -> "Alternative plan looks structurally better.";
            case ORIGINAL_LOOKS_BETTER -> "Original plan looks structurally better.";
            case STRUCTURALLY_SIMILAR -> "Plans look structurally similar.";
        };
    }

    private static TextArea sqlInput(String prompt) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefRowCount(4);
        area.setWrapText(true);
        return area;
    }

    private static TreeView<PlanNodeDisplay> createTree() {
        TreeView<PlanNodeDisplay> tree = new TreeView<>();
        tree.setShowRoot(false);
        tree.setCellFactory(view -> new TreeCell<>() {
            @Override
            protected void updateItem(PlanNodeDisplay item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item.type() + "  •  " + item.detail());
                setStyle(switch (item.type()) {
                    case SCAN -> "-fx-text-fill: #b45309; -fx-font-weight: bold;";
                    case SEARCH -> "-fx-text-fill: #047857; -fx-font-weight: bold;";
                    case TEMPORARY_BTREE -> "-fx-text-fill: #b91c1c; -fx-font-weight: bold;";
                    case COMPOUND -> "-fx-text-fill: #6d28d9;";
                    case OTHER -> "";
                });
            }
        });
        return tree;
    }

    private record PlanNodeDisplay(int id, PlanOperationType type, String detail) {
    }
}
