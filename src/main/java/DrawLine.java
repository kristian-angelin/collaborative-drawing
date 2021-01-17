import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Concrete class for drawing lines on a canvas.
 *
 * @author  Kristian Angelin
 * @version 1.0
 * @since   2021-01-17
 */

public class DrawLine extends DrawObject {

    private double endX;
    private double endY;

    DrawLine(){}

    // Copy constructor
    DrawLine(DrawLine drawLine) {
        super(drawLine);
        this.endX = drawLine.endX;
        this.endY = drawLine.endY;
    }
    public void setEndX(double endX) {
        this.endX = endX;
    }
    public void setEndY(double endY) {
        this.endY = endY;
    }

    @Override
    public void toCanvas(GraphicsContext context) {
        // Save current color/stroke width
        Color tempColor = (Color) context.getStroke();
        double tempLineWidth = context.getLineWidth();
        // Set properties and paint oval
        context.setLineWidth(getStrokeWidth());
        context.setStroke(getLineColor());
        context.strokeLine(getX(), getY(), endX, endY);
        // Restore color/stroke width
        context.setLineWidth(tempLineWidth);
        context.setStroke(tempColor);
    }

    @Override
    public String toString() {
        return "DrawLine{" +
                "endX=" + endX +
                ", endY=" + endY +
                super.toString() +
                '}';
    }
}
