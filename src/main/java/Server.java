import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private Observable<Socket> clients;
    private int port;
    private final ServerSocket serverSocket;
    private DataOutputStream out;
    //private List<String> listString;
    private boolean online = true;
    //Socket socket;
    private final List<Socket> clientList;

    Server(int port) throws IOException {
        //this.port = port;
        serverSocket = new ServerSocket(port);
        clientList = new ArrayList<>();
    }

    void startServer() throws IOException {
        //clientList = new ArrayList<>();
        //serverSocket = new ServerSocket(port);
        clients = clientConnections();
        clients.subscribe(this::addToSocketList);
        //getStream().subscribe(this::sendToClients);
        sendToClients();
    }
    /*// Observable for accepting connections
    Observable<ObjectInputStream> clientConnected() throws IOException {
        online = true;
        return Observable
                .<Socket>create(e -> {
                    while(online) {
                        Socket socket;
                        System.out.println("Accepting a connection...");
                        e.onNext(socket = serverSocket.accept());
                        //out = new DataOutputStream(socket.getOutputStream());
                        //out.writeUTF("Hej " + socket.getRemoteSocketAddress());
                        //out.writeObject(drawHistory().subscribe());
                    }
                })
                //.doOnComplete(() -> System.out.println("COMPLETE!"))
                .map(Socket::getInputStream)
                //.map(BufferedInputStream::new)
                .map(ObjectInputStream::new)
                //.map(Iterable::)
                //.map(ObjectInputStream::readObject)
                //.map(BufferedInputStream::)
                //.map(BufferedInputStream::new)
                //.map(BufferedInputStream::new)
                //.flatMap(s -> Observ)
                //.flatMap(s -> Observable.fromIterable(s::readObject.)).subscribeOn(Schedulers.io())
                .subscribeOn(Schedulers.io());
    }*/

    // Observable for accepting connections
    Observable<Socket> clientConnections() throws IOException {
        return Observable
                .<Socket>create(e -> {
                    while (online) {
                        System.out.println("Accepting a connection...");
                        e.onNext(serverSocket.accept());
                    }
                }).share().subscribeOn(Schedulers.io())// Accepts connections correctly, but subscribe gets
                .flatMap(s -> Observable.just(s)
                        .subscribeOn(Schedulers.io()))
                .observeOn(Schedulers.io());
                /*}).subscribeOn(Schedulers.io()) // Accepts connections correctly, but subscribe gets
                .flatMap(s -> Observable.just(s)    // blocked by other subscribes
                        .subscribeOn(Schedulers.io())).observeOn(Schedulers.io()).share();*/
                //}).subscribeOn(Schedulers.io()); // Only gets random sub executed
                //}).subscribeOn(Schedulers.io()).share(); // Blocks after 1 accept. Exit prints rest.
        /*return Observable
                .<Socket>create(e -> {
                    while (online) {
                        System.out.println("Accepting a connection...");
                        e.onNext(socket = serverSocket.accept());
                        //clientList.add(socket);
                        //System.out.println("Added connection to list! Total: " + clientList.size());
                        //out = new DataOutputStream(socket.getOutputStream());
                        //out.writeUTF("Hej " + socket.getRemoteSocketAddress());
                        //out.writeObject(drawHistory().subscribe());
                    }
                })
                //.toList(clientList.add()) TODO?!?!?!?!?!
                .map(Socket::getInputStream)
                .map(InputStreamReader::new)
                .map(BufferedReader::new)
                .map(BufferedReader::lines)
                .flatMap(stream -> Observable
                        .fromIterable(stream::iterator)).subscribeOn(Schedulers.io())
                .subscribeOn(Schedulers.io());*/
    }

    /*void startStream() throws IOException {
        clientConnections()
                //.flatMap(s -> Observable.just(s)
                //.subscribeOn(Schedulers.io())
                .subscribe(socket -> { clientList.add(socket);
                                    System.out.println("Added connection to list! Total: " + clientList.size());

        });
    }*/

    Observable<String> getStream() throws IOException {
        return clients
                .map(Socket::getInputStream)
                .map(InputStreamReader::new)
                .map(BufferedReader::new)
                .map(BufferedReader::lines)
                .flatMap(stream -> Observable
                        .fromIterable(stream::iterator).subscribeOn(Schedulers.io()))
                .share()
                .observeOn(Schedulers.io());
                //.subscribeOn(Schedulers.io())
                //.replay()
                //.autoConnect();

        //.publish().autoConnect();
    }

    void shutDown() throws IOException {
        online = false;
        serverSocket.close();
    }

    /*Socket acceptSocket() throws IOException {
        this.socket = serverSocket.accept();
        clientList.add(socket);
        System.out.println("Added connection to list! Total: " + clientList.size());
        return this.socket;
    }*/
    int getSocketListSize() {
        return clientList.size();
    }

    void addToSocketList(Socket sock) {
        clientList.add(sock);
    }

    void sendToClients() throws IOException {
        getStream().subscribe(data ->
                {
                    System.out.println("Sent to clients! Thread: " + Thread.currentThread().getName());
                    for(Socket s: clientList) {
                        out = new DataOutputStream(s.getOutputStream());
                        out.writeUTF(data);
                    }
                }
        );
    }
}
    /*Observable<DataInputStream> dataStream() {
        return Observable.
                <DataInputStream>create(e -> {
                   clientConnected().map(Socket::getInputStream);
                });
    }*/
    // Observable for accepting connections
    /*Observable<Socket> clientConnected() throws IOException {
        return Observable
                .<Socket>create(e -> {
                    while(true) {
                        Socket socket;
                        System.out.println("Accepting a connection...");
                        e.onNext(socket = serverSocket.accept());
                        out = new DataOutputStream(socket.getOutputStream());
                        out.writeUTF("Hej " + socket.getRemoteSocketAddress());
                        //out.writeObject(drawHistory().subscribe());
                    }
                })
                //.map(Socket::getInputStream)
                //.map(InputStreamReader::new)
                .subscribeOn(Schedulers.io());
    }*/
