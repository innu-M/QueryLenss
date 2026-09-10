package com.querylens;

import com.querylens.alternative.AlternativeQueryCompetitionService;
import com.querylens.alternative.AlternativeQueryGenerator;
import com.querylens.alternative.SQLiteIndexCatalogProvider;
import com.querylens.alternative.SQLiteReadOnlyQueryExecutor;
import com.querylens.persistence.DatabaseInitializer;
import com.querylens.persistence.ComparisonHistoryRepository;
import com.querylens.plan.PlanComparisonService;
import com.querylens.plan.SQLiteQueryPlanInspector;
import com.querylens.ui.BenchmarkControlsView;
import com.querylens.ui.AlternativeCompetitionView;
import com.querylens.ui.ComparisonHistoryView;
import com.querylens.ui.PlanTreeComparisonView;
import com.querylens.ui.ConnectionsView;
import com.querylens.ui.QueryWorkspaceView;
import com.querylens.ui.RecommendationsView;
import com.querylens.workspace.facade.QueryWorkspaceService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Screen;

import java.nio.file.Path;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        Path databasePath = Path.of("data", "querylens.db");
        new DatabaseInitializer().initialize(databasePath);
        ComparisonHistoryRepository historyRepository = new ComparisonHistoryRepository(databasePath);
        QueryWorkspaceService workspaceService = new QueryWorkspaceService(databasePath);
        QueryWorkspaceView workspaceView = new QueryWorkspaceView(workspaceService);

        TabPane navigation = new TabPane();
        navigation.getTabs().add(new Tab("Query Workspace", workspaceView));
        navigation.getTabs().add(new Tab("Connections", new ConnectionsView(workspaceService, workspaceView::refreshConnections)));
        navigation.getTabs().add(new Tab("Recommendations", new RecommendationsView(workspaceService)));
        navigation.getTabs().add(new Tab("Benchmark", createBenchmarkWorkspace(databasePath)));
        AlternativeQueryCompetitionService competitionService = new AlternativeQueryCompetitionService(
                new AlternativeQueryGenerator(new SQLiteIndexCatalogProvider()),
                new SQLiteReadOnlyQueryExecutor(),
                new SQLiteQueryPlanInspector(),
                historyRepository);
        navigation.getTabs().add(new Tab("Alternative Competition",
                new AlternativeCompetitionView(competitionService)));
        navigation.getTabs().add(new Tab("Comparison History",
                new ComparisonHistoryView(historyRepository)));
        navigation.getTabs().add(new Tab("Plan Trees",
                new PlanTreeComparisonView(new PlanComparisonService(new SQLiteQueryPlanInspector()))));
        navigation.getTabs().forEach(tab -> tab.setClosable(false));

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double width = Math.min(1100, screen.getWidth() * 0.9);
        double height = Math.min(680, screen.getHeight() * 0.85);
        Scene scene = new Scene(navigation, width, height);
        scene.getStylesheets().add(getClass().getResource("/com/querylens/ui/theme.css").toExternalForm());
        stage.setTitle("QueryLens");
        stage.setMinWidth(760);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    private VBox createBenchmarkWorkspace(Path databasePath) {
        Label title = new Label("QueryLens");
        title.getStyleClass().add("page-title");
        Label message = new Label("Your local query analysis workspace is ready.");
        Label database = new Label("Workspace database: " + databasePath.toAbsolutePath());
        Label benchmarkHeading = new Label("Benchmark controls");
        benchmarkHeading.getStyleClass().add("section-title");
        VBox root = new VBox(12, title, message, database, benchmarkHeading, new BenchmarkControlsView());
        root.setPadding(new Insets(28));
        return root;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

