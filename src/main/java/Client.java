import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

// TODO: Figure out where exception handling should go.

public class Client {

    private final Socket socket;
    //private DataOutputStream out;
    private final ObjectOutputStream out;
    //private OutputStreamWriter out;

    Client(String address, int port) throws IOException {
        //this.address = address;
        //this.port = port;
        socket = new Socket(address, port);
        //out = new DataOutputStream(socket.getOutputStream());
        out = new ObjectOutputStream(socket.getOutputStream());
        //out = new OutputStreamWriter(socket.getOutputStream());
    }


    void disconnect(){
        try {
            socket.close();
            out.close();
        } catch (IOException e) {
            System.err.println(e.toString());
        }
    }

    Observable<DrawObject> serverStream() {
        return Observable
                .<Socket>just(socket)
                .subscribeOn(Schedulers.io())
                .map(Socket::getInputStream)
                .map(ObjectInputStream::new)
                .flatMap(or -> Observable.create(e -> {
                    try {
                        while(true){

                            System.out.println("[STREAM] About to read!");
                            e.onNext(or.readObject());
                            System.out.println("[STREAM] Read complete!");
                        }
                    } catch (Exception ex) {
                        if(!e.isDisposed()) {
                            e.onError(ex);
                        }
                        else {
                            System.err.println("stream error: " + ex);
                        }
                    }

                }).subscribeOn(Schedulers.io()))
                //.onErrorReturn(throwable -> "ops")
                .map(object -> (DrawObject)object)
                .doOnNext(drawObject -> System.out.println("[S_RECEIVED]"
                                    + drawObject.toString() + " [THREAD]"
                                    + Thread.currentThread().getName()
                                    + System.lineSeparator()));
    }

    void sendToServer(DrawObject drawObject) {
        try {
            //System.out.println("[SENT]" + drawObject.toString() + "[TO]" + socket.getRemoteSocketAddress());
            //System.out.println("[OWN]" + socket.getLocalSocketAddress());
            out.writeObject(drawObject);
            out.reset();
            System.out.println("[SENT]" + drawObject.toString() + "[TO]" + socket.getRemoteSocketAddress());
            //System.out.println("[OWN]" + socket.getLocalSocketAddress());
        } catch (IOException e) {
            System.err.println(e.toString());
        }
    }
}
