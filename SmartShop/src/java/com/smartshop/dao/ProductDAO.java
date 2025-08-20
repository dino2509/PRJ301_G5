package com.smartshop.dao;

import com.smartshop.model.Product;
import com.smartshop.util.DB;

import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

public class ProductDAO {

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));

        Object catObj = rs.getObject("category_id");
        p.setCategoryId(catObj == null ? null : ((Number) catObj).intValue());

        p.setName(rs.getString("name"));
        p.setBrand(rs.getString("brand"));
        p.setColor(rs.getString("color"));
        p.setDescription(rs.getString("description"));
        p.setImageUrl(rs.getString("image_url"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setStock(rs.getInt("stock"));

        Object soldObj = rs.getObject("sold");
        if (soldObj != null) p.setSold(((Number) soldObj).intValue());

        Object ratingObj = rs.getObject("rating");
        p.setRating(ratingObj == null ? null : ((Number) ratingObj).doubleValue());

        p.setActive(rs.getBoolean("is_active"));
        return p;
    }

    public int countAll(String q, Integer categoryId, String brand, String color,
                        BigDecimal minPrice, BigDecimal maxPrice) {
        StringBuilder sb = new StringBuilder("SELECT COUNT(*) FROM Products WHERE is_active=1");
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isBlank()) { sb.append(" AND (name LIKE ? OR description LIKE ?)"); args.add("%"+q+"%"); args.add("%"+q+"%"); }
        if (categoryId != null) { sb.append(" AND category_id=?"); args.add(categoryId); }
        if (brand != null && !brand.isBlank()) { sb.append(" AND brand=?"); args.add(brand); }
        if (color != null && !color.isBlank()) { sb.append(" AND color=?"); args.add(color); }
        if (minPrice != null) { sb.append(" AND price>=?"); args.add(minPrice); }
        if (maxPrice != null) { sb.append(" AND price<=?"); args.add(maxPrice); }

        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sb.toString())) {
            for (int i = 0; i < args.size(); i++) ps.setObject(i + 1, args.get(i));
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<Product> search(String q, Integer categoryId, String brand, String color,
                                BigDecimal minPrice, BigDecimal maxPrice, String sort, int page, int pageSize) {
        StringBuilder sb = new StringBuilder("SELECT * FROM Products WHERE is_active=1");
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isBlank()) { sb.append(" AND (name LIKE ? OR description LIKE ?)"); args.add("%"+q+"%"); args.add("%"+q+"%"); }
        if (categoryId != null) { sb.append(" AND category_id=?"); args.add(categoryId); }
        if (brand != null && !brand.isBlank()) { sb.append(" AND brand=?"); args.add(brand); }
        if (color != null && !color.isBlank()) { sb.append(" AND color=?"); args.add(color); }
        if (minPrice != null) { sb.append(" AND price>=?"); args.add(minPrice); }
        if (maxPrice != null) { sb.append(" AND price<=?"); args.add(maxPrice); }

        String order = switch (sort == null ? "" : sort) {
            case "price_asc" -> "price ASC";
            case "price_desc" -> "price DESC";
            case "newest" -> "updated_at DESC";
            case "rating" -> "rating DESC";
            default -> "id DESC";
        };
        sb.append(" ORDER BY ").append(order).append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        int offset = (Math.max(page, 1) - 1) * pageSize;
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sb.toString())) {
            int idx = 1; for (Object o : args) ps.setObject(idx++, o);
            ps.setInt(idx++, offset); ps.setInt(idx, pageSize);
            List<Product> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
            return list;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Product find(int id) {
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM Products WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    public void create(Product p) {
        String sql = "INSERT INTO Products(category_id,name,brand,color,description,image_url,price,stock,is_active) " +
                     "VALUES(?,?,?,?,?,?,?,?,1)";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, p.getCategoryId());
            ps.setString(2, p.getName());
            ps.setString(3, p.getBrand());
            ps.setString(4, p.getColor());
            ps.setString(5, p.getDescription());
            ps.setString(6, p.getImageUrl());
            ps.setBigDecimal(7, p.getPrice());
            ps.setInt(8, p.getStock());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void update(Product p) {
        String sql = "UPDATE Products SET category_id=?, name=?, brand=?, color=?, description=?, image_url=?, " +
                     "price=?, stock=?, is_active=?, updated_at=SYSUTCDATETIME() WHERE id=?";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, p.getCategoryId());
            ps.setString(2, p.getName());
            ps.setString(3, p.getBrand());
            ps.setString(4, p.getColor());
            ps.setString(5, p.getDescription());
            ps.setString(6, p.getImageUrl());
            ps.setBigDecimal(7, p.getPrice());
            ps.setInt(8, p.getStock());
            ps.setBoolean(9, p.isActive());
            ps.setInt(10, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void delete(int id) {
        try (Connection c = DB.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM Products WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // ALIAS cho code cũ
    public Product findById(int id) { return find(id); }

    public List<Product> top(String type, int limit) {
        String order;
        switch (type == null ? "" : type.toLowerCase()) {
            case "featured":   order = "rating DESC, sold DESC, updated_at DESC"; break;
            case "new":
            case "newest":     order = "created_at DESC"; break;
            case "bestseller": order = "sold DESC"; break;
            default:           order = "updated_at DESC";
        }
        int n = Math.max(1, limit);
        String sql = "SELECT TOP " + n + " * FROM Products WHERE is_active=1 ORDER BY " + order; // TOP không bind '?'
        List<Product> list = new ArrayList<>();
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }
}
