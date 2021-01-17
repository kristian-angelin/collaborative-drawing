import javafx.scene.canvas.GraphicsContext;

/**
 * Concrete class for clearing a canvas.
 *
 * @author  Kristian Angelin
 * @version 1.0
 * @since   2021-01-17
 */

public class DrawCleanCanvas extends DrawObject{
    private final double canvasWidth;
    private final double canvasHeight;

    DrawCleanCanvas(double canvasWidth, double canvasHeight){
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
    }

    @Override
    public void toCanvas(GraphicsContext context) {
        context.clearRect(0,0, canvasWidth, canvasHeight);
    }

    @Override
    public String toString() {
        return "DrawCleanCanvas{" + '}';
    }
}
