import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

// TODO: Figure out where exception handling should go.

public class Client {

    private Socket socket;
    private DataOutputStream out;

    Client(String address, int port) throws IOException {
        socket = new Socket(address, port);
        out = new DataOutputStream(socket.getOutputStream());
    }

    void disconnect() throws IOException {
        socket.close();
    }

    Observable<String> serverStream() {
        return Observable
                .just(socket)
                .map(Socket::getInputStream)
                .map(InputStreamReader::new)
                .map(BufferedReader::new)
                .map(BufferedReader::lines)
                .flatMap(stream -> Observable
                        .fromIterable(stream::iterator).subscribeOn(Schedulers.io()));
    }

    void sendToServer(String msg) throws IOException {
        out.writeUTF(msg);
    }
}
