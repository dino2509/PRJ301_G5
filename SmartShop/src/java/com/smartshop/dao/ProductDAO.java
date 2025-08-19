package com.smartshop.dao;

import com.smartshop.model.Product;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

public class ProductDAO {

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setBrand(rs.getString("brand"));
        p.setColor(rs.getString("color"));
        p.setDescription(rs.getString("description"));
        p.setImageUrl(rs.getString("image_url"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setStock(rs.getInt("stock"));
        p.setSold(rs.getInt("sold"));
        p.setRating(rs.getDouble("rating"));
        return p;
    }

    public int countAll(String q, Integer categoryId, String brand, String color,
                        BigDecimal minPrice, BigDecimal maxPrice) {
        StringBuilder sb = new StringBuilder("SELECT COUNT(*) FROM Products WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            sb.append(" AND (name LIKE ? OR description LIKE ?)");
            args.add("%" + q + "%"); args.add("%" + q + "%");
        }
        if (categoryId != null) { sb.append(" AND category_id=?"); args.add(categoryId); }
        if (brand != null && !brand.isBlank()) { sb.append(" AND brand=?"); args.add(brand); }
        if (color != null && !color.isBlank()) { sb.append(" AND color=?"); args.add(color); }
        if (minPrice != null) { sb.append(" AND price>=?"); args.add(minPrice); }
        if (maxPrice != null) { sb.append(" AND price<=?"); args.add(maxPrice); }

        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sb.toString())) {
            for (int i = 0; i < args.size(); i++) ps.setObject(i + 1, args.get(i));
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    public List<Product> search(String q, Integer categoryId, String brand, String color,
                                BigDecimal minPrice, BigDecimal maxPrice,
                                String sort, int offset, int limit) {
        StringBuilder sb = new StringBuilder("SELECT * FROM Products WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            sb.append(" AND (name LIKE ? OR description LIKE ?)");
            args.add("%" + q + "%"); args.add("%" + q + "%");
        }
        if (categoryId != null) { sb.append(" AND category_id=?"); args.add(categoryId); }
        if (brand != null && !brand.isBlank()) { sb.append(" AND brand=?"); args.add(brand); }
        if (color != null && !color.isBlank()) { sb.append(" AND color=?"); args.add(color); }
        if (minPrice != null) { sb.append(" AND price>=?"); args.add(minPrice); }
        if (maxPrice != null) { sb.append(" AND price<=?"); args.add(maxPrice); }

        if (sort != null) {
            switch (sort) {
                case "price_asc"  -> sb.append(" ORDER BY price ASC");
                case "price_desc" -> sb.append(" ORDER BY price DESC");
                case "newest"     -> sb.append(" ORDER BY updated_at DESC");
                case "rating"     -> sb.append(" ORDER BY rating DESC");
                case "sold"       -> sb.append(" ORDER BY sold DESC");
                default           -> sb.append(" ORDER BY id DESC");
            }
        } else sb.append(" ORDER BY id DESC");

        sb.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");

        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sb.toString())) {
            int idx = 1;
            for (Object a : args) ps.setObject(idx++, a);
            ps.setInt(idx++, offset);
            ps.setInt(idx,   limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Product> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return List.of();
    }

    public Product findById(int id) {
        String sql = "SELECT * FROM Products WHERE id=?";
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return map(rs); }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public List<Product> top(String type, int limit) {
        String order = switch (type) {
            case "featured" -> "rating DESC";
            case "new"      -> "updated_at DESC";
            case "bestseller" -> "sold DESC";
            default         -> "id DESC";
        };
        String sql = "SELECT TOP(" + limit + ") * FROM Products ORDER BY " + order;
        try (Connection cn = DBContext.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Product> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (Exception e) { e.printStackTrace(); }
        return List.of();
    }
}
