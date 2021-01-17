import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class CollaborativeDrawing extends Application {
    private Canvas canvas;
    private GraphicsContext context;
    private final ToggleGroup drawTools = new ToggleGroup(); //TODO: Should we use this here?

    // Shapes used for drawing
    private final DrawRectangle rectangle = new DrawRectangle();
    private final DrawOval oval = new DrawOval();
    private final DrawLine line = new DrawLine();
    //private final DrawFreeHand freeHand = new DrawFreeHand();
    private DrawCleanCanvas cleanCanvas;
    // Store draw points for freehand drawing
    private final ArrayList<Double> xPoints = new ArrayList<>();
    private final ArrayList<Double> yPoints = new ArrayList<>();
;
    //TODO: REMOVE DEBUG
    private Server server;
    private Client client;
    boolean isClient = false;
    boolean isServer = false;
    private Disposable serverDisposable;
    private Disposable clientDisposable;
    private CompositeDisposable compositeDisposable;
    private Socket socket;
    private DataOutputStream out;

    private final Button connectBtn = new Button("Connect");
    private final Button hostBtn = new Button("Host");
    private final Button discBtn = new Button("Disconnect");

    private String port;
    private String ip;
    private static final int MIN_PORT_RANGE = 257;
    private static final int MAX_PORT_RANGE = 65535;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        compositeDisposable = new CompositeDisposable();
        // Setup the paintable canvas
        canvas = new Canvas(650, 600);
        cleanCanvas = new DrawCleanCanvas(canvas.getWidth(), canvas.getHeight());
        //GraphicsContext context;
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
        updateShapeColors(colorPicker.getValue()); // Set default shape color

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
                .subscribe(e -> {
                    context.setStroke(colorPicker.getValue());
                    updateShapeColors(colorPicker.getValue());
                });

        // Observable checking the slider for stroke size selection
        JavaFxObservable.valuesOf(slider.valueProperty())
                .subscribe(value -> {
                    context.setLineWidth((Double) value);
                    sliderLabel.setText("Stroke size: " + String.format("%.1f", value)); // String.format() for showing only one decimal.
                    updateShapeStrokeWidth((Double) value);
                });

        // Observable clearing canvas on clear canvas button press
        JavaFxObservable.actionEventsOf(clearCanvasBtn)
                .subscribe(e -> {
                    context.clearRect(0,0, canvas.getWidth(),canvas.getHeight());
                    sendDrawObject(cleanCanvas);
                });

        JavaFxObservable.actionEventsOf(connectBtn)
                .subscribe(e -> clientConnect(networkText));
        JavaFxObservable.actionEventsOf(hostBtn)
                .subscribe(e -> startServer(networkText));
        JavaFxObservable.actionEventsOf(discBtn)
                .subscribe(e -> disconnect()); //TODO: FIX addrow test


        // TODO: CONTINUE NEW WAY OF HANDLING MOUSE EVENTS
        Observable<MouseEvent> click = JavaFxObservable.eventsOf(canvas, MouseEvent.MOUSE_PRESSED);
        Observable<MouseEvent> drag = JavaFxObservable.eventsOf(canvas, MouseEvent.MOUSE_DRAGGED);
        Observable<MouseEvent> release = JavaFxObservable.eventsOf(canvas, MouseEvent.MOUSE_RELEASED);


        Observable.merge(click, drag, release)
                .filter(e -> freehandBtn.isSelected())
                .subscribe(me -> drawFreehand(me));
        Observable.merge(click, drag, release)
                .filter(e -> rectangleBtn.isSelected())
                .subscribe(me -> drawRectangle(me));
        Observable.merge(click, drag, release)
                .filter(e -> ovalBtn.isSelected())
                .subscribe(me -> drawOval(me));
        Observable.merge(click, drag, release)
                .filter(e -> lineBtn.isSelected())
                .subscribe(me -> drawLine(me));

    }

    int startServerDialog() {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Create server");
        dialog.setHeaderText("Enter port number! (" + MIN_PORT_RANGE + "-" + MAX_PORT_RANGE + ")");

        // Set the button types.
        ButtonType createServerButton = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createServerButton, ButtonType.CANCEL);

        // Disable button
        Button createButton = (Button) dialog.getDialogPane().lookupButton(createServerButton);
        createButton.setDisable(true);

        // Create field and label.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField port = new TextField();

        grid.add(new Label("Port:"), 0, 1);
        grid.add(port, 1, 1);
        dialog.getDialogPane().setContent(grid);


        // Check that we have a correct port number before enabling "Create" button
        port.textProperty().addListener((observable, oldValue, newValue) -> {
            // Check that we dont try to parseInt on empty String
            if(newValue.length() > 0) {
                createButton.setDisable(Integer.parseInt(newValue) < MIN_PORT_RANGE || Integer.parseInt(newValue) > MAX_PORT_RANGE);
            }
        });

        // Convert result
        dialog.setResultConverter(buttonType -> {
            if (buttonType == createServerButton) {
                if(port.getText() != null) {
                    return Integer.valueOf(port.getText());
                }
            }
            return 0; // Return 0 if cancel is pressed
        });

        // Wait for button press to get result
        dialog.showAndWait();
        return dialog.getResult();
    }

    Pair<String, Integer> joinServerDialog() {
        Dialog<Pair<String, Integer>> dialog = new Dialog<>();
        dialog.setTitle("Connect to server");
        dialog.setHeaderText("Enter ip address and port number! (Port number: " + MIN_PORT_RANGE + "-" + MAX_PORT_RANGE + ")");

        // Set the button types.
        ButtonType joinServerButton = new ButtonType("Join", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(joinServerButton, ButtonType.CANCEL);

        // Create the ip address and port labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField ip = new TextField();
        //ip.setPromptText("Localhost");
        TextField port = new TextField();
        //port.setPromptText("Password");

        grid.add(new Label("IP address:"), 0, 0);
        grid.add(ip, 1, 0);
        grid.add(new Label("Port:"), 0, 1);
        grid.add(port, 1, 1);
        dialog.getDialogPane().setContent(grid);

        // Disable join button
        Button joinButton = (Button) dialog.getDialogPane().lookupButton(joinServerButton);
        joinButton.setDisable(true);

        // Check that we have a correct port number before enabling "Join" button
        port.textProperty().addListener((observable, oldValue, newValue) -> {
            // Check that we dont try to parseInt on empty String
            if(newValue.length() > 0) {
                joinButton.setDisable(Integer.parseInt(newValue) < MIN_PORT_RANGE || Integer.parseInt(newValue) > MAX_PORT_RANGE);
            }
        });

        // Convert result
        dialog.setResultConverter(new Callback<ButtonType, Pair<String, Integer>>() {
            @Override
            public Pair<String, Integer> call(ButtonType buttonType) {
                if (buttonType == joinServerButton) {
                    return new Pair<String, Integer>(ip.getText(), Integer.valueOf(port.getText()));
                }
                return null;
            }
        });

        dialog.showAndWait();
        return dialog.getResult();
    }
    // TODO: EXCEPTIONS!!!
    void startServer(TextArea networkText) { //TODO: Remove debug stuff!
        int port = startServerDialog();
        if(port != 0) {

            //System.out.println("Port: " + port);
            try {

                //System.out.println("[EXECUTING] new Server()");
                server = new Server(port);
                isServer = true;
                //server.startServer();

                networkText.appendText("Server started on " + InetAddress.getLocalHost().getHostAddress() + " on port: " + port + System.lineSeparator());

                compositeDisposable.add(server.getObjectStream() // Compose to get ObservableTransformer
                        .subscribe(drawObject -> {
                                    //networkText.appendText("Data: " + drawObject.toString() + System.lineSeparator());
                                    System.out.println("[COLLADRAW RECEIVED]" + drawObject.toString() + System.lineSeparator());
                                    //server.sendToClients(drawObject);
                                    drawObject.toCanvas(context);
                                },
                                throwable -> {
                                    // runLater() on JavaFX thread
                                    Platform.runLater(() -> networkText.appendText("Server shutdown!" + System.lineSeparator()));
                                    //networkText.appendText("Server shutdown!" + System.lineSeparator());
                                    System.out.println("Server shutdown: " + throwable.toString());
                                }));
                discBtn.setDisable(false);
                connectBtn.setDisable(true);
                hostBtn.setDisable(true);
            } catch (IOException e) {
                networkText.appendText("Unable to start server!" + System.lineSeparator());
                System.err.println(e.toString());
            }
        }
    }

    void clientConnect(TextArea networkText) {
        // Connect to server
        Pair<String, Integer> ipAdressAndPort = joinServerDialog();
        if(ipAdressAndPort != null) {
            try {
                //client = new Client("localhost", 12345);
                System.out.println("[1. CONNECT] Before new client!");
                client = new Client(ipAdressAndPort.getKey(), ipAdressAndPort.getValue());
                System.out.println("[2. CONNECT] After new client!");
                networkText.appendText("Connected to server!" + System.lineSeparator());
                System.out.println("[3. CONNECT] After networkText!");
                isClient = true;
                System.out.println("[4. CONNECT] is Client!");

                compositeDisposable.add(client.serverStream().subscribe(drawObject -> {
                            Platform.runLater(() -> networkText.appendText("Data: " + drawObject.toString() + System.lineSeparator()));
                            //networkText.appendText("Data: " + drawObject.toString() + System.lineSeparator());
                            System.out.println("[RECEIVED]" + drawObject.toString() + System.lineSeparator());
                            drawObject.toCanvas(context);
                        },
                        throwable -> {
                            Platform.runLater(() -> networkText.appendText("Disconnected from server!" + System.lineSeparator()));
                            //networkText.appendText("Disconnected from server!" + System.lineSeparator());
                            System.out.println("Connection to server lost: " + throwable.toString());
                        }));
                System.out.println("[5. CONNECT] After subscribe!");

                discBtn.setDisable(false);
                connectBtn.setDisable(true);
                hostBtn.setDisable(true);
            } catch (IOException e) {
                networkText.appendText("Unable to connect to server!" + System.lineSeparator());
                System.err.println(e.toString());
            }
        }
    }

    void disconnect() {
        if(isClient) {
            //clientDisposable.dispose();
            client.disconnect();
            //client = null;
            isClient = false;
            //clientDisposable.dispose();
        }
        if(isServer) {
            //serverDisposable.dispose();
            server.shutDown();
            isServer = false;
            //server = null;
            //serverDisposable.dispose();
        }
        compositeDisposable.clear();
        discBtn.setDisable(true);
        connectBtn.setDisable(false);
        hostBtn.setDisable(false);

    }

    void drawFreehand(MouseEvent me) {
        if(me.getEventType() == MouseEvent.MOUSE_PRESSED) {
            xPoints.clear();
            yPoints.clear();
            xPoints.add(me.getX());
            yPoints.add(me.getY());

            context.beginPath();
            context.lineTo(me.getX(), me.getY());
            context.stroke();
        } else if(me.getEventType() == MouseEvent.MOUSE_DRAGGED) {
            xPoints.add(me.getX());
            yPoints.add(me.getY());

            context.lineTo(me.getX(), me.getY());
            context.stroke();
        } else if(me.getEventType() == MouseEvent.MOUSE_RELEASED) {
            xPoints.add(me.getX());
            yPoints.add(me.getY());

            context.lineTo(me.getX(), me.getY());
            context.stroke();
            context.closePath();

            sendDrawObject(new DrawFreeHand(xPoints, yPoints, context.getStroke()));
        }
    }

    void drawRectangle(MouseEvent me) {
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
            rectangle.toCanvas(context);
            sendDrawObject(new DrawRectangle(rectangle));
        }
    }
    void drawOval(MouseEvent me) {
        if(me.getEventType() == MouseEvent.MOUSE_PRESSED) {
            oval.setX(me.getX());
            oval.setY(me.getY());
        } else if(me.getEventType() == MouseEvent.MOUSE_RELEASED) {
            // Use Math.abs to handle negative numbers
            oval.setRadiusX(Math.abs(me.getX() - oval.getX()));
            oval.setRadiusY(Math.abs(me.getY() - oval.getY()));

            // Check if shape is was drawn to a negative coordinates
            if(oval.getX() > me.getX()) {
                oval.setX(me.getX());
            }
            if(oval.getY() > me.getY()) {
                oval.setY(me.getY());
            }

            oval.toCanvas(context);
            sendDrawObject(new DrawOval(oval));
        }
    }

    void drawLine(MouseEvent me) {
        if(me.getEventType() == MouseEvent.MOUSE_PRESSED) {
            line.setX(me.getX());
            line.setY(me.getY());
        } else if(me.getEventType() == MouseEvent.MOUSE_RELEASED) {
            line.setEndX(me.getX());
            line.setEndY(me.getY());
            // Draw line and send object
            line.toCanvas(context);
            sendDrawObject(new DrawLine(line));
        }
    }

    // Sends object to server or clients
    void sendDrawObject(DrawObject drawObject) {
        if(isClient) {
            client.sendToServer(drawObject);
            //System.out.println("[SEND] " + rectangle.toString());
        }
        if(server != null) {
            server.sendToClients(drawObject);
            System.out.println("Sockets list size: " + server.getSocketListSize());
            //System.out.println("[SEND] " + rectangle.toString());
        }
    }

    void updateShapeColors(Paint color) {
        rectangle.setColor(color);
        oval.setColor(color);
        line.setColor(color);
        //freeHand.setColor(color);
    }
    void updateShapeStrokeWidth(double width) {
        rectangle.setStrokeWidth(width);
        oval.setStrokeWidth(width);
        line.setStrokeWidth(width);
        //freeHand.setStrokeWidth(width);
    }
}
