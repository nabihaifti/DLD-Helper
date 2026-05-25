package com.example.dldtool.model;

import javafx.scene.canvas.GraphicsContext;

public class NorGate extends Gate  {

    public NorGate() { super("NOR", 2); }

    @Override
    public boolean evaluate() {
        return !(inputs[0] || inputs[1]);
    }

    @Override
    public void draw(GraphicsContext gc, boolean isSelected) {
        gc.setFill(isSelected ? COLOR_SELECTED : COLOR_BODY);
        gc.setStroke(COLOR_BORDER);
        gc.setLineWidth(2);

        // OR shape (slightly smaller to fit bubble)
        gc.beginPath();
        gc.moveTo(x, y);
        gc.quadraticCurveTo(x + width * 0.35, y,
                x + width - 12, y + height / 2);
        gc.quadraticCurveTo(x + width * 0.35, y + height,
                x, y + height);
        gc.quadraticCurveTo(x + width * 0.2, y + height / 2,
                x, y);
        gc.closePath();
        gc.fill();
        gc.stroke();

        // Bubble
        gc.setFill(COLOR_BODY);
        gc.fillOval(x + width - 12, y + height / 2 - 6, 12, 12);
        gc.strokeOval(x + width - 12, y + height / 2 - 6, 12, 12);

        drawPins(gc);
        drawLabel(gc);
    }
}