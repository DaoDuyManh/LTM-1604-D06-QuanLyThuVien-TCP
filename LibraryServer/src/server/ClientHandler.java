package server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final CommandProcessor processor;
    private String currentUser = null;
    private PrintWriter out; // giữ output stream để gửi realtime

    public ClientHandler(Socket socket, CommandProcessor processor) {
        this.socket = socket;
        this.processor = processor;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            this.out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                System.out.println("📩 Received from " + socket.getInetAddress() + ": " + line);

                if (line.startsWith("LOGIN")) {
                    // LOGIN:username:password (không có dấu cách sau dấu :)
                    String[] parts = line.split(":", 3);
                    if (parts.length >= 3) {
                        String username = parts[1];
                        String resp = processor.process(line, username);
                        if (resp.startsWith("SUCCESS")) {
                            currentUser = username;
                        }
                        // Đánh dấu rõ ràng đây là phản hồi cho request
                        out.println("RESP|" + resp.trim());
                        continue;
                    }
                }

                String resp = processor.process(line, currentUser);
                if (resp == null || resp.isEmpty()) resp = "FAIL";
                // Đánh dấu rõ ràng đây là phản hồi cho request
                out.println("RESP|" + resp.trim());
                System.out.println("📤 Server trả: " + resp);
            }
        } catch (IOException e) {
            System.out.println("⚠️ Mất kết nối client: " + socket.getInetAddress());
        } finally {
            LibraryServer.removeClient(this);
        }
    }

    // Cho phép server gửi tin nhắn bất kỳ xuống client
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
            out.flush();
        }
    }
}
