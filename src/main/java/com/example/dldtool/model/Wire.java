package com.example.dldtool.model;

import java.io.Serializable;

public class Wire implements Serializable {

    private Gate sourceGate;
    private Gate targetGate;
    private int  targetPinIndex;

    public Wire(Gate source, Gate target, int targetPin) {
        this.sourceGate     = source;
        this.targetGate     = target;
        this.targetPinIndex = targetPin;
    }

    // Carry signal from source output to target input
    public void propagate() {
        targetGate.setInput(targetPinIndex, sourceGate.getOutput());
    }

    public Gate getSource()      { return sourceGate; }
    public Gate getTarget()      { return targetGate; }
    public int  getTargetPin()   { return targetPinIndex; }
}