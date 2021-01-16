import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private Observable<Socket> clients;
    private final ServerSocket serverSocket;
    Observable<DrawObject> drawObjectStream;
    //private DataOutputStream out;
    //private OutputStreamWriter out;
    //private List<String> listString;
    private boolean online = true;
    //Socket socket;
    //private final List<Socket> clientList;
    //private final ConcurrentHashMap<Socket, OutputStreamWriter> list;
    private final ConcurrentHashMap<Socket, ObjectOutputStream> list;
    private Disposable clientDisposable;
    private ArrayList<DrawObject> objectsCache;

    Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        list = new ConcurrentHashMap<>();
        objectsCache = new ArrayList<>();
        startServer();
    }

    void startServer() {
        clients = clientConnections();
        drawObjectStream = clients.compose(getStream()).share();
        clientDisposable = clients.subscribe(socket -> {
                list.put(socket,new ObjectOutputStream(socket.getOutputStream()));
                sendDrawHistory(socket);
            },
            throwable -> System.out.println("Server shutdown: " + throwable.toString()));
        /*clients.compose(getStream()).subscribe(drawObject -> System.out.println("[COLLADRAW RECEIVED]" + drawObject.toString() + System.lineSeparator()),
                throwable -> System.out.println("Server shutdown: " + throwable.toString()));*/
        getObjectStream().subscribe(drawObject -> {
            sendToClients(drawObject);
            objectsCache.add(drawObject);
            System.out.println("[COLLADRAW RECEIVED]" + drawObject.toString() + System.lineSeparator());
        },
            throwable -> System.out.println("Server shutdown: " + throwable.toString()));
    }

    void sendDrawHistory(Socket socket) {
        for(DrawObject obj : objectsCache) {
            try {
                list.get(socket).writeObject(obj);
                list.get(socket).reset();
                System.out.println("[SENT]" + obj.toString() + System.lineSeparator());
            } catch (IOException e) {
                //statusText.appendText("Client disconnected!" + System.lineSeparator());
                list.remove(socket);
            }
        }
    }

    // Observable for accepting connections
    Observable<Socket> clientConnections() { //TODO should be private
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
                }).subscribeOn(Schedulers.io())// Accepts connections correctly, but subscribe gets
                .flatMap(s -> Observable.just(s)
                        .subscribeOn(Schedulers.io()))
                .share();
    }

    Observable<DrawObject> getObjectStream() {
        return drawObjectStream;
    }

    //public static ObservableTransformer<Socket, String> getStream() {
    public static ObservableTransformer<Socket, DrawObject> getStream() { // TODO: Test removing share() when all works
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
                            //or.close();
                            //e.onError(ex);
                            System.err.println("Socket closed: " + ex);
                        }
                    } catch (Exception ex) {
                        //if (!emission.isDisposed()) {
                            System.err.println("[EXCEPTION] stream.readObject: " + ex.toString());
                            //stream.close();
                            emission.onError(ex);
                        //}
                    }
                }).subscribeOn(Schedulers.io()))
                .map(object -> (DrawObject)object)
                .doOnNext(drawObject -> System.out.println("[S_RECEIVED]"
                        + drawObject.toString() + " [THREAD]"
                        + Thread.currentThread().getName()
                        + System.lineSeparator()));
    }

    void shutDown() {
        online = false;
        clientDisposable.dispose();
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Socket already closed!");
        }
    }

    int getSocketListSize() {
        //return clientList.size();
        return list.size();
    }

    void addToSocketList(Socket sock) throws IOException {
        //clientList.add(sock);
        list.put(sock,new ObjectOutputStream(sock.getOutputStream()));
        System.out.println("Socket sent to list!");
    }

    void sendToClients(DrawObject drawObject) { // TODO: Not entirely sure this works 100% is get(sock) ok? use index instead?
        //System.out.println("Sent to clients! Thread: " + Thread.currentThread().getName());
        objectsCache.add(drawObject);
        System.out.println("[SEND]" + drawObject.toString() + System.lineSeparator());
        for(Socket sock: list.keySet()) {
            try {
                list.get(sock).writeObject(drawObject);
                list.get(sock).reset();
                //System.out.println("[SENT]" + drawObject.toString() + System.lineSeparator());
                //list.get(sock).flush(); // TODO: Needed?
            } catch (IOException e) {
                list.remove(sock);
            }
        }
    }

    Observable<Socket> getClientsVar(){
        return clients;
    }

    ServerSocket getServerSocket() {return serverSocket;} // TODO: REMOVE DEBUG
}

