package client;

import util.Config;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * ClientConnection nâng cấp: tạo 1 thread đọc liên tục từ server.
 * - Các dòng bắt đầu bằng "RESP|" được đưa vào queue để các lời gọi readResponse() tiếp tục hoạt động như trước.
 * - Các dòng khác (ví dụ: UPDATE_...) được gửi tới các push listeners đã đăng ký.
 */
public class ClientConnection {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();
    private final List<Consumer<String>> pushListeners = new CopyOnWriteArrayList<>();

    private volatile boolean running = true;

    public ClientConnection() throws IOException {
        socket = new Socket(Config.SERVER_HOST, Config.SERVER_PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Reader thread: phân tách RESP| (reply cho request) và các message push khác
        Thread reader = new Thread(() -> {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    line = line.trim();
                    System.out.println("📥 Client nhận từ server: " + line); // Debug log
                    if (line.startsWith("RESP|")) {
                        responseQueue.put(line.substring(5));
                    } else {
                        // Backward-compat: nếu server CHƯA gắn tiền tố RESP| nhưng đây là
                        // một phản hồi đồng bộ quen thuộc, cũng đẩy vào responseQueue để tránh treo UI.
                        boolean looksLikeSync = line.startsWith("SUCCESS")
                                || line.startsWith("ERROR")
                                || line.startsWith("BOOK_LIST")
                                || line.startsWith("MYBOOKS")
                                || line.startsWith("PENDING")
                                || line.startsWith("HISTORY")
                                || line.startsWith("PENDING_OK")
                                || line.startsWith("FAIL");
                        if (looksLikeSync) {
                            responseQueue.put(line);
                            continue;
                        }
                        // Gửi tới các listener (non-blocking, CopyOnWriteArrayList)
                        for (Consumer<String> l : pushListeners) {
                            try {
                                l.accept(line);
                            } catch (Exception ex) {
                                // swallow listener errors
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                // Thread kết thúc
                System.out.println("⚠️ Reader thread kết thúc: " + e.getMessage());
            } finally {
                running = false;
            }
        }, "ClientConnection-Reader");
        reader.setDaemon(true);
        reader.start();
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    /**
     * Chờ và trả về RESP (phần sau "RESP|") từ server. Giữ tương thích với readResponse() cũ.
     */
    public String readResponse() throws IOException {
        try {
            String resp = responseQueue.take();
            return resp;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting response", e);
        }
    }

    public void registerPushListener(Consumer<String> listener) {
        if (listener != null) pushListeners.add(listener);
    }

    public void unregisterPushListener(Consumer<String> listener) {
        pushListeners.remove(listener);
    }

    public void close() throws IOException {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            // rethrow
            throw e;
        }
    }
}
