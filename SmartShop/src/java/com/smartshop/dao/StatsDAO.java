package com.smartshop.dao;
import com.smartshop.util.DB; import java.sql.*; import java.util.*; import java.math.*;

public class StatsDAO {
    public Map<String, BigDecimal> revenueByDay(){
        String sql="SELECT d, revenue FROM vRevenueByDay ORDER BY d DESC";
        Map<String,BigDecimal> m=new LinkedHashMap<>();
        try(Connection c=DB.getConnection(); PreparedStatement ps=c.prepareStatement(sql); ResultSet rs=ps.executeQuery()){
            while(rs.next()) m.put(rs.getString(1), rs.getBigDecimal(2));
        } catch(SQLException e){ throw new RuntimeException(e);}
        return m;
    }

    public Map<String, BigDecimal> revenueByMonth(){
        String sql="SELECT ym, revenue FROM vRevenueByMonth ORDER BY ym DESC";
        Map<String,BigDecimal> m=new LinkedHashMap<>();
        try(Connection c=DB.getConnection(); PreparedStatement ps=c.prepareStatement(sql); ResultSet rs=ps.executeQuery()){
            while(rs.next()) m.put(rs.getString(1), rs.getBigDecimal(2));
        } catch(SQLException e){ throw new RuntimeException(e);}
        return m;
    }

    public List<int[]> bestSellingProducts(){
        String sql="SELECT p.id, ISNULL(v.qty,0) qty FROM Products p LEFT JOIN vBestSellingProducts v ON p.id=v.product_id ORDER BY qty DESC";
        List<int[]> list=new ArrayList<>();
        try(Connection c=DB.getConnection(); PreparedStatement ps=c.prepareStatement(sql); ResultSet rs=ps.executeQuery()){
            while(rs.next()) list.add(new int[]{rs.getInt(1), rs.getInt(2)});
        } catch(SQLException e){ throw new RuntimeException(e);}
        return list;
    }

    public List<Object[]> topRatedProducts(){
        String sql="SELECT p.id, ISNULL(v.avg_rating,0) avg_rating, ISNULL(v.reviews,0) reviews FROM Products p LEFT JOIN vTopRatedProducts v ON p.id=v.product_id ORDER BY avg_rating DESC, reviews DESC";
        List<Object[]> list=new ArrayList<>();
        try(Connection c=DB.getConnection(); PreparedStatement ps=c.prepareStatement(sql); ResultSet rs=ps.executeQuery()){
            while(rs.next()) list.add(new Object[]{rs.getInt(1), rs.getDouble(2), rs.getInt(3)});
        } catch(SQLException e){ throw new RuntimeException(e);}
        return list;
    }

    public List<Object[]> productCountByCategory(){
        String sql="SELECT c.name, COUNT(p.id) FROM Categories c LEFT JOIN Products p ON p.category_id=c.id GROUP BY c.name ORDER BY c.name";
        List<Object[]> list=new ArrayList<>();
        try(Connection c=DB.getConnection(); PreparedStatement ps=c.prepareStatement(sql); ResultSet rs=ps.executeQuery()){
            while(rs.next()) list.add(new Object[]{rs.getString(1), rs.getInt(2)});
        } catch(SQLException e){ throw new RuntimeException(e);}
        return list;
    }

    public List<Object[]> newCustomersByDay(){
        String sql="SELECT CAST(created_at AS DATE) d, COUNT(*) FROM Users GROUP BY CAST(created_at AS DATE) ORDER BY d DESC";
        List<Object[]> list=new ArrayList<>();
        try(Connection c=DB.getConnection(); PreparedStatement ps=c.prepareStatement(sql); ResultSet rs=ps.executeQuery()){
            while(rs.next()) list.add(new Object[]{rs.getString(1), rs.getInt(2)});
        } catch(SQLException e){ throw new RuntimeException(e);}
        return list;
    }
}
