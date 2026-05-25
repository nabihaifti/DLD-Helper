package com.example.dldtool.model;

import javafx.scene.canvas.GraphicsContext;

public class NandGate extends Gate {

    public NandGate() { super("NAND", 2); }

    @Override
    public boolean evaluate() {
        return !(inputs[0] && inputs[1]);
    }

    @Override
    public void draw(GraphicsContext gc, boolean isSelected) {
        gc.setFill(isSelected ? COLOR_SELECTED : COLOR_BODY);
        gc.setStroke(COLOR_BORDER);
        gc.setLineWidth(2);

        // AND shape (slightly smaller to fit bubble)
        gc.beginPath();
        gc.moveTo(x, y);
        gc.lineTo(x + width * 0.45, y);
        gc.arc(x + width * 0.45, y + height / 2,
                height / 2, height / 2, 90, -180);
        gc.lineTo(x, y + height);
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