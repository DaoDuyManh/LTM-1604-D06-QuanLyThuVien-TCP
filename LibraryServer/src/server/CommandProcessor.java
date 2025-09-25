package server;

import model.Book;
import model.User;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CommandProcessor {
    private static final String ACCOUNT_FILE = "accounts.txt";
    private static final String USERS_FILE = "users.txt";
    private static final String BOOK_FILE = "books.txt";
    private static final String PENDING_FILE = "pending.txt";
    private static final String BORROW_HISTORY_FILE = "borrow_history.txt";

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final Map<String, User> accounts = new HashMap<>();
    private final Map<String, Book> books = new HashMap<>();

    public CommandProcessor() {
        loadAccounts();
        loadBooks();
    }

    // -------------------- ACCOUNT --------------------
    private synchronized void loadAccounts() {
        accounts.clear();
        Path p = Paths.get(ACCOUNT_FILE);
        if (!Files.exists(p)) return;
        try {
            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            for (String line : lines) {
                User u = User.fromString(line);
                if (u != null) accounts.put(u.getUsername(), u);
            }
        } catch (IOException e) {
            System.out.println("❌ Lỗi load accounts: " + e.getMessage());
        }
    }

    private synchronized void saveAccounts() {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(ACCOUNT_FILE),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (User u : accounts.values()) {
                bw.write(u.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("❌ Lỗi save accounts: " + e.getMessage());
        }
    }

    // -------------------- BOOK --------------------
    private synchronized void loadBooks() {
        books.clear();
        Path p = Paths.get(BOOK_FILE);
        if (!Files.exists(p)) return;
        try {
            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            for (String line : lines) {
                Book b = Book.fromString(line);
                if (b != null) books.put(b.getTitle(), b);
            }
        } catch (IOException e) {
            System.out.println("❌ Lỗi load books: " + e.getMessage());
        }
    }

    private synchronized void saveBooks() {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(BOOK_FILE),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (Book b : books.values()) {
                bw.write(b.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("❌ Lỗi save books: " + e.getMessage());
        }
    }

    // -------------------- PROCESS --------------------
    public synchronized String process(String input, String currentUser) {
        try {
            if (input == null || input.trim().isEmpty()) return "ERROR|Empty input";

            if (input.startsWith("LOGIN")) {
                loadAccounts();
                String[] parts = input.split("[: ]", 3);
                if (parts.length < 3) return "ERROR|Sai cú pháp LOGIN";
                return handleLogin(parts[1], parts[2]);
            }

            if (input.startsWith("REGISTER")) {
                loadAccounts();
                String[] parts = input.split(":", 6);
                if (parts.length < 6) return "ERROR|Sai cú pháp REGISTER";
                return handleRegister(parts[1], parts[2], parts[3], parts[4], parts[5]);
            }

            if (input.equals("LIST_BOOKS")) return handleListBooks();

            if (input.startsWith("SEARCH")) {
                String[] parts = input.split(" ", 2);
                if (parts.length < 2) return "ERROR|Sai cú pháp SEARCH";
                return handleSearch(parts[1]);
            }

            if (input.startsWith("MYBOOKS")) {
                String arg = input.replaceFirst("MYBOOKS[: ]", "").trim();
                return arg.isEmpty() ? handleMyBooks(currentUser) : handleMyBooks(arg);
            }

            if (input.startsWith("BORROW_REQUEST")) {
                String arg = input.replaceFirst("BORROW_REQUEST[: ]", "").trim();
                if (arg.isEmpty()) return "ERROR|Sai cú pháp BORROW_REQUEST";
                return handleBorrowRequest(arg);
            }

            if (input.startsWith("PENDING")) {
                String arg = input.replaceFirst("PENDING[: ]", "").trim();
                return arg.isEmpty() ? handlePendingListForAll() : handlePendingList(arg);
            }

            if (input.startsWith("ACCEPT_BORROW")) {
                String arg = input.replaceFirst("ACCEPT_BORROW[: ]", "").trim();
                return handleAcceptBorrow(arg);
            }

            if (input.startsWith("REJECT_BORROW")) {
                String arg = input.replaceFirst("REJECT_BORROW[: ]", "").trim();
                return handleRejectBorrow(arg);
            }

            if (input.startsWith("RETURN")) {
                String arg = input.replaceFirst("RETURN[: ]", "").trim();
                if (arg.isEmpty()) return "ERROR|Sai cú pháp RETURN";

                // Lấy user và title từ lệnh client
                String[] parts = arg.split("\\|", -1);
                if (parts.length < 2) return "ERROR|Thiếu thông tin user hoặc sách";
                String user = parts[0].trim();
                String bookTitle = parts[1].trim();

                return handleReturn(user, bookTitle);
            }

            if (input.startsWith("HISTORY")) {
                String arg = input.replaceFirst("HISTORY[: ]", "").trim();
                return arg.isEmpty() ? handleHistory(currentUser) : handleHistory(arg);
            }

        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }

        return "ERROR|Lệnh không hợp lệ";
    }

    // -------------------- HANDLERS --------------------
    private String handleLogin(String user, String pass) {
        User u = accounts.get(user);
        if (u == null) return "ERROR|User không tồn tại";
        if (!u.getPassword().equals(pass)) return "ERROR|Sai mật khẩu";
        return "SUCCESS|Đăng nhập thành công";
    }

    private String handleRegister(String user, String pass, String phone, String address, String email) {
        if (accounts.containsKey(user)) return "ERROR|User đã tồn tại";
        User u = new User(user, pass, phone, address, email);
        accounts.put(user, u);
        saveAccounts();
        try {
            Path up = Paths.get(USERS_FILE);
            Files.write(up,
                    Collections.singletonList(u.toString()),
                    StandardCharsets.UTF_8,
                    Files.exists(up) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
        } catch (IOException ex) {
            System.out.println("⚠️ Không thể ghi users.txt: " + ex.getMessage());
        }
        return "SUCCESS|Đăng ký thành công";
    }

    private String handleListBooks() {
        StringBuilder sb = new StringBuilder("BOOK_LIST");
        for (Book b : books.values()) {
            sb.append("|").append(b.getTitle()).append(",")
              .append(b.getAuthor()).append(",")
              .append(b.getCategory()).append(",")
              .append(b.getQuantity()).append(",")
              .append(b.getAvailableCount());
        }
        return sb.toString();
    }

    private String handleSearch(String keyword) {
        keyword = keyword.toLowerCase();
        StringBuilder sb = new StringBuilder("BOOK_LIST");
        for (Book b : books.values()) {
            if (b.getTitle().toLowerCase().contains(keyword) ||
                b.getAuthor().toLowerCase().contains(keyword) ||
                b.getCategory().toLowerCase().contains(keyword)) {
                sb.append("|").append(b.getTitle()).append(",")
                  .append(b.getAuthor()).append(",")
                  .append(b.getCategory()).append(",")
                  .append(b.getQuantity()).append(",")
                  .append(b.getAvailableCount());
            }
        }
        return sb.toString();
    }

    private String handleMyBooks(String user) throws IOException {
        StringBuilder sb = new StringBuilder("MYBOOKS");
        if (user == null || user.trim().isEmpty()) return sb.toString();

        Path p = Paths.get(BORROW_HISTORY_FILE);
        List<String> lines = Files.exists(p) ? Files.readAllLines(p, StandardCharsets.UTF_8) : new ArrayList<>();

        for (Book b : books.values()) {
            long count = b.getBorrowers().stream().filter(u -> u.equalsIgnoreCase(user)).count();
            if (count > 0) {
                String borrowDate = getBorrowDate(user, b.getTitle(), lines);
                String dueDate = getDueDate(user, b.getTitle(), lines);
                sb.append("|").append(b.getTitle()).append(",")
                  .append(b.getAuthor()).append(",")
                  .append(b.getCategory()).append(",")
                  .append(count).append(",")
                  .append(borrowDate).append(",")
                  .append(dueDate);
            }
        }
        return sb.toString();
    }

    private String getBorrowDate(String user, String title, List<String> lines) {
        for (String l : lines) {
            String[] arr = l.split("\\|", -1);
            if (arr.length >= 6 && arr[0].equals(user) && arr[1].equals(title) && arr[5].equals("BORROWED")) {
                return arr[2]; // borrowDate
            }
        }
        return "";
    }

    private String getDueDate(String user, String title, List<String> lines) {
        for (String l : lines) {
            String[] arr = l.split("\\|", -1);
            if (arr.length >= 6 && arr[0].equals(user) && arr[1].equals(title) && arr[5].equals("BORROWED")) {
                return arr[3]; // dueDate
            }
        }
        return "";
    }

    private String handleBorrowRequest(String arg) throws IOException {
        String[] parts = arg.split("\\|", -1);
        if (parts.length < 3) return "ERROR|Sai cú pháp BORROW_REQUEST";
        String user = parts[0].trim();
        String title = parts[1].trim();
        String dueDate = parts[2].trim();
        if (user.isEmpty() || title.isEmpty()) return "ERROR|Thiếu user hoặc title";

        String requestDate = LocalDateTime.now().format(ISO_FMT);
        String line = String.join("|", user, title, requestDate, dueDate, "PENDING");
        Files.write(Paths.get(PENDING_FILE),
                Collections.singletonList(line),
                StandardCharsets.UTF_8,
                Files.exists(Paths.get(PENDING_FILE)) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
        return "PENDING_OK|Đã gửi yêu cầu mượn";
    }

    private String handlePendingList(String user) throws IOException {
        Path p = Paths.get(PENDING_FILE);
        if (!Files.exists(p)) return "PENDING";
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder("PENDING");
        for (String l : lines) {
            String[] parts = l.split("\\|", -1);
            if (parts.length >= 5 && parts[0].trim().equals(user)) {
                Book b = books.get(parts[1]);
                if (b != null) {
                    sb.append("|").append(b.getTitle()).append(",")
                      .append(b.getAuthor()).append(",")
                      .append(b.getCategory()).append(",")
                      .append(parts[2]).append(",")
                      .append(parts[3]);
                }
            }
        }
        return sb.toString();
    }

    private String handlePendingListForAll() throws IOException {
        Path p = Paths.get(PENDING_FILE);
        if (!Files.exists(p)) return "PENDING";
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder("PENDING");
        for (String l : lines) {
            String[] parts = l.split("\\|", -1);
            if (parts.length >= 5) {
                Book b = books.get(parts[1]);
                if (b != null) {
                    sb.append("|").append(parts[0]).append(",")
                      .append(b.getTitle()).append(",")
                      .append(b.getAuthor()).append(",")
                      .append(b.getCategory()).append(",")
                      .append(parts[2]).append(",")
                      .append(parts[3]);
                }
            }
        }
        return sb.toString();
    }

    private String handleAcceptBorrow(String arg) throws IOException {
        String[] parts = arg.split("[:|]", -1);
        if (parts.length < 2) return "ERROR|Sai cú pháp ACCEPT_BORROW";
        String user = parts[0].trim();
        String title = parts[1].trim();

        Book b = books.get(title);
        if (b == null) return "ERROR|Không tìm thấy sách";
        if (b.getAvailableCount() <= 0) return "ERROR|Không còn sách trống";

        String dueDate = "";
        Path p = Paths.get(PENDING_FILE);
        if (Files.exists(p)) {
            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            for (String l : lines) {
                String[] arr = l.split("\\|", -1);
                if (arr.length >= 5 && arr[0].equals(user) && arr[1].equals(title)) {
                    dueDate = arr[3];
                    break;
                }
            }
        }

        b.addBorrower(user);
        saveBooks();

        String borrowDate = LocalDateTime.now().format(ISO_FMT);
        String line = String.join("|", user, title, borrowDate, dueDate, "", "BORROWED");
        Files.write(Paths.get(BORROW_HISTORY_FILE),
                Collections.singletonList(line),
                StandardCharsets.UTF_8,
                Files.exists(Paths.get(BORROW_HISTORY_FILE)) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);

        removePending(user, title);
        return "SUCCESS|Đã duyệt mượn";
    }

    private String handleRejectBorrow(String arg) throws IOException {
        String[] parts = arg.split("[:|]", -1);
        if (parts.length < 2) return "ERROR|Sai cú pháp REJECT_BORROW";
        String user = parts[0].trim();
        String title = parts[1].trim();
        removePending(user, title);
        return "SUCCESS|Đã từ chối yêu cầu";
    }

    private void removePending(String user, String title) throws IOException {
        Path p = Paths.get(PENDING_FILE);
        if (!Files.exists(p)) return;
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        List<String> updated = new ArrayList<>();
        for (String l : lines) {
            String[] parts = l.split("\\|", -1);
            if (parts.length >= 2) {
                if (!(parts[0].trim().equals(user) && parts[1].trim().equalsIgnoreCase(title))) {
                    updated.add(l);
                }
            }
        }
        Files.write(p, updated, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String handleReturn(String user, String title) throws IOException {
        if (user == null || user.trim().isEmpty()) return "ERROR|Chưa đăng nhập";

        String returnDate = LocalDateTime.now().format(ISO_FMT);

        Book b = books.get(title);
        if (b == null) return "ERROR|Không tìm thấy sách";
        if (!b.getBorrowers().removeIf(u -> u.equalsIgnoreCase(user))) {
            return "ERROR|Bạn chưa mượn sách này";
        }
        saveBooks();

        Path p = Paths.get(BORROW_HISTORY_FILE);
        List<String> lines = Files.exists(p) ? Files.readAllLines(p, StandardCharsets.UTF_8) : new ArrayList<>();
        List<String> updated = new ArrayList<>();
        boolean updatedFlag = false;

        for (String l : lines) {
            String[] arr = l.split("\\|", -1);
            if (arr.length >= 6 && arr[0].equals(user) && arr[1].equals(title) && arr[5].equals("BORROWED")) {
                arr[4] = returnDate;
                arr[5] = "RETURNED";
                updated.add(String.join("|", arr));
                updatedFlag = true;
            } else {
                updated.add(l);
            }
        }

        if (!updatedFlag) {
            String line = String.join("|", user, title, "", "", returnDate, "RETURNED");
            updated.add(line);
        }

        Files.write(p, updated, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return "SUCCESS|Trả sách thành công";
    }

    private String handleHistory(String user) throws IOException {
        if (user == null || user.trim().isEmpty()) return "HISTORY";
        Path p = Paths.get(BORROW_HISTORY_FILE);
        if (!Files.exists(p)) return "HISTORY";
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder("HISTORY");
        for (String l : lines) {
            String[] parts = l.split("\\|", -1);
            if (parts.length >= 6 && parts[0].trim().equals(user)) {
                Book b = books.get(parts[1]);
                if (b != null) {
                    sb.append("|").append(b.getTitle()).append(",")
                      .append(b.getAuthor()).append(",")
                      .append(b.getCategory()).append(",")
                      .append(parts[2]).append(",")
                      .append(parts[3]).append(",")
                      .append(parts[4]).append(",")
                      .append(parts[5]);
                }
            }
        }
        return sb.toString();
    }
}
