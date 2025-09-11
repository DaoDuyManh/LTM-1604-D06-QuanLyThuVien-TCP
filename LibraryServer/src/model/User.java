package model;

public class User {
    private String username;
    private String password;
    private String phone;
    private String address;
    private String email;

    public User(String username, String password, String phone, String address, String email) {
        this.username = username.trim();
        this.password = password.trim();
        this.phone = phone.trim();
        this.address = address.trim();
        this.email = email.trim();
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getEmail() { return email; }

    @Override
    public String toString() {
        // Lưu dạng CSV
        return username + "," + password + "," + phone + "," + address + "," + email;
    }

    public static User fromString(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        String[] parts = line.trim().split(",", -1); // giữ cả chuỗi rỗng
        if (parts.length == 5) {
            return new User(parts[0], parts[1], parts[2], parts[3], parts[4]);
        }
        return null;
    }
}
