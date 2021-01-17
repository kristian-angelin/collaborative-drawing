import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
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
import javafx.util.Pair;
import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Drawing program where a server can be started and clients connect to it
 * creating a shared drawing board.
 *
 * @author  Kristian Angelin
 * @version 1.0
 * @since   2021-01-17
 */

public class CollaborativeDrawing extends Application {
    private Canvas canvas;
    private GraphicsContext context;
    private final ToggleGroup drawTools = new ToggleGroup();

    // Shapes used for drawing
    private final DrawRectangle rectangle = new DrawRectangle();
    private final DrawOval oval = new DrawOval();
    private final DrawLine line = new DrawLine();
    private DrawCleanCanvas cleanCanvas;

    // Store draw points for free hand drawing
    private final ArrayList<Double> xPoints = new ArrayList<>();
    private final ArrayList<Double> yPoints = new ArrayList<>();

    // Variables for network connections
    private Server server;
    private Client client;
    private boolean isClient = false;
    private boolean isServer = false;
    TextArea statusText = new TextArea(); // Field for network text

    private CompositeDisposable compositeDisposable;

    private final Button connectBtn = new Button("Connect");
    private final Button hostBtn = new Button("Host");
    private final Button discBtn = new Button("Disconnect");

    // Constants
    private static final int MIN_PORT_RANGE = 257;
    private static final int MAX_PORT_RANGE = 65535;

    // Main
    public static void main(String[] args) {
        launch(args);
    }

    // Start JavaFx application
    @Override
    public void start(Stage primaryStage) {
        compositeDisposable = new CompositeDisposable();
        // Setup the paintable canvas
        canvas = new Canvas(650, 600);
        cleanCanvas = new DrawCleanCanvas(canvas.getWidth(), canvas.getHeight());
        context = canvas.getGraphicsContext2D();

        // Create buttons for draw shape selection
        ToggleButton rectangleBtn = new ToggleButton("Rectangle");
        ToggleButton ovalBtn = new ToggleButton("Oval");
        ToggleButton lineBtn = new ToggleButton("Line");
        ToggleButton freehandBtn = new ToggleButton("Freehand");

        rectangleBtn.setToggleGroup(drawTools);
        ovalBtn.setToggleGroup(drawTools);
        lineBtn.setToggleGroup(drawTools);
        freehandBtn.setToggleGroup(drawTools);

        drawTools.selectToggle(rectangleBtn);

        ColorPicker colorPicker = new ColorPicker(Color.BLACK); // Color picker with default color
        setShapeColors(colorPicker.getValue()); // Set default shape color

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

        statusText.setEditable(false);

        discBtn.setDisable(true);

        HBox connectionBox = new HBox(20);
        connectionBox.setPadding(new Insets(10));
        connectionBox.getChildren().addAll(connectBtn, hostBtn, discBtn, statusText);
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
                    setShapeColors(colorPicker.getValue());
                });

        // Observable checking the slider for stroke size selection
        JavaFxObservable.valuesOf(slider.valueProperty())
                .subscribe(value -> {
                    context.setLineWidth((Double) value);
                    sliderLabel.setText("Stroke size: " + String.format("%.1f", value)); // String.format() for showing only one decimal.
                    setShapeStrokeWidth((Double) value);
                });

        // Observable clearing canvas on clear canvas button press
        JavaFxObservable.actionEventsOf(clearCanvasBtn)
                .subscribe(e -> {
                    context.clearRect(0,0, canvas.getWidth(),canvas.getHeight());
                    sendDrawObject(cleanCanvas);
                });

        // Buttons clicks for network
        JavaFxObservable.actionEventsOf(connectBtn)
                .subscribe(e -> clientConnect());
        JavaFxObservable.actionEventsOf(hostBtn)
                .subscribe(e -> startServer());
        JavaFxObservable.actionEventsOf(discBtn)
                .subscribe(e -> disconnect());

        // Mouse events on the canvas
        Observable<MouseEvent> click = JavaFxObservable.eventsOf(canvas, MouseEvent.MOUSE_PRESSED);
        Observable<MouseEvent> drag = JavaFxObservable.eventsOf(canvas, MouseEvent.MOUSE_DRAGGED);
        Observable<MouseEvent> release = JavaFxObservable.eventsOf(canvas, MouseEvent.MOUSE_RELEASED);

        // Merge mouse events observables
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

    // Starts a server accepting client connections
    private void startServer() {
        int port = startServerDialog(); // Returns 0 on error
        if(port != 0) {
            try {
                server = new Server(port);
                isServer = true;

                statusText.appendText("Server started on " + InetAddress.getLocalHost().getHostAddress() + " on port: " + port + System.lineSeparator());

                compositeDisposable.add(server.getObjectStream()
                        .subscribe(drawObject -> {
                                    drawObject.toCanvas(context);
                                },
                                throwable -> {
                                    // runLater() on JavaFX thread
                                    Platform.runLater(() -> statusText.appendText("Server shutdown!" + System.lineSeparator()));
                                    System.out.println("Server shutdown: " + throwable.toString());
                                }));
                // Enable/disable buttons
                discBtn.setDisable(false);
                connectBtn.setDisable(true);
                hostBtn.setDisable(true);
            } catch (IOException e) {
                statusText.appendText("Unable to start server!" + System.lineSeparator());
                System.err.println(e.toString());
            }
        }
    }

    // Starts a client connection to a server
    private void clientConnect() {
        // Connect to server
        Pair<String, Integer> ipAddressAndPort = joinServerDialog();
        if(ipAddressAndPort != null) {
            try {
                // Create server connection
                client = new Client(ipAddressAndPort.getKey(), ipAddressAndPort.getValue());
                isClient = true;
                statusText.appendText("Connected to the server: " + client.getServerAddress() + System.lineSeparator());
                // Get server stream of drawObjects
                compositeDisposable.add(client.serverStream().subscribe(drawObject -> {
                            drawObject.toCanvas(context);
                        },
                        throwable -> {
                            // runLater() on JavaFX thread
                            Platform.runLater(() -> statusText.appendText("Disconnected from server!" + System.lineSeparator()));
                            System.out.println("Connection to server lost: " + throwable.toString());
                        }));
                // Enable/disable buttons
                discBtn.setDisable(false);
                connectBtn.setDisable(true);
                hostBtn.setDisable(true);
            } catch (IOException e) {
                statusText.appendText("Unable to connect to server!" + System.lineSeparator());
                System.err.println(e.toString());
            }
        }
    }

    // Creates a dialog window for starting a server
    private int startServerDialog() {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Create server");
        dialog.setHeaderText("Enter port number! (" + MIN_PORT_RANGE + "-" + MAX_PORT_RANGE + ")");

        // Set the button types.
        ButtonType createServerButton = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createServerButton, ButtonType.CANCEL);

        // Disable create button
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

    // Creates a dialog window for connecting to a server
    private Pair<String, Integer> joinServerDialog() {
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
        TextField port = new TextField();

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
        dialog.setResultConverter(buttonType -> {
            if (buttonType == joinServerButton) {
                return new Pair<String, Integer>(ip.getText(), Integer.valueOf(port.getText()));
            }
            return null;
        });
        // Wait for button press to get result
        dialog.showAndWait();
        return dialog.getResult();
    }

    // Connection disconnect
    private void disconnect() {
        if(isClient) {
            client.disconnect();
            isClient = false;
            statusText.appendText("Disconnected from server!" + System.lineSeparator());
        }
        if(isServer) {
            server.shutDown();
            isServer = false;
            statusText.appendText("Server shutdown!" + System.lineSeparator());
        }
        compositeDisposable.clear();
        discBtn.setDisable(true);
        connectBtn.setDisable(false);
        hostBtn.setDisable(false);
    }

    // Draw free hand from mouse events
    private void drawFreehand(MouseEvent me) {
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

    // Draw rectangle from mouse events
    private void drawRectangle(MouseEvent me) {
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

    // Draw oval from mouse events
    private void drawOval(MouseEvent me) {
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

    // Draw line from mouse events
    private void drawLine(MouseEvent me) {
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

    // Send drawObject to server/clients
    private void sendDrawObject(DrawObject drawObject) {
        if(isClient) {
            client.sendToServer(drawObject);
        }
        if(isServer) {
            server.sendToClients(drawObject);
        }
    }

    // Set color of shapes
    private void setShapeColors(Paint color) {
        rectangle.setColor(color);
        oval.setColor(color);
        line.setColor(color);
    }
    // Set the stroke width of shapes
    private void setShapeStrokeWidth(double width) {
        rectangle.setStrokeWidth(width);
        oval.setStrokeWidth(width);
        line.setStrokeWidth(width);
    }
}
