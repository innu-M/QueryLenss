package com.querylens;

import com.querylens.service.QueryExecutionService;
import com.querylens.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private final QueryExecutionService queryService = new QueryExecutionService();

    @Override
    public void init() {
        queryService.initialize();
    }

    @Override
    public void start(Stage stage) {
        MainView mainView = new MainView(queryService);
        stage.setTitle("QueryLens");
        Scene scene = new Scene(mainView.create(), 1000, 720);
        scene.getStylesheets().add(getClass().getResource("/com/querylens/ui/theme.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
