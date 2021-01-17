import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import java.util.ArrayList;

/**
 * Concrete class for drawing free hand shapes on a canvas.
 * Takes arrays of points that makes up the shape.
 *
 * @author  Kristian Angelin
 * @version 1.0
 * @since   2021-01-17
 */

public class DrawFreeHand extends DrawObject {

    private double[] xPoints;
    private double[] yPoints;
    private int nPoints = 0;

    DrawFreeHand() {}
    DrawFreeHand(ArrayList<Double> xPoints, ArrayList<Double> yPoints, Paint color) {
        addPoints(xPoints, yPoints);
        setColor(color);
    }

    public void addPoints(ArrayList<Double> xPoints, ArrayList<Double> yPoints) {
        this.xPoints = new double[xPoints.size()];
        this.yPoints = new double[yPoints.size()];
        this.yPoints = yPoints.stream().mapToDouble(Double::doubleValue).toArray();
        this.xPoints = xPoints.stream().mapToDouble(Double::doubleValue).toArray();
        nPoints = xPoints.size();
    }

    @Override
    public void toCanvas(GraphicsContext context) {
        // Save current color/stroke width
        Color tempColor = (Color) context.getStroke();
        double tempLineWidth = context.getLineWidth();
        // Set properties and paint oval
        context.setLineWidth(getStrokeWidth());
        context.setStroke(getLineColor());
        context.strokePolyline(xPoints, yPoints, nPoints);
        // Restore color/stroke width
        context.setLineWidth(tempLineWidth);
        context.setStroke(tempColor);
    }

    @Override
    public String toString() {
        return "DrawFreehand{" +
                "number of points=" + nPoints +
                '}';
    }
}
