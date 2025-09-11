package ui;

import client.ClientConnection;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class RegisterUI extends JFrame {
    private final ClientConnection connection;

    public RegisterUI(ClientConnection connection) {
        this.connection = connection;

        setTitle("Đăng ký tài khoản");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));

        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        JTextField phoneField = new JTextField();
        JTextField addressField = new JTextField();
        JTextField emailField = new JTextField();

        JButton registerBtn = new JButton("Đăng ký");
        JButton cancelBtn = new JButton("Hủy");

        panel.add(new JLabel("Tài khoản:"));
        panel.add(userField);
        panel.add(new JLabel("Mật khẩu:"));
        panel.add(passField);
        panel.add(new JLabel("Số điện thoại:"));
        panel.add(phoneField);
        panel.add(new JLabel("Địa chỉ:"));
        panel.add(addressField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(registerBtn);
        panel.add(cancelBtn);

        add(panel);

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
    }
}
