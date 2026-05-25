package com.example.dldtool.controller;

import com.example.dldtool.MainApplication;
import com.example.dldtool.model.BooleanSimplifier;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;

import java.util.List;

public class SimplifierController {

    @FXML private TextField expressionInput;
    @FXML private TextField ttInput;
    @FXML private Label     resultLabel;
    @FXML private Label     detailLabel;
    @FXML private TableView<ObservableList<String>> truthTable;

    private final BooleanSimplifier simplifier = new BooleanSimplifier();
    private boolean inserting = false;

    @FXML
    public void initialize() {
        expressionInput.setFocusTraversable(false);
        ttInput.setFocusTraversable(false);
        expressionInput.setOnMouseClicked(e -> expressionInput.requestFocus());
        ttInput.setOnMouseClicked(e -> ttInput.requestFocus());
    }

    // ── QUICK INSERT ──────────────────────────────────────────
    @FXML private void insertOR()     { insertAt("+"); }
    @FXML private void insertAND()    { insertAt("."); }
    @FXML private void insertNOT()    { insertAt("'"); }
    @FXML private void insertParens() { insertAt("()"); }

    private void insertAt(String text) {
        inserting = true;

        // Get current text and append to the END always
        String current = expressionInput.getText();
        String newText = current + text;
        expressionInput.setText(newText);

        int newPos = newText.length();
        expressionInput.requestFocus();
        expressionInput.positionCaret(newPos);

        inserting = false;
    }

    @FXML
    private void clearInput() {
        expressionInput.clear();
        resultLabel.setText("—");
        detailLabel.setText("");
    }

    // ── SIMPLIFIER ────────────────────────────────────────────
    @FXML
    private void simplify() {
        String expr = expressionInput.getText().trim();
        if (expr.isEmpty()) {
            resultLabel.setText("Please enter an expression.");
            detailLabel.setText("");
            return;
        }
        try {
            String result = simplifier.simplify(expr);
            resultLabel.setText(result);
            List<Character> vars = simplifier.getVariables();
            int numVars = vars.size();
            int minterms = (int) Math.pow(2, numVars);
            detailLabel.setText(numVars + " variable"
                    + (numVars != 1 ? "s" : "") + " detected ("
                    + vars.toString().replace("[","").replace("]","")
                    + ")  ·  " + minterms + " minterms  ·  SOP form");
        } catch (Exception e) {
            resultLabel.setText("Invalid expression — check your syntax.");
            detailLabel.setText("Use + for OR, . for AND, ' for NOT");
        }
    }

    // ── TRUTH TABLE ───────────────────────────────────────────
    @FXML
    private void generateTruthTable() {
        String expr = ttInput.getText().trim();
        if (expr.isEmpty()) return;

        try {
            List<String[]> rows = simplifier.getTruthTable(expr);
            List<Character> vars = simplifier.getVariables();

            truthTable.getColumns().clear();

            for (int i = 0; i < vars.size(); i++) {
                final int col = i;
                TableColumn<ObservableList<String>, String> column
                        = new TableColumn<>(String.valueOf(vars.get(i)));
                column.setCellValueFactory(data ->
                        new javafx.beans.property.SimpleStringProperty(
                                data.getValue().get(col)));
                column.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
                column.setPrefWidth(60);
                truthTable.getColumns().add(column);
            }

            TableColumn<ObservableList<String>, String> outCol
                    = new TableColumn<>("F");
            final int lastCol = vars.size();
            outCol.setCellValueFactory(data ->
                    new javafx.beans.property.SimpleStringProperty(
                            data.getValue().get(lastCol)));
            outCol.setPrefWidth(60);
            outCol.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null); setStyle("");
                    } else {
                        setText(item);
                        setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;"
                                + (item.equals("1")
                                ? "-fx-text-fill: #2D6B35;"
                                : "-fx-text-fill: #A03060;"));
                    }
                }
            });
            truthTable.getColumns().add(outCol);

            ObservableList<ObservableList<String>> data
                    = FXCollections.observableArrayList();
            for (String[] row : rows) {
                data.add(FXCollections.observableArrayList(row));
            }
            truthTable.setItems(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── NAVIGATION ────────────────────────────────────────────
    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApplication.class.getResource("menu-view.fxml"));
            Scene scene = new Scene(loader.load(), 700, 600);
            scene.getStylesheets().add(
                    MainApplication.class.getResource("styles.css").toExternalForm());
            MainApplication.mainStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}