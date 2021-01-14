import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.operators.observable.ObservableEmpty;
import io.reactivex.schedulers.Schedulers;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class Server {

    private Observable<Socket> clients;
    private final ServerSocket serverSocket;
    //private DataOutputStream out;
    //private OutputStreamWriter out;
    //private List<String> listString;
    private boolean online = true;
    //Socket socket;
    //private final List<Socket> clientList;
    //private final ConcurrentHashMap<Socket, OutputStreamWriter> list;
    private final ConcurrentHashMap<Socket, ObjectOutputStream> list;
    private Disposable disp;

    Server(int port) throws IOException {
        //System.out.println("[EXECUTING] new serverSocket()");
        serverSocket = new ServerSocket(port);
        list = new ConcurrentHashMap<>();
        //System.out.println("[EXECUTING] server.startServer()");
        startServer();
    }

    void startServer() {
        //System.out.println("[EXECUTING] clientConnections()");
        clients = clientConnections();
        //(System.out.println("[EXECUTING] clients.subscribe()");
        disp = clients.subscribe(this::addToSocketList,
                throwable -> System.out.println("Server shutdown: " + throwable.toString()));
        //System.out.println("Server local address: " + serverSocket.getLocalSocketAddress());
        //System.out.println("Server inet address: " + serverSocket.getInetAddress());
    }

    // Observable for accepting connections
    Observable<Socket> clientConnections() {
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


    //public static ObservableTransformer<Socket, String> getStream() {
    public static ObservableTransformer<Socket, DrawObject> getStream() {
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
        disp.dispose();
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

