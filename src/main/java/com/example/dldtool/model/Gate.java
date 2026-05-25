package com.example.dldtool.model;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.io.Serializable;

public abstract class Gate implements Serializable {

    protected String name;
    protected boolean[] inputs;
    protected boolean output;
    protected double x, y;
    public double width  = 70;
    public double height = 50;

    // Pastel sky-blue palette
    protected static final Color COLOR_BODY     = Color.web("#E8F5FB");
    protected static final Color COLOR_BORDER   = Color.web("#2E7FA8");
    protected static final Color COLOR_SELECTED = Color.web("#BFDFEF");
    protected static final Color COLOR_PIN_OFF  = Color.web("#2E7FA8");
    protected static final Color COLOR_PIN_ON   = Color.web("#F2C9D8");
    protected static final Color COLOR_WIRE_OFF = Color.web("#BFDFEF");
    protected static final Color COLOR_WIRE_ON  = Color.web("#A03060");
    protected static final Color COLOR_LABEL    = Color.web("#2E7FA8");

    public Gate(String name, int inputCount) {
        this.name   = name;
        this.inputs = new boolean[inputCount];
    }

    public abstract boolean evaluate();

    // Each gate draws its own symbol
    public abstract void draw(GraphicsContext gc, boolean isSelected);

    // Shared helper — draws input/output pin dots
    protected void drawPins(GraphicsContext gc) {
        double r = 5;
        for (int i = 0; i < inputs.length; i++) {
            double px = getInputPinX(i);
            double py = getInputPinY(i);
            gc.setFill(inputs[i] ? COLOR_PIN_ON : COLOR_PIN_OFF);
            gc.fillOval(px - r, py - r, r * 2, r * 2);
        }
        // output pin
        gc.setFill(output ? COLOR_PIN_ON : COLOR_PIN_OFF);
        gc.fillOval(getOutputPinX() - r, getOutputPinY() - r, r * 2, r * 2);
    }

    // Shared helper — draws the gate name label below the gate
    protected void drawLabel(GraphicsContext gc) {
        gc.setFill(COLOR_LABEL);
        gc.setFont(Font.font("Segoe UI", 11));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(name, x + width / 2, y + height + 14);
    }

    public void setInput(int index, boolean value) {
        if (index >= 0 && index < inputs.length) {
            inputs[index] = value;
        }
        output = evaluate();
    }

    public boolean getOutput()    { return output; }
    public String  getName()      { return name;   }
    public int     getInputCount(){ return inputs.length; }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX()      { return x; }
    public double getY()      { return y; }
    public double getWidth()  { return width; }
    public double getHeight() { return height; }

    public double getOutputPinX() { return x + width; }
    public double getOutputPinY() { return y + height / 2; }

    public double getInputPinX(int index) { return x; }
    public double getInputPinY(int index) {
        if (inputs.length == 1) return y + height / 2;
        double spacing = height / (inputs.length + 1);
        return y + spacing * (index + 1);
    }

    public boolean contains(double mx, double my) {
        return mx >= x && mx <= x + width &&
                my >= y && my <= y + height;
    }

    // Check if click is near an input pin
    public int getNearestInputPin(double mx, double my) {
        for (int i = 0; i < inputs.length; i++) {
            double dx = mx - getInputPinX(i);
            double dy = my - getInputPinY(i);
            if (Math.sqrt(dx * dx + dy * dy) < 10) return i;
        }
        return -1;
    }

    // Check if click is near the output pin
    public boolean isNearOutputPin(double mx, double my) {
        double dx = mx - getOutputPinX();
        double dy = my - getOutputPinY();
        return Math.sqrt(dx * dx + dy * dy) < 10;
    }
}