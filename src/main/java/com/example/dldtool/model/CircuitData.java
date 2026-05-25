package com.example.dldtool.model;

import com.example.dldtool.controller.SimulatorController;
import java.io.Serializable;
import java.util.List;

public class CircuitData implements Serializable {

    private static final long serialVersionUID = 1L;

    // All circuit components
    public List<Gate>     gates;
    public List<NodeData> nodeWires;
    public List<InputNodeData>  inputNodes;
    public List<OutputNodeData> outputNodes;

    // Simple serializable versions of InputNode/OutputNode
    public static class InputNodeData implements Serializable {
        private static final long serialVersionUID = 1L;
        public double x, y;
        public String label;
        public boolean value;

        public InputNodeData(double x, double y,
                             String label, boolean value) {
            this.x = x; this.y = y;
            this.label = label; this.value = value;
        }
    }

    public static class OutputNodeData implements Serializable {
        private static final long serialVersionUID = 1L;
        public double x, y;
        public String label;
        public boolean value;

        public OutputNodeData(double x, double y,
                              String label, boolean value) {
            this.x = x; this.y = y;
            this.label = label; this.value = value;
        }
    }

    // Stores wire connections as indices
    public static class NodeData implements Serializable {
        private static final long serialVersionUID = 1L;
        public String sourceType; // "GATE" or "INPUT"
        public int    sourceIndex;
        public String targetType; // "GATE" or "OUTPUT"
        public int    targetIndex;
        public int    targetPin;

        public NodeData(String sourceType, int sourceIndex,
                        String targetType, int targetIndex,
                        int targetPin) {
            this.sourceType  = sourceType;
            this.sourceIndex = sourceIndex;
            this.targetType  = targetType;
            this.targetIndex = targetIndex;
            this.targetPin   = targetPin;
        }
    }
}