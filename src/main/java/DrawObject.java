import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.io.Serializable;

//public abstract class DrawObject {
public class DrawObject implements Serializable{

    private double x;
    private double y;
    //private double startX;
    //private double startY;
    private double width;
    private double height;

    public DrawObject() {}

    /*DrawObject(double x, double y) {
        this.startX = x;
        this.startY = y;
    }*/

    DrawObject(double x, double y) {
        this.x = x;
        this.y = y;
    }


    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return "DrawObject{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                '}';
    }

    //public abstract String toStreamString();

    /*public String toStreamableString() {
        return x + ":" + y + System.lineSeparator();
    }*/
    /*private Shape currentShape;

    GraphicsContext context;

    DrawTool(GraphicsContext gContext){
        context = gContext;
    }

    public void changeShape(Shape shape) {
        currentShape = shape;
    }

    public void draw(Shape shape, MouseEvent start, MouseEvent end) {
        if(currentShape = Rectangle)
            Shape.
    }*/
}

