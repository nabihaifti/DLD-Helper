package com.example.dldtool.model;

import javafx.scene.canvas.GraphicsContext;

public class OrGate extends Gate {

    public OrGate() { super("OR", 2); }

    @Override
    public boolean evaluate() {
        return inputs[0] || inputs[1];
    }

    @Override
    public void draw(GraphicsContext gc, boolean isSelected) {
        gc.setFill(isSelected ? COLOR_SELECTED : COLOR_BODY);
        gc.setStroke(COLOR_BORDER);
        gc.setLineWidth(2);

        // OR gate shape: curved back, pointed front
        gc.beginPath();
        gc.moveTo(x, y);
        gc.quadraticCurveTo(x + width * 0.4, y,
                x + width, y + height / 2);
        gc.quadraticCurveTo(x + width * 0.4, y + height,
                x, y + height);
        gc.quadraticCurveTo(x + width * 0.2, y + height / 2,
                x, y);
        gc.closePath();
        gc.fill();
        gc.stroke();

        drawPins(gc);
        drawLabel(gc);
    }
}