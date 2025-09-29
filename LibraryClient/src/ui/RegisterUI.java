package ui;

import client.ClientConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;

public class RegisterUI extends JFrame {
    private final ClientConnection connection;

    // --- COLOR PALETTE (Đồng bộ với LoginUI) ---
    private static final Color PRIMARY_BLUE = new Color(0, 123, 255); // Màu chủ đạo (Nút Đăng ký)
    private static final Color BACKGROUND_LIGHT = new Color(245, 245, 245); // Nền sáng nhẹ
    private static final Color TEXT_DARK = new Color(33, 37, 41); // Chữ
    private static final Color BUTTON_SECONDARY = new Color(108, 117, 125); // Màu xám cho Hủy
    
    // --- FONT STYLES ---
    private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font INPUT_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 14);

    public RegisterUI(ClientConnection connection) {
        this.connection = connection;

        setTitle("✨ Đăng ký tài khoản mới");
        setSize(500, 450); // Tăng kích thước để chứa 6 dòng nội dung thoáng hơn
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BACKGROUND_LIGHT);

        // Panel chính: Vẫn dùng GridLayout 6x2, nhưng tăng Padding và khoảng cách
        JPanel panel = new JPanel(new GridLayout(6, 2, 20, 15)); // Tăng khoảng cách giữa các thành phần
        panel.setBorder(new EmptyBorder(30, 40, 30, 40)); // Thêm Padding lớn hơn
        panel.setBackground(BACKGROUND_LIGHT);

        // --- Khai báo Components (Giữ nguyên tên biến gốc) ---
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JTextField phoneField = new JTextField();
        JTextField addressField = new JTextField();
        JTextField emailField = new JTextField();

        JButton registerBtn = new JButton("Đăng ký");
        JButton cancelBtn = new JButton("Hủy");
        
        // --- Tùy chỉnh Components ---

        // 1. Labels
        JLabel userLabel = new JLabel("Tài khoản:");
        JLabel passLabel = new JLabel("Mật khẩu:");
        JLabel phoneLabel = new JLabel("Số điện thoại:");
        JLabel addressLabel = new JLabel("Địa chỉ:");
        JLabel emailLabel = new JLabel("Email:");
        
        userLabel.setFont(LABEL_FONT);
        passLabel.setFont(LABEL_FONT);
        phoneLabel.setFont(LABEL_FONT);
        addressLabel.setFont(LABEL_FONT);
        emailLabel.setFont(LABEL_FONT);

        userLabel.setForeground(TEXT_DARK);
        passLabel.setForeground(TEXT_DARK);
        phoneLabel.setForeground(TEXT_DARK);
        addressLabel.setForeground(TEXT_DARK);
        emailLabel.setForeground(TEXT_DARK);

        // 2. Input Fields
        JTextField[] fields = {userField, passField, phoneField, addressField, emailField};
        for(JTextField field : fields) {
            field.setFont(INPUT_FONT);
            field.setPreferredSize(new Dimension(200, 35));
            field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(5, 10, 5, 10) // Padding nội bộ
            ));
        }
        
        // 3. Buttons

        // Nút Đăng ký (Primary Blue)
        registerBtn.setFont(BUTTON_FONT);
        registerBtn.setBackground(PRIMARY_BLUE);
        registerBtn.setForeground(Color.WHITE); // Màu chữ trắng để tương phản
        registerBtn.setFocusPainted(false);
        registerBtn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15)); 
        
        // Nút Hủy (Secondary Gray)
        cancelBtn.setFont(BUTTON_FONT);
        cancelBtn.setBackground(BUTTON_SECONDARY);
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15)); 

        // --- Thêm vào Panel (GIỮ NGUYÊN THỨ TỰ GỐC) ---
        panel.add(userLabel);
        panel.add(userField);
        panel.add(passLabel);
        panel.add(passField);
        panel.add(phoneLabel);
        panel.add(phoneField);
        panel.add(addressLabel);
        panel.add(addressField);
        panel.add(emailLabel);
        panel.add(emailField);
        panel.add(registerBtn);
        panel.add(cancelBtn);

        add(panel);

        // --- Xử lý sự kiện (GIỮ NGUYÊN LOGIC GỐC CỦA BẠN) ---
        
        // Xử lý nút đăng ký
        registerBtn.addActionListener(e -> {
            try {
                String user = userField.getText().trim();
                String pass = new String(passField.getPassword()).trim();
                String phone = phoneField.getText().trim();
                String address = addressField.getText().trim();
                String email = emailField.getText().trim();

                if (user.isEmpty() || pass.isEmpty() || phone.isEmpty() || address.isEmpty() || email.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "⚠️ Vui lòng nhập đầy đủ thông tin!");
                    return;
                }

                connection.sendMessage("REGISTER:" + user + ":" + pass + ":" + phone + ":" + address + ":" + email);
                String resp = connection.readResponse();
                if (resp != null) resp = resp.trim();

                if ("SUCCESS".equalsIgnoreCase(resp) || resp.startsWith("SUCCESS")) {
                    JOptionPane.showMessageDialog(this, "✅ Đăng ký thành công! Hãy đăng nhập lại.");
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Đăng ký thất bại: " + resp);
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "❌ Lỗi kết nối server!");
            }
        });

        // Nút hủy
        cancelBtn.addActionListener(e -> dispose());
        
        // Hiển thị cửa sổ
        setVisible(true);
    }
}