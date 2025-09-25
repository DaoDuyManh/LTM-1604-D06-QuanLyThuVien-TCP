package ui;

import model.Book;
import model.User;
import server.CommandProcessor;
import util.Config;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class AdminUI extends JFrame {
    private JTabbedPane tabbedPane;
    private JTable userTable, bookTable, pendingTable, historyTable;
    private DefaultTableModel userModel, bookModel, pendingModel, historyModel;
    private static JTextArea logArea;

    private static final String BOOK_FILE = "books.txt";
    private static final String ACCOUNT_FILE = "accounts.txt";
    private static final String PENDING_FILE = "pending.txt";
    private static final String BORROW_HISTORY_FILE = "borrow_history.txt";
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private ServerSocket serverSocket;
    private Thread serverThread;
    private CommandProcessor processor;

    public AdminUI() {
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
    }

    private void initUI() {
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Users", buildUserPanel());
        tabbedPane.addTab("Books", buildBookPanel());
        tabbedPane.addTab("Pending Requests", buildPendingPanel());
        tabbedPane.addTab("Borrow History", buildHistoryPanel());

        JPanel serverPanel = new JPanel(new BorderLayout());
        serverPanel.add(buildLogPanel(), BorderLayout.CENTER);
        serverPanel.add(buildServerControlPanel(), BorderLayout.SOUTH);
        tabbedPane.addTab("Server Log", serverPanel);

        add(tabbedPane);
    }

    // -------------------- Users --------------------
    private JPanel buildUserPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        userModel = new DefaultTableModel(new String[]{"Tài khoản", "Mật khẩu", "SĐT", "Địa chỉ", "Email"},0);
        userTable = new JTable(userModel);
        JScrollPane scroll = new JScrollPane(userTable);

        JPanel control = new JPanel();
        JButton add = new JButton("Thêm User");
        JButton del = new JButton("Xóa User");
        JButton reload = new JButton("⟳ Tải lại");
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
            } catch (IOException ex) { appendLog("❌ Lỗi đọc users: " + ex.getMessage()); }
        });
    }

    private void addUser(){
        JTextField userField = new JTextField();
        JTextField passField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField addressField = new JTextField();
        JTextField emailField = new JTextField();
        JPanel p = new JPanel(new GridLayout(5,2));
        p.add(new JLabel("Tài khoản:")); p.add(userField);
        p.add(new JLabel("Mật khẩu:")); p.add(passField);
        p.add(new JLabel("Số điện thoại:")); p.add(phoneField);
        p.add(new JLabel("Địa chỉ:")); p.add(addressField);
        p.add(new JLabel("Email:")); p.add(emailField);
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
        int c=JOptionPane.showConfirmDialog(this,"Xóa user: "+user+" ?", "Confirm", JOptionPane.YES_NO_OPTION);
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
        bookModel = new DefaultTableModel(new String[]{"Tên sách","Tác giả","Thể loại","Số lượng","Available","Borrowers"},0);
        bookTable = new JTable(bookModel);
        bookTable.setRowHeight(26);
        JScrollPane scroll = new JScrollPane(bookTable);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i=3;i<bookTable.getColumnCount();i++)
            bookTable.getColumnModel().getColumn(i).setCellRenderer(center);

        JPanel control = new JPanel();
        JButton btnAdd = new JButton("Thêm sách");
        JButton btnDelete = new JButton("Xóa sách");
        JButton btnEditQty = new JButton("Chỉnh số lượng");
        JButton btnForceReturn = new JButton("Force Return");
        JButton btnReload = new JButton("⟳ Tải lại");
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
        JPanel p = new JPanel(new GridLayout(4,2));
        p.add(new JLabel("Tên sách:")); p.add(t);
        p.add(new JLabel("Tác giả:")); p.add(a);
        p.add(new JLabel("Thể loại:")); p.add(c);
        p.add(new JLabel("Số lượng:")); p.add(q);
        int r = JOptionPane.showConfirmDialog(this,p,"Thêm sách",JOptionPane.OK_CANCEL_OPTION);
        if(r==JOptionPane.OK_OPTION){
            try{
                int qty=Integer.parseInt(q.getText().trim());
                Book b=new Book(t.getText().trim(),a.getText().trim(),c.getText().trim(),qty,new ArrayList<>());
                List<Book> all=readAllBooks();
                Optional<Book> ex=all.stream().filter(x->x.getTitle().equalsIgnoreCase(b.getTitle())).findFirst();
                if(ex.isPresent()){ JOptionPane.showMessageDialog(this,"Sách đã tồn tại!"); return; }
                all.add(b);
                writeAllBooks(all);
                loadBooks();
            }catch(NumberFormatException ex){ JOptionPane.showMessageDialog(this,"Số lượng không hợp lệ"); }
            catch(IOException ex){ appendLog("❌ Lỗi thao tác file: " + ex.getMessage()); }
        }
    }

    private void deleteBook(){
        int row=bookTable.getSelectedRow(); if(row<0) return;
        String t=(String)bookModel.getValueAt(row,0);
        int c=JOptionPane.showConfirmDialog(this,"Xóa sách: "+t+" ?","Confirm",JOptionPane.YES_NO_OPTION);
        if(c==JOptionPane.YES_OPTION){
            try{
                List<Book> all=readAllBooks();
                all=all.stream().filter(b->!b.getTitle().equalsIgnoreCase(t)).collect(Collectors.toList());
                writeAllBooks(all);
                loadBooks();
            }catch(IOException ex){ appendLog("❌ Lỗi thao tác file: "+ex.getMessage()); }
        }
    }

    private void editQuantity(){
        int row=bookTable.getSelectedRow(); if(row<0) return;
        String t=(String)bookModel.getValueAt(row,0);
        String s=JOptionPane.showInputDialog(this,"Nhập số lượng mới:");
        if(s==null) return;
        try{
            int qty=Integer.parseInt(s);
            List<Book> all=readAllBooks();
            all.forEach(b->{ if(b.getTitle().equalsIgnoreCase(t)) b.setQuantity(qty); });
            writeAllBooks(all);
            loadBooks();
        }catch(NumberFormatException ex){ JOptionPane.showMessageDialog(this,"Số lượng không hợp lệ"); }
        catch(IOException ex){ appendLog("❌ Lỗi thao tác file: " + ex.getMessage()); }
    }

    private void forceReturn(){
        int row=bookTable.getSelectedRow(); if(row<0) return;
        String t=(String)bookModel.getValueAt(row,0);
        try{
            List<Book> all=readAllBooks();
            all.forEach(b->{ if(b.getTitle().equalsIgnoreCase(t)) b.getBorrowers().clear(); });
            writeAllBooks(all);
            loadBooks();
        }catch(IOException ex){ appendLog("❌ Lỗi thao tác file: " + ex.getMessage()); }
    }

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
        pendingModel = new DefaultTableModel(new String[]{"User","Book","Request Date","Due Date","Status"},0);
        pendingTable = new JTable(pendingModel);
        panel.add(new JScrollPane(pendingTable), BorderLayout.CENTER);

        JPanel control = new JPanel();
        JButton btnAccept = new JButton("✔ Chấp nhận");
        JButton btnReject = new JButton("✖ Từ chối");
        JButton btnReload = new JButton("⟳ Tải lại");
        control.add(btnAccept); control.add(btnReject); control.add(btnReload);

        btnReload.addActionListener(e->loadPendingRequests());

        btnAccept.addActionListener(e->{
            int row = pendingTable.getSelectedRow();
            if(row < 0) return;
            String user = (String) pendingModel.getValueAt(row,0);
            String book = (String) pendingModel.getValueAt(row,1);
            String cmd = "ACCEPT_BORROW:" + user + ":" + book;
            String resp = processor.process(cmd, "admin");
            JOptionPane.showMessageDialog(this, resp);
            loadPendingRequests();
            loadBooks();
            loadBorrowHistory();
        });

        btnReject.addActionListener(e->{
            int row = pendingTable.getSelectedRow();
            if(row < 0) return;
            String user = (String) pendingModel.getValueAt(row,0);
            String book = (String) pendingModel.getValueAt(row,1);
            String cmd = "REJECT_BORROW:" + user + ":" + book;
            String resp = processor.process(cmd, "admin");
            JOptionPane.showMessageDialog(this, resp);
            loadPendingRequests();
            loadBorrowHistory();
        });

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
        JPanel panel = new JPanel(new BorderLayout());
        // 5 cột: User – Book – Borrow Date – Return Date – Status
        historyModel = new DefaultTableModel(new String[]{"User","Book","Borrow Date","Return Date","Status"},0);
        historyTable = new JTable(historyModel);
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        JButton reload = new JButton("⟳ Tải lại"); 
        reload.addActionListener(e->loadBorrowHistory());
        panel.add(reload, BorderLayout.SOUTH);
        return panel;
    }

    private void loadBorrowHistory(){
        SwingUtilities.invokeLater(()->{
            historyModel.setRowCount(0);
            Path p=Paths.get(BORROW_HISTORY_FILE);
            if(!Files.exists(p)) return;
            try{
                for(String l:Files.readAllLines(p,StandardCharsets.UTF_8)){
                    if(l.trim().isEmpty()) continue;
                    // format cũ: user|title|borrowDate|returnDate|status
                    String[] parts = l.split("\\|", -1);
                    if (parts.length >= 5) {
                        String user = parts[0].trim();
                        String title = parts[1].trim();
                        String borrowDate = parts[2].trim();
                        String returnDate = parts[3].trim(); // cột này chính là due date
                        String status = parts[5].trim();
                        historyModel.addRow(new Object[]{user, title, borrowDate, returnDate, status});
                    }
                }
            }catch(IOException ex){ appendLog("❌ Lỗi đọc borrow history: "+ex.getMessage()); }
        });
    }


    // -------------------- Server --------------------
    private JPanel buildServerControlPanel() {
        JPanel panel = new JPanel();
        JButton btnStart = new JButton("Start Server");
        JButton btnStop = new JButton("Stop Server");
        btnStart.addActionListener(e->startServer());
        btnStop.addActionListener(e->stopServer());
        panel.add(btnStart); panel.add(btnStop);
        return panel;
    }

    private JScrollPane buildLogPanel() {
        logArea = new JTextArea();
        logArea.setEditable(false);
        return new JScrollPane(logArea);
    }

    public static void appendLog(String s) {
        SwingUtilities.invokeLater(()->{
            logArea.append(s+"\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void startServer(){
        if(serverThread!=null && serverThread.isAlive()){ appendLog("⚠️ Server đang chạy"); return; }
        serverThread = new Thread(()->{
            try{
                serverSocket=new ServerSocket(Config.SERVER_PORT);
                appendLog("✅ Server started at port "+Config.SERVER_PORT);
                while(!serverSocket.isClosed()){
                    Socket client=serverSocket.accept();
                    appendLog("🔗 Client connected: "+client.getInetAddress());
                    new Thread(()->handleClient(client)).start();
                }
            }catch(IOException e){ appendLog("❌ Lỗi server: "+e.getMessage()); }
        });
        serverThread.start();
    }

    private void stopServer(){
        try{ if(serverSocket!=null) serverSocket.close(); appendLog("🛑 Server stopped"); }
        catch(IOException e){ appendLog("❌ Lỗi stop server: "+e.getMessage()); }
    }

    private void handleClient(Socket client){
        try(BufferedReader in=new BufferedReader(new InputStreamReader(client.getInputStream(),StandardCharsets.UTF_8));
            PrintWriter out=new PrintWriter(new OutputStreamWriter(client.getOutputStream(),StandardCharsets.UTF_8),true)){
            String line;
            while((line=in.readLine())!=null){
                appendLog("📩 Received: "+line);
                String resp = processor.process(line, null);
                out.println(resp);
                appendLog("📤 Sent: "+resp);
            }
        }catch(IOException e){ appendLog("⚠️ Client disconnected: "+e.getMessage()); }
    }

    // -------------------- Main --------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminUI().setVisible(true));
    }
}
