package dao;

import java.util.*;
import model.User;
import util.FileHelper;
import util.Config;

public class UserDAO {

    public static List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        for (String line : FileHelper.readLines(Config.USERS_FILE)) {
            User u = User.fromString(line);
            if (u != null) list.add(u);
        }
        return list;
    }

    public static User findByUsername(String username) {
        for (User u : getAllUsers()) {
            if (u.getUsername().equalsIgnoreCase(username)) {
                return u;
            }
        }
        return null;
    }

    public static boolean register(String username, String password, String phone, String address, String email) {
        if (findByUsername(username) != null) {
            System.out.println("⚠️ User đã tồn tại: " + username);
            return false;
        }
        User newUser = new User(username, password, phone, address, email);
        FileHelper.appendLine(Config.USERS_FILE, newUser.toString());
        System.out.println("✅ Đã đăng ký user mới: " + username);
        return true;
    }

    public static boolean validateLogin(String username, String password) {
        User u = findByUsername(username);
        if (u == null) {
            System.out.println("❌ Không tìm thấy user: " + username);
            return false;
        }
        if (!u.getPassword().equals(password)) {
            System.out.println("❌ Sai mật khẩu cho user: " + username);
            return false;
        }
        System.out.println("✅ Đăng nhập thành công: " + username);
        return true;
    }
}
