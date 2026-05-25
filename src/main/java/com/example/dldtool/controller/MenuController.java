package com.example.dldtool.controller;

import com.example.dldtool.MainApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

public class MenuController {

    @FXML
    private void openSimulator() {
        loadScreen("simulator-view.fxml");
    }

    @FXML
    private void openSimplifier() {
        loadScreen("simplifier-view.fxml");
    }

    @FXML
    private void openKMap() {
        loadScreen("kmap-view.fxml");
    }
    private void loadScreen(String fxmlFile) {
        try {
            var resource = MainApplication.class.getResource(fxmlFile);
            if (resource == null) {
                System.err.println("Cannot find: " + fxmlFile);
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Scene scene = new Scene(loader.load(), 700, 640);
            scene.getStylesheets().add(
                    MainApplication.class.getResource("styles.css").toExternalForm()
            );
            MainApplication.mainStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}