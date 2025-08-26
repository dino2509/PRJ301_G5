package com.smartshop.dao;

import com.smartshop.util.DB;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

/** StatsDAO: dò bảng/cột, bỏ lọc trạng thái để luôn ra số liệu khi có dữ liệu */
public class StatsDAO {

    public Map<String, BigDecimal> revenueByDay()   { return aggBy("DAY"); }
    public Map<String, BigDecimal> revenueByMonth() { return aggBy("MONTH"); }
    public List<Object[]> bestSellingProducts()     { return bestSelling(); }
    public List<Object[]> topRatedProducts()        { return topRated(); }
    public List<Object[]> productCountByCategory()  { return prodCountByCat(); }
    public List<Object[]> newCustomersByDay()       { return newCusByDay(); }

    private Map<String, BigDecimal> aggBy(String level) {
        String fmt = level.equals("DAY") ? "yyyy-MM-dd" : "yyyy-MM";
        Map<String, BigDecimal> out = new LinkedHashMap<>();
        try (Connection c = DB.getConnection()) {
            String oTable = tableExists(c,"Orders")? "Orders" : (tableExists(c,"Order")? "Order" : null);
            if (oTable==null) return out;
            String created = col(c,oTable,"created_at","created","order_date","createdAt");
            String total   = col(c,oTable,"total","grand_total","amount","final_total");

            String iTable = tableExists(c,"OrderItems")? "OrderItems" : (tableExists(c,"OrderItem")? "OrderItem" : null);
            String oiOid  = iTable==null? null : col(c,iTable,"order_id","orderId");
            String oiQty  = iTable==null? null : col(c,iTable,"qty","quantity");
            String oiPrice= iTable==null? null : col(c,iTable,"price","unit_price","unitPrice");

            String sql;
            if (total != null) {
                sql = "SELECT FORMAT(o."+created+",'"+fmt+"') k, SUM(o."+total+") v FROM "+oTable+" o GROUP BY FORMAT(o."+created+",'"+fmt+"') ORDER BY k DESC";
            } else if (oiOid!=null && oiQty!=null && oiPrice!=null) {
                sql = "SELECT FORMAT(o."+created+",'"+fmt+"') k, SUM(oi."+oiQty+"*oi."+oiPrice+") v " +
                      "FROM "+oTable+" o JOIN "+iTable+" oi ON oi."+oiOid+"=o.id " +
                      "GROUP BY FORMAT(o."+created+",'"+fmt+"') ORDER BY k DESC";
            } else return out;

            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.put(rs.getString(1), rs.getBigDecimal(2));
            }
        } catch (SQLException ignore) {}
        return out;
    }

    private List<Object[]> bestSelling() {
        List<Object[]> list = new ArrayList<>();
        try (Connection c = DB.getConnection()) {
            String iTable = tableExists(c,"OrderItems")? "OrderItems" : (tableExists(c,"OrderItem")? "OrderItem" : null);
            if (iTable==null) return list;
            String prod = col(c,iTable,"product_id","productId");
            String qty  = col(c,iTable,"qty","quantity");
            if (prod==null || qty==null) return list;
            String sql = "SELECT "+prod+", SUM("+qty+") q FROM "+iTable+" GROUP BY "+prod+" HAVING SUM("+qty+")>0 ORDER BY q DESC, "+prod+" DESC";
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Object[]{rs.getInt(1), rs.getInt(2)});
            }
        } catch (SQLException ignore) {}
        return list;
    }

    private List<Object[]> topRated() {
        List<Object[]> list = new ArrayList<>();
        try (Connection c = DB.getConnection()) {
            String rTable = tableExists(c,"Reviews")? "Reviews" : null;
            if (rTable==null) return list;
            String rating = col(c,rTable,"rating","stars");
            String prod   = col(c,rTable,"product_id","productId");
            if (rating==null || prod==null) return list;
            String sql = "SELECT "+prod+", CAST(AVG(CAST("+rating+" AS DECIMAL(5,2))) AS DECIMAL(5,2)) a, COUNT(*) n FROM "+rTable+" GROUP BY "+prod+" ORDER BY a DESC, n DESC";
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Object[]{rs.getInt(1), rs.getBigDecimal(2), rs.getInt(3)});
            }
        } catch (SQLException ignore) {}
        return list;
    }

    private List<Object[]> prodCountByCat() {
        List<Object[]> list = new ArrayList<>();
        try (Connection c = DB.getConnection()) {
            String pTable = tableExists(c,"Products")? "Products" : (tableExists(c,"Product")? "Product" : null);
            String cTable = tableExists(c,"Categories")? "Categories" : (tableExists(c,"Category")? "Category" : null);
            if (pTable==null || cTable==null) return list;
            String pCat = n(col(c,pTable,"category_id","categoryId"));
            String pAct = n(col(c,pTable,"is_active","active"));
            String sql = "SELECT c.name, COUNT(p.id) cnt FROM "+cTable+" c LEFT JOIN "+pTable+" p ON p."+pCat+"=c.id "+
                         (pAct.isEmpty()? "":"AND p."+pAct+"=1 ") +
                         "GROUP BY c.name ORDER BY c.name";
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Object[]{rs.getString(1), rs.getInt(2)});
            }
        } catch (SQLException ignore) {}
        return list;
    }

    private List<Object[]> newCusByDay() {
        List<Object[]> list = new ArrayList<>();
        try (Connection c = DB.getConnection()) {
            String uTable = tableExists(c,"Users")? "Users" : (tableExists(c,"User")? "User" : null);
            if (uTable==null) return list;
            String created = col(c,uTable,"created_at","created","createdAt");
            if (created==null) return list;
            String sql = "SELECT CONVERT(varchar(10), CAST("+created+" AS DATE), 120) d, COUNT(*) n FROM "+uTable+" GROUP BY CAST("+created+" AS DATE) ORDER BY d DESC";
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Object[]{rs.getString(1), rs.getInt(2)});
            }
        } catch (SQLException ignore) {}
        return list;
    }

    private static boolean tableExists(Connection c, String t){
        try(ResultSet rs=c.getMetaData().getTables(null,null,t,null)){ return rs.next(); }
        catch(SQLException e){ return false; }
    }
    private static String col(Connection c, String table, String... opts) throws SQLException {
        DatabaseMetaData md = c.getMetaData();
        for(String o:opts){
            try(ResultSet rs=md.getColumns(null,null,table,o)){ if(rs.next()) return o; }
            try(ResultSet rs=md.getColumns(null,null,"dbo."+table,o)){ if(rs.next()) return o; }
        }
        return null;
    }
    private static String n(String s){ return s==null? "": s; }
}
