package ui;

import model.Book;
import model.User;
import server.LibraryServer;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class AdminUI extends JFrame {
    private JTabbedPane tabbedPane;
    private JTable userTable, bookTable;
    private DefaultTableModel userModel, bookModel;
    private static JTextArea logArea;

    private static final String BOOK_FILE = "books.txt";
    private static final String ACCOUNT_FILE = "accounts.txt";

    public AdminUI() {
        setTitle("📚 Library Admin Panel");
        setSize(1100, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
        loadUsers();
        loadBooks();
    }

    private void initUI() {
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Users", buildUserPanel());
        tabbedPane.addTab("Books", buildBookPanel());
        tabbedPane.addTab("Server Log", buildLogPanel());
        add(tabbedPane);
    }

    private JPanel buildUserPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        userModel = new DefaultTableModel(
            new String[]{"Tài khoản", "Mật khẩu", "Số điện thoại", "Địa chỉ", "Email"}, 0
        );
        userTable = new JTable(userModel);
        JScrollPane scroll = new JScrollPane(userTable);

        JPanel control = new JPanel();
        JButton add = new JButton("Thêm User");
        JButton del = new JButton("Xóa User");
        JButton reload = new JButton("Tải lại");
        control.add(add); control.add(del); control.add(reload);

        add.addActionListener(e -> addUser());
        del.addActionListener(e -> deleteUser());
        reload.addActionListener(e -> loadUsers());

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(control, BorderLayout.SOUTH);
        return panel;
    }

    private void loadUsers() {
        SwingUtilities.invokeLater(() -> {
            userModel.setRowCount(0);
            Path p = Paths.get(ACCOUNT_FILE);
            if (!Files.exists(p)) return;
            try {
                List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    User u = User.fromString(line);
                    if (u != null) {
                        userModel.addRow(new Object[]{
                            u.getUsername(), u.getPassword(), u.getPhone(),
                            u.getAddress(), u.getEmail()
                        });
                    }
                }
            } catch (IOException ex) {
                appendLog("❌ Lỗi đọc users: " + ex.getMessage());
            }
        });
    }

    private JPanel buildBookPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        bookModel = new DefaultTableModel(new String[]{"Tên sách", "Tác giả", "Thể loại", "Số lượng", "Available", "Borrowers"}, 0);
        bookTable = new JTable(bookModel);
        bookTable.setRowHeight(26);
        bookTable.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i=3;i<bookTable.getColumnCount();i++) 
            bookTable.getColumnModel().getColumn(i).setCellRenderer(center);

        JScrollPane scroll = new JScrollPane(bookTable);

        JPanel control = new JPanel();
        JButton btnAdd = new JButton("Thêm sách");
        JButton btnDelete = new JButton("Xóa sách");
        JButton btnEditQty = new JButton("Chỉnh số lượng");
        JButton btnForceReturn = new JButton("Force Return");
        JButton btnReload = new JButton("Tải lại");

        control.add(btnAdd); control.add(btnDelete); control.add(btnEditQty); control.add(btnForceReturn); control.add(btnReload);

        btnAdd.addActionListener(e -> addBook());
        btnDelete.addActionListener(e -> deleteBook());
        btnEditQty.addActionListener(e -> editQuantity());
        btnForceReturn.addActionListener(e -> forceReturn());
        btnReload.addActionListener(e -> loadBooks());

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(control, BorderLayout.SOUTH);
        return panel;
    }

    private void loadBooks() {
        SwingUtilities.invokeLater(() -> {
            bookModel.setRowCount(0);
            Path p = Paths.get(BOOK_FILE);
            if (!Files.exists(p)) return;
            try {
                List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    Book b = Book.fromString(line);
                    if (b == null) continue;
                    String borrowers = String.join(",", b.getBorrowers());
                    bookModel.addRow(new Object[]{
                            b.getTitle(), b.getAuthor(), b.getCategory(),
                            b.getQuantity(), b.getAvailableCount(), borrowers
                    });
                }
            } catch (IOException ex) {
                appendLog("❌ Lỗi đọc books: " + ex.getMessage());
            }
        });
    }

    private void addBook() {
        JTextField t = new JTextField();
        JTextField a = new JTextField();
        JTextField c = new JTextField();
        JTextField q = new JTextField("1");
        JPanel p = new JPanel(new GridLayout(4,2));
        p.add(new JLabel("Tên sách:")); p.add(t);
        p.add(new JLabel("Tác giả:")); p.add(a);
        p.add(new JLabel("Thể loại:")); p.add(c);
        p.add(new JLabel("Số lượng:")); p.add(q);
        int r = JOptionPane.showConfirmDialog(this, p, "Thêm sách", JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            try {
                int qty = Integer.parseInt(q.getText().trim());
                Book b = new Book(t.getText().trim(), a.getText().trim(), c.getText().trim(), qty, new ArrayList<>());
                List<Book> all = readAllBooks();
                Optional<Book> ex = all.stream().filter(x -> x.getTitle().equalsIgnoreCase(b.getTitle())).findFirst();
                if (ex.isPresent()) {
                    ex.get().setQuantity(ex.get().getQuantity() + b.getQuantity());
                } else {
                    all.add(b);
                }
                writeAllBooks(all);
                loadBooks();
            } catch (Exception ex) {
                appendLog("❌ Lỗi khi thêm sách: " + ex.getMessage());
            }
        }
    }

    private void deleteBook() {
        int row = bookTable.getSelectedRow();
        if (row < 0) return;
        String title = (String) bookModel.getValueAt(row, 0);
        try {
            List<Book> all = readAllBooks();
            List<Book> keep = all.stream().filter(b -> !b.getTitle().equalsIgnoreCase(title)).collect(Collectors.toList());
            writeAllBooks(keep);
            loadBooks();
        } catch (Exception ex) {
            appendLog("❌ Lỗi xóa sách: " + ex.getMessage());
        }
    }

    private void editQuantity() {
        int row = bookTable.getSelectedRow();
        if (row < 0) return;
        String title = (String) bookModel.getValueAt(row, 0);
        String current = String.valueOf(bookModel.getValueAt(row, 3));
        String input = JOptionPane.showInputDialog(this, "Số lượng mới:", current);
        if (input == null) return;
        try {
            int newQty = Integer.parseInt(input.trim());
            List<Book> all = readAllBooks();
            for (Book b : all) {
                if (b.getTitle().equalsIgnoreCase(title)) {
                    b.setQuantity(newQty);
                }
            }
            writeAllBooks(all);
            loadBooks();
        } catch (Exception ex) {
            appendLog("❌ Lỗi chỉnh số lượng: " + ex.getMessage());
        }
    }

    private void forceReturn() {
        int row = bookTable.getSelectedRow();
        if (row < 0) return;
        String title = (String) bookModel.getValueAt(row, 0);
        String borrowers = (String) bookModel.getValueAt(row, 5);
        if (borrowers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Sách này chưa có ai mượn.");
            return;
        }
        String user = JOptionPane.showInputDialog(this, "Nhập username cần thu hồi:", borrowers);
        if (user == null || user.trim().isEmpty()) return;
        try {
            List<Book> all = readAllBooks();
            for (Book b : all) {
                if (b.getTitle().equalsIgnoreCase(title)) {
                    b.removeBorrower(user.trim());
                }
            }
            writeAllBooks(all);
            loadBooks();
        } catch (Exception ex) {
            appendLog("❌ Lỗi force return: " + ex.getMessage());
        }
    }

    private List<Book> readAllBooks() throws IOException {
        List<Book> all = new ArrayList<>();
        Path p = Paths.get(BOOK_FILE);
        if (!Files.exists(p)) return all;
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        for (String l : lines) {
            if (l.trim().isEmpty()) continue;
            Book b = Book.fromString(l);
            if (b != null) all.add(b);
        }
        return all;
    }

    private void writeAllBooks(List<Book> list) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(BOOK_FILE), StandardCharsets.UTF_8)) {
            for (Book b : list) {
                bw.write(b.toString());
                bw.newLine();
            }
        }
    }

    private JPanel buildLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(logArea);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    public static void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
        });
    }

    private void addUser() {
        JTextField userField = new JTextField();
        JTextField passField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField addressField = new JTextField();
        JTextField emailField = new JTextField();

        JPanel p = new JPanel(new GridLayout(5, 2));
        p.add(new JLabel("Tài khoản:")); p.add(userField);
        p.add(new JLabel("Mật khẩu:")); p.add(passField);
        p.add(new JLabel("Số điện thoại:")); p.add(phoneField);
        p.add(new JLabel("Địa chỉ:")); p.add(addressField);
        p.add(new JLabel("Email:")); p.add(emailField);

        int r = JOptionPane.showConfirmDialog(this, p, "Thêm User", JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            User u = new User(
                userField.getText(),
                passField.getText(),
                phoneField.getText(),
                addressField.getText(),
                emailField.getText()
            );

            if (u.getUsername().isEmpty() || u.getPassword().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tài khoản và mật khẩu không được để trống");
                return;
            }

            try {
                Path path = Paths.get(ACCOUNT_FILE);
                List<String> lines = Files.exists(path) ? Files.readAllLines(path, StandardCharsets.UTF_8) : new ArrayList<>();
                boolean exists = lines.stream().anyMatch(l -> l.startsWith(u.getUsername() + ","));
                if (exists) {
                    JOptionPane.showMessageDialog(this, "User đã tồn tại!");
                    return;
                }
                lines.add(u.toString());
                Files.write(path, lines, StandardCharsets.UTF_8);
                loadUsers();
            } catch (IOException ex) {
                appendLog("❌ Lỗi thêm user: " + ex.getMessage());
            }
        }
    }

    private void deleteUser() {
        int row = userTable.getSelectedRow();
        if (row < 0) return;
        String user = (String) userModel.getValueAt(row, 0);
        int c = JOptionPane.showConfirmDialog(this, "Xóa user: " + user + " ?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) {
            try {
                Path path = Paths.get(ACCOUNT_FILE);
                if (!Files.exists(path)) return;
                List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                List<String> keep = lines.stream()
                        .filter(l -> !l.startsWith(user + ","))
                        .toList();
                Files.write(path, keep, StandardCharsets.UTF_8);
                loadUsers();
            } catch (IOException ex) {
                appendLog("❌ Lỗi xóa user: " + ex.getMessage());
            }
        }
    }

    // ----------------- MAIN -----------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AdminUI ui = new AdminUI();
            ui.setVisible(true);
            appendLog("✅ AdminUI đã khởi động...");

            // Khởi động server trong background
            new Thread(() -> {
                try {
                    server.LibraryServer.main(new String[]{});
                } catch (Exception e) {
                    if (e.getCause() instanceof java.net.BindException 
                        || e instanceof java.net.BindException) {
                        appendLog("⚠️ Server đã chạy rồi, bỏ qua khởi động lại.");
                    } else {
                        appendLog("❌ Lỗi khởi động server: " + e.getMessage());
                    }
                }
            }).start();
            appendLog("⏳ Đang khởi động server...");
        });
    }
}
