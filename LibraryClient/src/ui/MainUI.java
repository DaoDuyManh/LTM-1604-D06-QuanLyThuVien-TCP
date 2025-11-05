package ui;

import client.ClientConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
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

    // --- COLOR PALETTE (Đồng bộ với Login/Register) ---
    private static final Color PRIMARY_BLUE = new Color(0, 123, 255); // Màu chủ đạo
    private static final Color BACKGROUND_LIGHT = new Color(248, 249, 250); // Nền sáng
    private static final Color FOREGROUND_DARK = new Color(33, 37, 41); // Chữ đen đậm
    private static final Color HEADER_BG = PRIMARY_BLUE; // Nền Header Bảng
    private static final Color TABLE_STRIPE = new Color(240, 245, 255); // Màu sọc ngựa vằn

    // --- FONT STYLES ---
    private static final Font MAIN_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 14);


    public MainUI(ClientConnection connection, String username) {
        this.connection = connection;
        this.username = username;

        setTitle("📚 Thư viện - Người dùng: " + username);
        setSize(1100, 680); // Tăng kích thước để thoáng hơn
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND_LIGHT);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(HEADER_FONT);

        // ---------------- TAB 1: Tất cả sách ----------------
        // SỬ DỤNG PHƯƠNG THỨC XÂY DỰNG UI MỚI
        JPanel allPanel = buildAllBooksPanel();
        tabbedPane.addTab("📚 Tất cả sách", allPanel);
        allBooksTable = (JTable) ((JScrollPane) allPanel.getComponent(1)).getViewport().getView(); // Lấy lại JTable
        allBooksModel = (DefaultTableModel) allBooksTable.getModel(); // Lấy lại Model

        // ---------------- TAB 2: Sách đã mượn ----------------
        // SỬ DỤNG PHƯƠNG THỨC XÂY DỰNG UI MỚI
        JPanel myPanel = buildMyBooksPanel();
        tabbedPane.addTab("📖 Sách đã mượn", myPanel);
        myBooksTable = (JTable) ((JScrollPane) myPanel.getComponent(1)).getViewport().getView();
        myBooksModel = (DefaultTableModel) myBooksTable.getModel();

        // ---------------- TAB 3: Đang chờ duyệt ----------------
        // SỬ DỤNG PHƯƠNG THỨC XÂY DỰNG UI MỚI
        JPanel pendingPanel = buildPendingPanel();
        tabbedPane.addTab("📑 Đang chờ duyệt", pendingPanel);
        pendingTable = (JTable) ((JScrollPane) pendingPanel.getComponent(1)).getViewport().getView();
        pendingModel = (DefaultTableModel) pendingTable.getModel();

        // ---------------- TAB 4: Lịch sử ----------------
        // SỬ DỤNG PHƯƠNG THỨC XÂY DỰNG UI MỚI
        JPanel historyPanel = buildHistoryPanel();
        tabbedPane.addTab("📜 Lịch sử", historyPanel);
        historyTable = (JTable) ((JScrollPane) historyPanel.getComponent(1)).getViewport().getView();
        historyModel = (DefaultTableModel) historyTable.getModel();


        add(tabbedPane);

        // ---------------- Load dữ liệu ban đầu (Logic cũ) ----------------
        loadAllBooks();
        loadMyBooks();
        loadPendingBooks();
        loadHistory();

        // ---------------- Sự kiện nút (Logic cũ, được gán trong build*Panel) ----------------
        // Các nút đã được gán sự kiện trong các phương thức build*Panel

        setVisible(true);

        // Không dùng broadcast nữa: bật auto-polling định kỳ để luôn cập nhật
        startAutoPolling();
    }
    
    // --- UTILITY: Tạo Style cho Bảng (Cải tiến hàm gốc) ---
    private void styleTable(JTable table) {
        table.setRowHeight(30);
        table.setFont(MAIN_FONT);
        table.setBackground(Color.WHITE);
        table.setForeground(FOREGROUND_DARK);
        table.setGridColor(new Color(230, 230, 230));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(200, 220, 255));
        
        // Header Style
        JTableHeader header = table.getTableHeader();
        header.setFont(HEADER_FONT);
        header.setBackground(HEADER_BG); 
        header.setForeground(Color.WHITE); // Màu chữ trắng
        header.setPreferredSize(new Dimension(header.getWidth(), 38)); 
        header.setReorderingAllowed(false);
        
        // Center Renderer
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++)
            table.getColumnModel().getColumn(i).setCellRenderer(center);
        
        // Zebra Striping (Sọc ngựa vằn)
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : TABLE_STRIPE); 
                }
                c.setFont(MAIN_FONT);
                return c;
            }
        });
    }

    // --- UTILITY: Tạo Style cho Nút ---
    private void styleButton(JButton button, Color bgColor) {
        button.setFont(BUTTON_FONT);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE); 
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); 
    }

    // ----------------- PANELS BUILDER (Phần bị thay đổi để styling) -----------------

    // --- TAB 1: Tất cả sách ---
    private JPanel buildAllBooksPanel() {
        JPanel allPanel = new JPanel(new BorderLayout());
        allPanel.setBackground(BACKGROUND_LIGHT);
        allPanel.setBorder(new EmptyBorder(15, 15, 15, 15)); // Padding cho Tab

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Tên sách", "Tác giả", "Thể loại", "Tổng", "Có sẵn"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);

        // Panel Tìm kiếm và Tải lại
        JPanel searchPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPane.setBackground(BACKGROUND_LIGHT);
        searchPane.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        JTextField searchField = new JTextField(30);
        searchField.setFont(MAIN_FONT);
        searchField.setPreferredSize(new Dimension(300, 35));
        
        JButton searchBtn = new JButton("🔍 Tìm sách");
        
        styleButton(searchBtn, PRIMARY_BLUE);

        JLabel searchLabel = new JLabel("Tìm kiếm:");
        searchLabel.setFont(LABEL_FONT);
        
        searchPane.add(searchLabel);
        searchPane.add(searchField);
    searchPane.add(searchBtn);

        allPanel.add(searchPane, BorderLayout.NORTH);
        allPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Panel Action
        JPanel actionPanel = new JPanel();
        actionPanel.setBackground(BACKGROUND_LIGHT);
        JButton borrowBtn = new JButton("📗 Yêu cầu Mượn sách");
        styleButton(borrowBtn, new Color(40, 167, 69)); // Xanh lá
        
        actionPanel.add(borrowBtn);
        allPanel.add(actionPanel, BorderLayout.SOUTH);

        // ---------------- Sự kiện nút (Giữ nguyên logic) ----------------
        searchBtn.addActionListener(e -> {
            String key = searchField.getText().trim();
            if (key.isEmpty()) loadAllBooks();
            else searchBooks(key);
        });
        borrowBtn.addActionListener(e -> borrowBook());

        return allPanel;
    }

    // --- TAB 2: Sách đã mượn ---
    private JPanel buildMyBooksPanel() {
        JPanel myPanel = new JPanel(new BorderLayout());
        myPanel.setBackground(BACKGROUND_LIGHT);
        myPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Tên sách", "Tác giả", "Thể loại", "SL mượn", "Ngày mượn", "Ngày trả dự kiến"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);

        JPanel myTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        myTop.setBackground(BACKGROUND_LIGHT);
        myTop.setBorder(new EmptyBorder(0, 0, 10, 0));
        
    // remove manual reload button to rely on realtime updates
        
        JLabel myLabel = new JLabel("📖 Sách bạn đang mượn:");
        myLabel.setFont(LABEL_FONT);
        
        myTop.add(myLabel);
    // no reload button

        myPanel.add(myTop, BorderLayout.NORTH);
        myPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel myActionPanel = new JPanel();
        myActionPanel.setBackground(BACKGROUND_LIGHT);
        JButton returnBtn = new JButton("📕 Trả sách đã chọn");
        styleButton(returnBtn, new Color(220, 53, 69)); // Đỏ
        
        myActionPanel.add(returnBtn);
        myPanel.add(myActionPanel, BorderLayout.SOUTH);
        
        // ---------------- Sự kiện nút (Giữ nguyên logic) ----------------
        returnBtn.addActionListener(e -> returnBook());
        
        return myPanel;
    }

    // --- TAB 3: Đang chờ duyệt ---
    private JPanel buildPendingPanel() {
        JPanel pendingPanel = new JPanel(new BorderLayout());
        pendingPanel.setBackground(BACKGROUND_LIGHT);
        pendingPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Tên sách", "Tác giả", "Thể loại", "Ngày gửi yêu cầu", "Ngày trả dự kiến"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);

        JPanel pendingTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        pendingTop.setBackground(BACKGROUND_LIGHT);
        pendingTop.setBorder(new EmptyBorder(0, 0, 10, 0));
        
    // remove manual reload button to rely on realtime updates
        
        JLabel pendingLabel = new JLabel("📑 Sách đang chờ admin duyệt:");
        pendingLabel.setFont(LABEL_FONT);
        
        pendingTop.add(pendingLabel);
    // no reload button

        pendingPanel.add(pendingTop, BorderLayout.NORTH);
        pendingPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        
        // ---------------- Sự kiện nút (Giữ nguyên logic) ----------------
    // no reload action

        return pendingPanel;
    }

    // --- TAB 4: Lịch sử ---
    private JPanel buildHistoryPanel() {
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBackground(BACKGROUND_LIGHT);
        historyPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Tên sách", "Tác giả", "Thể loại", "Ngày mượn", "Ngày trả", "Trạng thái"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        styleTable(table);

        JPanel historyTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        historyTop.setBackground(BACKGROUND_LIGHT);
        historyTop.setBorder(new EmptyBorder(0, 0, 10, 0));
        
    // remove manual reload button to rely on realtime updates
        
        JLabel historyLabel = new JLabel("📜 Lịch sử giao dịch mượn sách:");
        historyLabel.setFont(LABEL_FONT);
        
        historyTop.add(historyLabel);
    // no reload button

        historyPanel.add(historyTop, BorderLayout.NORTH);
        historyPanel.add(new JScrollPane(table), BorderLayout.CENTER);
        
        // ---------------- Sự kiện nút (Giữ nguyên logic) ----------------
    // no reload action

        return historyPanel;
    }


    // ----------------- LOAD DATA (Giữ nguyên logic) -----------------
    private void loadAllBooks() {
        try {
            connection.sendMessage("LIST_BOOKS");
            String resp = connection.readResponse();
            updateAllBooksTable(resp);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "⚠️ Lỗi tải danh sách sách!");
        }
    }

    // ----------------- AUTO POLLING (không dùng broadcast) -----------------
    private javax.swing.Timer pollTimer;
    private volatile boolean polling = false;
    private void startAutoPolling() {
        pollTimer = new javax.swing.Timer(2000, e -> {
            if (polling) return;
            polling = true;
            new Thread(() -> {
                try {
                    // Gọi server để lấy dữ liệu mới nhất
                    connection.sendMessage("LIST_BOOKS");
                    String listResp = connection.readResponse();

                    connection.sendMessage("MYBOOKS " + username);
                    String myResp = connection.readResponse();

                    connection.sendMessage("PENDING " + username);
                    String pendResp = connection.readResponse();

                    connection.sendMessage("HISTORY " + username);
                    String histResp = connection.readResponse();

                    // Cập nhật UI trên EDT
                    SwingUtilities.invokeLater(() -> {
                        updateAllBooksTable(listResp);
                        updateMyBooksTable(myResp);
                        updatePendingTable(pendResp);
                        updateHistoryTable(histResp);
                    });
                } catch (IOException ignored) {
                } finally {
                    polling = false;
                }
            }, "MainUI-Poller").start();
        });
        pollTimer.setRepeats(true);
        pollTimer.start();
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

    // ----------------- UPDATE TABLE (Giữ nguyên logic) -----------------
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
                String requestDate = formatDate(info[3]);
                String dueDate = formatDate(info[4]);
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

    // ----------------- ACTIONS (Giữ nguyên logic) -----------------
    private void borrowBook() {
        // Tái tạo lại tham chiếu đến allBooksTable và allBooksModel
        // do chúng ta đã thay đổi cách khởi tạo trong constructor
        JTable currentAllBooksTable = (JTable) ((JScrollPane) ((JPanel) ((JTabbedPane) getContentPane().getComponent(0)).getComponentAt(0)).getComponent(1)).getViewport().getView();
        
        int row = currentAllBooksTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "⚠️ Vui lòng chọn sách để mượn.");
            return;
        }
        String title = (String) currentAllBooksTable.getValueAt(row, 0);

        // ---------------- Chọn ngày trả sách với JSpinner ----------------
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1); // Đảm bảo ngày trả >= ngày hiện tại
        Date minDate = calendar.getTime();
        
        // Thiết lập ngày mặc định (ví dụ: sau 7 ngày)
        calendar.add(Calendar.DAY_OF_MONTH, 6); 
        Date defaultDate = calendar.getTime();

        SpinnerDateModel dateModel = new SpinnerDateModel(defaultDate, minDate, null, Calendar.DAY_OF_MONTH);
        JSpinner dateSpinner = new JSpinner(dateModel);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd"));
        
        // Thêm tiêu đề cho hộp thoại
        JPanel datePanel = new JPanel(new BorderLayout(10, 10));
        datePanel.add(new JLabel("Chọn ngày bạn dự kiến trả sách (sau hôm nay):", JLabel.CENTER), BorderLayout.NORTH);
        datePanel.add(dateSpinner, BorderLayout.CENTER);


        int option = JOptionPane.showOptionDialog(
                this,
                datePanel,
                "📗 MƯỢN SÁCH: " + title,
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
        }
    }

    private void returnBook() {
        // Tái tạo lại tham chiếu đến myBooksTable
        JTable currentMyBooksTable = (JTable) ((JScrollPane) ((JPanel) ((JTabbedPane) getContentPane().getComponent(0)).getComponentAt(1)).getComponent(1)).getViewport().getView();
        
        int row = currentMyBooksTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "⚠️ Vui lòng chọn sách để trả.");
            return;
        }
        String title = (String) currentMyBooksTable.getValueAt(row, 0);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Bạn có chắc chắn muốn trả sách: " + title + "?", 
            "Xác nhận trả sách", 
            JOptionPane.YES_NO_OPTION, 
            JOptionPane.QUESTION_MESSAGE);
            
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }


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
        }
    }
}