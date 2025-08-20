package com.smartshop.dao;

import com.smartshop.model.Category;
import com.smartshop.util.DB;
import java.sql.*;
import java.util.*;

public class CategoryDAO {

    private Category map(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setSlug(rs.getString("slug"));
        return c;
    }

    public java.util.List<Category> findAll() {
        java.util.List<Category> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM Categories ORDER BY name";
        try (Connection c = DB.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}
