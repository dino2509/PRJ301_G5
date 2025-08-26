package com.smartshop.dao;

import com.smartshop.model.Product;
import com.smartshop.util.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO bổ sung để làm việc với sale/sale_price mà KHÔNG sửa ProductDAO cũ.
 * Bạn có thể gọi các method dưới đây từ admin controller.
 */
public class ProductPricingDAO {

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setBrand(rs.getString("brand"));
        p.setColor(rs.getString("color"));
        p.setDescription(rs.getString("description"));
        p.setImageUrl(rs.getString("image_url"));
        p.setPrice(rs.getBigDecimal("price"));

        // cột mới
        try { p.setSale(rs.getBigDecimal("sale")); } catch (SQLException ignore) {}
        try { p.setSalePrice(rs.getBigDecimal("sale_price")); } catch (SQLException ignore) {}

        // tương thích với dữ liệu cũ
        try { p.setOriginalPrice(rs.getBigDecimal("original_price")); } catch (SQLException ignore) {}
        try { p.setPromoEndAt(rs.getTimestamp("promo_end_at")); } catch (SQLException ignore) {}

        try { p.setCategoryId((Integer) rs.getObject("category_id")); } catch (SQLException ignore) {}
        try { p.setStock((Integer) rs.getObject("stock")); } catch (SQLException ignore) {}
        try { p.setSold((Integer) rs.getObject("sold")); } catch (SQLException ignore) {}
        try { p.setRating((Double) rs.getObject("rating")); } catch (SQLException ignore) {}
        return p;
    }

    public Product findByIdWithSale(int id) throws SQLException {
        String sql = "SELECT id,name,brand,color,description,image_url,price,sale,sale_price,"
                   + "original_price,promo_end_at,category_id,stock,sold,rating "
                   + "FROM dbo.Products WHERE id=?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Product> listWithSale(int limit, int offset) throws SQLException {
        String sql = "SELECT id,name,brand,color,description,image_url,price,sale,sale_price,"
                   + "original_price,promo_end_at,category_id,stock,sold,rating "
                   + "FROM dbo.Products ORDER BY id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, offset);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Product> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    /** Cập nhật theo % sale. Trigger sẽ tự tính sale_price. */
    public void updateSalePercent(int productId, BigDecimal salePercent) throws SQLException {
        String sql = "UPDATE dbo.Products SET sale=? , sale_price=NULL WHERE id=?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, salePercent);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    /** Cập nhật theo giá sau sale. Trigger sẽ tự tính lại % sale. */
    public void updateSalePrice(int productId, BigDecimal salePrice) throws SQLException {
        String sql = "UPDATE dbo.Products SET sale=NULL , sale_price=? WHERE id=?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, salePrice);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    /** Xóa khuyến mãi: đưa về giá gốc. */
    public void clearSale(int productId) throws SQLException {
        String sql = "UPDATE dbo.Products SET sale=0, sale_price=price WHERE id=?";
        try (Connection con = DB.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
        }
    }
}
