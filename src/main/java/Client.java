import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

// TODO: Figure out where exception handling should go.

public class Client {

    //private final String address;
    //private final int port;

    private Socket socket;
    private DataOutputStream out;

    Client(String address, int port) throws IOException {
        //this.address = address;
        //this.port = port;
        socket = new Socket(address, port);
        out = new DataOutputStream(socket.getOutputStream());
    }

    void start() throws IOException {
        //socket = new Socket(address, port);
        //out = new DataOutputStream(socket.getOutputStream());
    }

    void disconnect() throws IOException {
        socket.close();
    }

    Observable<String> serverStream() {
        return Observable
                .<Socket>create(e -> e.onNext(socket))
                .subscribeOn(Schedulers.io())
                .share()
                .map(Socket::getInputStream)
                .map(InputStreamReader::new)
                .map(BufferedReader::new)
                .map(BufferedReader::lines)
                .flatMap(stream -> Observable
                        .fromIterable(stream::iterator).subscribeOn(Schedulers.newThread()))
                .observeOn(Schedulers.io());
    }

    void sendToServer(String msg) throws IOException {
        out.writeUTF(msg);
    }
}
