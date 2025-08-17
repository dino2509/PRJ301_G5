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

    public User findByUsername(String username) {
        String sql = "SELECT * FROM Users WHERE username=?";
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM Users WHERE email=?";
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
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

    public boolean create(User u, String rawPassword) {
        return createWithError(u, rawPassword) == null;
    }

    public String createWithError(User u, String rawPassword) {
        String sql = "INSERT INTO Users(username,password_hash,email,phone,full_name,role,active) VALUES(?,?,?,?,?,?,1)";
        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt()); // sinh hash tương thích thư viện hiện tại
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
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /** Login với chế độ tương thích:
     *  - Nếu password_hash là bcrypt ($2*): dùng BCrypt.checkpw
     *  - Nếu password_hash là plain: so chuỗi, rồi nâng cấp sang bcrypt ngay
     */
    public User authenticate(String username, String rawPassword) {
        String sql = "SELECT * FROM Users WHERE username=? AND active=1";
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql,
                     ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                String stored = rs.getString("password_hash");
                boolean ok;
                if (stored != null && stored.startsWith("$2")) {
                    ok = BCrypt.checkpw(rawPassword, stored);
                } else {
                    ok = rawPassword != null && rawPassword.equals(stored);
                    if (ok) { // nâng cấp về bcrypt
                        String newHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
                        try (PreparedStatement up = cn.prepareStatement(
                                "UPDATE Users SET password_hash=? WHERE id=?")) {
                            up.setString(1, newHash);
                            up.setInt(2, rs.getInt("id"));
                            up.executeUpdate();
                        }
                    }
                }
                return ok ? map(rs) : null;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

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
        String sql = "UPDATE Users SET password_hash=?, reset_token=NULL, reset_expires=NULL WHERE id=?";
        String hash = BCrypt.hashpw(newRawPassword, BCrypt.gensalt());
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public boolean saveResetToken(int userId, String token, LocalDateTime expires) {
        String sql = "UPDATE Users SET reset_token=?, reset_expires=? WHERE id=?";
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setTimestamp(2, Timestamp.valueOf(expires));
            ps.setInt(3, userId);
            return ps.executeUpdate() == 1;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }



    /** Dùng cho servlet reset admin */
    public boolean forceSetPassword(int userId, String rawPassword) {
        return changePassword(userId, rawPassword);
    }
    
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

// Tìm theo token còn hạn
public User findByResetToken(String token) {
    String sql = "SELECT * FROM Users WHERE reset_token=? AND reset_expires>SYSDATETIME()";
    try (Connection cn = DBContext.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
        ps.setString(1, token);
        try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
    } catch (Exception e) { e.printStackTrace(); }
    return null;
}

// Tìm theo mã 6 số còn hạn
public User findByResetCode(String code) {
    String sql = "SELECT * FROM Users WHERE reset_code=? AND reset_expires>SYSDATETIME()";
    try (Connection cn = DBContext.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
        ps.setString(1, code);
        try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
    } catch (Exception e) { e.printStackTrace(); }
    return null;
}

// Xoá cả token + code + hạn sau khi đổi mật khẩu
public void clearResetToken(int userId) {
    String sql = "UPDATE Users SET reset_token=NULL, reset_code=NULL, reset_expires=NULL WHERE id=?";
    try (Connection cn = DBContext.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
        ps.setInt(1, userId);
        ps.executeUpdate();
    } catch (Exception e) { e.printStackTrace(); }
}
}
