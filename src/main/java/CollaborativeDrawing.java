import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class CollaborativeDrawing extends Application {

    private final ToggleGroup drawTools = new ToggleGroup(); //TODO: Should we use this here?

    // Shapes used for drawing
    private final Rectangle rectangle = new Rectangle();
    private final Ellipse oval = new Ellipse();
    private final Line line = new Line();

    //TODO: REMOVE DEBUG
    private Server server;
    private Socket socket;
    private DataOutputStream out;

    private final Button connectBtn = new Button("Connect");
    private final Button hostBtn = new Button("Host");
    private final Button discBtn = new Button("Disconnect");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        // Setup the paintable canvas
        Canvas canvas = new Canvas(650, 600);
        GraphicsContext context;
        context = canvas.getGraphicsContext2D();

        // Create buttons for draw shape selection
        ToggleButton rectangleBtn = new ToggleButton("Rectangle");
        ToggleButton ovalBtn = new ToggleButton("Oval");
        ToggleButton lineBtn = new ToggleButton("Line");
        ToggleButton freehandBtn = new ToggleButton("Freehand");

        // All shape buttons in a group for easy select/deselect
        //ToggleGroup drawTools = new ToggleGroup();

        rectangleBtn.setToggleGroup(drawTools);
        ovalBtn.setToggleGroup(drawTools);
        lineBtn.setToggleGroup(drawTools);
        freehandBtn.setToggleGroup(drawTools);

        drawTools.selectToggle(rectangleBtn);

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

        /*Button connectBtn = new Button("Connect");
        Button hostBtn = new Button("Host");
        Button discBtn = new Button("Disconnect");*/

        TextArea networkText = new TextArea();
        networkText.setEditable(false);

        /*networkText.setText("Click!" + System.lineSeparator());
        networkText.appendText("Tap!" + System.lineSeparator());*/
        discBtn.setDisable(true);

        HBox connectionBox = new HBox(20);
        connectionBox.setPadding(new Insets(10));
        connectionBox.getChildren().addAll(connectBtn, hostBtn, discBtn, networkText);
        connectionBox.setStyle("-fx-background-color: #e3e3e3;"
                                + "-fx-border-color: #ababab;"
                                + "-fx-border-width: 3;");
        connectionBox.setPrefHeight(100);

        // Create layout panes
        BorderPane pane = new BorderPane();
        pane.setLeft(toolBox);
        pane.setCenter(canvas);
        pane.setBottom(connectionBox);

        Scene scene = new Scene(pane, 800, 700);
        primaryStage.setTitle("Collaborative Drawing");
        primaryStage.setScene(scene);
        primaryStage.show();

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

        JavaFxObservable.actionEventsOf(connectBtn)
                .subscribe(e -> clientConnect(networkText));
        JavaFxObservable.actionEventsOf(hostBtn)
                .subscribe(e -> {
                    startServer(networkText);
                });
        JavaFxObservable.actionEventsOf(discBtn)
                .subscribe(e -> disconnect()); //TODO: FIX addrow test


        //JavaFxObservable.eventsOf(canvas, MouseEvent.MOUSE_PRESSED)
                //.subscribe(mEvent -> startDraw());

        // TODO: CONTINUE NEW WAY OF HANDLING MOUSE EVENTS
        Observable<MouseEvent> click = JavaFxObservable.eventsOf(canvas, MouseEvent.MOUSE_PRESSED);
        Observable<MouseEvent> drag = JavaFxObservable.eventsOf(canvas, MouseEvent.MOUSE_DRAGGED);
        Observable<MouseEvent> release = JavaFxObservable.eventsOf(canvas, MouseEvent.MOUSE_RELEASED);

        /*Observable<drawEvent> drawEvent = Observable.merge(click, drag, release)
                .map()*/

        /*Observable.merge(click, drag, release) // TODO: Remove freehand all
                .subscribe(me -> drawFreehand(context, me));*/

        Observable.merge(click, drag, release)
                .filter(e -> freehandBtn.isSelected())
                .subscribe(me -> drawFreehand(context, me));
        Observable.merge(click, drag, release)
                .filter(e -> rectangleBtn.isSelected())
                .subscribe(me -> drawRectangle(context, me));
        Observable.merge(click, drag, release)
                .filter(e -> ovalBtn.isSelected())
                .subscribe(me -> drawOval(context, me));
        Observable.merge(click, drag, release)
                .filter(e -> lineBtn.isSelected())
                .subscribe(me -> drawLine(context, me));

        /*Observable.merge(observable2, observable3) TODO: ONLY DOIN DRAWLINE
                .subscribe(me -> drawLine(context, me));*/


       /* JavaFxObservable.eventsOf(canvas, MouseEvent.MOUSE_DRAGGED)
                .takeWhile(e -> freehandBtn.isSelected())
                .subscribe(me -> drawLine(context, me));

        JavaFxObservable.eventsOf(canvas, MouseEvent.MOUSE_RELEASED)
                .takeWhile(e -> freehandBtn.isSelected())
                .subscribe(me -> drawLine(context, me));*/


        // Observable for handling mouse events on the canvas (painting)
        /*JavaFxObservable.eventsOf(canvas, MouseEvent.ANY)
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
                            System.out.println("Closed path");
                            context.lineTo(me.getX(), me.getY());
                            context.stroke();
                            context.closePath();
                        }
                    }
                });*/
    }
    // TODO: EXCEPTIONS!!!
    void startServer(TextArea networkText) throws IOException { //TODO: Remove debug stuff!
        discBtn.setDisable(false);
        connectBtn.setDisable(true);
        hostBtn.setDisable(true);
        if(server == null)
        server = new Server(12345);
        networkText.appendText("Start server!" + System.lineSeparator());
        Observable<Socket> cnn = server.clientConnections();
        cnn.subscribe(socket1 -> server.addToSocketList(socket1));
        cnn.map(Socket::getInputStream)
                .map(InputStreamReader::new)
                .map(BufferedReader::new)
                .map(BufferedReader::lines)
                .flatMap(stream -> Observable
                        .fromIterable(stream::iterator)).subscribeOn(Schedulers.io())
            .subscribe(s -> networkText.appendText("Data: " + s + System.lineSeparator()));
        cnn.subscribe(s -> System.out.println("Socket: " + s.toString()));
        //server.clientConnections()
        //        .subscribe(s -> networkText.appendText("Data: " + s + System.lineSeparator()));
        /*server.clientConnected()
                .subscribe(socket -> networkText.appendText("Client connected from: "
                                                                + socket.getRemoteSocketAddress()
                                                                + System.lineSeparator()));*/
                /*server.clientConnected()
                .doOnComplete(() -> System.out.println("COMPLETE!"))
                //.map(o -> (DrawObject)o)
                .subscribe(stream -> {
                    DrawObject draw = (DrawObject) stream.readObject();
                            networkText.appendText("Data: "
                                    + draw.getX() + ", " + draw.getY()
                                    + System.lineSeparator());
                        });*/
        //TODO: Fingers crossed this is it!
        /*server.clientConnected()
                .doOnComplete(() -> System.out.println("COMPLETE!"))
                .map(o -> (DrawObject)o)
                .subscribe(object -> {
                            networkText.appendText("Data: "
                                    + object.getX() + ", " + object.getY()
                                    + System.lineSeparator());
                        });*/
        /*server.drawHistory()
                .subscribe(s -> networkText.appendText(s + System.lineSeparator()),
                        s -> networkText.appendText("Error!" + System.lineSeparator()),
                        () -> networkText.appendText("Completed list!" + System.lineSeparator()));*/
        /*JavaFxObservable.actionEventsOf(test)
                .subscribe(s -> networkText.appendText("Change: " + s.toString() + System.lineSeparator()));*/
        /*Observable.(drawTools)
                .subscribe(s -> networkText.appendText("Tap!" + System.lineSeparator()));*/
    }

    void clientConnect(TextArea networkText) {
        try {
            socket = new Socket("localhost", 12345);
            //DataInputStream in = new DataInputStream(socket.getInputStream());

            networkText.appendText("Connected to server!" + System.lineSeparator());
            out = new DataOutputStream(socket.getOutputStream());
            //String test = "Test\n";
            out.writeUTF(new DrawObject(2.1, 4.4).toStreamableString());
            //out.reset();
            //out.flush();
            //out.writeObject(new DrawObject(20, 3));
            //out.reset();
            //out.flush();
            /*Observable.<String>create(e -> {
                networkText.appendText("Got: " + in.readUTF()); //TODO!!! GET CLIENT READING!!!!
            })
            .subscribe(s -> networkText.appendText("Recieved: " + s + System.lineSeparator()));*/
            discBtn.setDisable(false);
            connectBtn.setDisable(true);
            hostBtn.setDisable(true);
        } catch (IOException e) {
            networkText.appendText("Unable to connect to server!" + System.lineSeparator());
        }
    }

    void disconnect() throws IOException {
        if(socket != null) {
            socket.close();
        }
        if(server != null) {
            server.shutDown();
        }
        discBtn.setDisable(true);
        connectBtn.setDisable(false);
        hostBtn.setDisable(false);

    }

    void drawFreehand(GraphicsContext context, MouseEvent me) throws IOException {
        if(me.getEventType() == MouseEvent.MOUSE_PRESSED) {
            context.beginPath();
            context.lineTo(me.getX(), me.getY());
        } else if(me.getEventType() == MouseEvent.MOUSE_DRAGGED) {
            context.lineTo(me.getX(), me.getY());
            context.stroke();
        } else if(me.getEventType() == MouseEvent.MOUSE_RELEASED) {
            context.lineTo(me.getX(), me.getY());
            context.stroke();
            context.closePath();
        }
    }
    void drawRectangle(GraphicsContext context, MouseEvent me) throws IOException {
        if(me.getEventType() == MouseEvent.MOUSE_PRESSED) {
            rectangle.setX(me.getX());
            rectangle.setY(me.getY());
        } else if(me.getEventType() == MouseEvent.MOUSE_RELEASED) {

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
            context.strokeRect(rectangle.getX(), rectangle.getY(),
                                rectangle.getWidth(), rectangle.getHeight());
            if(socket != null) { // TODO: TEST SENDING OUTPUT FROM CLIENT!
                out.writeUTF(new DrawObject(10,10).toStreamableString());
                //out.flush();
                System.out.println("Client: DATA SENT!");
            }
            if(server != null) {
                System.out.println("Sockets list size: " + server.getSocketListSize());
            }
        }
    }
    void drawOval(GraphicsContext context, MouseEvent me) {
        if(me.getEventType() == MouseEvent.MOUSE_PRESSED) {
            oval.setCenterX(me.getX());
            oval.setCenterY(me.getY());
        } else if(me.getEventType() == MouseEvent.MOUSE_RELEASED) {
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

        }
    }
    void drawLine(GraphicsContext context, MouseEvent me) {
        if(me.getEventType() == MouseEvent.MOUSE_PRESSED) {
            line.setStartX(me.getX());
            line.setStartY(me.getY());
        } else if(me.getEventType() == MouseEvent.MOUSE_RELEASED) {
            line.setEndX(me.getX());
            line.setEndY(me.getY());
            context.strokeLine(line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY());
        }
    }
}
