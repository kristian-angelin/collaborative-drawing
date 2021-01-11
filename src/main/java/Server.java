import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
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
    private int port;
    private final ServerSocket serverSocket;
    //private DataOutputStream out;
    //private OutputStreamWriter out;
    //private List<String> listString;
    private boolean online = true;
    //Socket socket;
    //private final List<Socket> clientList;
    //private final ConcurrentHashMap<Socket, OutputStreamWriter> list;
    private final ConcurrentHashMap<Socket, ObjectOutputStream> list;

    Server(int port) throws IOException {
        //this.port = port;
        serverSocket = new ServerSocket(port);
        //clientList = new ArrayList<>();
        list = new ConcurrentHashMap<>();
    }

    void startServer() throws IOException {
        //clientList = new ArrayList<>();
        //serverSocket = new ServerSocket(port);
        clients = clientConnections();
        clients.subscribe(this::addToSocketList,
                throwable -> System.out.println("Server shutdown: " + throwable.toString()));
        //getStream().subscribe(this::sendToClients);
        //sendToClients();
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
    Observable<Socket> clientConnections() {
        return Observable
                .<Socket>create(e -> {
                    try{
                        while (online) {
                            System.out.println("Accepting a connection...");
                            e.onNext(serverSocket.accept());
                        }
                    } catch (IOException ex) {
                        e.onError(ex);
                    }
                }).subscribeOn(Schedulers.io()).share()// Accepts connections correctly, but subscribe gets
                .flatMap(s -> Observable.just(s)
                        .subscribeOn(Schedulers.newThread()))
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

    /*Observable<String> getStream() throws IOException {
        return clients
                .map(Socket::getInputStream)
                .map(InputStreamReader::new)
                .map(BufferedReader::new)
                .map(BufferedReader::lines)
                .flatMap(stream -> Observable
                        .fromIterable(stream::iterator).subscribeOn(Schedulers.io()))
                .observeOn(Schedulers.io())
                .share();
                //.subscribeOn(Schedulers.io())
                //.replay()
                //.autoConnect();

        //.publish().autoConnect();
    }*/

    /*public static ObservableTransformer<Socket, String> getStream() {
        return up -> up
                .map(Socket::getInputStream)
                .map(InputStreamReader::new)
                .map(BufferedReader::new)
                .map(BufferedReader::lines)
                //.share()
                //.flatMap(stream -> Observable
                  //      .fromIterable(stream::iterator).subscribeOn(Schedulers.newThread()))
                .flatMap(stream -> Observable
                        .fromIterable(stream::iterator).subscribeOn(Schedulers.newThread()))
                //.map(Stream::toString)
                //.subscribeOn(Schedulers.newThread())
                //.observeOn(Schedulers.io())
                //.publish().autoConnect()
                ;
    }*/
    //public static ObservableTransformer<Socket, String> getStream() {
    public static ObservableTransformer<Socket, DrawObject> getStream() {
        return up -> up
                .map(Socket::getInputStream)
                //.map(InputStreamReader::new)
                //.map(InputStream::new)
                .map(ObjectInputStream::new)
                .flatMap(or -> Observable.create(e -> {
                    // Look for error on reading objects from a socket, such as client disconnect
                    try{
                        while(true){
                            e.onNext(or.readObject());
                        }
                    } catch (EOFException ex) {
                        e.onError(ex);
                    }
                }).subscribeOn(Schedulers.io()))
                .map(object -> (DrawObject)object);
                /*.map(BufferedReader::new)
                .map(BufferedReader::lines)
                //.share()
                //.flatMap(stream -> Observable
                //      .fromIterable(stream::iterator).subscribeOn(Schedulers.newThread()))
                .flatMap(stream -> Observable
                        .fromIterable(stream::iterator).subscribeOn(Schedulers.newThread()))*/
                //.map(Stream::toString)
                //.subscribeOn(Schedulers.newThread())
                //.observeOn(Schedulers.io())
                //.publish().autoConnect()

    }

    /*public static ObservableTransformer<Socket, Object> getStream() {
        return up -> up
                .map(Socket::getInputStream)
                .map(ObjectInputStream::new)
                .map(ObjectInputStream::readObject)
                //.map(o -> ((DrawObject) o))
                //.map(BufferedReader::new)
                //.map(BufferedReader::lines)
                //.share()
                //.flatMap(stream -> Observable
                //      .fromIterable(stream::iterator).subscribeOn(Schedulers.newThread()))
                //.flatMap(stream -> Observable
                  //      .fromIterable(stream::iterator).subscribeOn(Schedulers.newThread()))
                //.map(Stream::toString)
                //.subscribeOn(Schedulers.newThread())
                //.observeOn(Schedulers.io())
                //.publish().autoConnect()
                ;
    }*/

    /*public static ObservableTransformer<Socket, String> getStream() {
        return up -> up
                .map(Socket::toString);
    }*/

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
        //return clientList.size();
        return list.size();
    }

    /*void addToSocketList(Socket sock) throws IOException {
        //clientList.add(sock);
        list.put(sock,new OutputStreamWriter(sock.getOutputStream()));
        System.out.println("Socket sent to list!");
    }*/
    void addToSocketList(Socket sock) throws IOException {
        //clientList.add(sock);
        list.put(sock,new ObjectOutputStream(sock.getOutputStream()));
        System.out.println("Socket sent to list!");
    }

    void sendToClients(DrawObject data) { // TODO: Not entirely sure this works 100% is get(sock) ok? use index instead?
        //System.out.println("Sent to clients! Thread: " + Thread.currentThread().getName());
        for(Socket sock: list.keySet()) {
            try {
                list.get(sock).writeObject(data);
                list.get(sock).flush();
            } catch (IOException e) {
                list.remove(sock);
            }
        }
    }

    /*void sendToClients(String data) { // TODO: Not entirely sure this works 100% is get(sock) ok? use index instead?
        //System.out.println("Sent to clients! Thread: " + Thread.currentThread().getName());
        for(Socket sock: list.keySet()) {
            try {
                list.get(sock).write(data + System.lineSeparator());
                list.get(sock).flush();
            } catch (IOException e) {
                list.remove(sock);
            }
        }
        /*for(Socket s: clientList) {
            out = new OutputStreamWriter(s.getOutputStream());
            out.write(data + System.lineSeparator());
            out.flush();
            System.out.println("[SENT]" + data + "[TO]" + s.getRemoteSocketAddress());
        }*/
        /*getClientsVar()
            .compose(getStream())
            .subscribe(data ->
            {
                System.out.println("Sent to clients! Thread: " + Thread.currentThread().getName());
                for(Socket s: clientList) {
                    out = new DataOutputStream(s.getOutputStream());
                    out.writeUTF(data);
                }
            }
        );*/
        //clients.subscribe(socket1 -> System.out.println("Socket sent to clients!"));
    //}

    Observable<Socket> getClientsVar(){
        return clients;
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
