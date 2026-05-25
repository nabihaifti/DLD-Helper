package com.example.dldtool;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApplication extends Application {

    public static Stage mainStage;

    @Override
    public void start(Stage stage) throws Exception {
        mainStage = stage;

        FXMLLoader fxmlLoader = new FXMLLoader(
                MainApplication.class.getResource("/com/example/dldtool/menu-view.fxml")
        );

        Scene scene = new Scene(fxmlLoader.load(), 700, 640);
        scene.getStylesheets().add(
                MainApplication.class.getResource("/com/example/dldtool/styles.css").toExternalForm()
        );

        stage.setTitle("DLD Helper");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}