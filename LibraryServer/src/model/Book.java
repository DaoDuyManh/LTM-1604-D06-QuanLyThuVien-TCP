package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Book {
    private String title;
    private String author;
    private String category;
    private int quantity;
    private List<String> borrowers; // danh sách username đã mượn (mỗi phần tử = 1 bản)

    public Book(String title, String author, String category, int quantity, List<String> borrowers) {
        this.title = title == null ? "" : title.trim();
        this.author = author == null ? "" : author.trim();
        this.category = category == null ? "" : category.trim();
        this.quantity = Math.max(0, quantity);
        this.borrowers = borrowers == null ? new ArrayList<>() : new ArrayList<>(borrowers);
    }

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getCategory() { return category; }
    public int getQuantity() { return quantity; }
    public List<String> getBorrowers() { return borrowers; }

    public void setQuantity(int q) { this.quantity = Math.max(0, q); }
    public void setBorrowers(List<String> b) { this.borrowers = new ArrayList<>(b); }

    public int getBorrowedCount() { return borrowers.size(); }
    public int getAvailableCount() { return Math.max(0, quantity - getBorrowedCount()); }
    public boolean isAvailable() { return getAvailableCount() > 0; }

 
    public void addBorrower(String username) {
        if (username == null || username.trim().isEmpty()) return;
        String u = username.trim();
        if (getAvailableCount() > 0) {
            borrowers.add(u); // cho phép trùng lặp để biểu diễn mượn nhiều bản
        }
    }

    /**
     * Xóa 1 occurrence của username (trả 1 bản).
     * remove(Object) sẽ xóa occurrence đầu tiên.
     */
    public void removeBorrower(String username) {
        if (username == null) return;
        borrowers.remove(username.trim());
    }

    // Format lưu ra file: title|author|category|quantity|user1,user2,...
    @Override
    public String toString() {
        String b = borrowers.isEmpty() ? "" : String.join(",", borrowers);
        return title + "|" + author + "|" + category + "|" + quantity + "|" + b;
    }

    // Parse từ 1 dòng file (giữ tương thích với file hiện tại)
    public static Book fromString(String line) {
        if (line == null) return null;
        String[] parts = line.split("\\|", -1);
        if (parts.length < 4) return null;
        String title = parts[0].trim();
        String author = parts[1].trim();
        String category = parts[2].trim();
        int quantity = 0;
        try { quantity = Integer.parseInt(parts[3].trim()); } catch (Exception e) { quantity = 0; }
        List<String> borrowers = new ArrayList<>();
        if (parts.length >= 5 && !parts[4].trim().isEmpty()) {
            borrowers = Arrays.stream(parts[4].split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return new Book(title, author, category, quantity, borrowers);
    }

    public Object getId() {
        return null;
    }
}
