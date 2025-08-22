package com.smartshop.dao;
import com.smartshop.util.DB; import java.sql.*; import java.util.*; import java.math.*;

public class OrderDAO {
    
    public static class Item {
        public int productId;
        public int qty;
        public BigDecimal price;
        public Item(int productId, int qty, BigDecimal price) {
            this.productId = productId; this.qty = qty; this.price = price;
        }
    }

    public int createOrder(int userId,
                           String fullName, String phone, String email, String address,
                           String paymentMethod, boolean paid, BigDecimal total,
                           List<Item> items) {
        String oSql = "INSERT INTO dbo.Orders(user_id, full_name, phone, email, address, total, payment_method, payment_status) " +
                      "VALUES(?,?,?,?,?,?,?,?)";
        String iSql = "INSERT INTO dbo.OrderItems(order_id, product_id, qty, price) VALUES(?,?,?,?)";
        try (Connection cn = DB.getConnection()) {
            cn.setAutoCommit(false);
            try (PreparedStatement psO = cn.prepareStatement(oSql, Statement.RETURN_GENERATED_KEYS)) {
                psO.setInt(1, userId);
                psO.setString(2, fullName);
                psO.setString(3, phone);
                psO.setString(4, email);
                psO.setString(5, address);
                psO.setBigDecimal(6, total);
                psO.setString(7, paymentMethod);
                psO.setString(8, paid ? "PAID" : "PENDING");
                psO.executeUpdate();
                int orderId;
                try (ResultSet keys = psO.getGeneratedKeys()) {
                    keys.next();
                    orderId = keys.getInt(1);
                }
                try (PreparedStatement psI = cn.prepareStatement(iSql)) {
                    for (Item it : items) {
                        psI.setInt(1, orderId);
                        psI.setInt(2, it.productId);
                        psI.setInt(3, it.qty);
                        psI.setBigDecimal(4, it.price);
                        psI.addBatch();
                    }
                    psI.executeBatch();
                }
                cn.commit();
                return orderId;
            } catch (SQLException ex) {
                cn.rollback();
                throw ex;
            } finally {
                cn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static String nvl(String s){ return s==null? "": s; }
    
    public int createOrderFromCart(int userId, String shipName, String shipPhone, String shipAddr, String paymentMethod){
        try(Connection c=DB.getConnection()){
            c.setAutoCommit(false);
            int cartId; try(PreparedStatement ps=c.prepareStatement("SELECT TOP 1 id FROM Carts WHERE user_id=? AND status='OPEN'")){
                ps.setInt(1,userId); try(ResultSet rs=ps.executeQuery()){ if(!rs.next()) throw new RuntimeException("Empty cart"); cartId=rs.getInt(1);}
            }
            BigDecimal total = BigDecimal.ZERO;
            try(PreparedStatement ps=c.prepareStatement("SELECT qty, unit_price FROM CartItems WHERE cart_id=?")){
                ps.setInt(1,cartId); try(ResultSet rs=ps.executeQuery()){
                    while(rs.next()){ total= total.add(rs.getBigDecimal("unit_price").multiply(new BigDecimal(rs.getInt("qty")))); }
                }
            }
            int orderId;
            try(PreparedStatement ps=c.prepareStatement(
                    "INSERT INTO Orders(user_id,status,total_amount,shipping_name,shipping_phone,shipping_address,payment_method) " +
                    "VALUES(?, 'PENDING', ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)){
                ps.setInt(1,userId); ps.setBigDecimal(2,total); ps.setString(3,shipName); ps.setString(4,shipPhone); ps.setString(5,shipAddr); ps.setString(6,paymentMethod);
                ps.executeUpdate(); try(ResultSet k=ps.getGeneratedKeys()){ k.next(); orderId=k.getInt(1);}
            }
            try(PreparedStatement ps=c.prepareStatement("INSERT INTO OrderItems(order_id,product_id,qty,unit_price) SELECT ?, product_id, qty, unit_price FROM CartItems WHERE cart_id=?")){
                ps.setInt(1,orderId); ps.setInt(2,cartId); ps.executeUpdate();
            }
            try(PreparedStatement ps=c.prepareStatement("UPDATE Carts SET status='ORDERED', updated_at=SYSUTCDATETIME() WHERE id=?")){
                ps.setInt(1,cartId); ps.executeUpdate();
            }
            try(PreparedStatement ps=c.prepareStatement("INSERT INTO Carts(user_id) VALUES(?)")){
                ps.setInt(1,userId); ps.executeUpdate();
            }
            c.commit();
            return orderId;
        } catch(SQLException e){ throw new RuntimeException(e);}
    }

    public void updateStatus(int orderId, String status){
        try(Connection c=DB.getConnection(); PreparedStatement ps=c.prepareStatement("UPDATE Orders SET status=?, updated_at=SYSUTCDATETIME() WHERE id=?")){
            ps.setString(1,status); ps.setInt(2,orderId); ps.executeUpdate();
        } catch(SQLException e){ throw new RuntimeException(e);}
    }

    public void deleteIfUnpaid(int orderId){
        try(Connection c=DB.getConnection(); PreparedStatement ps=c.prepareStatement("DELETE FROM Orders WHERE id=? AND status='PENDING'")){
            ps.setInt(1,orderId); ps.executeUpdate();
        } catch(SQLException e){ throw new RuntimeException(e);}
    }

    public java.util.List<Object[]> listAll(){
        String sql="SELECT o.id, u.username, o.status, o.total_amount, o.created_at FROM Orders o JOIN Users u ON o.user_id=u.id ORDER BY o.id DESC";
        java.util.List<Object[]> list=new java.util.ArrayList<>();
        try(Connection c=DB.getConnection(); PreparedStatement ps=c.prepareStatement(sql); ResultSet rs=ps.executeQuery()){
            while(rs.next()) list.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getBigDecimal(4), rs.getTimestamp(5)});
        } catch(SQLException e){ throw new RuntimeException(e);}
        return list;
    }
}
