import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;

import java.util.ArrayList;

public class DrawFreeHand extends DrawObject {

    private double[] xPoints;
    private double[] yPoints;
    private int nPoints = 0;

    DrawFreeHand() {}

    public void addPoints(ArrayList<Double> xPoints, ArrayList<Double> yPoints) {
        this.xPoints = new double[xPoints.size()];
        this.yPoints = new double[yPoints.size()];
        this.yPoints = yPoints.stream().mapToDouble(Double::doubleValue).toArray();
        this.xPoints = xPoints.stream().mapToDouble(Double::doubleValue).toArray();
        nPoints = xPoints.size();
    }

    @Override
    public void toCanvas(GraphicsContext context) {

        Color tempColor = (Color) context.getStroke();
        double tempLineWidth = context.getLineWidth();
        context.setLineWidth(getStrokeWidth());
        context.setStroke(getLineColor());

        context.strokePolyline(xPoints, yPoints, nPoints);

        context.setLineWidth(tempLineWidth);
        context.setStroke(tempColor);
    }

    @Override
    public String toString() {
        return "DrawFreehand{" +
                "points=" + nPoints +
                '}';
    }
}
