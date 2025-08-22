package com.smartshop.dao;

import com.smartshop.model.User;
import com.smartshop.util.DB;
import com.smartshop.util.PasswordUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    
    public static class Profile {
        public int id;
        public String username;
        public String fullName;
        public String phone;
        public String email;
        public String address;
        public double walletBalance;

        public Profile() {}
    }   
    
        public Profile getProfile(int userId) {
        String sql = "SELECT id, username, full_name, phone, email, address, wallet_balance FROM dbo.Users WHERE id=?";
        try (Connection cn = DB.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Profile p = new Profile();
                p.id = rs.getInt("id");
                p.username = rs.getString("username");
                p.fullName = rs.getString("full_name");
                p.phone = rs.getString("phone");
                p.email = rs.getString("email");
                p.address = rs.getString("address");
                p.walletBalance = rs.getDouble("wallet_balance");
                return p;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateAddress(int userId, String address) {
        String sql = "UPDATE dbo.Users SET address=? WHERE id=?";
        try (Connection cn = DB.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, address);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHashStr(rs.getString("password_hash"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setAddress(rs.getString("address"));
        u.setFullName(rs.getString("full_name"));
        try { u.setStatus(rs.getString("status")); } catch (SQLException ignore) {}
        try { u.setActive(rs.getBoolean("active")); } catch (SQLException ignore) {}
        return u;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM Users WHERE id=?";
        try (Connection cn = DB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM Users WHERE username=?";
        try (Connection cn = DB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM Users WHERE email=?";
        try (Connection cn = DB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM Users WHERE username=?";
        try (Connection cn = DB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    /** Kiểm tra mật khẩu. Hỗ trợ SHA-256+salt (bytes) và fallback BCrypt/chuỗi thô nếu DB cũ. */
    public boolean verifyPassword(User u, String raw) {
        if (u == null || raw == null) return false;
        String sql = "SELECT password_salt, password_hash FROM Users WHERE id=?";
        try (Connection cn = DB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, u.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                byte[] salt = null, hashBytes = null;
                try { salt = rs.getBytes(1); } catch (SQLException ignore) {}
                try { hashBytes = rs.getBytes(2); } catch (SQLException ignore) {}

                if (salt != null && hashBytes != null)
                    return PasswordUtil.matches(salt, hashBytes, raw);

                // fallback nếu cột password_hash lưu String (BCrypt)
                String hashStr = null;
                try { hashStr = rs.getString(2); } catch (SQLException ignore) {}
                if (hashStr == null) return false;
                if (hashStr.startsWith("$2")) return BCrypt.checkpw(raw, hashStr);
                return raw.equals(hashStr);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean changePassword(int userId, String newRaw) {
        byte[] salt = PasswordUtil.newSalt();
        byte[] hash = PasswordUtil.sha256(salt, newRaw);
        String sql = "UPDATE Users SET password_salt=?, password_hash=?, reset_token=NULL, reset_code=NULL, reset_expires=NULL WHERE id=?";
        try (Connection cn = DB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setBytes(1, salt); ps.setBytes(2, hash); ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean updateProfile(User u) {
        String sql = "UPDATE Users SET email=?, full_name=?, phone=?, address=? WHERE id=?";
        try (Connection cn = DB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, u.getEmail());
            ps.setString(2, u.getFullName());
            ps.setString(3, u.getPhone());
            ps.setString(4, u.getAddress());
            ps.setInt(5, u.getId());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    /** Trả null nếu OK, ngược lại trả message lỗi. */
    public String createWithError(User u, String rawPassword) {
        if (u.getUsername() == null || u.getUsername().isBlank()) return "Username rỗng";
        if (usernameExists(u.getUsername())) return "Username đã tồn tại";

        byte[] salt = PasswordUtil.newSalt();
        byte[] hash = PasswordUtil.sha256(salt, rawPassword);

        try (Connection cn = DB.getConnection()) {
            cn.setAutoCommit(false);
            int userId;
            try (PreparedStatement ps = cn.prepareStatement(
                    "INSERT INTO Users(username,email,full_name,phone,address,password_salt,password_hash,status) VALUES(?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, u.getUsername());
                ps.setString(2, u.getEmail());
                ps.setString(3, u.getFullName());
                ps.setString(4, u.getPhone());
                ps.setString(5, u.getAddress());
                ps.setBytes(6, salt);
                ps.setBytes(7, hash);
                ps.setString(8, "ACTIVE");
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); userId = keys.getInt(1); }
            }
            try (PreparedStatement ps = cn.prepareStatement(
                    "INSERT INTO UserRoles(user_id, role_id) SELECT ?, id FROM Roles WHERE name=?")) {
                ps.setInt(1, userId); ps.setString(2, "USER"); ps.executeUpdate();
            }
            cn.commit();
            return null;
        } catch (SQLException e) { e.printStackTrace(); return e.getMessage(); }
    }

    public void saveResetChallenge(int userId, String token, String code, LocalDateTime exp) {
        String sql = "UPDATE Users SET reset_token=?, reset_code=?, reset_expires=? WHERE id=?";
        try (Connection cn = DB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setString(2, code);
            ps.setObject(3, Timestamp.valueOf(exp));
            ps.setInt(4, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean setResetCodeAndToken(int userId, String code, int expireMinutes) {
        String sql = "UPDATE Users SET reset_code=?, reset_token=?, reset_expires=DATEADD(MINUTE, ?, SYSUTCDATETIME()) WHERE id=?";
        try (Connection cn = DB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, code);
            ps.setInt(3, expireMinutes);
            ps.setInt(4, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public User findByResetTokenAndEmail(String token, String email) {
        String sql = "SELECT * FROM Users WHERE reset_token=? AND email=? AND reset_expires>SYSUTCDATETIME()";
        try (Connection cn = DB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setString(2, email);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }
    
    public User findByResetToken(String token) {
        String sql = "SELECT * FROM Users WHERE reset_token=? AND reset_expires>SYSUTCDATETIME()";
        try (Connection cn = DB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    public User findByResetCode(String code) {
        String sql = "SELECT * FROM Users WHERE reset_code=? AND reset_expires>SYSUTCDATETIME()";
        try (Connection cn = DB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    public void clearResetToken(int userId) {
        String sql = "UPDATE Users SET reset_token=NULL, reset_code=NULL, reset_expires=NULL WHERE id=?";
        try (Connection cn = DB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean forceSetPassword(int userId, String rawPassword) {
        return changePassword(userId, rawPassword);
    }

    public List<String> rolesOf(int userId) {
        List<String> roles = new ArrayList<>();
        String sql = "SELECT r.name FROM UserRoles ur JOIN Roles r ON ur.role_id=r.id WHERE ur.user_id=?";
        try (Connection cn = DB.getConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) roles.add(rs.getString(1)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return roles;
    }
    
        public void create(String username, String email, String fullName, String phone, String address, String password, String role) {
        String sql = "INSERT INTO Users(username,email,full_name,phone,address,password_salt,password_hash) VALUES(?,?,?,?,?,?,?)";
        byte[] salt = PasswordUtil.newSalt();
        byte[] hash = PasswordUtil.sha256(salt, password);
        try (Connection c = DB.getConnection()) {
            c.setAutoCommit(false);
            int userId;
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, email);
                ps.setString(3, fullName);
                ps.setString(4, phone);
                ps.setString(5, address);
                ps.setBytes(6, salt);
                ps.setBytes(7, hash);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); userId = keys.getInt(1); }
            }
            if (role != null) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO UserRoles(user_id, role_id) SELECT ?, id FROM Roles WHERE name=?")) {
                    ps.setInt(1, userId);
                    ps.setString(2, role);
                    ps.executeUpdate();
                }
            }
            c.commit();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void update(User u) {
        String sql = "UPDATE Users SET email=?, full_name=?, phone=?, address=? status=? WHERE id=?";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getEmail());
            ps.setString(2, u.getFullName());
            ps.setString(3, u.getPhone());
            ps.setString(4, u.getAddress());
            ps.setString(5, u.getStatus());
            ps.setInt(6, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void delete(int id) {
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM Users WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
    
    
    public List<User> list(int offset, int limit, String q) {
        String sql = "SELECT * FROM Users WHERE (? IS NULL OR username LIKE ? OR email LIKE ? OR full_name LIKE ?) " +
                     "ORDER BY id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<User> list = new ArrayList<>();
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String like = (q==null || q.isBlank())? null : "%"+q+"%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setInt(5, offset);
            ps.setInt(6, limit);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }
    
    public User authenticate(String username, String rawPassword) {
        String sql = "SELECT * FROM Users WHERE username=? AND active=1";
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                User u = map(rs);
                String stored = u.getPasswordHashStr();
                boolean ok;
                if (stored != null && stored.startsWith("$2")) {
                    ok = BCrypt.checkpw(rawPassword, stored);
                } else {
                    ok = rawPassword != null && rawPassword.equals(stored);
                    if (ok) { // nâng cấp sang bcrypt
                        changePassword(u.getId(), rawPassword);
                        u = findById(u.getId());
                    }
                }
                return ok ? u : null;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

}
