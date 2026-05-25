package com.example.dldtool.model;

import javafx.scene.canvas.GraphicsContext;

public class NotGate extends Gate  {

    public NotGate() { super("NOT", 1); }

    @Override
    public boolean evaluate() {
        return !inputs[0];
    }

    @Override
    public void draw(GraphicsContext gc, boolean isSelected) {
        gc.setFill(isSelected ? COLOR_SELECTED : COLOR_BODY);
        gc.setStroke(COLOR_BORDER);
        gc.setLineWidth(2);

        // Triangle body
        gc.beginPath();
        gc.moveTo(x, y);
        gc.lineTo(x + width - 10, y + height / 2);
        gc.lineTo(x, y + height);
        gc.closePath();
        gc.fill();
        gc.stroke();

        // Bubble at output
        gc.setFill(COLOR_BODY);
        gc.fillOval(x + width - 10, y + height / 2 - 5, 10, 10);
        gc.strokeOval(x + width - 10, y + height / 2 - 5, 10, 10);

        drawPins(gc);
        drawLabel(gc);
    }
}