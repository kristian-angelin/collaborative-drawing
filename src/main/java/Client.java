import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import java.io.*;
import java.net.Socket;

/**
 * Class for handling client connection to the server. Reads the
 * stream of drawObjects.
 *
 * @author  Kristian Angelin
 * @version 1.0
 * @since   2021-01-17
 */

public class Client {

    private final Socket socket;
    private final ObjectOutputStream out;

    Client(String address, int port) throws IOException {
        socket = new Socket(address, port);
        out = new ObjectOutputStream(socket.getOutputStream());
    }

    // Disconnects client
    public void disconnect(){
        try {
            socket.close();
            out.close();
        } catch (IOException e) {
            System.err.println(e.toString());
        }
    }

    // Returns the server stream of objects
    public Observable<DrawObject> serverStream() {
        return Observable
                .<Socket>just(socket)
                .subscribeOn(Schedulers.io())
                .map(Socket::getInputStream)
                .map(ObjectInputStream::new)
                .flatMap(or -> Observable.create(e -> {
                    try {
                        while(true){
                            e.onNext(or.readObject());
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
                .map(object -> (DrawObject)object);
    }

    // Send object to server
    public void sendToServer(DrawObject drawObject) {
        try {
            out.writeObject(drawObject);
            out.reset();
        } catch (IOException e) {
            System.err.println(e.toString());
        }
    }

    // Returns the connected server address as a string
    public String getServerAddress() {
        String ipAddress = socket.getInetAddress().toString();
        return ipAddress.substring(ipAddress.indexOf("/") + 1);
    }
}
