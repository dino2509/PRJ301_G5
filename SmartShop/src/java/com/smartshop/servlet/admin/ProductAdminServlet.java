package com.smartshop.servlet.admin;

import com.smartshop.dao.CategoryDAO;
import com.smartshop.dao.ProductDAO;
import com.smartshop.model.Category;
import com.smartshop.model.Product;
import com.smartshop.util.DB;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.*;

public class ProductAdminServlet extends HttpServlet {

    private static final String VIEW_LIST = "/WEB-INF/views/admin/products.jsp";
    private static final String VIEW_FORM = "/WEB-INF/views/admin/product_form.jsp";

    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    /* ===== doGet giữ nguyên hành vi listing và edit/new ===== */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = n(req.getParameter("action"));

        if ("new".equalsIgnoreCase(action)) {
            req.setAttribute("p", new Product());
            req.setAttribute("categories", categoryDAO.findAll());
            req.setAttribute("sale", new BigDecimal("0"));
            req.setAttribute("sale_price", null);
            req.getRequestDispatcher(VIEW_FORM).forward(req, resp);
            return;
        }

        if ("edit".equalsIgnoreCase(action)) {
            int id = i(req.getParameter("id"), -1);
            if (id > 0) {
                Product cur = productDAO.findById(id);
                req.setAttribute("p", cur);
                req.setAttribute("categories", categoryDAO.findAll());
                loadPricingForEdit(req, cur);
            }
            req.getRequestDispatcher(VIEW_FORM).forward(req, resp);
            return;
        }

        List<Product> list = listAllProducts();
        req.setAttribute("list", list);

        Map<Integer, String> categoryNames = new HashMap<>();
        try { for (Category c : categoryDAO.findAll()) categoryNames.put(c.getId(), c.getName()); }
        catch (Exception ignore) {}
        req.setAttribute("categoryNames", categoryNames);

        loadPricingMaps(req, list);
        req.getRequestDispatcher(VIEW_LIST).forward(req, resp);
    }

    /* ===== doPost: thêm nhánh bulk_pricing; các nhánh khác giữ nguyên ===== */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = n(req.getParameter("action")).toLowerCase();

        try {
            if ("bulk_pricing".equals(action)) {
                handleBulkPricing(req);
                resp.sendRedirect(req.getContextPath() + "/admin/products");
                return;
            }

            if ("delete".equals(action)) {
                int id = i(req.getParameter("id"), -1);
                if (id > 0) productDAO.delete(id);
                resp.sendRedirect(req.getContextPath() + "/admin/products");
                return;
            }

            // create/update sản phẩm (giữ nguyên tối đa)
            Product p = new Product();
            p.setId(i(req.getParameter("id"), 0));
            p.setCategoryId(i(req.getParameter("category_id"), 0));
            p.setName(n(req.getParameter("name")));
            p.setBrand(n(req.getParameter("brand")));
            p.setImageUrl(n(req.getParameter("image_url")));
            p.setDescription(n(req.getParameter("description")));
            p.setPrice(parseMoney(n(req.getParameter("price"))));
            p.setStock(i(req.getParameter("stock"), 0));

            boolean hasActiveParam = req.getParameterMap().containsKey("active")
                    || req.getParameterMap().containsKey("is_active");

            if (p.getId() > 0) {
                Product cur = productDAO.findById(p.getId());
                boolean desiredActive = hasActiveParam ? parseActive(req, false)
                        : (cur != null && cur.isActive());
                p.setActive(desiredActive);
                productDAO.update(p);
            } else {
                p.setActive(parseActive(req, true));
                productDAO.create(p);
            }

            // pricing đơn lẻ từ form add/edit
            int pid = p.getId() > 0 ? p.getId() : findLastInsertedId();
            BigDecimal sale = parsePercent(n(req.getParameter("sale")));
            BigDecimal salePrice = parseMoneyNullable(n(req.getParameter("sale_price")));

            if (sale == null && salePrice == null) {
                updatePricing(pid, new BigDecimal("0"), p.getPrice());
            } else if (sale != null && salePrice == null) {
                updatePricing(pid, clampSale(sale), calcSalePrice(p.getPrice(), sale));
            } else if (sale == null && salePrice != null) {
                updatePricing(pid, clampSale(calcSalePercent(p.getPrice(), salePrice)), salePrice);
            } else {
                updatePricing(pid, clampSale(sale), salePrice);
            }

        } catch (Exception e) {
            throw new ServletException(e);
        }

        resp.sendRedirect(req.getContextPath() + "/admin/products");
    }

    /* ===== BULK PRICING ===== */
    private void handleBulkPricing(HttpServletRequest req) throws SQLException {
        // nhận ids từ nhiều cách: ids, ids[], selected=comma
        Set<Integer> ids = new LinkedHashSet<>();
        String[] v1 = req.getParameterValues("ids");
        if (v1 != null) for (String s : v1) { int x = i(s, 0); if (x > 0) ids.add(x); }
        String[] v2 = req.getParameterValues("ids[]");
        if (v2 != null) for (String s : v2) { int x = i(s, 0); if (x > 0) ids.add(x); }
        String sel = n(req.getParameter("selected"));
        if (!sel.isEmpty()) for (String s : sel.split(",")) { int x = i(s.trim(), 0); if (x > 0) ids.add(x); }
        if (ids.isEmpty()) return;

        String mode = n(req.getParameter("mode")); // minus | percent
        BigDecimal amount = parseMoneyNullable(n(req.getParameter("amount")));
        BigDecimal percent = parsePercent(n(req.getParameter("percent")));

        try (Connection c = DB.getConnection()) {
            String[] st = pickProductSchemaTable(c);
            if (st == null) return;
            ensureSaleColumns(c, st[0], st[1]);
            String pk = findPkColumn(c, st[0], st[1]);

            if ("percent".equalsIgnoreCase(mode) && percent != null) {
                final String sql =
                        "UPDATE " + qf(st[0], st[1]) +
                        " SET sale = ?, sale_price = ROUND(price * (1 - ?/100.0), 0) " +
                        " WHERE " + qi(pk) + " = ?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    for (int id : ids) {
                        ps.setBigDecimal(1, clampSale(percent));
                        ps.setBigDecimal(2, clampSale(percent));
                        ps.setInt(3, id);
                        ps.executeUpdate();
                    }
                }
                return;
            }

            if (!"minus".equalsIgnoreCase(mode) || amount == null) return;

            // trừ tiền: sale_price = max(0, coalesce(sale_price,price) - amount),
            // sau đó recalc % sale
            final String sql1 =
                    "UPDATE " + qf(st[0], st[1]) +
                    " SET sale_price = CASE " +
                    "   WHEN COALESCE(sale_price, price) - ? < 0 THEN 0 " +
                    "   ELSE COALESCE(sale_price, price) - ? END " +
                    " WHERE " + qi(pk) + " = ?";
            final String sql2 =
                    "UPDATE " + qf(st[0], st[1]) +
                    " SET sale = CASE WHEN price > 0 THEN ROUND((1 - sale_price/price) * 100.0, 2) ELSE 0 END " +
                    " WHERE " + qi(pk) + " = ?";
            try (PreparedStatement ps1 = c.prepareStatement(sql1);
                 PreparedStatement ps2 = c.prepareStatement(sql2)) {
                for (int id : ids) {
                    ps1.setBigDecimal(1, amount);
                    ps1.setBigDecimal(2, amount);
                    ps1.setInt(3, id);
                    ps1.executeUpdate();

                    ps2.setInt(1, id);
                    ps2.executeUpdate();
                }
            }
        }
    }

    /* ===== hỗ trợ listing ===== */
    private List<Product> listAllProducts() {
        try { return productDAO.listAll(); }
        catch (Throwable t) {
            List<Product> out = new ArrayList<>();
            try (Connection c = DB.getConnection()) {
                String[] st = pickProductSchemaTable(c);
                if (st == null) return out;
                try (PreparedStatement ps = c.prepareStatement("SELECT * FROM " + qf(st[0], st[1]));
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.add(mapProduct(rs));
                }
            } catch (Exception ignore) {}
            return out;
        }
    }

    /* ===== pricing helpers (đã dùng trước đó) ===== */
    private void loadPricingMaps(HttpServletRequest req, List<Product> list) {
        Map<Integer, BigDecimal> sales = new HashMap<>();
        Map<Integer, BigDecimal> salePrices = new HashMap<>();
        for (Product p : list) { sales.put(p.getId(), new BigDecimal("0")); salePrices.put(p.getId(), p.getPrice()); }
        try (Connection c = DB.getConnection()) {
            String[] st = pickProductSchemaTable(c);
            if (st != null) {
                ensureSaleColumns(c, st[0], st[1]);
                String pk = findPkColumn(c, st[0], st[1]);
                String sql = "SELECT " + qi(pk) + " AS pid, sale, sale_price FROM " + qf(st[0], st[1]);
                try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt("pid");
                        BigDecimal s = rs.getBigDecimal("sale");
                        BigDecimal sp = rs.getBigDecimal("sale_price");
                        if (s != null) sales.put(id, s);
                        if (sp != null) salePrices.put(id, sp);
                    }
                }
            }
        } catch (SQLException ignore) {}
        req.setAttribute("sales", sales);
        req.setAttribute("salePrices", salePrices);
    }

    private void loadPricingForEdit(HttpServletRequest req, Product cur) {
        BigDecimal sale = null, salePrice = null;
        if (cur == null) { req.setAttribute("sale", new BigDecimal("0")); req.setAttribute("sale_price", null); return; }
        try (Connection c = DB.getConnection()) {
            String[] st = pickProductSchemaTable(c);
            if (st != null) {
                ensureSaleColumns(c, st[0], st[1]);
                String pk = findPkColumn(c, st[0], st[1]);
                String sql = "SELECT sale, sale_price FROM " + qf(st[0], st[1]) + " WHERE " + qi(pk) + " = ?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, cur.getId());
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) { sale = rs.getBigDecimal(1); salePrice = rs.getBigDecimal(2); } }
                }
            }
        } catch (Exception ignore) {}
        if (sale == null) sale = new BigDecimal("0");
        if (salePrice == null) salePrice = cur.getPrice();
        req.setAttribute("sale", sale);
        req.setAttribute("sale_price", salePrice);
    }

    private void updatePricing(int productId, BigDecimal sale, BigDecimal salePrice) throws SQLException {
        if (productId <= 0) return;
        try (Connection c = DB.getConnection()) {
            String[] st = pickProductSchemaTable(c);
            if (st == null) return;
            ensureSaleColumns(c, st[0], st[1]);
            String pk = findPkColumn(c, st[0], st[1]);
            String sql = "UPDATE " + qf(st[0], st[1]) + " SET sale = ?, sale_price = ? WHERE " + qi(pk) + " = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setBigDecimal(1, clampSale(sale));
                ps.setBigDecimal(2, salePrice);
                ps.setInt(3, productId);
                ps.executeUpdate();
            }
        }
    }

    private static BigDecimal calcSalePrice(BigDecimal price, BigDecimal salePercent) {
        if (price == null) return null;
        if (salePercent == null) return price;
        BigDecimal oneMinus = BigDecimal.ONE.subtract(salePercent.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        return price.multiply(oneMinus).setScale(0, RoundingMode.HALF_UP);
    }
    private static BigDecimal calcSalePercent(BigDecimal price, BigDecimal salePrice) {
        if (price == null || salePrice == null || price.compareTo(BigDecimal.ZERO) <= 0) return new BigDecimal("0");
        BigDecimal ratio = BigDecimal.ONE.subtract(salePrice.divide(price, 6, RoundingMode.HALF_UP));
        return ratio.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }
    private static BigDecimal clampSale(BigDecimal sale) {
        if (sale == null) return new BigDecimal("0");
        if (sale.compareTo(BigDecimal.ZERO) < 0) return new BigDecimal("0");
        if (sale.compareTo(new BigDecimal("100")) > 0) return new BigDecimal("100");
        return sale.setScale(2, RoundingMode.HALF_UP);
    }

    /* ===== DB helpers ===== */
    private static String qi(String ident) { return "[" + ident.replace("]", "]]") + "]"; }
    private static String qf(String schema, String table) { return qi(schema) + "." + qi(table); }
    private String[] pickProductSchemaTable(Connection c) throws SQLException {
        String[] st = findSchemaTable(c, "Products"); if (st != null) return st;
        return findSchemaTable(c, "Product");
    }
    private String[] findSchemaTable(Connection c, String table) throws SQLException {
        final String sql =
                "SELECT s.name AS schema_name, t.name AS table_name " +
                "FROM sys.tables t JOIN sys.schemas s ON s.schema_id = t.schema_id " +
                "WHERE t.name = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new String[]{rs.getString("schema_name"), rs.getString("table_name")};
            }
        }
        return null;
    }
    private String findPkColumn(Connection c, String schema, String table) {
        String sql = "SELECT TOP 1 * FROM " + qf(schema, table);
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next() || true) {
                String id = colName(rs, "id");
                if (id != null && !id.isEmpty()) return id;
                String pid = colName(rs, "product_id");
                if (pid != null && !pid.isEmpty()) return pid;
            }
        } catch (SQLException ignore) {}
        return "id";
    }
    private void ensureSaleColumns(Connection c, String schema, String table) throws SQLException {
        if (!hasColumn(c, schema, table, "sale")) {
            try (Statement st = c.createStatement()) {
                st.execute("ALTER TABLE " + qf(schema, table) + " ADD sale DECIMAL(5,2) NULL");
            }
        }
        if (!hasColumn(c, schema, table, "sale_price")) {
            try (Statement st = c.createStatement()) {
                st.execute("ALTER TABLE " + qf(schema, table) + " ADD sale_price DECIMAL(19,2) NULL");
            }
        }
        try (Statement st = c.createStatement()) {
            st.execute("UPDATE " + qf(schema, table) + " SET sale = ISNULL(sale,0), sale_price = ISNULL(sale_price, price)");
        }
    }
    private boolean hasColumn(Connection c, String schema, String table, String col) throws SQLException {
        final String sql =
                "SELECT 1 FROM sys.columns c " +
                "JOIN sys.tables t ON c.object_id=t.object_id " +
                "JOIN sys.schemas s ON s.schema_id=t.schema_id " +
                "WHERE s.name=? AND t.name=? AND c.name=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, schema); ps.setString(2, table); ps.setString(3, col);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /* ===== mapping + utils ===== */
    private static Product mapProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(intCol(rs, "id", "product_id"));
        p.setName(strCol(rs, "name", "product_name"));
        p.setBrand(strCol(rs, "brand"));
        p.setDescription(strCol(rs, "description", "desc"));
        p.setImageUrl(strCol(rs, "image_url", "image", "img_url", "imageUrl"));
        p.setPrice(decCol(rs, "price", "unit_price"));
        p.setCategoryId(intCol(rs, "category_id", "categoryId"));
        p.setStock(intCol(rs, "stock", "quantity", "qty"));
        try {
            String ac = colName(rs, "is_active", "active");
            if (ac != null) p.setActive(rs.getInt(ac) == 1 || rs.getBoolean(ac));
        } catch (Exception ignore) {}
        return p;
    }

    private static String colName(ResultSet rs, String... cands) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        for (int i=1;i<=md.getColumnCount();i++) {
            String label = md.getColumnLabel(i), name = md.getColumnName(i);
            for (String c: cands) if (c.equalsIgnoreCase(label) || c.equalsIgnoreCase(name)) return label;
        }
        return null;
    }
    private static String strCol(ResultSet rs, String... cands) throws SQLException {
        String c = colName(rs,cands); return c==null? "" : rs.getString(c);
    }
    private static int intCol(ResultSet rs, String... cands) throws SQLException {
        String c = colName(rs,cands); return c==null? 0 : rs.getInt(c);
    }
    private static BigDecimal decCol(ResultSet rs, String... cands) throws SQLException {
        String c = colName(rs,cands); return c==null? BigDecimal.ZERO : rs.getBigDecimal(c);
    }

    private static String n(String s){ return s==null? "" : s.trim(); }
    private static int i(String s,int d){ try{ return Integer.parseInt(s);}catch(Exception e){ return d; } }

    /** "10.000.000" -> 10000000 */
    private static BigDecimal parseMoney(String s){
        if (s==null) return BigDecimal.ZERO;
        String clean = s.replaceAll("[^\\d]", "");
        if (clean.isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(clean);
    }
    private static BigDecimal parseMoneyNullable(String s){
        if (s==null) return null;
        String clean = s.replaceAll("[^\\d]", "");
        if (clean.isEmpty()) return null;
        return new BigDecimal(clean);
    }
    /** "10", "10,5", "10.5" */
    private static BigDecimal parsePercent(String s){
        if (s==null || s.isEmpty()) return null;
        String norm = s.replace(',', '.').replaceAll("[^0-9.]", "");
        if (norm.isEmpty()) return null;
        try { return new BigDecimal(norm); } catch (Exception e){ return null; }
    }

    private int findLastInsertedId() {
        try (Connection c = DB.getConnection()) {
            String[] st = pickProductSchemaTable(c);
            if (st == null) return 0;
            String pk = findPkColumn(c, st[0], st[1]);
            String sql = "SELECT MAX(" + qi(pk) + ") AS max_id FROM " + qf(st[0], st[1]);
            try (PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("max_id");
            }
        } catch (Exception ignore) {}
        return 0;
    }
    
    private static boolean parseActive(HttpServletRequest req, boolean def) {
    Map<String, String[]> pm = req.getParameterMap();
    boolean provided = pm.containsKey("active") || pm.containsKey("is_active");
    if (!provided) return def;

    String a = req.getParameter("active");
    String b = req.getParameter("is_active");
    return (a != null && (a.equalsIgnoreCase("on") || a.equals("1") || a.equalsIgnoreCase("true")))
        || (b != null && (b.equalsIgnoreCase("on") || b.equals("1") || b.equalsIgnoreCase("true")));
}
}
