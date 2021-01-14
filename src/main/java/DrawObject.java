import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.io.Serializable;

public abstract class DrawObject implements Serializable {
//public class DrawObject implements Serializable{

    private double x;
    private double y;

    private double lineWidth;
    private String lineColor; // Color stored as hexadecimal (web)

    DrawObject(){}

    DrawObject(double x, double y) {
        this.x = x;
        this.y = y;
    }


    protected double getX() {
        return x;
    }
    protected double getY() {
        return y;
    }
    protected double getStrokeWidth() {
        return lineWidth;
    }
    protected Paint getLineColor() {
        return Color.web(lineColor);
    }

    public void setX(double x) {
        this.x = x;
    }
    public void setY(double y) {
        this.y = y;
    }
    public void setStrokeWidth(double width) {
        lineWidth = width;
    }
    public void setColor(Paint color) {
        lineColor = color.toString();
    }

    public abstract void toCanvas(GraphicsContext context);

    @Override
    public String toString() {
        return ", x=" + x +
                ", y=" + y +
                ", lineWidth=" + lineWidth +
                ", lineColor='" + lineColor + '\'';
    }
}

