package ui;

import client.ClientConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainUI extends JFrame {
    private final ClientConnection connection;
    private final String username;

    private final DefaultTableModel allBooksModel;
    private final DefaultTableModel myBooksModel;
    private final DefaultTableModel pendingModel;
    private final DefaultTableModel historyModel;

    private final JTable allBooksTable;
    private final JTable myBooksTable;
    private final JTable pendingTable;
    private final JTable historyTable;

    private final Map<String, String[]> books = new HashMap<>();

    private final SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public MainUI(ClientConnection connection, String username) {
        this.connection = connection;
        this.username = username;

        setTitle("📚 Thư viện - Người dùng: " + username);
        setSize(1000, 620);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        // ---------------- TAB 1: Tất cả sách ----------------
        JPanel allPanel = new JPanel(new BorderLayout());
        allBooksModel = new DefaultTableModel(
                new Object[]{"Tên sách", "Tác giả", "Thể loại", "Tổng", "Có sẵn"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        allBooksTable = new JTable(allBooksModel);
        styleTable(allBooksTable);

        JPanel searchPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = new JTextField(25);
        JButton searchBtn = new JButton("🔍 Tìm");
        JButton reloadBtn = new JButton("⟳ Tải lại");
        searchPane.add(new JLabel("Tìm sách:"));
        searchPane.add(searchField);
        searchPane.add(searchBtn);
        searchPane.add(reloadBtn);

        allPanel.add(searchPane, BorderLayout.NORTH);
        allPanel.add(new JScrollPane(allBooksTable), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel();
        JButton borrowBtn = new JButton("📗 Mượn sách");
        actionPanel.add(borrowBtn);
        allPanel.add(actionPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("📚 Tất cả sách", allPanel);

        // ---------------- TAB 2: Sách đã mượn ----------------
        JPanel myPanel = new JPanel(new BorderLayout());
        myBooksModel = new DefaultTableModel(
                new Object[]{"Tên sách", "Tác giả", "Thể loại", "SL mượn", "Ngày mượn", "Ngày trả"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        myBooksTable = new JTable(myBooksModel);
        styleTable(myBooksTable);

        JPanel myTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton reloadMyBtn = new JButton("⟳ Tải lại");
        myTop.add(new JLabel("📖 Sách bạn đang mượn:"));
        myTop.add(reloadMyBtn);

        myPanel.add(myTop, BorderLayout.NORTH);
        myPanel.add(new JScrollPane(myBooksTable), BorderLayout.CENTER);

        JPanel myActionPanel = new JPanel();
        JButton returnBtn = new JButton("📕 Trả sách");
        myActionPanel.add(returnBtn);
        myPanel.add(myActionPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("📖 Sách đã mượn", myPanel);

        // ---------------- TAB 3: Đang chờ duyệt ----------------
        JPanel pendingPanel = new JPanel(new BorderLayout());
        pendingModel = new DefaultTableModel(
                new Object[]{"Tên sách", "Tác giả", "Thể loại", "Ngày gửi yêu cầu", "Ngày trả"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        pendingTable = new JTable(pendingModel);
        styleTable(pendingTable);

        JPanel pendingTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton reloadPendingBtn = new JButton("⟳ Tải lại");
        pendingTop.add(new JLabel("📑 Sách đang chờ duyệt:"));
        pendingTop.add(reloadPendingBtn);

        pendingPanel.add(pendingTop, BorderLayout.NORTH);
        pendingPanel.add(new JScrollPane(pendingTable), BorderLayout.CENTER);

        tabbedPane.addTab("📑 Đang chờ duyệt", pendingPanel);

        // ---------------- TAB 4: Lịch sử ----------------
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyModel = new DefaultTableModel(
                new Object[]{"Tên sách", "Tác giả", "Thể loại", "Ngày mượn", "Ngày trả", "Trạng thái"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        historyTable = new JTable(historyModel);
        styleTable(historyTable);

        JPanel historyTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton reloadHistoryBtn = new JButton("⟳ Tải lại");
        historyTop.add(new JLabel("📜 Lịch sử mượn sách:"));
        historyTop.add(reloadHistoryBtn);

        historyPanel.add(historyTop, BorderLayout.NORTH);
        historyPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        tabbedPane.addTab("📜 Lịch sử", historyPanel);

        add(tabbedPane);

        // ---------------- Load dữ liệu ban đầu ----------------
        loadAllBooks();
        loadMyBooks();
        loadPendingBooks();
        loadHistory();

        // ---------------- Sự kiện nút ----------------
        reloadBtn.addActionListener(e -> loadAllBooks());
        reloadMyBtn.addActionListener(e -> loadMyBooks());
        reloadPendingBtn.addActionListener(e -> loadPendingBooks());
        reloadHistoryBtn.addActionListener(e -> loadHistory());
        searchBtn.addActionListener(e -> {
            String key = searchField.getText().trim();
            if (key.isEmpty()) loadAllBooks();
            else searchBooks(key);
        });
        borrowBtn.addActionListener(e -> borrowBook());
        returnBtn.addActionListener(e -> returnBook());

        setVisible(true);
    }

    private void styleTable(JTable table) {
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++)
            table.getColumnModel().getColumn(i).setCellRenderer(center);
    }

    // ----------------- LOAD DATA -----------------
    private void loadAllBooks() {
        try {
            connection.sendMessage("LIST_BOOKS");
            String resp = connection.readResponse();
            updateAllBooksTable(resp);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "⚠️ Lỗi tải danh sách sách!");
        }
    }

    private void searchBooks(String keyword) {
        try {
            connection.sendMessage("SEARCH " + keyword);
            String resp = connection.readResponse();
            updateAllBooksTable(resp);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "⚠️ Lỗi tìm sách!");
        }
    }

    private void loadMyBooks() {
        try {
            connection.sendMessage("MYBOOKS " + username);
            String resp = connection.readResponse();
            updateMyBooksTable(resp);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "⚠️ Lỗi tải sách đã mượn!");
        }
    }

    private void loadPendingBooks() {
        try {
            connection.sendMessage("PENDING " + username);
            String resp = connection.readResponse();
            updatePendingTable(resp);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "⚠️ Lỗi tải sách đang chờ duyệt!");
        }
    }

    private void loadHistory() {
        try {
            connection.sendMessage("HISTORY " + username);
            String resp = connection.readResponse();
            updateHistoryTable(resp);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "⚠️ Lỗi tải lịch sử!");
        }
    }

    // ----------------- UPDATE TABLE -----------------
    private void updateAllBooksTable(String resp) {
        allBooksModel.setRowCount(0);
        books.clear();
        if (resp == null || !resp.startsWith("BOOK_LIST")) return;

        String[] parts = resp.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            String[] info = parts[i].split(",", -1);
            if (info.length >= 5) {
                allBooksModel.addRow(new Object[]{info[0], info[1], info[2], info[3], info[4]});
                books.put(info[0], new String[]{info[1], info[2]});
            }
        }
    }

    private void updateMyBooksTable(String resp) {
        myBooksModel.setRowCount(0);
        if (resp == null || !resp.startsWith("MYBOOKS")) return;

        String[] parts = resp.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            String[] info = parts[i].split(",", -1);
            if (info.length >= 6) {
                String title = info[0];
                String[] bookInfo = books.getOrDefault(title, new String[]{"", ""});
                String borrowDate = formatDate(info[4]);
                String dueDate = formatDate(info[5]);
                myBooksModel.addRow(new Object[]{title, bookInfo[0], bookInfo[1], info[3], borrowDate, dueDate});
            }
        }
    }

    private void updatePendingTable(String resp) {
        pendingModel.setRowCount(0);
        if (resp == null || !resp.startsWith("PENDING")) return;

        String[] parts = resp.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            String[] info = parts[i].split(",", -1);
            if (info.length >= 5) {
                String title = info[0];
                String[] bookInfo = books.getOrDefault(title, new String[]{"", ""});
                String requestDate = info[3];
                String dueDate = info[4];
                pendingModel.addRow(new Object[]{title, bookInfo[0], bookInfo[1], requestDate, dueDate});
            }
        }
    }

    private void updateHistoryTable(String resp) {
        historyModel.setRowCount(0);
        if (resp == null || !resp.startsWith("HISTORY")) return;

        String[] parts = resp.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            String[] info = parts[i].split(",", -1);
            if (info.length >= 5) {
                String title = info[0];
                String[] bookInfo = books.getOrDefault(title, new String[]{"", ""});
                String borrowDate = formatDate(info[3 - 1]);
                String returnDate = formatDate(info[4 - 1]);
                String status = info[info.length - 1];
                historyModel.addRow(new Object[]{title, bookInfo[0], bookInfo[1], borrowDate, returnDate, status});
            }
        }
    }

    private String formatDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        try {
            Date date = inputDateFormat.parse(raw);
            return outputDateFormat.format(date);
        } catch (ParseException e) {
            return raw;
        }
    }

    // ----------------- ACTIONS -----------------
    private void borrowBook() {
        int row = allBooksTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "⚠️ Vui lòng chọn sách để mượn.");
            return;
        }
        String title = (String) allBooksTable.getValueAt(row, 0);

        // ---------------- Chọn ngày trả sách với JSpinner ----------------
        SpinnerDateModel dateModel = new SpinnerDateModel(new Date(), new Date(), null, Calendar.DAY_OF_MONTH);
        JSpinner dateSpinner = new JSpinner(dateModel);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd"));

        int option = JOptionPane.showOptionDialog(
                this,
                dateSpinner,
                "Chọn ngày trả sách",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                null
        );

        if (option != JOptionPane.OK_OPTION) {
            return;
        }

        Date selectedDate = (Date) dateSpinner.getValue();
        String dueDate = new SimpleDateFormat("yyyy-MM-dd").format(selectedDate);

        try {
            connection.sendMessage("BORROW_REQUEST " + username + "|" + title + "|" + dueDate);
            String resp = connection.readResponse();
            if (resp.startsWith("PENDING_OK")) {
                JOptionPane.showMessageDialog(this, "✅ Yêu cầu mượn đã gửi, chờ admin duyệt.");
            } else {
                JOptionPane.showMessageDialog(this, "❌ " + resp);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "❌ Lỗi kết nối tới server!");
        } finally {
            loadAllBooks();
            loadMyBooks();
            loadPendingBooks();
            loadHistory();
        }
    }

    private void returnBook() {
        int row = myBooksTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "⚠️ Vui lòng chọn sách để trả.");
            return;
        }
        String title = (String) myBooksTable.getValueAt(row, 0);

        try {
            connection.sendMessage("RETURN " + username + "|" + title);
            String resp = connection.readResponse();
            if (resp.startsWith("SUCCESS")) {
                JOptionPane.showMessageDialog(this, "📗 Trả thành công: " + title);
            } else {
                JOptionPane.showMessageDialog(this, "❌ " + resp);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "❌ Lỗi kết nối tới server!");
        } finally {
            loadAllBooks();
            loadMyBooks();
            loadPendingBooks();
            loadHistory();
        }
    }
}
