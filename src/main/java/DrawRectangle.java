import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Concrete drawObject class for drawing rectangles on a canvas.
 *
 * @author  Kristian Angelin
 * @version 1.0
 * @since   2021-01-17
 */

public class DrawRectangle extends DrawObject {
    private double height;
    private double width;

    DrawRectangle(){}

    DrawRectangle(double startX, double startY, double width, double height) {
        super(startX, startY);
        this.width = width;
        this.height = height;
    }

    // Copy constructor
    DrawRectangle(DrawRectangle drawRectangle) {
        super(drawRectangle);
        this.height = drawRectangle.height;
        this.width = drawRectangle.width;
    }

    public void setHeight(double height) {
        this.height = height;
    }
    public void setWidth(double width) {
        this.width = width;
    }

    @Override
    public void toCanvas(GraphicsContext context) {
        // Save current color/stroke width
        Color tempColor = (Color) context.getStroke();
        double tempLineWidth = context.getLineWidth();
        // Set properties and paint oval
        context.setLineWidth(getStrokeWidth());
        context.setStroke(getLineColor());
        context.strokeRect(getX(), getY(), width, height);
        // Restore color/stroke width
        context.setLineWidth(tempLineWidth);
        context.setStroke(tempColor);
    }

    @Override
    public String toString() {
        return "DrawRectangle{" +
                "height=" + height +
                ", width=" + width +
                super.toString() +
                '}';
    }
}
