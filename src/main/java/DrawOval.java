import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Concrete drawObject class for drawing ovals on a canvas.
 *
 * @author  Kristian Angelin
 * @version 1.0
 * @since   2021-01-17
 */

public class DrawOval extends DrawObject {

    private double radiusX;
    private double radiusY;

    DrawOval() {}

    // Copy constructor
    DrawOval(DrawOval drawOval) {
        super(drawOval);
        this.radiusX = drawOval.radiusX;
        this.radiusY = drawOval.radiusY;
    }

    public void setRadiusX(double radiusX) {
        this.radiusX = radiusX;
    }
    public void setRadiusY(double radiusY) {
        this.radiusY = radiusY;
    }

    @Override
    public void toCanvas(GraphicsContext context) {
        // Save current color/stroke width
        Color tempColor = (Color) context.getStroke();
        double tempLineWidth = context.getLineWidth();
        // Set properties and paint oval
        context.setLineWidth(getStrokeWidth());
        context.setStroke(getLineColor());
        context.strokeOval(getX(), getY(), radiusX, radiusY);
        // Restore color/stroke width
        context.setLineWidth(tempLineWidth);
        context.setStroke(tempColor);
    }

    @Override
    public String toString() {
        return "DrawLine{" +
                "radiusX=" + radiusX +
                ", radiusY=" + radiusY +
                super.toString() +
                '}';
    }

}
