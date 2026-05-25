package com.example.dldtool.controller;

import com.example.dldtool.MainApplication;
import com.example.dldtool.model.kmap.KMapSolver;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.List;
import java.util.Set;

public class KMapController {

    @FXML private Canvas kmapCanvas;
    @FXML private Label  resultLabel;
    @FXML private Button btn2, btn3, btn4, btn5, btn6;

    private KMapSolver solver;
    private int currentVars = 4;

    // Colors
    private static final Color COL_HEADER = Color.web("#C5DCC8");
    private static final Color COL_BORDER = Color.web("#2D6B35");
    private static final Color COL_CELL_0 = Color.web("#FEFEFE");
    private static final Color COL_CELL_1 = Color.web("#C5DCC8");
    private static final Color COL_CELL_X = Color.web("#F7E8A0");
    private static final Color COL_TEXT_0 = Color.web("#2E7FA8");
    private static final Color COL_TEXT_1 = Color.web("#2D6B35");
    private static final Color COL_TEXT_X = Color.web("#8A6E00");
    private static final Color[] GROUP_COLORS = {
            Color.web("#A03060"), Color.web("#2E7FA8"),
            Color.web("#8A6E00"), Color.web("#4A3888"),
            Color.web("#C05000"), Color.web("#006060")
    };

    // Layout — non-final so they change per variable count
    private static final double PAD = 10;
    private static final double GAP = 16;
    private double CELL  = 52;
    private double HDR_W = 36;
    private double HDR_H = 28;

    // ── INITIALIZE ────────────────────────────────────────────
    @FXML
    public void initialize() {
        currentVars = 4;
        solver = new KMapSolver(4);
        resultLabel.setText("—");
        highlightVarButton(btn4);
        setupCanvasClick();
        // Draw after layout pass using runLater
        javafx.application.Platform.runLater(() -> {
            updateSizes();
            drawKMap();
        });
    }

    // ── UPDATE SIZES ──────────────────────────────────────────
    private void updateSizes() {
        double availW = kmapCanvas.getWidth()  - PAD * 2;
        double availH = kmapCanvas.getHeight() - PAD * 2 - 20;

        switch (currentVars) {
            case 2:
                HDR_W = 44; HDR_H = 32;
                // 2x2 grid — make cells big
                CELL = Math.min(availW / 3.5, availH / 3.0);
                CELL = Math.min(CELL, 110);
                break;
            case 3:
                HDR_W = 44; HDR_H = 32;
                // 2x4 grid
                CELL = Math.min((availW - HDR_W) / 4, (availH - HDR_H) / 2);
                CELL = Math.min(CELL, 100);
                break;
            case 4:
                HDR_W = 44; HDR_H = 32;
                // 4x4 grid
                CELL = Math.min((availW - HDR_W) / 4, (availH - HDR_H) / 4);
                CELL = Math.min(CELL, 90);
                break;
            case 5:
                HDR_W = 36; HDR_H = 26;
                // Two 4x4 grids side by side
                double subW5 = (availW - GAP) / 2;
                CELL = Math.min((subW5 - HDR_W) / 4,
                        (availH - HDR_H - 20) / 4);
                CELL = Math.min(CELL, 58);
                break;
            case 6:
                HDR_W = 30; HDR_H = 22;
                // Four 4x4 grids in 2x2
                double subW6 = (availW - GAP) / 2;
                double subH6 = (availH - GAP - 30) / 2;
                CELL = Math.min((subW6 - HDR_W) / 4,
                        (subH6 - HDR_H) / 4);
                CELL = Math.min(CELL, 44);
                break;
        }
        CELL = Math.max(CELL, 22); // never smaller than 22
    }

    // ── VARIABLE BUTTONS ──────────────────────────────────────
    @FXML private void setVars2() { setVars(2, btn2); }
    @FXML private void setVars3() { setVars(3, btn3); }
    @FXML private void setVars4() { setVars(4, btn4); }
    @FXML private void setVars5() { setVars(5, btn5); }
    @FXML private void setVars6() { setVars(6, btn6); }

    private void setVars(int vars, Button btn) {
        currentVars = vars;
        solver = new KMapSolver(vars);
        resultLabel.setText("—");
        highlightVarButton(btn);
        updateSizes();
        setupCanvasClick();
        drawKMap();
    }

    private void highlightVarButton(Button active) {
        for (Button b : new Button[]{btn2,btn3,btn4,btn5,btn6})
            b.getStyleClass().remove("var-btn-active");
        active.getStyleClass().add("var-btn-active");
    }

    // ── DRAW ──────────────────────────────────────────────────
    private void drawKMap() {
        updateSizes();
        GraphicsContext gc = kmapCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, kmapCanvas.getWidth(), kmapCanvas.getHeight());

        switch (currentVars) {
            case 2: draw4x4Sub(gc, PAD, PAD, 2, 0, null); break;
            case 3: draw4x4Sub(gc, PAD, PAD, 3, 0, null); break;
            case 4: draw4x4Sub(gc, PAD, PAD, 4, 0, null); break;
            case 5: drawFiveVar(gc, null);  break;
            case 6: drawSixVar(gc, null);   break;
        }
    }

    private void drawGroups(List<Set<Integer>> groups) {
        updateSizes();
        GraphicsContext gc = kmapCanvas.getGraphicsContext2D();
        switch (currentVars) {
            case 2: draw4x4Sub(gc, PAD, PAD, 2, 0, groups); break;
            case 3: draw4x4Sub(gc, PAD, PAD, 3, 0, groups); break;
            case 4: draw4x4Sub(gc, PAD, PAD, 4, 0, groups); break;
            case 5: drawFiveVar(gc, groups); break;
            case 6: drawSixVar(gc, groups);  break;
        }
    }

    // 5-variable: two 4x4 grids side by side
    private void drawFiveVar(GraphicsContext gc, List<Set<Integer>> groups) {
        double subW  = HDR_W + 4 * CELL;
        double startX1 = PAD;
        double startX2 = PAD + subW + GAP;
        double startY  = PAD + 20;

        gc.setFill(COL_BORDER);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("A = 0", startX1 + subW / 2, PAD + 14);
        gc.fillText("A = 1", startX2 + subW / 2, PAD + 14);

        draw4x4Sub(gc, startX1, startY, 5, 0,  groups);
        draw4x4Sub(gc, startX2, startY, 5, 16, groups);
    }

    // 6-variable: four 4x4 grids in 2x2
    private void drawSixVar(GraphicsContext gc, List<Set<Integer>> groups) {
        double subW  = HDR_W + 4 * CELL;
        double subH  = HDR_H + 4 * CELL;
        double startX1 = PAD;
        double startX2 = PAD + subW + GAP;
        double startY1 = PAD + 20;
        double startY2 = PAD + 20 + subH + GAP + 14;

        gc.setFill(COL_BORDER);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        gc.setTextAlign(TextAlignment.CENTER);

        gc.fillText("A'B' (00)", startX1 + subW/2, PAD + 14);
        draw4x4Sub(gc, startX1, startY1, 6, 0,  groups);

        gc.fillText("A'B  (01)", startX2 + subW/2, PAD + 14);
        draw4x4Sub(gc, startX2, startY1, 6, 16, groups);

        gc.fillText("AB'  (10)", startX1 + subW/2, startY2 - 4);
        draw4x4Sub(gc, startX1, startY2, 6, 32, groups);

        gc.fillText("AB   (11)", startX2 + subW/2, startY2 - 4);
        draw4x4Sub(gc, startX2, startY2, 6, 48, groups);
    }

    // Draw a single sub-map
    private void draw4x4Sub(GraphicsContext gc, double ox, double oy,
                            int vars, int mintermOffset,
                            List<Set<Integer>> groups) {
        int[] gray4 = {0,1,3,2};
        int[] gray2 = {0,1};

        int rows, cols;
        int[] rowGray, colGray;
        String rowVarLabel, colVarLabel;
        String[] rowLabels, colLabels;

        switch (vars) {
            case 2:
                rows=2; cols=2;
                rowGray=gray2; colGray=gray2;
                rowVarLabel="A"; colVarLabel="B";
                rowLabels=new String[]{"0","1"};
                colLabels=new String[]{"0","1"};
                break;
            case 3:
                rows=2; cols=4;
                rowGray=gray2; colGray=gray4;
                rowVarLabel="A"; colVarLabel="BC";
                rowLabels=new String[]{"0","1"};
                colLabels=new String[]{"00","01","11","10"};
                break;
            default:
                rows=4; cols=4;
                rowGray=gray4; colGray=gray4;
                if (vars==4)      { rowVarLabel="AB"; colVarLabel="CD"; }
                else if (vars==5) { rowVarLabel="BC"; colVarLabel="DE"; }
                else              { rowVarLabel="CD"; colVarLabel="EF"; }
                rowLabels=new String[]{"00","01","11","10"};
                colLabels=new String[]{"00","01","11","10"};
                break;
        }

        // Header backgrounds
        gc.setFill(COL_HEADER);
        gc.fillRect(ox + HDR_W, oy, cols * CELL, HDR_H);
        gc.fillRect(ox, oy + HDR_H, HDR_W, rows * CELL);

        // Corner diagonal
        gc.setStroke(COL_BORDER);
        gc.setLineWidth(1);
        gc.strokeLine(ox, oy, ox + HDR_W, oy + HDR_H);

        // Corner labels
        double fontSize = CELL > 50 ? 10 : 8;
        gc.setFill(COL_BORDER);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, fontSize));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(rowVarLabel, ox + 2, oy + HDR_H - 2);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(colVarLabel, ox + HDR_W - 2, oy + HDR_H / 2);

        // Column headers
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, Math.max(fontSize, 9)));
        gc.setTextAlign(TextAlignment.CENTER);
        for (int c = 0; c < cols; c++) {
            gc.setFill(COL_BORDER);
            gc.fillText(colLabels[c],
                    ox + HDR_W + c * CELL + CELL / 2,
                    oy + HDR_H / 2 + 4);
        }

        // Row headers
        for (int r = 0; r < rows; r++) {
            gc.setFill(COL_BORDER);
            gc.fillText(rowLabels[r],
                    ox + HDR_W / 2,
                    oy + HDR_H + r * CELL + CELL / 2 + 4);
        }

        // Cells
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int mintermIdx = mintermOffset
                        + (rowGray[r] << (cols == 2 ? 1 : 2))
                        + colGray[c];

                int val = solver.getCell(mintermIdx);
                double cx = ox + HDR_W + c * CELL;
                double cy = oy + HDR_H + r * CELL;

                // Background
                Color bg = val==1 ? COL_CELL_1 :
                        val==2 ? COL_CELL_X : COL_CELL_0;
                gc.setFill(bg);
                gc.fillRect(cx, cy, CELL, CELL);

                // Border
                gc.setStroke(COL_BORDER);
                gc.setLineWidth(1);
                gc.strokeRect(cx, cy, CELL, CELL);

                // Value
                Color tc = val==1 ? COL_TEXT_1 :
                        val==2 ? COL_TEXT_X : COL_TEXT_0;
                gc.setFill(tc);
                double valFontSize = CELL > 50 ? 16 : CELL > 36 ? 13 : 10;
                gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, valFontSize));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(val==2 ? "X" : String.valueOf(val),
                        cx + CELL/2, cy + CELL/2 + valFontSize/3);

                // Minterm number (small)
                if (CELL > 30) {
                    gc.setFill(Color.web("#AAAAAA"));
                    gc.setFont(Font.font("Segoe UI", 8));
                    gc.fillText(String.valueOf(mintermIdx),
                            cx + CELL - 4, cy + CELL - 3);
                }
            }
        }

        // Outer border
        gc.setStroke(COL_BORDER);
        gc.setLineWidth(2);
        gc.strokeRect(ox + HDR_W, oy + HDR_H,
                cols * CELL, rows * CELL);

        // Group highlights
        if (groups != null) {
            for (int g = 0; g < groups.size(); g++) {
                Set<Integer> group = groups.get(g);
                Color col = GROUP_COLORS[g % GROUP_COLORS.length];
                gc.setStroke(col);
                gc.setLineWidth(3);
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        int mintermIdx = mintermOffset
                                + (rowGray[r] << (cols==2 ? 1 : 2))
                                + colGray[c];
                        if (group.contains(mintermIdx)) {
                            double cx = ox + HDR_W + c * CELL;
                            double cy = oy + HDR_H + r * CELL;
                            gc.setFill(Color.color(
                                    col.getRed(), col.getGreen(),
                                    col.getBlue(), 0.18));
                            gc.fillRoundRect(cx+2, cy+2,
                                    CELL-4, CELL-4, 8, 8);
                            gc.strokeRoundRect(cx+2, cy+2,
                                    CELL-4, CELL-4, 8, 8);
                        }
                    }
                }
            }
        }
    }

    // ── CANVAS CLICK ──────────────────────────────────────────
    private void setupCanvasClick() {
        kmapCanvas.setOnMouseClicked(e -> {
            int mintermIdx = getMintermAtPoint(e.getX(), e.getY());
            if (mintermIdx >= 0) {
                int current = solver.getCell(mintermIdx);
                solver.setCell(mintermIdx, (current + 1) % 3);
                drawKMap();
            }
        });
    }

    private int getMintermAtPoint(double mx, double my) {
        switch (currentVars) {
            case 2: return hitTest(mx, my, PAD, PAD, 2, 0);
            case 3: return hitTest(mx, my, PAD, PAD, 3, 0);
            case 4: return hitTest(mx, my, PAD, PAD, 4, 0);
            case 5: {
                double subW = HDR_W + 4 * CELL;
                double startY = PAD + 20;
                int hit = hitTest(mx, my, PAD, startY, 5, 0);
                if (hit >= 0) return hit;
                return hitTest(mx, my, PAD + subW + GAP, startY, 5, 16);
            }
            case 6: {
                double subW  = HDR_W + 4 * CELL;
                double subH  = HDR_H + 4 * CELL;
                double startY1 = PAD + 20;
                double startY2 = PAD + 20 + subH + GAP + 14;
                int hit;
                hit = hitTest(mx, my, PAD,          startY1, 6, 0);
                if (hit >= 0) return hit;
                hit = hitTest(mx, my, PAD+subW+GAP, startY1, 6, 16);
                if (hit >= 0) return hit;
                hit = hitTest(mx, my, PAD,          startY2, 6, 32);
                if (hit >= 0) return hit;
                hit = hitTest(mx, my, PAD+subW+GAP, startY2, 6, 48);
                if (hit >= 0) return hit;
                return -1;
            }
            default: return -1;
        }
    }

    private int hitTest(double mx, double my, double ox, double oy,
                        int vars, int offset) {
        int[] gray4 = {0,1,3,2};
        int[] gray2 = {0,1};
        int rows, cols;
        int[] rowGray, colGray;

        switch (vars) {
            case 2: rows=2; cols=2; rowGray=gray2; colGray=gray2; break;
            case 3: rows=2; cols=4; rowGray=gray2; colGray=gray4; break;
            default: rows=4; cols=4; rowGray=gray4; colGray=gray4; break;
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double cx = ox + HDR_W + c * CELL;
                double cy = oy + HDR_H + r * CELL;
                if (mx >= cx && mx <= cx + CELL &&
                        my >= cy && my <= cy + CELL) {
                    return offset
                            + (rowGray[r] << (cols==2 ? 1 : 2))
                            + colGray[c];
                }
            }
        }
        return -1;
    }

    // ── ACTIONS ───────────────────────────────────────────────
    @FXML
    private void solve() {
        String result = solver.solve();
        resultLabel.setText(result);
        drawGroups(solver.getGroups());
    }

    @FXML
    private void clearMap() {
        solver = new KMapSolver(currentVars);
        resultLabel.setText("—");
        drawKMap();
    }

    @FXML
    private void retry() {
        solver = new KMapSolver(currentVars);
        resultLabel.setText("—");
        drawKMap();
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApplication.class.getResource("menu-view.fxml"));
            Scene scene = new Scene(loader.load(), 700, 640);
            scene.getStylesheets().add(
                    MainApplication.class.getResource("styles.css")
                            .toExternalForm());
            MainApplication.mainStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}