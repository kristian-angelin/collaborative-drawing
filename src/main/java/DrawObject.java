import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

public class DrawObject {

    private double x;
    private double y;

    public DrawObject() {}

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

    public String toStreamableString() {
        return x + ":" + y + System.lineSeparator();
    }
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

