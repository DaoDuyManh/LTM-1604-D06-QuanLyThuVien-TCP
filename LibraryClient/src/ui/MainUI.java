package ui;

import client.ClientConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;

public class MainUI extends JFrame {
    private final ClientConnection connection;
    private final String username;

    private final DefaultTableModel allBooksModel;
    private final DefaultTableModel myBooksModel;

    private final JTable allBooksTable;
    private final JTable myBooksTable;

    public MainUI(ClientConnection connection, String username) {
        this.connection = connection;
        this.username = username;

        setTitle("📚 Thư viện mượn sách - Người dùng: " + username);
        setSize(950, 620);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // TabPane
        JTabbedPane tabbedPane = new JTabbedPane();

        // -------- TAB 1: Tất cả sách --------
        JPanel allPanel = new JPanel(new BorderLayout());
        allBooksModel = new DefaultTableModel(
                new Object[]{"Tên sách", "Tác giả", "Thể loại", "Tổng", "Có sẵn"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        allBooksTable = new JTable(allBooksModel);
        styleTable(allBooksTable);

        // thanh tìm kiếm
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
        JButton borrowBtn = new JButton("📗 Mượn");
        actionPanel.add(borrowBtn);
        allPanel.add(actionPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("📚 Tất cả sách", allPanel);

        // -------- TAB 2: Sách đã mượn --------
        JPanel myPanel = new JPanel(new BorderLayout());
        myBooksModel = new DefaultTableModel(
                new Object[]{"Tên sách", "Tác giả", "Thể loại", "SL đã mượn"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        myBooksTable = new JTable(myBooksModel);
        styleTable(myBooksTable);

        JPanel myTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton reloadMyBtn = new JButton("⟳ Tải lại");
        myTop.add(new JLabel("📖 Sách bạn đã mượn:"));
        myTop.add(reloadMyBtn);

        myPanel.add(myTop, BorderLayout.NORTH);
        myPanel.add(new JScrollPane(myBooksTable), BorderLayout.CENTER);

        JPanel myActionPanel = new JPanel();
        JButton returnBtn = new JButton("📕 Trả");
        myActionPanel.add(returnBtn);
        myPanel.add(myActionPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("📖 Sách đã mượn", myPanel);

        add(tabbedPane);

        // Load data
        loadAllBooks();
        loadMyBooks();

        // Events
        reloadBtn.addActionListener(e -> loadAllBooks());
        reloadMyBtn.addActionListener(e -> loadMyBooks());
        searchBtn.addActionListener(e -> {
            String key = searchField.getText().trim();
            if (key.isEmpty()) {
                loadAllBooks();
            } else {
                searchBooks(key);
            }
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
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(center);
        }
    }

    // ----------------- LOAD DATA -----------------
    private void loadAllBooks() {
        try {
            connection.sendMessage("LIST_BOOKS");
            String resp = connection.readResponse();
            updateAllBooksTable(resp);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "⚠️ Lỗi khi tải danh sách sách!");
        }
    }

    private void searchBooks(String keyword) {
        try {
            connection.sendMessage("SEARCH " + keyword);
            String resp = connection.readResponse();
            updateAllBooksTable(resp);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "⚠️ Lỗi khi tìm sách!");
        }
    }

    private void loadMyBooks() {
        try {
            connection.sendMessage("MYBOOKS " + username);
            String resp = connection.readResponse();
            updateMyBooksTable(resp);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "⚠️ Lỗi khi tải sách đã mượn!");
        }
    }

    private void updateAllBooksTable(String resp) {
        allBooksModel.setRowCount(0);
        if (resp == null || !resp.startsWith("BOOK_LIST")) return;

        String[] parts = resp.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            String[] info = parts[i].split(",", -1);
            if (info.length >= 5) {
                allBooksModel.addRow(new Object[]{info[0], info[1], info[2], info[3], info[4]});
            }
        }
    }

    private void updateMyBooksTable(String resp) {
        myBooksModel.setRowCount(0);
        if (resp == null || !resp.startsWith("MYBOOKS")) return;

        String[] parts = resp.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            String[] info = parts[i].split(",", -1);
            if (info.length >= 4) { // ✅ phải có 4 cột
                String title = info[0];
                String author = info[1];
                String category = info[2];
                String count = info[3]; // số lượng mượn
                myBooksModel.addRow(new Object[]{title, author, category, count});
            }
        }
    }


    // ----------------- ACTIONS -----------------
    private void borrowBook() {
        int row = allBooksTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "⚠️ Vui lòng chọn một cuốn để mượn.");
            return;
        }
        String title = (String) allBooksModel.getValueAt(row, 0);

        try {
            connection.sendMessage("BORROW " + title);
            String resp = connection.readResponse();
            if (resp.startsWith("SUCCESS")) {
                JOptionPane.showMessageDialog(this, "✅ Mượn thành công: " + title);
            } else {
                JOptionPane.showMessageDialog(this, "❌ " + resp);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "❌ Lỗi kết nối tới server!");
        } finally {
            loadAllBooks();
            loadMyBooks();
        }
    }

    private void returnBook() {
        int row = myBooksTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "⚠️ Vui lòng chọn một cuốn để trả.");
            return;
        }
        String title = (String) myBooksModel.getValueAt(row, 0);

        try {
            connection.sendMessage("RETURN " + title);
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
        }
    }
}
