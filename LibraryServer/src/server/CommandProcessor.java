package server;

import model.Book;
import model.User;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class CommandProcessor {
    private static final String ACCOUNT_FILE = "accounts.txt";
    private static final String BOOK_FILE = "books.txt";

    private final Map<String, User> accounts = new HashMap<>();
    private final Map<String, Book> books = new HashMap<>();

    public CommandProcessor() {
        loadAccounts();
        loadBooks();
    }

    // -------------------- LOAD / SAVE --------------------
    private synchronized void loadAccounts() {
        accounts.clear();
        Path p = Paths.get(ACCOUNT_FILE);
        if (!Files.exists(p)) return;
        try {
            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                User u = User.fromString(line);
                if (u != null) accounts.put(u.getUsername(), u);
            }
            System.out.println("🔑 Loaded accounts: " + accounts.size());
        } catch (IOException e) {
            System.out.println("❌ Lỗi load accounts: " + e.getMessage());
        }
    }

    private synchronized void saveAccounts() {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(ACCOUNT_FILE), StandardCharsets.UTF_8)) {
            for (User u : accounts.values()) {
                bw.write(u.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("❌ Lỗi save accounts: " + e.getMessage());
        }
    }

    private synchronized void loadBooks() {
        books.clear();
        Path p = Paths.get(BOOK_FILE);
        if (!Files.exists(p)) return;
        try {
            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                Book b = Book.fromString(line);
                if (b != null) books.put(b.getTitle(), b);
            }
            System.out.println("📚 Loaded books: " + books.size());
        } catch (IOException e) {
            System.out.println("❌ Lỗi load books: " + e.getMessage());
        }
    }

    private synchronized void saveBooks() {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(BOOK_FILE), StandardCharsets.UTF_8)) {
            for (Book b : books.values()) {
                bw.write(b.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("❌ Lỗi save books: " + e.getMessage());
        }
    }

    // -------------------- COMMANDS --------------------
    public synchronized String process(String input, String username) {
        try {
            if (input.startsWith("LOGIN")) {
                String[] parts = input.split("[: ]", 3);
                if (parts.length < 3) return "ERROR|Sai cú pháp LOGIN";
                return handleLogin(parts[1], parts[2]);
            }
            if (input.startsWith("REGISTER")) {
                String[] parts = input.split(":", 6);
                if (parts.length < 6) return "ERROR|Sai cú pháp REGISTER";
                return handleRegister(parts[1], parts[2], parts[3], parts[4], parts[5]);
            }
            if (input.equals("LIST_BOOKS")) {
                return handleListBooks();
            }
            if (input.startsWith("SEARCH")) {
                String[] parts = input.split(" ", 2);
                if (parts.length < 2) return "ERROR|Sai cú pháp SEARCH";
                return handleSearch(parts[1]);
            }
            if (input.startsWith("MYBOOKS")) {
                String arg = input.replaceFirst("MYBOOKS[: ]", "").trim();
                if (arg.isEmpty()) return "ERROR|Sai cú pháp MYBOOKS";
                return handleMyBooks(arg);
            }
            if (input.startsWith("BORROW")) {
                String arg = input.replaceFirst("BORROW[: ]", "").trim();
                if (arg.isEmpty()) return "ERROR|Sai cú pháp BORROW";
                return handleBorrow(username, arg);
            }
            if (input.startsWith("RETURN")) {
                String arg = input.replaceFirst("RETURN[: ]", "").trim();
                if (arg.isEmpty()) return "ERROR|Sai cú pháp RETURN";
                return handleReturn(username, arg);
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

    private String handleMyBooks(String user) {
        StringBuilder sb = new StringBuilder("MYBOOKS");
        for (Book b : books.values()) {
            long count = b.getBorrowers().stream().filter(u -> u.equals(user)).count();
            if (count > 0) {
                sb.append("|").append(b.getTitle()).append(",")
                  .append(b.getAuthor()).append(",")
                  .append(b.getCategory()).append(",")
                  .append(count);
            }
        }
        return sb.toString();
    }

    private String handleBorrow(String user, String title) {
        Book b = books.get(title);
        if (b == null) return "ERROR|Không tìm thấy sách";
        if (b.getAvailableCount() <= 0) return "ERROR|Sách đã hết";
        b.addBorrower(user);
        saveBooks();
        return "SUCCESS|Mượn sách thành công";
    }

    private String handleReturn(String user, String title) {
        Book b = books.get(title);
        if (b == null) return "ERROR|Không tìm thấy sách";
        if (!b.getBorrowers().contains(user)) return "ERROR|Bạn chưa mượn sách này";
        b.removeBorrower(user); // trả 1 bản
        saveBooks();
        return "SUCCESS|Trả sách thành công";
    }
}
