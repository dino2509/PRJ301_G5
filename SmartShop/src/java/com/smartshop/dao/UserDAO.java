package com.smartshop.dao;

import com.smartshop.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;

public class UserDAO {

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(rs.getString("role"));
        u.setActive(rs.getBoolean("active"));
        return u;
    }

    // --- Finders ---
    public User findById(int id) {
        String sql = "SELECT * FROM Users WHERE id=?";
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM Users WHERE username=?";
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM Users WHERE email=?";
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM Users WHERE username=?";
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // --- Create ---
    public boolean create(User u, String rawPassword) { return createWithError(u, rawPassword) == null; }

    public String createWithError(User u, String rawPassword) {
        String sql = "INSERT INTO Users(username,password_hash,email,phone,full_name,role,active) VALUES(?,?,?,?,?,?,1)";
        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, hash);
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getPhone());
            ps.setString(5, u.getFullName());
            ps.setString(6, u.getRole() == null ? "CUSTOMER" : u.getRole());
            ps.executeUpdate();
            return null;
        } catch (Exception e) { e.printStackTrace(); return e.getMessage(); }
    }

    // --- Auth with compatibility (plain -> bcrypt upgrade) ---
    public User authenticate(String username, String rawPassword) {
        String sql = "SELECT * FROM Users WHERE username=? AND active=1";
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                User u = map(rs);
                String stored = u.getPasswordHash();
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

    // --- Profile / Password ---
    public boolean updateProfile(User u) {
        String sql = "UPDATE Users SET email=?, phone=?, full_name=? WHERE id=?";
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, u.getEmail());
            ps.setString(2, u.getPhone());
            ps.setString(3, u.getFullName());
            ps.setInt(4, u.getId());
            return ps.executeUpdate() == 1;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public boolean changePassword(int userId, String newRawPassword) {
        String sql = "UPDATE Users SET password_hash=?, reset_token=NULL, reset_code=NULL, reset_expires=NULL WHERE id=?";
        String hash = BCrypt.hashpw(newRawPassword, BCrypt.gensalt());
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // --- Reset challenge (token + 6-digit code) ---
    public boolean saveResetChallenge(int userId, String token, String code, LocalDateTime expires) {
        String sql = "UPDATE Users SET reset_token=?, reset_code=?, reset_expires=? WHERE id=?";
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setString(2, code);
            ps.setTimestamp(3, Timestamp.valueOf(expires));
            ps.setInt(4, userId);
            return ps.executeUpdate() == 1;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public User findByResetToken(String token) {
        String sql = "SELECT * FROM Users WHERE reset_token=? AND reset_expires>SYSDATETIME()";
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public User findByResetCode(String code) {
        String sql = "SELECT * FROM Users WHERE reset_code=? AND reset_expires>SYSDATETIME()";
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public void clearResetToken(int userId) {
        String sql = "UPDATE Users SET reset_token=NULL, reset_code=NULL, reset_expires=NULL WHERE id=?";
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // tuỳ chọn: dùng để ép đặt mật khẩu khi cần
    public boolean forceSetPassword(int userId, String rawPassword) { return changePassword(userId, rawPassword); }
}
