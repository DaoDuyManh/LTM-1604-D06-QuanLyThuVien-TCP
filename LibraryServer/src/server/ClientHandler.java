package server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final CommandProcessor processor;
    private String currentUser = null;

    public ClientHandler(Socket socket, CommandProcessor processor) {
        this.socket = socket;
        this.processor = processor;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                System.out.println("📩 Received from " + socket.getInetAddress() + ": " + line);

             
                if (line.startsWith("LOGIN")) {
                    String[] parts = line.split("[: ]", 3);
                    if (parts.length >= 3) {
                        String username = parts[1];
                        String resp = processor.process(line, username);
                        if (resp.startsWith("SUCCESS")) {
                            currentUser = username;
                        }
                        out.println(resp.trim());
                        out.flush();
                        continue;
                    }
                }

        
                String resp = processor.process(line, currentUser);
                if (resp == null || resp.isEmpty()) resp = "FAIL";
                out.println(resp.trim());
                out.flush();
                System.out.println("📤 Server trả: " + resp);
            }
        } catch (IOException e) {
            System.out.println("⚠️ Mất kết nối client: " + socket.getInetAddress());
        }
    }
}
