import io.reactivex.rxjavafx.observables.JavaFxObservable;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class CollaborativeDrawing extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        // Setup the paintable canvas
        Canvas canvas = new Canvas(850, 800);
        GraphicsContext context;
        context = canvas.getGraphicsContext2D();

        // Create buttons for draw shape selection
        ToggleButton rectangleBtn = new ToggleButton("Rectangle");
        ToggleButton ovalBtn = new ToggleButton("Oval");
        ToggleButton lineBtn = new ToggleButton("Line");
        ToggleButton freehandBtn = new ToggleButton("Freehand");

        // All shape buttons in a group for easy select/deselect
        ToggleGroup drawTools = new ToggleGroup();

        rectangleBtn.setToggleGroup(drawTools);
        ovalBtn.setToggleGroup(drawTools);
        lineBtn.setToggleGroup(drawTools);
        freehandBtn.setToggleGroup(drawTools);

        ColorPicker colorPicker = new ColorPicker(Color.BLACK); // Color picker with default color

        // Slider for setting size of strokes
        Slider slider = new Slider(1, 40, 3);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(7.0);
        slider.setMinorTickCount(3);
        Label sliderLabel = new Label("Stroke size: " + slider.getValue());

        Button clearCanvasBtn = new Button("Clear canvas");

        // Create toolbar with all tools
        VBox toolBox = new VBox(13);
        toolBox.setPadding(new Insets(5));
        // CSS to set style of toolbox
        toolBox.setStyle("-fx-background-color: #e3e3e3;"
                        + "-fx-border-color: #ababab;"
                        + "-fx-border-width: 3;");
        toolBox.setPrefWidth(150);
        toolBox.getChildren().addAll(rectangleBtn, ovalBtn, lineBtn, freehandBtn,
                                        colorPicker, sliderLabel, slider, clearCanvasBtn);

        // Create layout panes
        BorderPane pane = new BorderPane();
        pane.setLeft(toolBox);
        pane.setCenter(canvas);

        Scene scene = new Scene(pane, 1000, 800);
        primaryStage.setTitle("Collaborative Drawing");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Shapes used for drawing
        Rectangle rectangle = new Rectangle();
        Ellipse oval = new Ellipse();
        Line line = new Line();

        // Observable checking the color selection
        JavaFxObservable.actionEventsOf(colorPicker)
                .subscribe(e -> context.setStroke(colorPicker.getValue()));

        // Observable checking the slider for stroke size selection
        JavaFxObservable.valuesOf(slider.valueProperty())
                .subscribe(e -> {
                    context.setLineWidth(slider.getValue());
                    sliderLabel.setText("Stroke size: " + String.format("%.1f", slider.getValue())); // String.format() for showing only one decimal.
                });

        // Observable clearing canvas on clear canvas button press
        JavaFxObservable.actionEventsOf(clearCanvasBtn)
                .subscribe(e -> context.clearRect(0,0, canvas.getWidth(),canvas.getHeight()));

        // Observable for handling mouse events on the canvas (painting)
        JavaFxObservable.eventsOf(canvas, MouseEvent.ANY)
                .filter(me -> me.getEventType() == MouseEvent.MOUSE_PRESSED
                        || me.getEventType() == MouseEvent.MOUSE_RELEASED
                        || me.getEventType() == MouseEvent.MOUSE_DRAGGED)
                .subscribe(me ->{

                    // Begin selected shape on initial mouse press
                    if(me.getEventType() == MouseEvent.MOUSE_PRESSED) {
                        if(rectangleBtn.isSelected()) {
                            rectangle.setX(me.getX());
                            rectangle.setY(me.getY());
                        } else if(ovalBtn.isSelected()) {
                            oval.setCenterX(me.getX());
                            oval.setCenterY(me.getY());
                        } else if(lineBtn.isSelected()) {
                            line.setStartX(me.getX());
                            line.setStartY(me.getY());
                        } else if(freehandBtn.isSelected()) {
                            context.beginPath();
                            context.lineTo(me.getX(), me.getY());
                        }
                    }
                    // If free draw is selected save mouse movement when dragging
                    else if (me.getEventType() == MouseEvent.MOUSE_DRAGGED){
                        if(freehandBtn.isSelected()) {
                            context.lineTo(me.getX(), me.getY());
                            context.stroke();
                        }
                    }
                    // Create selected shapes on mouse release
                    else if(me.getEventType() == MouseEvent.MOUSE_RELEASED) {
                        if(rectangleBtn.isSelected()) {
                            // Use Math.abs to handle negative numbers
                            rectangle.setWidth(Math.abs(me.getX() - rectangle.getX()));
                            rectangle.setHeight(Math.abs(me.getY() - rectangle.getY()));

                            // Check if shape is was drawn to a negative coordinates
                            if(rectangle.getX() > me.getX()) {
                                rectangle.setX(me.getX());
                            }
                            if(rectangle.getY() > me.getY()) {
                                rectangle.setY(me.getY());
                            }
                            context.strokeRect(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());

                        } else if(ovalBtn.isSelected()) {
                            // Use Math.abs to handle negative numbers
                            oval.setRadiusX(Math.abs(me.getX() - oval.getCenterX()));
                            oval.setRadiusY(Math.abs(me.getY() - oval.getCenterY()));

                            // Check if shape is was drawn to a negative coordinates
                            if(oval.getCenterX() > me.getX()) {
                                oval.setCenterX(me.getX());
                            }
                            if(oval.getCenterY() > me.getY()) {
                                oval.setCenterY(me.getY());
                            }
                            context.strokeOval(oval.getCenterX(), oval.getCenterY(), oval.getRadiusX(), oval.getRadiusY());

                        } else if(lineBtn.isSelected()) {
                            line.setEndX(me.getX());
                            line.setEndY(me.getY());
                            context.strokeLine(line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY());

                        } else if(freehandBtn.isSelected()) {
                            context.lineTo(me.getX(), me.getY());
                            context.stroke();
                            context.closePath();
                        }
                    }
                });
    }
}
