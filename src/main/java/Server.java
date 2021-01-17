import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class for creating a server accepting client connections. Reads a
 * stream of drawObjects sent from clients and resends the objects
 * to the rest of the clients.
 *
 * @author  Kristian Angelin
 * @version 1.0
 * @since   2021-01-17
 */

public class Server {

    private Observable<DrawObject> drawObjectStream;
    private Disposable clientDisposable;
    private final ServerSocket serverSocket;
    private final ConcurrentHashMap<Socket, ObjectOutputStream> socketOutput;
    private final ArrayList<DrawObject> objectsCache;
    private boolean online = true;

    Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        socketOutput = new ConcurrentHashMap<>();
        objectsCache = new ArrayList<>();
        startServer();
    }

    // Starts the server
    private void startServer() {
        Observable<Socket> clients = clientConnections();
        drawObjectStream = clients.compose(getStream()).share();
        // Accept client connections
        clientDisposable = clients.subscribe(socket -> {
            socketOutput.put(socket,new ObjectOutputStream(socket.getOutputStream()));
            sendDrawHistory(socket);
        },
            throwable -> System.out.println("Server shutdown: " + throwable.toString()));

        // Read the drawObject stream
        getObjectStream().subscribe(drawObject -> {
            sendToClients(drawObject);
            objectsCache.add(drawObject);
        },
            throwable -> System.out.println("Server shutdown: " + throwable.toString()));
    }

    // Send all previous objects sent to server (history)
    private void sendDrawHistory(Socket socket) {
        for(DrawObject obj : objectsCache) {
            try {
                socketOutput.get(socket).writeObject(obj);
                socketOutput.get(socket).reset();
            } catch (IOException e) {
                socketOutput.remove(socket);
            }
        }
    }

    // Observable for accepting connections
    private Observable<Socket> clientConnections() {
        return Observable
                .<Socket>create(e -> {
                    try{
                        while (online) {
                            System.out.println("Accepting a connection...");
                            e.onNext(serverSocket.accept());
                        }
                    } catch (IOException ex) {
                        if(!e.isDisposed()) {
                            e.onError(ex);
                        }
                    }
                }).subscribeOn(Schedulers.io())
                .flatMap(s -> Observable.just(s)
                        .subscribeOn(Schedulers.io()))
                .share();
    }

    // Get the object stream Observable
    public Observable<DrawObject> getObjectStream() {
        return drawObjectStream;
    }

    // ObservableTransformer for getting the drawObject stream.
    private static ObservableTransformer<Socket, DrawObject> getStream() {
        return up -> up
                .map(Socket::getInputStream)
                .map(ObjectInputStream::new)
                .flatMap(stream -> Observable.create(emission -> {
                    // Look for error on reading objects from a socket, such as client disconnect
                    try{
                        while(true){
                            emission.onNext(stream.readObject());
                        }
                    } catch (EOFException ex) {
                        if(!emission.isDisposed()) {
                            System.err.println("Socket closed: " + ex);
                        }
                    } catch (Exception ex) {
                        emission.onError(ex);

                    }
                }).subscribeOn(Schedulers.io()))
                .map(object -> (DrawObject)object);
    }

    // Shutdown and clean up server
    public void shutDown() {
        online = false;
        clientDisposable.dispose();
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Socket already closed!");
        }
    }

    // Send drawObject to all connected clients
    public void sendToClients(DrawObject drawObject) {
        objectsCache.add(drawObject);
        for(Socket sock: socketOutput.keySet()) {
            try {
                socketOutput.get(sock).writeObject(drawObject);
                socketOutput.get(sock).reset();
            } catch (IOException e) {
                socketOutput.remove(sock);
            }
        }
    }
}

