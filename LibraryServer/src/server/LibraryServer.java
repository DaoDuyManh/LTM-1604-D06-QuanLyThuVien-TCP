package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class LibraryServer {
    private static final int PORT = 1234;

    public static void main(String[] args) {
        System.out.println("📚 Library Server khởi động...");

        // Tạo CommandProcessor dùng chung cho toàn bộ server
        CommandProcessor processor = new CommandProcessor();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ Server lắng nghe tại cổng " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("🔗 Client kết nối: " + clientSocket.getRemoteSocketAddress());

                // Truyền processor vào ClientHandler
                new Thread(new ClientHandler(clientSocket, processor)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
