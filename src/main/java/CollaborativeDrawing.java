import io.reactivex.*;
//import io.reactivex.Observable;
//import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.sources.Change;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

//import java.awt.*;
import java.awt.*;
//import java.awt.event.ActionEvent;
import java.util.EventListener;
//import java.awt.event.MouseEvent;

public class CollaborativeDrawing extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

            Observable<Shape> shapes;

        Canvas canvas = new Canvas(850, 800);
        GraphicsContext graphicsContext;
        graphicsContext = canvas.getGraphicsContext2D();
        //gc.setLineWidth(1);

        // Create buttons for tools
        ToggleButton rectangleBtn = new ToggleButton("Rectangle");
        ToggleButton ovalBtn = new ToggleButton("Oval");
        ToggleButton lineBtn = new ToggleButton("Line");
        ToggleButton freehandBtn = new ToggleButton("Freehand");

        // Shapes used for drawing
        Rectangle rect = new Rectangle();
        Ellipse ellipse = new Ellipse();
        Line line = new Line();

        rectangleBtn.setUserData(rect);
        ovalBtn.setUserData(ellipse);
        lineBtn.setUserData("Line");
        freehandBtn.setUserData("Freehand");

        ToggleGroup drawTools = new ToggleGroup();

        rectangleBtn.setToggleGroup(drawTools);
        ovalBtn.setToggleGroup(drawTools);
        lineBtn.setToggleGroup(drawTools);
        freehandBtn.setToggleGroup(drawTools);

        // Create toolbar with all tools
        VBox toolBox = new VBox(10);
        toolBox.setPadding(new Insets(5));
        toolBox.setStyle("-fx-background-color: #999");
        toolBox.setPrefWidth(150);
        toolBox.getChildren().addAll(rectangleBtn, ovalBtn, lineBtn, freehandBtn); // Observable list

        // Create layout panes
        BorderPane pane = new BorderPane();
        pane.setLeft(toolBox);
        pane.setCenter(canvas);

        Scene scene = new Scene(pane, 1000, 800);

        primaryStage.setTitle("Collaborative Drawing");
        primaryStage.setScene(scene);
        primaryStage.show();

        Shape shapeList;
        //Shape currentSelection;

        //Rectangle rect = new Rectangle();
        //Ellipse ellipse = new Ellipse();

        //currentSelection = rect;
        //Observable<ObservableList<Toggle>> toolSelection = JavaFxObservable.eventsOf(drawTools.getSelectedToggle(), ActionEvent.ACTION); emitOnChanged(drawTools.selectedToggleProperty().getValue()); //eventsOf(drawTools, ChangeListener<drawTools.getSelectedToggle()>());
        //Observable<Toggle> toolSelection = JavaFxObservable.eventsOf(drawTools, ChangeListener<drawTools.getSelectedToggle()>());
        // Set up behaviour
        //var drawToolSelection = JavaFxObservable.valuesOf(drawTools.selectedToggleProperty());
        //Observable<ActionEvent> selectRectangle = JavaFxObservable.actionEventsOf(rectangleBtn);
        Observable<MouseEvent> mouseDragEvent = JavaFxObservable.eventsOf(canvas, MouseEvent.ANY);

        /*JavaFxObservable.actionEventsOf((javafx.scene.control.MenuItem) drawTools.getSelectedToggle()).subscribe(e->{
            System.out.println("SHOULD WORK!");
            System.out.println("e: " + e.toString());
        });*/
        /*selectRectangle.subscribe(e->{
            currentSelection = new Rectangle();
        });*/
        // TODO: Look at making each button its own observable.
        /*drawToolSelection.subscribe(e-> {
            System.out.println("SHOULD WORK!");
            System.out.println("e: " + e.getUserData());
        });*/
        mouseDragEvent
            .filter(event -> event.getEventType() == MouseEvent.MOUSE_PRESSED
                        || event.getEventType() == MouseEvent.MOUSE_RELEASED
                        || event.getEventType() == MouseEvent.MOUSE_DRAGGED)
            .subscribe(e ->{
                //Shape currentSelection = (Shape) drawTools.getSelectedToggle().getUserData();
                if(e.getEventType() == MouseEvent.MOUSE_PRESSED) {
                    if(rectangleBtn.isSelected()) {
                        System.out.println("IT IS A RECTANGLE!!!!");
                        rect.setX(e.getX());
                        rect.setY(e.getY());
                    } else if(ovalBtn.isSelected()) {
                        ellipse.setCenterX(e.getX());
                        ellipse.setCenterY(e.getY());
                        ellipse.setRadiusX(e.getX());
                        ellipse.setRadiusY(e.getY());
                        graphicsContext.strokeOval(ellipse.getCenterX(), ellipse.getCenterY(), ellipse.getRadiusX(), ellipse.getRadiusY());
                    } else if(lineBtn.isSelected()) {

                    } else if(freehandBtn.isSelected()) {

                    }
                    else {
                        System.out.println("IT IS NULL!!!");
                    }
                }
                if(e.getEventType() == MouseEvent.MOUSE_RELEASED) {
                    if(rectangleBtn.isSelected()) {
                        rect.setWidth(Math.abs(e.getX() - rect.getX()));
                        rect.setHeight(Math.abs(e.getY() - rect.getY()));

                        if(rect.getX() > e.getX()) {
                            rect.setX(e.getX());
                        }

                        if(rect.getY() > e.getY()) {
                            rect.setY(e.getY());
                        }

                        graphicsContext.strokeRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                    }
                }
            /*System.out.println("X: " + e.getX() + " Y: " + e.getY());
            System.out.println("Selected shape: " + drawTools.getSelectedToggle());
            Rectangle rectangle = new Rectangle();
            rectangle.setX(e.getX());
            rectangle.setY(e.getY());
            rectangle.setHeight(100);
            rectangle.setWidth(100);
            graphicsContext.strokeRect(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());*/
        });
        //JavaFxObservable.eventsOf(this, MouseEvent.ANY)
    }

    /*private Observable<Shape> getShapeSelection(ToggleGroup toggleGroup) {

        Observable<Shape> shape = JavaFxObservable.valuesOf(toggleGroup.selectedToggleProperty())
                    .subscribe(e -> {
                        System.out.println("SHOULD WORK!");
                        return e.getUserData();
        });
    }*/
}
