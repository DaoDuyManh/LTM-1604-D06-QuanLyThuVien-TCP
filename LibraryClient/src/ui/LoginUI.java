package ui;

import client.ClientConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;

public class LoginUI extends JFrame {
    private final ClientConnection connection;

    // --- COLOR PALETTE ---
    private static final Color PRIMARY_BLUE = new Color(0, 123, 255); // Màu chủ đạo (Nút Đăng nhập)
    private static final Color BACKGROUND_LIGHT = new Color(245, 245, 245); // Nền sáng nhẹ
    private static final Color TEXT_DARK = new Color(33, 37, 41); // Chữ
    private static final Color BUTTON_SECONDARY = new Color(108, 117, 125); // Màu xám cho Đăng ký
    
    // --- FONT STYLES ---
    private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font INPUT_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 14);


    public LoginUI(ClientConnection connection) {
        this.connection = connection;

        setTitle("👤 Đăng nhập Hệ thống Thư viện");
        setSize(400, 250); // Tăng kích thước để đẹp hơn
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND_LIGHT); // Đặt màu nền cho Frame

        // Panel chính: Vẫn dùng GridLayout 3x2, nhưng thêm Padding
        JPanel panel = new JPanel(new GridLayout(3, 2, 15, 15)); // Tăng khoảng cách giữa các thành phần
        panel.setBorder(new EmptyBorder(25, 30, 25, 30)); // Thêm Padding
        panel.setBackground(BACKGROUND_LIGHT);

        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();

        JButton loginBtn = new JButton("Đăng nhập");
        JButton registerBtn = new JButton("Đăng ký");
        
        // --- Tùy chỉnh Component ---

        // Tùy chỉnh Label
        JLabel userLabel = new JLabel("Tài khoản:");
        JLabel passLabel = new JLabel("Mật khẩu:");
        userLabel.setFont(LABEL_FONT);
        passLabel.setFont(LABEL_FONT);
        userLabel.setForeground(TEXT_DARK);
        passLabel.setForeground(TEXT_DARK);
        
        // Tùy chỉnh Input Field
        userField.setFont(INPUT_FONT);
        passField.setFont(INPUT_FONT);
        userField.setPreferredSize(new Dimension(150, 30));
        passField.setPreferredSize(new Dimension(150, 30));
        
        // Tùy chỉnh nút Đăng nhập (Primary)
        loginBtn.setFont(BUTTON_FONT);
        loginBtn.setBackground(PRIMARY_BLUE);
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        loginBtn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15)); 

        // Tùy chỉnh nút Đăng ký (Secondary)
        registerBtn.setFont(BUTTON_FONT);
        registerBtn.setBackground(BUTTON_SECONDARY);
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);
        registerBtn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15)); 

        // --- Thêm vào Panel ---
        panel.add(userLabel);
        panel.add(userField);
        panel.add(passLabel);
        panel.add(passField);
        panel.add(loginBtn);
        panel.add(registerBtn);

        add(panel);

        // --- Sự kiện (GIỮ NGUYÊN LOGIC GỐC CỦA BẠN) ---
        
        // Sự kiện đăng nhập
        loginBtn.addActionListener(e -> {
            try {
                String user = userField.getText().trim();
                String pass = new String(passField.getPassword()).trim();

                if (user.isEmpty() || pass.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "⚠️ Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
                    return;
                }

                connection.sendMessage("LOGIN:" + user + ":" + pass);
                String resp = connection.readResponse();
                if (resp != null) resp = resp.trim();

                if ("SUCCESS".equalsIgnoreCase(resp) || resp.startsWith("SUCCESS")) {
                    JOptionPane.showMessageDialog(this, "✅ Đăng nhập thành công!");
                    dispose();
                    new MainUI(connection, user);
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Sai tài khoản hoặc mật khẩu!");
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "❌ Lỗi kết nối server!");
            }
        });

        // Sự kiện mở cửa sổ đăng ký
        registerBtn.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                // Giả định RegisterUI và MainUI có trong project của bạn
                // Nếu RegisterUI không có, dòng này sẽ gây lỗi
                // Tuy nhiên, tôi giữ nguyên logic theo yêu cầu
                RegisterUI regUI = new RegisterUI(connection);
                regUI.setVisible(true);
            });
        });

        setVisible(true);
    }
}