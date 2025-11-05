package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class LibraryServer {
    private static final int PORT = 1234;

    // Danh sách client đang kết nối (thread-safe)
    private static final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.out.println("📚 Library Server khởi động...");

        CommandProcessor processor = new CommandProcessor();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ Server lắng nghe tại cổng " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("🔗 Client kết nối: " + clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(clientSocket, processor);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Gửi message đến tất cả client
    public static void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    // Xoá client khi ngắt kết nối
    public static void removeClient(ClientHandler handler) {
        clients.remove(handler);
    }
}
