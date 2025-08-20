package com.smartshop.dao;
import com.smartshop.util.DB; import java.sql.*;

public class ReviewDAO {
    public void upsert(int userId, int productId, int rating, String comment){
        String sql = "MERGE ProductReviews AS tgt " +
                     "USING (SELECT ? AS product_id, ? AS user_id) src " +
                     "ON tgt.product_id=src.product_id AND tgt.user_id=src.user_id " +
                     "WHEN MATCHED THEN UPDATE SET rating=?, comment=?, created_at=SYSUTCDATETIME() " +
                     "WHEN NOT MATCHED THEN INSERT(product_id,user_id,rating,comment) VALUES(src.product_id, src.user_id, ?, ?)";
        try(Connection c=DB.getConnection(); PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1, productId); ps.setInt(2, userId); ps.setInt(3, rating); ps.setString(4, comment); ps.setInt(5, rating); ps.setString(6, comment);
            ps.executeUpdate();
        } catch(SQLException e){ throw new RuntimeException(e);}
    }
}
