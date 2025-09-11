package client;

import util.Config;
import java.io.*;
import java.net.Socket;

public class ClientConnection {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public ClientConnection() throws IOException {
        socket = new Socket(Config.SERVER_HOST, Config.SERVER_PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String readResponse() throws IOException {
        return in.readLine();
    }

    public void close() throws IOException {
        if (socket != null) socket.close();
    }
}
