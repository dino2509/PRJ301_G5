package com.smartshop.servlet.shop;

import com.smartshop.dao.ProductDAO;
import com.smartshop.model.Product;
import com.smartshop.util.DB;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet("/products")
public class ProductsServlet extends HttpServlet {
    private final ProductDAO dao = new ProductDAO();
    private static final String VIEW = "/WEB-INF/views/shop/products.jsp";

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String q = s(req.getParameter("q"));
        Integer cat = toInt(req.getParameter("categoryId"));
        String brand = s(req.getParameter("brand"));
        String color = s(req.getParameter("color"));
        BigDecimal min = toDec(req.getParameter("min"));
        BigDecimal max = toDec(req.getParameter("max"));
        String sort = s(req.getParameter("sort"));
        int page = toInt(req.getParameter("page"), 1);
        int size = toInt(req.getParameter("pageSize"), 40);

        List<Product> list = safeSearch(q, cat, brand, color, min, max, sort, page, size);
        req.setAttribute("products", list);
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    private List<Product> safeSearch(String q, Integer cat, String brand, String color,
                                     BigDecimal min, BigDecimal max, String sort, int page, int size) {
        try {
            Method m = dao.getClass().getMethod("search",
                    String.class, Integer.class, String.class, String.class,
                    BigDecimal.class, BigDecimal.class, String.class, int.class, int.class);
            @SuppressWarnings("unchecked")
            List<Product> r = (List<Product>) m.invoke(dao, q, cat, brand, color, min, max, sort, page, size);
            if (r != null) return r;
        } catch (Throwable ignore) {}

        List<Product> all = fetchAllViaDAO();
        if (all.isEmpty()) all = fetchAllViaJDBC();

        String k = q == null ? "" : q.toLowerCase(Locale.ROOT);
        final String kb = k;
        List<Product> filtered = all.stream().filter(p -> {
            boolean ok = true;
            if (!kb.isEmpty()) ok &= (nz(p.getName()).toLowerCase().contains(kb)
                    || nz(p.getBrand()).toLowerCase().contains(kb)
                    || nz(p.getDescription()).toLowerCase().contains(kb));
            if (cat != null && cat > 0) ok &= p.getCategoryId() == cat;
            if (!s(brand).isEmpty()) ok &= nz(p.getBrand()).equalsIgnoreCase(brand);
            if (!s(color).isEmpty()) ok &= nz(p.getColor()).equalsIgnoreCase(color);
            if (min != null && p.getPrice()!=null) ok &= p.getPrice().compareTo(min) >= 0;
            if (max != null && p.getPrice()!=null) ok &= p.getPrice().compareTo(max) <= 0;
            return ok;
        }).collect(Collectors.toList());

        if ("price_asc".equalsIgnoreCase(sort)) filtered.sort(Comparator.comparing(Product::getPrice));
        else if ("price_desc".equalsIgnoreCase(sort)) filtered.sort(Comparator.comparing(Product::getPrice).reversed());
        else filtered.sort(Comparator.comparingInt(Product::getId));

        if (size <= 0) size = 40;
        if (page <= 0) page = 1;
        int from = Math.min((page - 1) * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return filtered.subList(from, to);
    }

    @SuppressWarnings("unchecked")
    private List<Product> fetchAllViaDAO() {
        try { Method m = dao.getClass().getMethod("listAll"); return (List<Product>) m.invoke(dao); }
        catch (Throwable ignore) {}
        try { Method m = dao.getClass().getMethod("findAll"); return (List<Product>) m.invoke(dao); }
        catch (Throwable ignore) {}
        try { Method m = dao.getClass().getMethod("top", String.class, int.class); return (List<Product>) m.invoke(dao, "newest", 10000); }
        catch (Throwable ignore) {}
        return Collections.emptyList();
    }

    private List<Product> fetchAllViaJDBC() {
        List<Product> out = new ArrayList<>();
        try (Connection c = DB.getConnection()) {
            String table = tableExists(c,"Products") ? "Products" : (tableExists(c,"Product") ? "Product" : null);
            if (table == null) return out;
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM "+table);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapProduct(rs));
            }
        } catch (Exception ignore) {}
        return out;
    }

    // === mapper (trùng với admin) ===
    private static Product mapProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(intCol(rs, "id"));
        p.setName(strCol(rs, "name","product_name"));
        p.setBrand(strCol(rs, "brand"));
        p.setColor(strCol(rs, "color"));
        p.setDescription(strCol(rs, "description","desc"));
        p.setImageUrl(strCol(rs, "image_url","image","img_url","imageUrl"));
        p.setPrice(decCol(rs, "price","unit_price","sale_price"));
        p.setCategoryId(intCol(rs, "category_id","categoryId"));
        p.setStock(intCol(rs, "stock","quantity","qty"));
        return p;
    }
    private static boolean tableExists(Connection c, String t) {
        try (ResultSet rs = c.getMetaData().getTables(null, null, t, null)) { return rs.next(); }
        catch (SQLException e){ return false; }
    }
    private static String colName(ResultSet rs, String... cands) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        for (int i=1;i<=md.getColumnCount();i++) {
            String label = md.getColumnLabel(i); String name = md.getColumnName(i);
            for (String c: cands) if (c.equalsIgnoreCase(label) || c.equalsIgnoreCase(name)) return label;
        }
        return null;
    }
    private static String strCol(ResultSet rs, String... cands) throws SQLException {
        String c = colName(rs,cands); return c==null? "": rs.getString(c);
    }
    private static int intCol(ResultSet rs, String... cands) throws SQLException {
        String c = colName(rs,cands); return c==null? 0 : rs.getInt(c);
    }
    private static BigDecimal decCol(ResultSet rs, String... cands) throws SQLException {
        String c = colName(rs,cands); return c==null? BigDecimal.ZERO : rs.getBigDecimal(c);
    }

    private static String s(String v){ return v==null? "": v.trim(); }
    private static String nz(String v){ return v==null? "": v; }
    private static Integer toInt(String s){ return toInt(s, null); }
    private static Integer toInt(String s, Integer d){ try{ return s==null||s.isBlank()?d:Integer.valueOf(s);}catch(Exception e){return d;} }
    private static BigDecimal toDec(String s){ try{ return s==null||s.isBlank()?null:new BigDecimal(s);}catch(Exception e){return null;} }
}
