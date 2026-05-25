package com.example.dldtool.model;

import com.example.dldtool.controller.SimulatorController;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CustomGate extends Gate {
    private static final long serialVersionUID = 1L;

    private Circuit internalCircuit;
    private List<InputNode> blockInputs;
    private List<OutputNode> blockOutputs;
    private String blockName;
    private List<SimulatorController.InputNode>  internalInputNodes;
    private List<SimulatorController.OutputNode> internalOutputNodes;
    private List<SimulatorController.NodeWire>   internalNodeWires;

    public static class InputNode implements Serializable {
        private static final long serialVersionUID = 1L;
        public double x, y;
        public boolean value = false;
        public String label;
        public InputNode(double x, double y, String label) {
            this.x = x; this.y = y; this.label = label;
        }
    }

    public static class OutputNode implements Serializable {
        private static final long serialVersionUID = 1L;
        public double x, y;
        public boolean value = false;
        public String label;
        public OutputNode(double x, double y, String label) {
            this.x = x; this.y = y; this.label = label;
        }
    }

    public CustomGate(String blockName,
                      Circuit internalCircuit,
                      List<InputNode> inputs,
                      List<OutputNode> outputs,
                      List<SimulatorController.InputNode>  internalInputNodes,
                      List<SimulatorController.OutputNode> internalOutputNodes,
                      List<SimulatorController.NodeWire>   internalNodeWires) {
        super(blockName, inputs.size());
        this.blockName           = blockName;
        this.internalCircuit     = internalCircuit;
        this.blockInputs         = inputs;
        this.blockOutputs        = outputs;
        this.internalInputNodes  = internalInputNodes;
        this.internalOutputNodes = internalOutputNodes;
        this.internalNodeWires   = internalNodeWires;
        this.width  = 80;
        this.height = Math.max(50, inputs.size() * 20 + 20);
    }

    @Override
    public boolean evaluate() {
        // Step 1 — push CustomGate inputs into internal InputNodes
        for (int i = 0; i < inputs.length &&
                i < internalInputNodes.size(); i++) {
            internalInputNodes.get(i).value = inputs[i];
        }

        // Step 2 — propagate InputNode → Gate connections
        for (SimulatorController.NodeWire nw : internalNodeWires) {
            if (nw.source instanceof SimulatorController.InputNode
                    && nw.target instanceof Gate) {
                boolean val = ((SimulatorController.InputNode)
                        nw.source).value;
                ((Gate) nw.target).setInput(nw.targetPin, val);
            }
        }

        // Step 3 — propagate Gate → Gate connections
        for (Wire w : internalCircuit.getWires()) {
            w.propagate();
        }

        // Step 4 — evaluate all internal gates
        for (Gate g : internalCircuit.getGates()) {
            g.evaluate();
        }

        // Step 5 — propagate Gate → OutputNode connections
        for (SimulatorController.NodeWire nw : internalNodeWires) {
            if (nw.target instanceof SimulatorController.OutputNode) {
                boolean val = nw.getSourceValue();
                ((SimulatorController.OutputNode) nw.target).value = val;
            }
        }

        // Step 6 — read output value from internal OutputNodes
        if (!internalOutputNodes.isEmpty()) {
            output = internalOutputNodes.get(0).value;
        }
        return output;
    }

    @Override
    public void draw(GraphicsContext gc, boolean isSelected) {
        gc.setFill(isSelected ? COLOR_SELECTED :
                Color.web("#F3F0FB"));
        gc.setStroke(Color.web("#4A3888"));
        gc.setLineWidth(2);

        gc.fillRoundRect(x, y, width, height, 12, 12);
        gc.strokeRoundRect(x, y, width, height, 12, 12);

        gc.setFill(Color.web("#4A3888"));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(blockName, x + width / 2, y + height / 2 + 4);

        gc.setFont(Font.font("Segoe UI", 9));
        for (int i = 0; i < inputs.length; i++) {
            String label = i < blockInputs.size() ?
                    blockInputs.get(i).label : "I" + i;
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setFill(Color.web("#4A3888"));
            gc.fillText(label, x + 4, getInputPinY(i) + 4);
        }

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFill(Color.web("#4A3888"));
        if (!blockOutputs.isEmpty()) {
            gc.fillText(blockOutputs.get(0).label,
                    x + width - 4, getOutputPinY() + 4);
        }

        drawPins(gc);
    }

    public String getBlockName()  { return blockName; }
    public Circuit getInternalCircuit() { return internalCircuit; }
    public List<InputNode>  getBlockInputs()  { return blockInputs; }
    public List<OutputNode> getBlockOutputs() { return blockOutputs; }
}