package client;

import ui.LoginUI;

import javax.swing.*;

public class ClientApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                ClientConnection connection = new ClientConnection();
                new LoginUI(connection);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Không thể kết nối tới server!");
                e.printStackTrace();
            }
        });
    }
}
