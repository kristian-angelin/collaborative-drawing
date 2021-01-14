import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class DrawLine extends DrawObject {

    private double endX;
    private double endY;

    DrawLine(){}

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
