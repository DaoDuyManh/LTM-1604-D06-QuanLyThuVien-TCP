package ui;

import model.Book;
import model.User;
import server.CommandProcessor;
// import util.Config; // vẫn dùng cho SERVER_PORT ở startServer()

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
// import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class AdminUI extends JFrame {
    private JTabbedPane tabbedPane;
    private JTable userTable, bookTable, pendingTable;
    private DefaultTableModel userModel, bookModel, pendingModel;
    
    // Borrow History - 3 bảng riêng
    private JTable borrowingUsersTable;
    private DefaultTableModel borrowingUsersModel;
    private JTable currentBorrowsTable;
    private DefaultTableModel currentBorrowsModel;
    private JTable returnedBooksTable;
    private DefaultTableModel returnedBooksModel;
    private static JTextArea logArea;

    private static final String BOOK_FILE = "books.txt";
    private static final String ACCOUNT_FILE = "accounts.txt";
    private static final String PENDING_FILE = "pending.txt";
    private static final String BORROW_HISTORY_FILE = "borrow_history.txt";
    // private static final String SERVER_HOST = "localhost"; // host của LibraryServer (không dùng)

    private CommandProcessor processor;

    // Server nội bộ (theo yêu cầu): Start/Stop trực tiếp trong AdminUI
    private ServerSocket serverSocket;
    private Thread serverThread;

 // 🎨 Light Theme - CÁC HẰNG SỐ MÀU
    private static final Color BACKGROUND_LIGHT   = Color.WHITE;              
    private static final Color BACKGROUND_STRIPE  = new Color(248, 249, 250); 
    private static final Color BACKGROUND_CONTROL = new Color(230, 230, 230); 
    private static final Color FOREGROUND_DARK    = Color.BLACK;              
    private static final Color TABLE_HEADER_BG    = new Color(0, 102, 255);  
    private static final Color SELECTION_COLOR    = new Color(200, 220, 255); 
    private static final Color GRID_COLOR_LIGHT = new Color(220, 220, 220); // Màu kẻ mờ

    // Nút Accent
    // private static final Color BUTTON_PRIMARY = new Color(52, 152, 219);  
    private static final Color BUTTON_SUCCESS = new Color(46, 204, 113); 
    private static final Color BUTTON_DANGER  = new Color(231, 76, 60);  
    private static final Color BUTTON_CONTROL_FG = new Color(33, 37, 41); 
    private static final Color BUTTON_CONTROL_BG = new Color(210, 210, 210); 


    
    private static final Font MAIN_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 15);

    public AdminUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // Ignored
        }

        setTitle("📚 Library Admin Panel");
        setSize(1200, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        processor = new CommandProcessor();

        initUI();

        loadUsers();
        loadBooks();
        loadPendingRequests();
        loadBorrowHistory();

        // Bật cơ chế auto-refresh định kỳ (không dùng broadcast)
        startAutoRefresh();
    }
    
    // --- Phương thức Helper để tùy chỉnh Bảng (Đã hoàn thiện) ---
    private void customizeTable(JTable table) {
        table.setFont(MAIN_FONT);
        table.setRowHeight(30); 
        table.setSelectionBackground(SELECTION_COLOR);
        table.setSelectionForeground(FOREGROUND_DARK);
        table.setBackground(BACKGROUND_LIGHT);
        table.setForeground(FOREGROUND_DARK);
        
        // Cấu hình lưới: Hiển thị cả đường kẻ ngang và dọc, với màu mờ
        table.setShowGrid(true); 
        table.setShowVerticalLines(true); 
        table.setIntercellSpacing(new Dimension(1, 1)); 
        table.setGridColor(GRID_COLOR_LIGHT); // Màu lưới xám rất nhạt

        // Sọc ngựa vằn (Zebra Striping)
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? BACKGROUND_LIGHT : BACKGROUND_STRIPE);
                }
                c.setFont(MAIN_FONT);
                // Căn giữa nội dung ô (chỉ cho các cột số)
                if (column >= 3) {
                     setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                     setHorizontalAlignment(SwingConstants.LEFT);
                }
                return c;
            }
        });
        
        JTableHeader header = table.getTableHeader();
        header.setFont(HEADER_FONT);
        header.setBackground(TABLE_HEADER_BG);
        header.setForeground(Color.WHITE); 
        header.setPreferredSize(new Dimension(header.getWidth(), 35)); 
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);
        
        // Căn giữa tiêu đề cột và áp dụng đường kẻ dọc MỜ cho Header
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer(){
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JComponent c = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(TABLE_HEADER_BG);
                c.setForeground(Color.WHITE);
                c.setFont(HEADER_FONT);
                setHorizontalAlignment(SwingConstants.CENTER);
                
                // Áp dụng đường kẻ dọc MỜ (viền phải 1px, màu xám nhạt)
                c.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, GRID_COLOR_LIGHT)); 
                
                return c;
            }
        };
        header.setDefaultRenderer(headerRenderer);
        
        // Đường kẻ ngang dưới header vẫn giữ nguyên
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, TABLE_HEADER_BG.darker())); 
    }
    
    // --- Phương thức Helper để tùy chỉnh Nút Accent (Màu nền) ---
    private void customizeButton(JButton button, Color bgColor, Color fgColor) {
        button.setFont(MAIN_FONT);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        // Nút có màu nền nổi bật
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgColor.darker(), 1), 
            BorderFactory.createEmptyBorder(7, 14, 7, 14) 
        ));
    }
    
    // Overload cho nút thông thường (Control/Secondary - Nút Tải lại, Edit Qty)
    private void customizeButton(JButton button) {
        button.setFont(MAIN_FONT);
        button.setBackground(BUTTON_CONTROL_BG); // Nền xám
        button.setForeground(BUTTON_CONTROL_FG); // Chữ đen
        button.setFocusPainted(false);
        // Nút phẳng, viền xám nhẹ
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BUTTON_CONTROL_BG.darker(), 1), 
            BorderFactory.createEmptyBorder(7, 14, 7, 14) 
        ));
    }

    private void initUI() {
        getContentPane().setBackground(BACKGROUND_LIGHT);
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(HEADER_FONT);
        tabbedPane.setBackground(BACKGROUND_LIGHT);
        tabbedPane.setForeground(FOREGROUND_DARK); 

        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
        UIManager.put("TabbedPane.tabInsets", new Insets(8, 15, 8, 15)); 
        UIManager.put("TabbedPane.selectedBackground", BACKGROUND_LIGHT); 
        UIManager.put("TabbedPane.selected", BACKGROUND_CONTROL); 


        
        tabbedPane.addTab("👥 Users", buildUserPanel());
        tabbedPane.addTab("📖 Books", buildBookPanel());
        tabbedPane.addTab("⏳ Pending Requests", buildPendingPanel());
        tabbedPane.addTab("📜 Borrow History", buildHistoryPanel());

    JPanel serverPanel = new JPanel(new BorderLayout());
    serverPanel.setBackground(BACKGROUND_LIGHT);
    serverPanel.add(buildLogPanel(), BorderLayout.CENTER);
    serverPanel.add(buildServerControlPanel(), BorderLayout.SOUTH);
    tabbedPane.addTab("⚙️ Server Log", serverPanel);

        add(tabbedPane);
    }

    // -------------------- Users --------------------
    private JPanel buildUserPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_LIGHT);
        
        userModel = new DefaultTableModel(new String[]{"Tài khoản", "Mật khẩu", "SĐT", "Địa chỉ", "Email"},0);
        userTable = new JTable(userModel);
        customizeTable(userTable);
        
        // Áp dụng wrapper panel và padding cho bảng UserUI
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setBorder(new EmptyBorder(10, 10, 10, 10)); // Padding 10px 
        JScrollPane scroll = new JScrollPane(userTable);
        scroll.getViewport().setBackground(BACKGROUND_LIGHT);
        tableWrapper.add(scroll, BorderLayout.CENTER);

        JPanel control = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15)); 
        control.setBackground(BACKGROUND_CONTROL);
        control.setBorder(new EmptyBorder(10, 10, 10, 10));
        
    JButton add = new JButton("➕ Thêm User");
    JButton del = new JButton("🗑️ Xóa User");
        
    customizeButton(add, BACKGROUND_CONTROL, BUTTON_SUCCESS.darker()); 
    customizeButton(del, BACKGROUND_CONTROL, BUTTON_DANGER.darker()); 

        
    control.add(add); control.add(del);

        add.addActionListener(e -> addUser());
        del.addActionListener(e -> deleteUser());
    // bỏ nút tải lại, cập nhật tự động qua broadcast

        panel.add(tableWrapper, BorderLayout.CENTER); // Thêm wrapper panel
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
            } catch (IOException ex) { appendLog("❌ Lỗi đọc users: " + ex.getMessage()); }
        });
    }

    private void addUser(){
        JTextField userField = new JTextField();
        JTextField passField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField addressField = new JTextField();
        JTextField emailField = new JTextField();
        
        JPanel p = new JPanel(new GridLayout(5,2, 5, 5));
        p.add(new JLabel("Tài khoản:", SwingConstants.RIGHT)); p.add(userField);
        p.add(new JLabel("Mật khẩu:", SwingConstants.RIGHT)); p.add(passField);
        p.add(new JLabel("Số điện thoại:", SwingConstants.RIGHT)); p.add(phoneField);
        p.add(new JLabel("Địa chỉ:", SwingConstants.RIGHT)); p.add(addressField);
        p.add(new JLabel("Email:", SwingConstants.RIGHT)); p.add(emailField);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        int r=JOptionPane.showConfirmDialog(this,p,"Thêm User",JOptionPane.OK_CANCEL_OPTION);
        if(r==JOptionPane.OK_OPTION){
            String cmd = String.join(":", "REGISTER",
                    userField.getText().trim(), passField.getText().trim(),
                    phoneField.getText().trim(), addressField.getText().trim(), emailField.getText().trim());
            String resp = processor.process(cmd, null);
            JOptionPane.showMessageDialog(this, resp);
            loadUsers();
        }
    }

    private void deleteUser(){
        int row=userTable.getSelectedRow(); if(row<0) return;
        String user=(String)userModel.getValueAt(row,0);
        int c=JOptionPane.showConfirmDialog(this,"Xóa user: "+user+" ?\nHành động này không thể hoàn tác.", "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if(c==JOptionPane.YES_OPTION){
            try{
                Path path=Paths.get(ACCOUNT_FILE);
                if(!Files.exists(path)) return;
                List<String> lines=Files.readAllLines(path,StandardCharsets.UTF_8);
                List<String> keep=lines.stream().filter(l->!l.startsWith(user+",")).collect(Collectors.toList());
                Files.write(path,keep,StandardCharsets.UTF_8);
                loadUsers();
            }catch(IOException ex){ appendLog("❌ Lỗi xóa user: " + ex.getMessage()); }
        }
    }

    // -------------------- Books --------------------
    private JPanel buildBookPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_LIGHT);
        
        bookModel = new DefaultTableModel(new String[]{"Tên sách","Tác giả","Thể loại","Số lượng","Available","Borrowers"},0);
        bookTable = new JTable(bookModel);
        customizeTable(bookTable);
        
        // Căn giữa các cột số lượng
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i=3;i<bookTable.getColumnCount();i++)
            bookTable.getColumnModel().getColumn(i).setCellRenderer(center);
        
        // Áp dụng wrapper panel và padding cho bảng BookUI
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setBorder(new EmptyBorder(10, 10, 10, 10)); // Padding 10px
        JScrollPane scroll = new JScrollPane(bookTable);
        scroll.getViewport().setBackground(BACKGROUND_LIGHT);
        tableWrapper.add(scroll, BorderLayout.CENTER);


        JPanel control = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        control.setBackground(BACKGROUND_CONTROL);
        control.setBorder(new EmptyBorder(10, 10, 10, 10));

    JButton btnAdd = new JButton("➕ Thêm sách");
    JButton btnDelete = new JButton("🗑️ Xóa sách");
    JButton btnEditQty = new JButton("✍️ Chỉnh số lượng");
    JButton btnImport = new JButton("⬇️ Import");
        
        customizeButton(btnAdd, BUTTON_SUCCESS, Color.WHITE); 
        customizeButton(btnDelete, BUTTON_DANGER, Color.WHITE); 
    customizeButton(btnEditQty); 
    customizeButton(btnImport, new Color(52, 152, 219), Color.WHITE); 
        
        
    control.add(btnAdd); control.add(btnDelete); control.add(btnEditQty); control.add(btnImport);

        btnAdd.addActionListener(e -> addBook());
        btnDelete.addActionListener(e -> deleteBook());
        btnEditQty.addActionListener(e -> editQuantity());
    btnImport.addActionListener(e -> importBooks());

        panel.add(tableWrapper, BorderLayout.CENTER); // Thêm wrapper panel
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
                for (String l : lines) {
                    if (l.trim().isEmpty()) continue;
                    Book b = Book.fromString(l); 
                    if (b == null) continue;
                    String borrowers = String.join(",", b.getBorrowers());
                    bookModel.addRow(new Object[]{
                            b.getTitle(), b.getAuthor(), b.getCategory(),
                            b.getQuantity(), b.getAvailableCount(), borrowers
                    });
                }
            } catch (IOException ex) { appendLog("❌ Lỗi đọc books: " + ex.getMessage()); }
        });
    }

    private void addBook() {
        JTextField t = new JTextField(); JTextField a = new JTextField(); JTextField c = new JTextField(); JTextField q = new JTextField("1");
        JPanel p = new JPanel(new GridLayout(4,2, 5, 5));
        p.add(new JLabel("Tên sách:", SwingConstants.RIGHT)); p.add(t);
        p.add(new JLabel("Tác giả:", SwingConstants.RIGHT)); p.add(a);
        p.add(new JLabel("Thể loại:", SwingConstants.RIGHT)); p.add(c);
        p.add(new JLabel("Số lượng:", SwingConstants.RIGHT)); p.add(q);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        int r = JOptionPane.showConfirmDialog(this,p,"Thêm sách mới",JOptionPane.OK_CANCEL_OPTION);
        if(r==JOptionPane.OK_OPTION){
            try{
                int qty=Integer.parseInt(q.getText().trim());
                Book b=new Book(t.getText().trim(),a.getText().trim(),c.getText().trim(),qty,new ArrayList<>()); 
                List<Book> all=readAllBooks();
                Optional<Book> ex=all.stream().filter(x->x.getTitle().equalsIgnoreCase(b.getTitle())).findFirst();
                if(ex.isPresent()){ JOptionPane.showMessageDialog(this,"Sách đã tồn tại!"); return; }
                all.add(b);
                writeAllBooks(all);
                // Không dùng broadcast/SYNC_ALL nữa
                loadBooks();
            }catch(NumberFormatException ex){ JOptionPane.showMessageDialog(this,"Số lượng không hợp lệ"); }
            catch(IOException ex){ appendLog("❌ Lỗi thao tác file: " + ex.getMessage()); }
        }
    }

    private void deleteBook(){
        int row=bookTable.getSelectedRow(); if(row<0) return;
        String t=(String)bookModel.getValueAt(row,0);
        int c=JOptionPane.showConfirmDialog(this,"Xóa sách: "+t+" ?\n(Nên Force Return trước)", "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if(c==JOptionPane.YES_OPTION){
            try{
                List<Book> all=readAllBooks();
                all=all.stream().filter(b->!b.getTitle().equalsIgnoreCase(t)).collect(Collectors.toList());
                writeAllBooks(all);
                // Không dùng broadcast/SYNC_ALL nữa
                loadBooks();
            }catch(IOException ex){ appendLog("❌ Lỗi thao tác file: "+ex.getMessage()); }
        }
    }

    private void editQuantity(){
        int row=bookTable.getSelectedRow(); if(row<0) return;
        String t=(String)bookModel.getValueAt(row,0);
        String s=JOptionPane.showInputDialog(this,"Nhập số lượng mới cho sách '"+t+"':");
        if(s==null) return;
        try{
            int qty=Integer.parseInt(s);
            List<Book> all=readAllBooks();
            all.forEach(b->{ if(b.getTitle().equalsIgnoreCase(t)) b.setQuantity(qty); }); 
            writeAllBooks(all);
            // Không dùng broadcast/SYNC_ALL nữa
            loadBooks();
        }catch(NumberFormatException ex){ JOptionPane.showMessageDialog(this,"Số lượng không hợp lệ"); }
            catch(IOException ex){ appendLog("❌ Lỗi thao tác file: " + ex.getMessage()); }
    }

    private void importBooks(){
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn file import (CSV hoặc TXT)");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV/TXT files", "csv", "txt"));
        int r = chooser.showOpenDialog(this);
        if (r != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        int total = 0, added = 0, updated = 0, skipped = 0;
        try {
            List<Book> all = readAllBooks();
            Map<String, Book> byTitle = new HashMap<>();
            for (Book b : all) byTitle.put(b.getTitle().toLowerCase(), b);

            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    total++;
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) { skipped++; continue; }

                    String lower = line.toLowerCase();
                    // Bỏ qua dòng header phổ biến
                    if (lower.contains("title") || lower.contains("tên") && lower.contains("tác giả")) { skipped++; continue; }

                    String delim = line.contains("|") ? "\\|" : ",";
                    String[] parts = line.split(delim, -1);

                    String title = "";
                    String author = "";
                    String category = "";
                    int qty = 1;
                    try {
                        if (parts.length >= 4) {
                            title = parts[0].trim();
                            author = parts[1].trim();
                            category = parts[2].trim();
                            qty = parseIntSafe(parts[3].trim(), 1);
                        } else if (parts.length == 3) {
                            title = parts[0].trim();
                            author = parts[1].trim();
                            category = parts[2].trim();
                        } else if (parts.length == 2) {
                            title = parts[0].trim();
                            if (isNumeric(parts[1].trim())) qty = Integer.parseInt(parts[1].trim());
                            else author = parts[1].trim();
                        } else if (parts.length == 1) {
                            title = parts[0].trim();
                        }
                    } catch (Exception ex) {
                        skipped++;
                        continue;
                    }

                    if (title.isEmpty()) { skipped++; continue; }
                    if (qty <= 0) qty = 1;

                    Book exist = byTitle.get(title.toLowerCase());
                    if (exist != null) {
                        exist.setQuantity(exist.getQuantity() + qty);
                        updated++;
                    } else {
                        Book nb = new Book(title, author, category, qty, new ArrayList<>());
                        all.add(nb);
                        byTitle.put(title.toLowerCase(), nb);
                        added++;
                    }
                }
            }

            writeAllBooks(all);
            loadBooks();
            JOptionPane.showMessageDialog(this, String.format(
                "Import hoàn tất.\nTổng dòng: %d\nThêm mới: %d\nCập nhật số lượng: %d\nBỏ qua: %d",
                total, added, updated, skipped
            ));
        } catch (IOException ex) {
            appendLog("❌ Lỗi import: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Lỗi import: " + ex.getMessage());
        }
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private boolean isNumeric(String s) {
        try { Integer.parseInt(s); return true; } catch (Exception e) { return false; }
    }

    // Force Return chức năng cũ đã gỡ bỏ theo yêu cầu; thay bằng Import sách

    private List<Book> readAllBooks() throws IOException{
        Path p=Paths.get(BOOK_FILE);
        if(!Files.exists(p)) return new ArrayList<>();
        List<Book> res=new ArrayList<>();
        for(String l:Files.readAllLines(p,StandardCharsets.UTF_8)){
            Book b=Book.fromString(l); if(b!=null) res.add(b);
        }
        return res;
    }

    private void writeAllBooks(List<Book> books) throws IOException{
        List<String> lines=books.stream().map(Book::toString).collect(Collectors.toList());
        Files.write(Paths.get(BOOK_FILE), lines, StandardCharsets.UTF_8);
    }

    // -------------------- Pending Requests --------------------
    private JPanel buildPendingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_LIGHT);
        
        pendingModel = new DefaultTableModel(new String[]{"User","Book","Request Date","Due Date","Status"},0);
        pendingTable = new JTable(pendingModel);
        customizeTable(pendingTable);
        
        // Áp dụng wrapper panel và padding cho bảng Pending
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setBorder(new EmptyBorder(10, 10, 10, 10)); // Padding 10px
        JScrollPane scroll = new JScrollPane(pendingTable);
        scroll.getViewport().setBackground(BACKGROUND_LIGHT);
        tableWrapper.add(scroll, BorderLayout.CENTER);


        JPanel control = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        control.setBackground(BACKGROUND_CONTROL);
        control.setBorder(new EmptyBorder(10, 10, 10, 10));

    JButton btnAccept = new JButton("✔ Chấp nhận");
    JButton btnReject = new JButton("✖ Từ chối");
        
        customizeButton(btnAccept, BUTTON_SUCCESS, Color.WHITE); 
    customizeButton(btnReject, BUTTON_DANGER, Color.WHITE); 
        
    control.add(btnAccept); control.add(btnReject);

    // bỏ nút tải lại, rely on broadcast

        btnAccept.addActionListener(e->{
            int row = pendingTable.getSelectedRow();
            if(row < 0) { JOptionPane.showMessageDialog(this, "Vui lòng chọn yêu cầu để chấp nhận."); return; }
            String user = (String) pendingModel.getValueAt(row,0);
            String book = (String) pendingModel.getValueAt(row,1);
            String cmd = "ACCEPT_BORROW:" + user + ":" + book;
            String resp = processor.process(cmd, "admin");
            JOptionPane.showMessageDialog(this, resp);
            // tự refresh tại chỗ
            loadPendingRequests();
            loadBooks();
            loadBorrowHistory();
        });

        btnReject.addActionListener(e->{
            int row = pendingTable.getSelectedRow();
            if(row < 0) { JOptionPane.showMessageDialog(this, "Vui lòng chọn yêu cầu để từ chối."); return; }
            String user = (String) pendingModel.getValueAt(row,0);
            String book = (String) pendingModel.getValueAt(row,1);
            String cmd = "REJECT_BORROW:" + user + ":" + book;
            String resp = processor.process(cmd, "admin");
            JOptionPane.showMessageDialog(this, resp);
            loadPendingRequests();
            loadBorrowHistory();
        });

        panel.add(tableWrapper, BorderLayout.CENTER); // Thêm wrapper panel
        panel.add(control, BorderLayout.SOUTH);
        return panel;
    }

    private void loadPendingRequests(){
        SwingUtilities.invokeLater(()->{
            pendingModel.setRowCount(0);
            Path p=Paths.get(PENDING_FILE);
            if(!Files.exists(p)) return;
            try{
                for(String l:Files.readAllLines(p,StandardCharsets.UTF_8)){
                    if(l.trim().isEmpty()) continue;
                    String[] parts = l.split("\\|", -1);
                    if (parts.length >= 5) {
                        String user = parts[0].trim();
                        String title = parts[1].trim();
                        String requestDate = parts[2].trim();
                        String dueDate = parts[3].trim();
                        String status = parts[4].trim();
                        pendingModel.addRow(new Object[]{user, title, requestDate, dueDate, status});
                    }
                }
            }catch(IOException ex){ appendLog("❌ Lỗi đọc pending: "+ex.getMessage()); }
        });
    }

 // -------------------- Borrow History --------------------
    private JPanel buildHistoryPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(BACKGROUND_LIGHT);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // === BẢNG TRÊN: Danh sách Users đang mượn sách ===
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(BACKGROUND_LIGHT);
        
        JLabel topLabel = new JLabel("👥 Users đang mượn sách");
        topLabel.setFont(HEADER_FONT.deriveFont(16f));
        topLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        topPanel.add(topLabel, BorderLayout.NORTH);
        
        borrowingUsersModel = new DefaultTableModel(new String[]{"Username", "Số sách đang mượn"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        borrowingUsersTable = new JTable(borrowingUsersModel);
        customizeTable(borrowingUsersTable);
        borrowingUsersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane usersScroll = new JScrollPane(borrowingUsersTable);
        usersScroll.getViewport().setBackground(BACKGROUND_LIGHT);
        usersScroll.setPreferredSize(new Dimension(0, 200));
        topPanel.add(usersScroll, BorderLayout.CENTER);

        // === BẢNG DƯỚI: Chi tiết sách của user được chọn ===
        JPanel bottomPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        bottomPanel.setBackground(BACKGROUND_LIGHT);

        // Bảng 1: Sách đang mượn
        JPanel borrowingPanel = new JPanel(new BorderLayout());
        borrowingPanel.setBackground(BACKGROUND_LIGHT);
        
        JLabel borrowingLabel = new JLabel("📚 Sách đang mượn");
        borrowingLabel.setFont(HEADER_FONT.deriveFont(15f));
        borrowingLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
        borrowingPanel.add(borrowingLabel, BorderLayout.NORTH);
        
        currentBorrowsModel = new DefaultTableModel(new String[]{"Tên sách", "Ngày mượn", "Ngày phải trả"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        currentBorrowsTable = new JTable(currentBorrowsModel);
        customizeTable(currentBorrowsTable);
        
        JScrollPane borrowingScroll = new JScrollPane(currentBorrowsTable);
        borrowingScroll.getViewport().setBackground(BACKGROUND_LIGHT);
        borrowingPanel.add(borrowingScroll, BorderLayout.CENTER);

        // Bảng 2: Sách đã trả
        JPanel returnedPanel = new JPanel(new BorderLayout());
        returnedPanel.setBackground(BACKGROUND_LIGHT);
        
        JLabel returnedLabel = new JLabel("✅ Sách đã trả");
        returnedLabel.setFont(HEADER_FONT.deriveFont(15f));
        returnedLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
        returnedPanel.add(returnedLabel, BorderLayout.NORTH);
        
        returnedBooksModel = new DefaultTableModel(new String[]{"Tên sách", "Ngày mượn", "Ngày phải trả", "Ngày trả"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        returnedBooksTable = new JTable(returnedBooksModel);
        customizeTable(returnedBooksTable);
        
        JScrollPane returnedScroll = new JScrollPane(returnedBooksTable);
        returnedScroll.getViewport().setBackground(BACKGROUND_LIGHT);
        returnedPanel.add(returnedScroll, BorderLayout.CENTER);

        bottomPanel.add(borrowingPanel);
        bottomPanel.add(returnedPanel);

        // === LISTENER: Khi click vào user, load chi tiết sách ===
        borrowingUsersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = borrowingUsersTable.getSelectedRow();
                if (row >= 0) {
                    String username = (String) borrowingUsersModel.getValueAt(row, 0);
                    loadUserBookDetails(username);
                }
            }
        });

        // === LAYOUT CHÍNH ===
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, bottomPanel);
        splitPane.setResizeWeight(0.3);
        splitPane.setDividerLocation(220);
        splitPane.setBackground(BACKGROUND_LIGHT);
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        return mainPanel;
    }

    private void loadBorrowHistory(){
        SwingUtilities.invokeLater(() -> {
            borrowingUsersModel.setRowCount(0);
            currentBorrowsModel.setRowCount(0);
            returnedBooksModel.setRowCount(0);
            
            Path p = Paths.get(BORROW_HISTORY_FILE);
            if (!Files.exists(p)) return;
            
            try {
                // Đọc tất cả records
                List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                Map<String, Integer> borrowingUsers = new HashMap<>();
                
                for (String l : lines) {
                    if (l.trim().isEmpty()) continue;
                    String[] parts = l.split("\\|", -1);
                    if (parts.length >= 6) {
                        String user = parts[0].trim();
                        String status = parts[5].trim();
                        
                        // Chỉ đếm những user có sách BORROWED
                        if ("BORROWED".equalsIgnoreCase(status)) {
                            borrowingUsers.put(user, borrowingUsers.getOrDefault(user, 0) + 1);
                        }
                    }
                }
                
                // Hiển thị danh sách users đang mượn sách
                borrowingUsers.forEach((user, count) -> {
                    borrowingUsersModel.addRow(new Object[]{user, count});
                });
                
                // Auto-select user đầu tiên nếu có
                if (borrowingUsersModel.getRowCount() > 0) {
                    borrowingUsersTable.setRowSelectionInterval(0, 0);
                    String firstUser = (String) borrowingUsersModel.getValueAt(0, 0);
                    loadUserBookDetails(firstUser);
                }
                
            } catch (IOException ex) {
                appendLog("❌ Lỗi đọc borrow history: " + ex.getMessage());
            }
        });
    }

    private void loadUserBookDetails(String username) {
        SwingUtilities.invokeLater(() -> {
            currentBorrowsModel.setRowCount(0);
            returnedBooksModel.setRowCount(0);
            
            Path p = Paths.get(BORROW_HISTORY_FILE);
            if (!Files.exists(p)) return;
            
            try {
                List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                
                for (String l : lines) {
                    if (l.trim().isEmpty()) continue;
                    String[] parts = l.split("\\|", -1);
                    if (parts.length >= 6) {
                        String user = parts[0].trim();
                        if (!user.equalsIgnoreCase(username)) continue;
                        
                        String title = parts[1].trim();
                        String borrowDate = parts[2].trim();
                        String dueDate = parts[3].trim();
                        String returnDate = parts[4].trim();
                        String status = parts[5].trim();
                        
                        // Định dạng ngày cho đẹp hơn
                        String borrowDateFormatted = formatDate(borrowDate);
                        String dueDateFormatted = formatDate(dueDate);
                        String returnDateFormatted = formatDate(returnDate);
                        
                        if ("BORROWED".equalsIgnoreCase(status)) {
                            // Sách đang mượn
                            currentBorrowsModel.addRow(new Object[]{
                                title, 
                                borrowDateFormatted, 
                                dueDateFormatted
                            });
                        } else if ("RETURNED".equalsIgnoreCase(status)) {
                            // Sách đã trả
                            returnedBooksModel.addRow(new Object[]{
                                title, 
                                borrowDateFormatted, 
                                dueDateFormatted, 
                                returnDateFormatted
                            });
                        }
                    }
                }
            } catch (IOException ex) {
                appendLog("❌ Lỗi đọc chi tiết user: " + ex.getMessage());
            }
        });
    }

    private String formatDate(String date) {
        if (date == null || date.isEmpty()) return "";
        // Nếu có timestamp ISO (yyyy-MM-ddTHH:mm:ss), chỉ lấy phần ngày
        if (date.contains("T")) {
            return date.split("T")[0];
        }
        return date;
    }


    // -------------------- Server --------------------
    private JScrollPane buildLogPanel() {
        logArea = new JTextArea();
        logArea.setEditable(false);
        // Thiết lập giao diện tối cho Log để dễ đọc hơn
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        logArea.setBackground(new Color(34, 49, 63)); 
        logArea.setForeground(new Color(236, 240, 241)); 
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(52, 73, 94)), 
            "  SERVER ACTIVITY LOG  ", 
            javax.swing.border.TitledBorder.LEFT, 
            javax.swing.border.TitledBorder.TOP, 
            HEADER_FONT.deriveFont(Font.BOLD, 14f), 
            new Color(189, 195, 199) 
        ));
        return scrollPane;
    }

    public static void appendLog(String s) {
        SwingUtilities.invokeLater(()->{
            logArea.append(s+"\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    private JPanel buildServerControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        panel.setBackground(BACKGROUND_CONTROL);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JButton btnStart = new JButton("▶️ Start Server");
        JButton btnStop = new JButton("⏹️ Stop Server");
        
        // Nút Start/Stop
        customizeButton(btnStart, new Color(52, 152, 219), Color.WHITE); 
        customizeButton(btnStop, BUTTON_DANGER, Color.WHITE);
        
        btnStart.addActionListener(e->startServer());
        btnStop.addActionListener(e->stopServer());
        panel.add(btnStart); panel.add(btnStop);
        return panel;
    }

    private void startServer(){
        if(serverThread!=null && serverThread.isAlive()){ appendLog("⚠️ Server đang chạy"); return; }
        serverThread = new Thread(()->{
            try{
                serverSocket=new ServerSocket(util.Config.SERVER_PORT);
                appendLog("✅ Server started at port "+ util.Config.SERVER_PORT);
                while(!serverSocket.isClosed()){
                    Socket client=serverSocket.accept();
                    appendLog("🔗 Client connected: "+client.getInetAddress().getHostAddress());
                    new Thread(()->handleClient(client)).start();
                }
            }catch(IOException e){ 
                if(serverSocket!=null && serverSocket.isClosed()) return; 
                appendLog("❌ Lỗi server: "+e.getMessage());
            }
        }, "Admin-EmbeddedServer");
        serverThread.start();
    }

    private void stopServer(){
        try{ 
            if(serverSocket!=null) serverSocket.close(); 
            appendLog("🛑 Server stopped"); 
        }
        catch(IOException e){ appendLog("❌ Lỗi stop server: "+e.getMessage()); }
    }

    private void handleClient(Socket client){
        try(BufferedReader in=new BufferedReader(new InputStreamReader(client.getInputStream(),StandardCharsets.UTF_8));
            PrintWriter out=new PrintWriter(new OutputStreamWriter(client.getOutputStream(),StandardCharsets.UTF_8),true)){
            String line;
            while((line=in.readLine())!=null){
                appendLog("📩 Received from "+client.getInetAddress().getHostAddress()+": "+line);
                String resp = processor.process(line, null);
                out.println(resp);
                appendLog("📤 Sent to "+client.getInetAddress().getHostAddress()+": "+resp);
            }
        }catch(IOException e){ 
            appendLog("⚠️ Client disconnected ("+client.getInetAddress().getHostAddress()+"): "+e.getMessage()); 
        }
    }

    // -------------------- Auto refresh without broadcast --------------------
    private javax.swing.Timer autoTimer;
    private volatile boolean refreshing = false;
    private void startAutoRefresh(){
        autoTimer = new javax.swing.Timer(2000, e -> {
            if (refreshing) return;
            refreshing = true;
            new Thread(() -> {
                try {
                    loadUsers();
                    loadBooks();
                    loadPendingRequests();
                    loadBorrowHistory();
                } finally {
                    refreshing = false;
                }
            }, "Admin-AutoRefresh").start();
        });
        autoTimer.setRepeats(true);
        autoTimer.start();
    }

    // (Đã loại bỏ phần Start/Stop server nội bộ để tránh chạy 2 server khác nhau)

    // -------------------- Main --------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminUI().setVisible(true));
    }

    // (Không dùng broadcast nữa, bỏ push listener và gửi lệnh qua socket)
}