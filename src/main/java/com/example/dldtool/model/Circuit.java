package com.example.dldtool.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Circuit implements Serializable {

    private List<Gate> gates = new ArrayList<>();
    private List<Wire> wires = new ArrayList<>();

    public void addGate(Gate gate) { gates.add(gate); }
    public void addWire(Wire wire) { wires.add(wire); }

    public void removeGate(Gate gate) {
        gates.remove(gate);
        wires.removeIf(w ->
                w.getSource() == gate || w.getTarget() == gate
        );
    }

    // Run one simulation tick
    public void simulate() {
        for (Wire w : wires) {
            w.propagate();
        }
        for (Gate g : gates) {
            g.evaluate();
        }
    }

    public List<Gate> getGates() { return gates; }
    public List<Wire> getWires() { return wires; }

    public void clear() {
        gates.clear();
        wires.clear();
    }}