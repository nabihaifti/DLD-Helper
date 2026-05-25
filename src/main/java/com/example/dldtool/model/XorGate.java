
package com.example.dldtool.model;

import javafx.scene.canvas.GraphicsContext;

public class XorGate extends Gate{

    public XorGate() { super("XOR", 2); }

    @Override
    public boolean evaluate() {
        return inputs[0] ^ inputs[1];
    }

    @Override
    public void draw(GraphicsContext gc, boolean isSelected) {
        gc.setFill(isSelected ? COLOR_SELECTED : COLOR_BODY);
        gc.setStroke(COLOR_BORDER);
        gc.setLineWidth(2);

        // OR shape
        gc.beginPath();
        gc.moveTo(x + 8, y);
        gc.quadraticCurveTo(x + width * 0.4 + 8, y,
                x + width, y + height / 2);
        gc.quadraticCurveTo(x + width * 0.4 + 8, y + height,
                x + 8, y + height);
        gc.quadraticCurveTo(x + width * 0.2 + 8, y + height / 2,
                x + 8, y);
        gc.closePath();
        gc.fill();
        gc.stroke();

        // Extra curve line at the back for XOR
        gc.setFill(null);
        gc.beginPath();
        gc.moveTo(x, y);
        gc.quadraticCurveTo(x + width * 0.2, y + height / 2, x, y + height);
        gc.stroke();

        drawPins(gc);
        drawLabel(gc);
    }
}