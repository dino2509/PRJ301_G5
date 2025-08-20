package com.smartshop.dao;
import com.smartshop.model.CartItem; import com.smartshop.util.DB; import java.sql.*; import java.util.*; import java.math.*;

public class CartDAO {
    private CartItem map(ResultSet rs) throws SQLException {
        CartItem ci=new CartItem();
        ci.setId(rs.getInt("id")); ci.setProductId(rs.getInt("product_id")); ci.setProductName(rs.getString("name"));
        ci.setImageUrl(rs.getString("image_url")); ci.setUnitPrice(rs.getBigDecimal("unit_price")); ci.setQty(rs.getInt("qty"));
        return ci;
    }

    private int getOpenCartId(Connection c, int userId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT TOP 1 id FROM Carts WHERE user_id=? AND status='OPEN'")) {
            ps.setInt(1, userId);
            try (ResultSet rs=ps.executeQuery()) { if (rs.next()) return rs.getInt(1);}
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO Carts(user_id) VALUES(?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId); ps.executeUpdate(); try(ResultSet k=ps.getGeneratedKeys()){ k.next(); return k.getInt(1);}
        }
    }

    public void addItem(int userId, int productId, int qty) {
        String upsert = "MERGE CartItems AS tgt " +
                        "USING (SELECT ? AS cart_id, ? AS product_id) src " +
                        "ON tgt.cart_id=src.cart_id AND tgt.product_id=src.product_id " +
                        "WHEN MATCHED THEN UPDATE SET qty=tgt.qty+? " +
                        "WHEN NOT MATCHED THEN INSERT(cart_id,product_id,qty,unit_price) " +
                        "SELECT src.cart_id, src.product_id, ?, p.price FROM Products p WHERE p.id=src.product_id;";
        try(Connection c=DB.getConnection()){
            int cartId = getOpenCartId(c, userId);
            try(PreparedStatement ps=c.prepareStatement(upsert)){
                ps.setInt(1, cartId); ps.setInt(2, productId); ps.setInt(3, qty); ps.setInt(4, qty); ps.executeUpdate();
            }
        } catch(SQLException e){ throw new RuntimeException(e);}
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

    public int getOpenCartId(int userId){
        try(Connection c=DB.getConnection()){ return getOpenCartId(c,userId);}
        catch(SQLException e){ throw new RuntimeException(e);}
    }
}
