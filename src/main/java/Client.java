import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

// TODO: Figure out where exception handling should go.

public class Client {

    //private final String address;
    //private final int port;

    private final Socket socket;
    //private DataOutputStream out;
    private ObjectOutputStream out;
    //private OutputStreamWriter out;

    Client(String address, int port) throws IOException {
        //this.address = address;
        //this.port = port;
        socket = new Socket(address, port);
        //out = new DataOutputStream(socket.getOutputStream());
        out = new ObjectOutputStream(socket.getOutputStream());
        //out = new OutputStreamWriter(socket.getOutputStream());
    }

    /*void start() throws IOException {
        //socket = new Socket(address, port);
        //out = new DataOutputStream(socket.getOutputStream());
    }*/

    void disconnect() throws IOException {
        socket.close();
    }

    Observable<DrawObject> serverStream() {
        return Observable
                .<Socket>just(socket)
                .map(Socket::getInputStream)
                .map(ObjectInputStream::new)
                .flatMap(or -> Observable.create(e -> {
                    try {
                        while(true){
                            e.onNext(or.readObject());
                        }
                    } catch (Exception ex) {
                        e.onError(ex);
                    }

                }).subscribeOn(Schedulers.newThread()))
                //.onErrorReturn(throwable -> "ops")
                .map(object -> (DrawObject)object)
                ;
    }
    /*Observable<String> serverStream() {
        return Observable
                .<Socket>just(socket)
                //.subscribeOn(Schedulers.io())
                .map(Socket::getInputStream)
                .map(InputStreamReader::new)
                .map(BufferedReader::new)
                .map(BufferedReader::lines)
                .flatMap(stream -> Observable
                        .fromIterable(stream::iterator).subscribeOn(Schedulers.newThread()))
                .publish().autoConnect()

                ;
    }*/
    /*Observable<String> serverStream() {
        return Observable
                .<Socket>just(socket)
                //.subscribeOn(Schedulers.io())
                .map(Socket::getInputStream)
                .map(InputStreamReader::new)
                .map(BufferedReader::new)
                .map(BufferedReader::lines)
                .flatMap(stream -> Observable
                        .fromIterable(stream::iterator).subscribeOn(Schedulers.newThread()))
                .publish().autoConnect()

                ;
    }*/

    void sendToServer(DrawObject data) {
        try {
            //System.out.println("[SENT]" + data.toString() + "[TO]" + socket.getRemoteSocketAddress());
            //System.out.println("[OWN]" + socket.getLocalSocketAddress());
            out.writeObject(data);
            out.flush();
            System.out.println("[SENT]" + data.toString() + "[TO]" + socket.getRemoteSocketAddress());
            System.out.println("[OWN]" + socket.getLocalSocketAddress());
        } catch (IOException e) {
            System.err.println("Socketerror!!");
        }
    }
    /*void sendToServer(DrawObject obj) throws IOException {
        System.out.println("Sending object!");
        out.writeObject(obj);
        //out.flush();
    }*/
}
