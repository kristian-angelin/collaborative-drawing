import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

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
        //System.out.println("[START] " + toString());
        Color tempColor = (Color) context.getStroke();
        double tempLineWidth = context.getLineWidth();
        context.setLineWidth(getStrokeWidth());
        context.setStroke(getLineColor());
        //System.out.println("[STROKE] " + toString());
        context.strokeRect(getX(), getY(), width, height);
        context.setLineWidth(tempLineWidth);
        context.setStroke(tempColor);
        //System.out.println("[END] " + toString());
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
