package ui;

import client.ClientConnection;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class LoginUI extends JFrame {
    private final ClientConnection connection;

    public LoginUI(ClientConnection connection) {
        this.connection = connection;

        setTitle("Đăng nhập");
        setSize(350, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();

        JButton loginBtn = new JButton("Đăng nhập");
        JButton registerBtn = new JButton("Đăng ký");

        panel.add(new JLabel("Tài khoản:"));
        panel.add(userField);
        panel.add(new JLabel("Mật khẩu:"));
        panel.add(passField);
        panel.add(loginBtn);
        panel.add(registerBtn);

        add(panel);

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
                RegisterUI regUI = new RegisterUI(connection);
                regUI.setVisible(true);
            });
        });

        setVisible(true);
    }
}
