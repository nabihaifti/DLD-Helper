package com.example.dldtool.model;

import javafx.scene.canvas.GraphicsContext;

public class AndGate extends Gate {

    public AndGate() { super("AND", 2); }

    @Override
    public boolean evaluate() {
        return inputs[0] && inputs[1];
    }

    @Override
    public void draw(GraphicsContext gc, boolean isSelected) {
        gc.setFill(isSelected ? COLOR_SELECTED : COLOR_BODY);
        gc.setStroke(COLOR_BORDER);
        gc.setLineWidth(2);

        // AND gate shape: rectangle on left, D-curve on right
        gc.beginPath();
        gc.moveTo(x, y);
        gc.lineTo(x + width * 0.5, y);
        gc.arc(x + width * 0.5, y + height / 2,
                height / 2, height / 2, 90, -180);
        gc.lineTo(x, y + height);
        gc.closePath();
        gc.fill();
        gc.stroke();

        drawPins(gc);
        drawLabel(gc);
    }
}