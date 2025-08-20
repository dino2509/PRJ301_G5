package com.smartshop.dao;

import com.smartshop.model.CartItem;
import com.smartshop.util.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CartDAO {
    
    private CartItem map(ResultSet rs) throws SQLException {
        CartItem ci=new CartItem();
        ci.setId(rs.getInt("id")); ci.setProductId(rs.getInt("product_id")); ci.setProductName(rs.getString("name"));
        ci.setImageUrl(rs.getString("image_url")); ci.setUnitPrice(rs.getBigDecimal("unit_price")); ci.setQty(rs.getInt("qty"));
        return ci;
    }


    // ---------- Public safe wrappers (không ném lỗi ra ngoài) ----------
    public static BigDecimal safeGetProductPrice(int productId, BigDecimal fallback) {
        try (Connection cn = DB.getConnection()) {
            BigDecimal p = getProductPrice(cn, productId);
            return p != null ? p : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    public static void safeAddItemForUser(int userId, int productId, int qty, BigDecimal unitPrice) {
        try (Connection cn = DB.getConnection()) {
            cn.setAutoCommit(false);
            int cartId = ensureOpenCart(cn, userId);
            upsertItemIncrement(cn, cartId, productId, qty, unitPrice);
            cn.commit();
        } catch (Exception ignore) {}
    }

    public static void safeSetItemQtyForUser(int userId, int productId, int qty, BigDecimal unitPrice) {
        try (Connection cn = DB.getConnection()) {
            cn.setAutoCommit(false);
            int cartId = ensureOpenCart(cn, userId);
            setItemQty(cn, cartId, productId, qty, unitPrice);
            cn.commit();
        } catch (Exception ignore) {}
    }

    public static void safeRemoveItemForUser(int userId, int productId) {
        try (Connection cn = DB.getConnection()) {
            int cartId = getOpenCartId(cn, userId);
            if (cartId > 0) removeItem(cn, cartId, productId);
        } catch (Exception ignore) {}
    }

    // ---------- Core helpers ----------
    private static int ensureOpenCart(Connection cn, int userId) throws SQLException {
        int id = getOpenCartId(cn, userId);
        if (id > 0) return id;
        try (PreparedStatement ps = cn.prepareStatement(
                "INSERT INTO dbo.Carts(user_id,status) VALUES(?, 'OPEN');",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        // Fallback select
        return getOpenCartId(cn, userId);
    }

    private static int getOpenCartId(Connection cn, int userId) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT TOP 1 id FROM dbo.Carts WHERE user_id=? AND status='OPEN' ORDER BY id DESC")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static BigDecimal getProductPrice(Connection cn, int productId) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT price FROM dbo.Products WHERE id=?")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : null;
            }
        }
    }

    private static String qtyCol(Connection cn) throws SQLException {
        // Tương thích cả schema có 'quantity' hoặc 'qty'
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT CASE WHEN EXISTS (SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID('dbo.CartItems') AND name='quantity') THEN 'quantity' ELSE 'qty' END")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private static boolean itemExists(Connection cn, int cartId, int productId) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT 1 FROM dbo.CartItems WHERE cart_id=? AND product_id=?")) {
            ps.setInt(1, cartId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void upsertItemIncrement(Connection cn, int cartId, int productId, int qty, BigDecimal unitPrice) throws SQLException {
        String qc = qtyCol(cn);
        if (itemExists(cn, cartId, productId)) {
            String sql = "UPDATE dbo.CartItems SET " + qc + " = " + qc + " + ?, unit_price=? WHERE cart_id=? AND product_id=?";
            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setInt(1, qty);
                ps.setBigDecimal(2, unitPrice);
                ps.setInt(3, cartId);
                ps.setInt(4, productId);
                ps.executeUpdate();
            }
        } else {
            String sql = "INSERT INTO dbo.CartItems(cart_id, product_id, " + qc + ", unit_price) VALUES(?,?,?,?)";
            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setInt(1, cartId);
                ps.setInt(2, productId);
                ps.setInt(3, qty);
                ps.setBigDecimal(4, unitPrice);
                ps.executeUpdate();
            }
        }
    }

    private static void setItemQty(Connection cn, int cartId, int productId, int qty, BigDecimal unitPrice) throws SQLException {
        String qc = qtyCol(cn);
        if (qty <= 0) {
            removeItem(cn, cartId, productId);
            return;
        }
        if (itemExists(cn, cartId, productId)) {
            String sql = "UPDATE dbo.CartItems SET " + qc + "=?, unit_price=? WHERE cart_id=? AND product_id=?";
            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setInt(1, qty);
                ps.setBigDecimal(2, unitPrice);
                ps.setInt(3, cartId);
                ps.setInt(4, productId);
                ps.executeUpdate();
            }
        } else {
            String sql = "INSERT INTO dbo.CartItems(cart_id, product_id, " + qc + ", unit_price) VALUES(?,?,?,?)";
            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setInt(1, cartId);
                ps.setInt(2, productId);
                ps.setInt(3, qty);
                ps.setBigDecimal(4, unitPrice);
                ps.executeUpdate();
            }
        }
    }

    private static void removeItem(Connection cn, int cartId, int productId) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "DELETE FROM dbo.CartItems WHERE cart_id=? AND product_id=?")) {
            ps.setInt(1, cartId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }
    
        public List<CartItem> listItems(int userId){
        String sql="SELECT ci.*, p.name, p.image_url FROM CartItems ci " +
                   "JOIN Carts c ON ci.cart_id=c.id JOIN Products p ON ci.product_id=p.id " +
                   "WHERE c.user_id=? AND c.status='OPEN' ORDER BY ci.id";
        List<CartItem> list=new ArrayList<>();
        try(Connection c=DB.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,userId); try(ResultSet rs=ps.executeQuery()){ while(rs.next()) list.add(map(rs)); }
        } catch(SQLException e){ throw new RuntimeException(e);}
        return list;
    }

    public void updateQty(int userId, int itemId, int qty){
        String sql="UPDATE ci SET ci.qty=? FROM CartItems ci JOIN Carts c ON ci.cart_id=c.id " +
                   "WHERE ci.id=? AND c.user_id=? AND c.status='OPEN'";
        try(Connection c=DB.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,qty); ps.setInt(2,itemId); ps.setInt(3,userId); ps.executeUpdate();
        } catch(SQLException e){ throw new RuntimeException(e);}
    }

    public void removeItem(int userId, int itemId){
        String sql="DELETE ci FROM CartItems ci JOIN Carts c ON ci.cart_id=c.id WHERE ci.id=? AND c.user_id=? AND c.status='OPEN'";
        try(Connection c=DB.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,itemId); ps.setInt(2,userId); ps.executeUpdate();
        } catch(SQLException e){ throw new RuntimeException(e);}
    }

    public BigDecimal total(int userId){
        String sql="SELECT SUM(ci.qty*ci.unit_price) FROM CartItems ci JOIN Carts c ON ci.cart_id=c.id WHERE c.user_id=? AND c.status='OPEN'";
        try(Connection c=DB.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,userId); try(ResultSet rs=ps.executeQuery()){ if(rs.next() && rs.getBigDecimal(1)!=null) return rs.getBigDecimal(1); }
        } catch(SQLException e){ throw new RuntimeException(e);}
        return BigDecimal.ZERO;
    }
}
