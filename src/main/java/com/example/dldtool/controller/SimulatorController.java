package com.example.dldtool.controller;

import com.example.dldtool.MainApplication;
import com.example.dldtool.model.*;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import javafx.stage.FileChooser;

import java.util.ArrayList;
import java.util.List;
import com.example.dldtool.model.CircuitData;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class SimulatorController {

    @FXML private Canvas simulatorCanvas;
    @FXML private Label  statusLabel;
    @FXML private VBox   toolAND, toolOR, toolNOT, toolXOR, toolNAND, toolNOR;
    @FXML private VBox   toolWire, toolInput, toolOutput, toolDelete;
    @FXML private Canvas andPreview, orPreview, notPreview;
    @FXML private Canvas xorPreview, nandPreview, norPreview;
    private static final String BLOCKS_DIR =
            System.getProperty("user.home") + "/dld_blocks/";
    // Tool modes
    private enum Tool { AND, OR, NOT, XOR, NAND, NOR, WIRE, INPUT, OUTPUT, DELETE, NONE }
    private Tool currentTool = Tool.NONE;

    private final Circuit circuit = new Circuit();
    private final List<InputNode>  inputNodes  = new ArrayList<>();
    private final List<OutputNode> outputNodes = new ArrayList<>();
    // Custom blocks registry
    private final List<CustomGate> savedBlocks = new ArrayList<>();
    private final Map<String, Object[]> blockRegistry = new HashMap<>();

    // Wire drawing state
    private Gate   wireSourceGate = null;
    private double wireTempX, wireTempY;

    // Simulation state
    private boolean simRunning = false;
    private AnimationTimer simTimer;

    // Currently selected/hovered gate
    private Gate selectedGate = null;
    // Drag state
    private boolean wasDragging = false;
    // For renaming
    private InputNode  renamingInput  = null;
    private OutputNode renamingOutput = null;

    // ── Simple inner classes for Input/Output nodes ──────────────
    public static class InputNode implements Serializable{
        private static final long serialVersionUID = 1L;
        public double x, y;
        public boolean value = false;
        public String label;
        public static final double R = 18;

        public InputNode(double x, double y, String label) {
            this.x = x; this.y = y; this.label = label;
        }

        public boolean contains(double mx, double my) {
            double dx = mx - x, dy = my - y;
            return Math.sqrt(dx*dx + dy*dy) < R;
        }

        public void draw(GraphicsContext gc) {
            gc.setFill(value ? Color.web("#2E7FA8") : Color.web("#E8F5FB"));
            gc.setStroke(Color.web("#2E7FA8"));
            gc.setLineWidth(2);
            gc.fillOval(x - R, y - R, R*2, R*2);
            gc.strokeOval(x - R, y - R, R*2, R*2);
            gc.setFill(value ? Color.WHITE : Color.web("#2E7FA8"));
            gc.setFont(Font.font("Segoe UI", 11));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(value ? "1" : "0", x, y + 4);
            gc.setFill(Color.web("#2E7FA8"));
            gc.fillText(label, x, y + R + 12);
            // output pin dot
            gc.setFill(Color.web("#2E7FA8"));
            gc.fillOval(x + R - 4, y - 4, 8, 8);
        }

        public double getOutputPinX() { return x + R; }
        public double getOutputPinY() { return y; }

        public boolean isNearOutputPin(double mx, double my) {
            double dx = mx - getOutputPinX();
            double dy = my - getOutputPinY();
            return Math.sqrt(dx*dx + dy*dy) < 10;
        }
    }

    public static class OutputNode implements Serializable{
        private static final long serialVersionUID = 1L;
        public double x, y;
        public boolean value = false;
        public String label;
        public static final double R = 18;

        public OutputNode(double x, double y, String label) {
            this.x = x; this.y = y; this.label = label;
        }

        public boolean contains(double mx, double my) {
            double dx = mx - x, dy = my - y;
            return Math.sqrt(dx*dx + dy*dy) < R;
        }

        public void draw(GraphicsContext gc) {
            gc.setFill(value ? Color.web("#C5DCC8") : Color.web("#EBF4EC"));
            gc.setStroke(Color.web("#2D6B35"));
            gc.setLineWidth(2);
            gc.fillOval(x - R, y - R, R*2, R*2);
            gc.strokeOval(x - R, y - R, R*2, R*2);
            gc.setFill(value ? Color.web("#2D6B35") : Color.web("#2D6B35"));
            gc.setFont(Font.font("Segoe UI", 11));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(value ? "1" : "0", x, y + 4);
            gc.setFill(Color.web("#2D6B35"));
            gc.fillText(label, x, y + R + 12);
            // input pin dot
            gc.setFill(Color.web("#2D6B35"));
            gc.fillOval(x - R - 4, y - 4, 8, 8);
        }

        public double getInputPinX() { return x - R; }
        public double getInputPinY() { return y; }

        public boolean isNearInputPin(double mx, double my) {
            double dx = mx - getInputPinX();
            double dy = my - getInputPinY();
            return Math.sqrt(dx*dx + dy*dy) < 10;
        }
    }

    // Wires that connect to input/output nodes
    private final List<NodeWire> nodeWires = new ArrayList<>();

    public static class NodeWire implements Serializable{
        private static final long serialVersionUID = 1L;
        public Object source; // Gate or InputNode
        public Object target; // Gate or OutputNode
        public int targetPin; // only used if target is a Gate

        public NodeWire(Object source, Object target, int targetPin) {
            this.source = source;
            this.target = target;
            this.targetPin = targetPin;
        }

        public boolean getSourceValue() {
            if (source instanceof Gate)      return ((Gate) source).getOutput();
            if (source instanceof InputNode) return ((InputNode) source).value;
            return false;
        }

        public double getStartX() {
            if (source instanceof Gate)      return ((Gate) source).getOutputPinX();
            if (source instanceof InputNode) return ((InputNode) source).getOutputPinX();
            return 0;
        }

        public double getStartY() {
            if (source instanceof Gate)      return ((Gate) source).getOutputPinY();
            if (source instanceof InputNode) return ((InputNode) source).getOutputPinY();
            return 0;
        }

        public double getEndX() {
            if (target instanceof Gate)       return ((Gate) target).getInputPinX(targetPin);
            if (target instanceof OutputNode) return ((OutputNode) target).getInputPinX();
            return 0;
        }

        public double getEndY() {
            if (target instanceof Gate)       return ((Gate) target).getInputPinY(targetPin);
            if (target instanceof OutputNode) return ((OutputNode) target).getInputPinY();
            return 0;
        }
    }

    // ── FXML Initialize ──────────────────────────────────────────
    @FXML
    public void initialize() {
        drawToolPreviews();
        setupCanvas();
        selectTool(Tool.NONE, null);
        loadSavedBlocks(); // ← add this

        javafx.application.Platform.runLater(() -> {
            simulatorCanvas.getScene().setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    selectTool(Tool.NONE, null);
                    setStatus("Tool deselected — drag gates to move them.");
                }
            });
        });
    }


    // Draw small gate symbols in the toolbar
    private void drawToolPreviews() {
        drawPreview(andPreview,  new AndGate());
        drawPreview(orPreview,   new OrGate());
        drawPreview(notPreview,  new NotGate());
        drawPreview(xorPreview,  new XorGate());
        drawPreview(nandPreview, new NandGate());
        drawPreview(norPreview,  new NorGate());
    }

    private void drawPreview(Canvas canvas, Gate gate) {
        gate.setPosition(4, 2);
        gate.width  = 42;
        gate.height = 30;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gate.draw(gc, false);
    }

    // ── Canvas setup ─────────────────────────────────────────────
    private void setupCanvas() {
        GraphicsContext gc = simulatorCanvas.getGraphicsContext2D();
        redrawCanvas(gc);

        simulatorCanvas.setOnMousePressed(e -> handleMousePressed(e.getX(), e.getY()));
        simulatorCanvas.setOnMouseDragged(e -> handleMouseDragged(e.getX(), e.getY()));
        simulatorCanvas.setOnMouseReleased(e -> handleMouseReleased(e.getX(), e.getY()));
        simulatorCanvas.setOnMouseClicked(e -> handleCanvasClick(e.getX(), e.getY()));
        simulatorCanvas.setOnMouseMoved(e  -> handleMouseMove(e.getX(), e.getY()));

        simulatorCanvas.getScene(); // need scene access
        javafx.application.Platform.runLater(() -> {
            simulatorCanvas.getScene().setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    selectTool(Tool.NONE, null);
                    setStatus("Tool deselected. Click and drag gates to move them.");
                }
            });
        });
        simulatorCanvas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                handleDoubleClick(e.getX(), e.getY());
            } else {
                handleCanvasClick(e.getX(), e.getY());
            }
        });
    }
    private void loadSavedBlocks() {
        File dir = new File(BLOCKS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
            return;
        }
        File[] files = dir.listFiles(
                (d, name) -> name.endsWith(".dld"));
        if (files == null) return;

        for (File f : files) {
            try {
                List<String> lines = Files.readAllLines(f.toPath());
                String blockName = f.getName()
                        .replace(".dld", "")
                        .replace("_", " ");

                List<CustomGate.InputNode> blockIns  = new ArrayList<>();
                List<CustomGate.OutputNode> blockOuts = new ArrayList<>();

                for (String line : lines) {
                    String[] parts = line.trim().split(" ");
                    if (parts.length < 2) continue;
                    if (parts[0].equals("INPUT")) {
                        double nx = parts.length > 2 ?
                                Double.parseDouble(parts[2]) : 50;
                        double ny = parts.length > 3 ?
                                Double.parseDouble(parts[3]) : 100;
                        blockIns.add(new CustomGate.InputNode(
                                nx, ny, parts[1]));
                    } else if (parts[0].equals("OUTPUT")) {
                        double nx = parts.length > 2 ?
                                Double.parseDouble(parts[2]) : 400;
                        double ny = parts.length > 3 ?
                                Double.parseDouble(parts[3]) : 100;
                        blockOuts.add(new CustomGate.OutputNode(
                                nx, ny, parts[1]));
                    }
                }

                if (!blockIns.isEmpty() && !blockOuts.isEmpty()) {
                    blockRegistry.put(blockName,
                            new Object[]{blockIns, blockOuts, 0});
                    setStatus("Loaded block: " + blockName);
                }
            } catch (Exception e) {
                System.err.println("Failed to load block: "
                        + f.getName());
            }
        }
    }
    private void redrawCanvas(GraphicsContext gc) {
        gc.clearRect(0, 0, simulatorCanvas.getWidth(), simulatorCanvas.getHeight());

        // Draw dot grid
        gc.setFill(Color.web("#BFDFEF"));
        for (double gx = 20; gx < simulatorCanvas.getWidth(); gx += 24) {
            for (double gy = 20; gy < simulatorCanvas.getHeight(); gy += 24) {
                gc.fillOval(gx - 2, gy - 2, 4, 4);
            }
        }

        // Draw all node wires
        for (NodeWire nw : nodeWires) {
            boolean on = nw.getSourceValue();
            gc.setStroke(on ? Color.web("#A03060") : Color.web("#BFDFEF"));
            gc.setLineWidth(on ? 3 : 2);
            double sx = nw.getStartX(), sy = nw.getStartY();
            double ex = nw.getEndX(),   ey = nw.getEndY();
            // L-shaped wire
            double midX = (sx + ex) / 2;
            gc.beginPath();
            gc.moveTo(sx, sy);
            gc.lineTo(midX, sy);
            gc.lineTo(midX, ey);
            gc.lineTo(ex, ey);
            gc.stroke();
        }

        // Draw input nodes
        for (InputNode n : inputNodes) n.draw(gc);

        // Draw output nodes
        for (OutputNode n : outputNodes) n.draw(gc);

        // Draw gates
        for (Gate g : circuit.getGates()) {
            g.draw(gc, g == selectedGate);
        }

        // Draw in-progress wire
        if (wireSourceGate != null) {
            gc.setStroke(Color.web("#F2C9D8"));
            gc.setLineWidth(2);
            gc.setLineDashes(6, 4);
            gc.strokeLine(
                    wireSourceGate.getOutputPinX(),
                    wireSourceGate.getOutputPinY(),
                    wireTempX, wireTempY
            );
            gc.setLineDashes(0);
        }
    }
    // ── SAVE AS BLOCK ─────────────────────────────────────────
    @FXML
    private void saveAsBlock() {
        if (circuit.getGates().isEmpty()) {
            setStatus("No gates to save as block!");
            return;
        }
        if (inputNodes.isEmpty() || outputNodes.isEmpty()) {
            setStatus("Block needs at least one input and one output!");
            return;
        }

        javafx.scene.control.TextInputDialog dialog =
                new javafx.scene.control.TextInputDialog("MyBlock");
        dialog.setTitle("Save as Custom Block");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter block name:");
        dialog.getDialogPane().setStyle("-fx-background-color:#F3F0FB;");

        dialog.showAndWait().ifPresent(name -> {
            if (name.trim().isEmpty()) return;

            String blockName = name.trim();

            List<CustomGate.InputNode> blockIns = new ArrayList<>();
            for (InputNode n : inputNodes) {
                blockIns.add(new CustomGate.InputNode(
                        n.x, n.y, n.label));
            }
            List<CustomGate.OutputNode> blockOuts = new ArrayList<>();
            for (OutputNode n : outputNodes) {
                blockOuts.add(new CustomGate.OutputNode(
                        n.x, n.y, n.label));
            }

            // Save to registry
            blockRegistry.put(blockName,
                    new Object[]{blockIns, blockOuts,
                            circuit.getGates().size()});

            // Save to file so it persists
            try {
                File dir = new File(BLOCKS_DIR);
                if (!dir.exists()) dir.mkdirs();

                String fileName = blockName.replace(" ", "_") + ".dld";
                File blockFile  = new File(BLOCKS_DIR + fileName);

                try (PrintWriter pw = new PrintWriter(blockFile)) {
                    // Save gates
                    for (Gate g : circuit.getGates()) {
                        pw.println("GATE " + g.getName() + " " +
                                (int)g.getX() + " " + (int)g.getY());
                    }
                    // Save inputs
                    for (InputNode n : inputNodes) {
                        pw.println("INPUT " + n.label + " " +
                                (int)n.x + " " + (int)n.y);
                    }
                    // Save outputs
                    for (OutputNode n : outputNodes) {
                        pw.println("OUTPUT " + n.label + " " +
                                (int)n.x + " " + (int)n.y);
                    }
                    // Save wires
                    List<Gate> gateList = circuit.getGates();
                    for (NodeWire nw : nodeWires) {
                        String src, tgt;
                        if (nw.source instanceof Gate) {
                            src = "GATE:" +
                                    gateList.indexOf(nw.source);
                        } else {
                            src = "INPUT:" +
                                    inputNodes.indexOf(nw.source);
                        }
                        if (nw.target instanceof Gate) {
                            tgt = "GATE:" +
                                    gateList.indexOf(nw.target) +
                                    ":" + nw.targetPin;
                        } else {
                            tgt = "OUTPUT:" +
                                    outputNodes.indexOf(nw.target);
                        }
                        pw.println("WIRE " + src + " " + tgt);
                    }
                }

                setStatus("Block '" + blockName +
                        "' saved to " + BLOCKS_DIR);

                javafx.scene.control.Alert alert =
                        new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Block Saved");
                alert.setHeaderText(null);
                alert.setContentText("'" + blockName +
                        "' saved permanently!\n" +
                        "It will be available every time you open the app.\n" +
                        inputNodes.size() + " inputs, " +
                        outputNodes.size() + " outputs, " +
                        circuit.getGates().size() + " gates.");
                alert.getDialogPane().setStyle(
                        "-fx-background-color:#F3F0FB;");
                alert.showAndWait();

            } catch (Exception e) {
                setStatus("Error saving block: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // ── SAVE CIRCUIT ──────────────────────────────────────────
    @FXML
    private void saveCircuit() {
        if (circuit.getGates().isEmpty() && inputNodes.isEmpty()) {
            setStatus("Nothing to save!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Circuit");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "DLD Circuit", "*.dld"));
        fileChooser.setInitialFileName("circuit.dld");

        File file = fileChooser.showSaveDialog(
                MainApplication.mainStage);
        if (file == null) return;

        try {
            // Build CircuitData object
            CircuitData data = new CircuitData();
            data.gates = new ArrayList<>(circuit.getGates());

            // Save input nodes as InputNodeData
            data.inputNodes = new ArrayList<>();
            for (InputNode n : inputNodes) {
                data.inputNodes.add(
                        new CircuitData.InputNodeData(
                                n.x, n.y, n.label, n.value));
            }

            // Save output nodes as OutputNodeData
            data.outputNodes = new ArrayList<>();
            for (OutputNode n : outputNodes) {
                data.outputNodes.add(
                        new CircuitData.OutputNodeData(
                                n.x, n.y, n.label, n.value));
            }

            // Save wire connections as index-based NodeData
            data.nodeWires = new ArrayList<>();
            List<Gate> gateList = circuit.getGates();

            for (NodeWire nw : nodeWires) {
                String srcType;
                int    srcIdx;
                String tgtType;
                int    tgtIdx;
                int    tgtPin = nw.targetPin;

                if (nw.source instanceof Gate) {
                    srcType = "GATE";
                    srcIdx  = gateList.indexOf(nw.source);
                } else {
                    srcType = "INPUT";
                    srcIdx  = inputNodes.indexOf(nw.source);
                }

                if (nw.target instanceof Gate) {
                    tgtType = "GATE";
                    tgtIdx  = gateList.indexOf(nw.target);
                } else {
                    tgtType = "OUTPUT";
                    tgtIdx  = outputNodes.indexOf(nw.target);
                }

                data.nodeWires.add(new CircuitData.NodeData(
                        srcType, srcIdx,
                        tgtType, tgtIdx, tgtPin));
            }

            // Write object to file using ObjectOutputStream
            try (FileOutputStream fos = new FileOutputStream(file);
                 ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(data);
            }

            setStatus("Circuit saved to " + file.getName());

        } catch (Exception e) {
            setStatus("Error saving: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // ── LOAD CIRCUIT ──────────────────────────────────────────
    @FXML
    private void loadCircuit() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Circuit");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "DLD Circuit", "*.dld"));

        File file = fileChooser.showOpenDialog(
                MainApplication.mainStage);
        if (file == null) return;

        try {
            // Read object from file using ObjectInputStream
            CircuitData data;
            try (FileInputStream fis = new FileInputStream(file);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                data = (CircuitData) ois.readObject();
            }

            // Clear current circuit
            circuit.clear();
            inputNodes.clear();
            outputNodes.clear();
            nodeWires.clear();
            wireSourceGate     = null;
            pendingInputSource = null;
            selectedGate       = null;

            // Restore gates
            for (Gate g : data.gates) {
                circuit.addGate(g);
            }

            // Restore input nodes
            for (CircuitData.InputNodeData nd : data.inputNodes) {
                InputNode n = new InputNode(nd.x, nd.y, nd.label);
                n.value = nd.value;
                inputNodes.add(n);
            }

            // Restore output nodes
            for (CircuitData.OutputNodeData nd : data.outputNodes) {
                OutputNode n = new OutputNode(nd.x, nd.y, nd.label);
                n.value = nd.value;
                outputNodes.add(n);
            }

            // Restore wires using index references
            List<Gate> gateList = circuit.getGates();
            for (CircuitData.NodeData nd : data.nodeWires) {
                Object source = null;
                Object target = null;

                if (nd.sourceType.equals("GATE")) {
                    if (nd.sourceIndex < gateList.size())
                        source = gateList.get(nd.sourceIndex);
                } else {
                    if (nd.sourceIndex < inputNodes.size())
                        source = inputNodes.get(nd.sourceIndex);
                }

                if (nd.targetType.equals("GATE")) {
                    if (nd.targetIndex < gateList.size())
                        target = gateList.get(nd.targetIndex);
                } else {
                    if (nd.targetIndex < outputNodes.size())
                        target = outputNodes.get(nd.targetIndex);
                }

                if (source != null && target != null) {
                    nodeWires.add(new NodeWire(
                            source, target, nd.targetPin));
                }
            }

            redrawCanvas(simulatorCanvas.getGraphicsContext2D());
            setStatus("Circuit loaded from " + file.getName()
                    + " — " + circuit.getGates().size() + " gates, "
                    + nodeWires.size() + " wires.");

        } catch (ClassNotFoundException e) {
            setStatus("Error: File format not recognised.");
            e.printStackTrace();
        } catch (Exception e) {
            setStatus("Error loading: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    private void clearAll() {
        javafx.scene.control.Alert confirm =
                new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Canvas");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "Clear everything on the canvas? This cannot be undone.");
        confirm.getDialogPane().setStyle("-fx-background-color:#FBF0F5;");

        confirm.showAndWait().ifPresent(result -> {
            if (result == javafx.scene.control.ButtonType.OK) {
                if (simTimer != null) simTimer.stop();
                simRunning = false;
                circuit.clear();
                inputNodes.clear();
                outputNodes.clear();
                nodeWires.clear();
                wireSourceGate    = null;
                pendingInputSource = null;
                selectedGate      = null;
                redrawCanvas(simulatorCanvas.getGraphicsContext2D());
                setStatus("Canvas cleared.");
            }
        });
    }

    // Helper to create gate by name string
    private Gate createGateByName(String name) {
        switch (name.toUpperCase()) {
            case "AND":  return new AndGate();
            case "OR":   return new OrGate();
            case "NOT":  return new NotGate();
            case "XOR":  return new XorGate();
            case "NAND": return new NandGate();
            case "NOR":  return new NorGate();
            default:     return null;
        }
    }
    @FXML
    private void showBlocks() {
        if (blockRegistry.isEmpty()) {
            setStatus("No saved blocks yet. Build a circuit and click 'Save Block'.");
            return;
        }

        List<String> blockNames = new ArrayList<>(blockRegistry.keySet());

        javafx.scene.control.ChoiceDialog<String> dialog =
                new javafx.scene.control.ChoiceDialog<>(
                        blockNames.get(0), blockNames);
        dialog.setTitle("Saved Blocks");
        dialog.setHeaderText("Select a block to place on canvas");
        dialog.setContentText("Block:");
        dialog.getDialogPane().setStyle("-fx-background-color:#F3F0FB;");

        dialog.showAndWait().ifPresent(selectedName -> {
            try {
                // Read the full .dld file for this block
                File blockFile = new File(BLOCKS_DIR
                        + selectedName.replace(" ", "_") + ".dld");

                if (!blockFile.exists()) {
                    setStatus("Block file not found: " + selectedName);
                    return;
                }

                List<String> lines =
                        java.nio.file.Files.readAllLines(blockFile.toPath());

                // Reconstruct internal circuit from file
                Circuit internalCircuit = new Circuit();
                List<Gate> internalGates = new ArrayList<>();
                List<InputNode> internalInputs = new ArrayList<>();
                List<OutputNode> internalOutputs = new ArrayList<>();
                List<NodeWire> internalWires = new ArrayList<>();

                // First pass — create gates and nodes
                for (String line : lines) {
                    String[] parts = line.trim().split(" ");
                    if (parts.length < 2) continue;

                    switch (parts[0]) {
                        case "GATE": {
                            Gate g = createGateByName(parts[1]);
                            if (g != null) {
                                double gx = parts.length > 2 ?
                                        Double.parseDouble(parts[2]) : 100;
                                double gy = parts.length > 3 ?
                                        Double.parseDouble(parts[3]) : 100;
                                g.setPosition(gx, gy);
                                internalCircuit.addGate(g);
                                internalGates.add(g);
                            }
                            break;
                        }
                        case "INPUT": {
                            double nx = parts.length > 2 ?
                                    Double.parseDouble(parts[2]) : 50;
                            double ny = parts.length > 3 ?
                                    Double.parseDouble(parts[3]) : 100;
                            internalInputs.add(
                                    new InputNode(nx, ny, parts[1]));
                            break;
                        }
                        case "OUTPUT": {
                            double nx = parts.length > 2 ?
                                    Double.parseDouble(parts[2]) : 400;
                            double ny = parts.length > 3 ?
                                    Double.parseDouble(parts[3]) : 100;
                            internalOutputs.add(
                                    new OutputNode(nx, ny, parts[1]));
                            break;
                        }
                    }
                }

                // Second pass — create wires
                for (String line : lines) {
                    String[] parts = line.trim().split(" ");
                    if (parts.length < 3 || !parts[0].equals("WIRE"))
                        continue;

                    String[] srcParts = parts[1].split(":");
                    String[] tgtParts = parts[2].split(":");

                    Object source = null;
                    Object target = null;
                    int targetPin = 0;

                    // Resolve source
                    if (srcParts[0].equals("GATE")) {
                        int idx = Integer.parseInt(srcParts[1]);
                        if (idx < internalGates.size())
                            source = internalGates.get(idx);
                    } else if (srcParts[0].equals("INPUT")) {
                        int idx = Integer.parseInt(srcParts[1]);
                        if (idx < internalInputs.size())
                            source = internalInputs.get(idx);
                    }

                    // Resolve target
                    if (tgtParts[0].equals("GATE")) {
                        int idx = Integer.parseInt(tgtParts[1]);
                        if (idx < internalGates.size())
                            target = internalGates.get(idx);
                        if (tgtParts.length > 2)
                            targetPin = Integer.parseInt(tgtParts[2]);
                    } else if (tgtParts[0].equals("OUTPUT")) {
                        int idx = Integer.parseInt(tgtParts[1]);
                        if (idx < internalOutputs.size())
                            target = internalOutputs.get(idx);
                    }

                    if (source != null && target != null) {
                        internalWires.add(
                                new NodeWire(source, target, targetPin));
                    }
                }

                // Build CustomGate input/output interface
                List<CustomGate.InputNode> blockIns = new ArrayList<>();
                for (InputNode n : internalInputs) {
                    blockIns.add(
                            new CustomGate.InputNode(n.x, n.y, n.label));
                }
                List<CustomGate.OutputNode> blockOuts = new ArrayList<>();
                for (OutputNode n : internalOutputs) {
                    blockOuts.add(
                            new CustomGate.OutputNode(n.x, n.y, n.label));
                }

                if (blockIns.isEmpty() || blockOuts.isEmpty()) {
                    setStatus("Block has no inputs or outputs!");
                    return;
                }

                // Create CustomGate with the reconstructed circuit
                CustomGate cg = new CustomGate(
                        selectedName,
                        internalCircuit,
                        blockIns,
                        blockOuts,
                        internalInputs,   // ← pass internal InputNodes
                        internalOutputs,  // ← pass internal OutputNodes
                        internalWires     // ← pass internal NodeWires
                );

                // Place in centre of canvas
                cg.setPosition(
                        simulatorCanvas.getWidth()  / 2 - cg.getWidth()  / 2,
                        simulatorCanvas.getHeight() / 2 - cg.getHeight() / 2);

                // Add to main circuit
                circuit.addGate(cg);

                // Also add internal wires so simulation works
                for (NodeWire nw : internalWires) {
                    if (nw.source instanceof Gate &&
                            nw.target instanceof Gate) {
                        internalCircuit.addWire(
                                new Wire((Gate) nw.source,
                                        (Gate) nw.target,
                                        nw.targetPin));
                    }
                }

                redrawCanvas(simulatorCanvas.getGraphicsContext2D());
                setStatus("Block '" + selectedName +
                        "' placed! Connect its pins with the Wire tool.");

            } catch (Exception e) {
                setStatus("Error loading block: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // ── Mouse handlers ───────────────────────────────────────────
    private void handleCanvasClick(double mx, double my) {

        GraphicsContext gc = simulatorCanvas.getGraphicsContext2D();


        switch (currentTool) {

            case AND:   placeGate(new AndGate(),  mx, my); break;
            case OR:    placeGate(new OrGate(),   mx, my); break;
            case NOT:   placeGate(new NotGate(),  mx, my); break;
            case XOR:   placeGate(new XorGate(),  mx, my); break;
            case NAND:  placeGate(new NandGate(), mx, my); break;
            case NOR:   placeGate(new NorGate(),  mx, my); break;

            case INPUT:
                String inLabel = "I" + (inputNodes.size() + 1);
                inputNodes.add(new InputNode(mx, my, inLabel));
                setStatus("Input node placed. Click it during simulation to toggle.");
                break;

            case OUTPUT:
                String outLabel = "O" + (outputNodes.size() + 1);
                outputNodes.add(new OutputNode(mx, my, outLabel));
                setStatus("Output node placed.");
                break;

            case WIRE:
                handleWireClick(mx, my);
                break;

            case DELETE:
                handleDelete(mx, my);
                break;

            case NONE:
                // Toggle input nodes during simulation
                if (simRunning) {
                    for (InputNode n : inputNodes) {
                        if (n.contains(mx, my)) {
                            n.value = !n.value;
                            runSimulationTick();
                        }
                    }
                }
                break;
        }
        redrawCanvas(gc);
    }

    private void handleMouseMove(double mx, double my) {
        wireTempX = mx;
        wireTempY = my;
        if (wireSourceGate != null) {
            redrawCanvas(simulatorCanvas.getGraphicsContext2D());
        }
    }

    private void placeGate(Gate gate, double mx, double my) {
        gate.setPosition(mx - gate.getWidth() / 2, my - gate.getHeight() / 2);
        circuit.addGate(gate);
        setStatus(gate.getName() + " gate placed. Use Wire tool to connect pins.");
    }

    private void handleWireClick(double mx, double my) {
        // Check if clicking an output pin of a gate
        for (Gate g : circuit.getGates()) {
            if (g.isNearOutputPin(mx, my)) {
                wireSourceGate = g;
                setStatus("Wire started from " + g.getName() + " output. Click an input pin to connect.");
                return;
            }
        }
        // Check if clicking output pin of an input node
        for (InputNode n : inputNodes) {
            if (n.isNearOutputPin(mx, my)) {
                wireSourceGate = null;
                // handle input node as source
                handleInputNodeWire(n, mx, my);
                return;
            }
        }

        // If wire is in progress, check input pins of gates
        if (wireSourceGate != null) {
            for (Gate g : circuit.getGates()) {
                int pin = g.getNearestInputPin(mx, my);
                if (pin >= 0) {
                    nodeWires.add(new NodeWire(wireSourceGate, g, pin));
                    wireSourceGate = null;
                    setStatus("Wire connected!");
                    return;
                }
            }
            // Check output nodes
            for (OutputNode out : outputNodes) {
                if (out.isNearInputPin(mx, my)) {
                    nodeWires.add(new NodeWire(wireSourceGate, out, 0));
                    wireSourceGate = null;
                    setStatus("Wire connected to output!");
                    return;
                }
            }
        }
        wireSourceGate = null;
    }

    // Store pending input node source for wire
    private InputNode pendingInputSource = null;

    private void handleInputNodeWire(InputNode source, double mx, double my) {
        pendingInputSource = source;
        setStatus("Wire started from input node. Click a gate input pin.");
        simulatorCanvas.setOnMouseClicked(e -> {
            double ex = e.getX(), ey = e.getY();
            for (Gate g : circuit.getGates()) {
                int pin = g.getNearestInputPin(ex, ey);
                if (pin >= 0 && pendingInputSource != null) {
                    nodeWires.add(new NodeWire(pendingInputSource, g, pin));
                    pendingInputSource = null;
                    setStatus("Wire connected!");
                    redrawCanvas(simulatorCanvas.getGraphicsContext2D());
                    return;
                }
            }
            setupCanvas();
            handleCanvasClick(ex, ey);
        });
    }
    private void handleDoubleClick(double mx, double my) {
        // Check input nodes
        for (InputNode n : inputNodes) {
            if (n.contains(mx, my)) {
                javafx.scene.control.TextInputDialog dialog =
                        new javafx.scene.control.TextInputDialog(n.label);
                dialog.setTitle("Rename Input");
                dialog.setHeaderText(null);
                dialog.setContentText("Enter new label:");
                // Style the dialog
                dialog.getDialogPane().setStyle(
                        "-fx-background-color: #FBF0F5;");
                dialog.showAndWait().ifPresent(name -> {
                    if (!name.trim().isEmpty()) {
                        n.label = name.trim();
                        redrawCanvas(simulatorCanvas.getGraphicsContext2D());
                    }
                });
                return;
            }
        }
        // Check output nodes
        for (OutputNode n : outputNodes) {
            if (n.contains(mx, my)) {
                javafx.scene.control.TextInputDialog dialog =
                        new javafx.scene.control.TextInputDialog(n.label);
                dialog.setTitle("Rename Output");
                dialog.setHeaderText(null);
                dialog.setContentText("Enter new label:");
                dialog.getDialogPane().setStyle(
                        "-fx-background-color: #EBF4EC;");
                dialog.showAndWait().ifPresent(name -> {
                    if (!name.trim().isEmpty()) {
                        n.label = name.trim();
                        redrawCanvas(simulatorCanvas.getGraphicsContext2D());
                    }
                });
                return;
            }
        }
    }
    @FXML
    private void generateTruthTable() {
        if (inputNodes.isEmpty() || outputNodes.isEmpty()) {
            setStatus("Need at least one input and one output node!");
            return;
        }
        if (simTimer != null) simTimer.stop();
        simRunning = false;

        int numInputs = inputNodes.size();
        int numRows   = (int) Math.pow(2, numInputs);

        // Build table content
        StringBuilder sb = new StringBuilder();

        // Header row
        for (InputNode n : inputNodes) {
            sb.append(String.format("%-6s", n.label));
        }
        sb.append("| ");
        for (OutputNode n : outputNodes) {
            sb.append(String.format("%-6s", n.label));
        }
        sb.append("\n");

        // Separator
        sb.append("-".repeat((numInputs + outputNodes.size()) * 6 + 2));
        sb.append("\n");

        // Data rows
        for (int i = 0; i < numRows; i++) {
            // Set input values
            for (int j = 0; j < numInputs; j++) {
                boolean val = ((i >> (numInputs - 1 - j)) & 1) == 1;
                inputNodes.get(j).value = val;
                sb.append(String.format("%-6s", val ? "1" : "0"));
            }

            // Run simulation
            for (int tick = 0; tick < 3; tick++) {
                runSimulationTick();
            }

            sb.append("| ");
            for (OutputNode n : outputNodes) {
                sb.append(String.format("%-6s", n.value ? "1" : "0"));
            }
            sb.append("\n");
        }

        // Reset inputs to 0
        for (InputNode n : inputNodes) n.value = false;
        runSimulationTick();

        // Show in a dialog
        javafx.scene.control.TextArea textArea =
                new javafx.scene.control.TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setFont(Font.font("Courier New", 13));
        textArea.setStyle(
                "-fx-control-inner-background: #F3F0FB;" +
                        "-fx-font-family: 'Courier New';");
        textArea.setPrefRowCount(Math.min(numRows + 4, 20));
        textArea.setPrefColumnCount(
                (numInputs + outputNodes.size()) * 6 + 4);

        javafx.scene.control.ButtonType copyBtn =
                new javafx.scene.control.ButtonType("Copy");
        javafx.scene.control.ButtonType closeBtn =
                new javafx.scene.control.ButtonType("Close",
                        javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);

        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog =
                new javafx.scene.control.Dialog<>();
        dialog.setTitle("Truth Table");
        dialog.setHeaderText("Generated from circuit — "
                + numInputs + " inputs, "
                + outputNodes.size() + " outputs");
        dialog.getDialogPane().setContent(textArea);
        dialog.getDialogPane().getButtonTypes().addAll(copyBtn, closeBtn);
        dialog.getDialogPane().setStyle("-fx-background-color: #F3F0FB;");

        dialog.showAndWait().ifPresent(result -> {
            if (result == copyBtn) {
                javafx.scene.input.ClipboardContent content =
                        new javafx.scene.input.ClipboardContent();
                content.putString(sb.toString());
                javafx.scene.input.Clipboard.getSystemClipboard()
                        .setContent(content);
                setStatus("Truth table copied to clipboard!");
            }
        });

        setStatus("Truth table generated for "
                + numInputs + " input(s), "
                + outputNodes.size() + " output(s).");
    }

    private void handleDelete(double mx, double my) {
        // Delete gates
        Gate toRemove = null;
        for (Gate g : circuit.getGates()) {
            if (g.contains(mx, my)) { toRemove = g; break; }
        }
        if (toRemove != null) {
            circuit.removeGate(toRemove);
            final Gate removed = toRemove;
            nodeWires.removeIf(w -> w.source == removed || w.target == removed);
            setStatus("Gate deleted.");
            return;
        }
        // Delete input nodes
        inputNodes.removeIf(n -> n.contains(mx, my));
        // Delete output nodes
        outputNodes.removeIf(n -> n.contains(mx, my));
    }

    // ── Simulation ───────────────────────────────────────────────
    private void runSimulationTick() {
        // Propagate input node values into gates
        for (NodeWire nw : nodeWires) {
            if (nw.target instanceof Gate) {
                boolean val = nw.getSourceValue();
                ((Gate) nw.target).setInput(nw.targetPin, val);
            }
        }
        // Evaluate all gates
        for (Gate g : circuit.getGates()) g.evaluate();

        // Update output nodes
        for (NodeWire nw : nodeWires) {
            if (nw.target instanceof OutputNode) {
                ((OutputNode) nw.target).value = nw.getSourceValue();
            }
        }
        redrawCanvas(simulatorCanvas.getGraphicsContext2D());
    }

    // ── Tool selection ───────────────────────────────────────────
    private VBox currentToolVBox = null;

    private void selectTool(Tool tool, VBox toolBox) {
        if (currentToolVBox != null) {
            currentToolVBox.getStyleClass().remove("tool-btn-selected");
        }
        currentTool = tool;
        currentToolVBox = toolBox;
        if (toolBox != null) {
            toolBox.getStyleClass().add("tool-btn-selected");
        }
        wireSourceGate = null;
        pendingInputSource = null;
    }

    @FXML private void selectToolAND()    { selectTool(Tool.AND,    toolAND);    setStatus("AND gate selected. Click canvas to place."); }
    @FXML private void selectToolOR()     { selectTool(Tool.OR,     toolOR);     setStatus("OR gate selected. Click canvas to place."); }
    @FXML private void selectToolNOT()    { selectTool(Tool.NOT,    toolNOT);    setStatus("NOT gate selected. Click canvas to place."); }
    @FXML private void selectToolXOR()    { selectTool(Tool.XOR,    toolXOR);    setStatus("XOR gate selected. Click canvas to place."); }
    @FXML private void selectToolNAND()   { selectTool(Tool.NAND,   toolNAND);   setStatus("NAND gate selected. Click canvas to place."); }
    @FXML private void selectToolNOR()    { selectTool(Tool.NOR,    toolNOR);    setStatus("NOR gate selected. Click canvas to place."); }
    @FXML private void selectToolWire()   { selectTool(Tool.WIRE,   toolWire);   setStatus("Wire tool selected. Click an output pin (right side of gate) to start."); }
    @FXML private void selectToolInput()  { selectTool(Tool.INPUT,  toolInput);  setStatus("Input tool selected. Click canvas to place."); }
    @FXML private void selectToolOutput() { selectTool(Tool.OUTPUT, toolOutput); setStatus("Output tool selected. Click canvas to place."); }
    @FXML private void selectToolDelete() { selectTool(Tool.DELETE, toolDelete); setStatus("Delete tool selected. Click a component to remove it."); }

    // ── Simulation controls ──────────────────────────────────────
    @FXML
    private void startSimulation() {
        if (simRunning) return;
        simRunning = true;
        currentTool = Tool.NONE;
        if (currentToolVBox != null) {
            currentToolVBox.getStyleClass().remove("tool-btn-selected");
            currentToolVBox = null;
        }
        setStatus("Simulation running! Click input nodes to toggle them.");
        simTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                runSimulationTick();
            }
        };
        simTimer.start();
    }

    @FXML
    private void pauseSimulation() {
        if (simTimer != null) simTimer.stop();
        simRunning = false;
        setStatus("Simulation paused.");
    }

    @FXML
    private void resetSimulation() {
        if (simTimer != null) simTimer.stop();
        simRunning = false;
        for (InputNode n : inputNodes) n.value = false;
        for (Gate g : circuit.getGates()) {
            for (int i = 0; i < g.getInputCount(); i++) g.setInput(i, false);
        }
        for (OutputNode n : outputNodes) n.value = false;
        redrawCanvas(simulatorCanvas.getGraphicsContext2D());
        setStatus("Simulation reset. All inputs set to 0.");
    }

    @FXML
    private void goBack() {
        if (simTimer != null) simTimer.stop();
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApplication.class.getResource("/com/example/dldtool/menu-view.fxml")
            );
            Scene scene = new Scene(loader.load(), 700, 600);
            scene.getStylesheets().add(
                    MainApplication.class.getResource("/com/example/dldtool/styles.css").toExternalForm()
            );
            MainApplication.mainStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private void handleMousePressed(double mx, double my) {}
    private void handleMouseDragged(double mx, double my) {}
    private void handleMouseReleased(double mx, double my) {}}