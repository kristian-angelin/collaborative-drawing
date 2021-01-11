

public class DrawRectangle extends DrawObject {
    private final double x;
    private final double y;
    private final double height;
    private final double width;

    DrawRectangle(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /*@Override
    public String toStreamString() {
        return null;
    }*/
}
