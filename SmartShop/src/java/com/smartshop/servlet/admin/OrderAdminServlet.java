package com.smartshop.servlet.admin;

import com.smartshop.util.DB;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.*;
import java.util.*;

@WebServlet("/admin/orders")
public class OrderAdminServlet extends HttpServlet {

    public static class Row {
        private final int id; private final Integer userId; private final String username;
        private final String status; private final java.sql.Timestamp created; private final java.math.BigDecimal total;
        public Row(int id,Integer uid,String un,String st,java.sql.Timestamp cr,java.math.BigDecimal tt){
            this.id=id;this.userId=uid;this.username=un;this.status=st;this.created=cr;this.total=tt;}
        public int getId(){return id;} public Integer getUserId(){return userId;}
        public String getUsername(){return username;} public String getStatus(){return status;}
        public java.math.BigDecimal getTotal(){return total;} public java.sql.Timestamp getCreated(){return created;}
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("orders", load());
        req.getRequestDispatcher("/WEB-INF/views/admin/orders.jsp").forward(req, resp);
    }

    private List<Row> load(){
        List<Row> out=new ArrayList<>();
        try (Connection c = DB.getConnection()) {
            String oTable = tableExists(c,"Orders")? "Orders" : (tableExists(c,"Order")? "Order" : null);
            if (oTable==null) return out;

            String created = col(c,oTable,"created_at","created","order_date","createdAt");
            String status  = col(c,oTable,"status","order_status");
            String total   = col(c,oTable,"total","grand_total","amount","final_total");
            String userId  = col(c,oTable,"user_id","userId");

            String uTable = tableExists(c,"Users")? "Users" : (tableExists(c,"User")? "User" : null);
            String userNameCol = uTable==null? null : col(c,uTable,"username","user_name","email");

            String sql;
            if (total!=null) {
                sql = "SELECT TOP 200 o.id, "+(userId==null?"NULL":("o."+userId))+" uid, "+
                        (userNameCol==null?"NULL":("u."+userNameCol))+" uname, "+
                        (status==null?"NULL":("o."+status))+" st, o."+created+" cr, o."+total+" tt " +
                        "FROM "+oTable+" o "+(userId==null||userNameCol==null? "":"LEFT JOIN "+uTable+" u ON u.id=o."+userId+" ") +
                        "ORDER BY o.id DESC";
            } else {
                String iTable = tableExists(c,"OrderItems")? "OrderItems" : (tableExists(c,"OrderItem")? "OrderItem" : null);
                if (iTable==null) return out;
                String oiOid = col(c,iTable,"order_id","orderId");
                String oiQty = col(c,iTable,"qty","quantity");
                String oiPrice = col(c,iTable,"price","unit_price","unitPrice");
                if (oiOid==null||oiQty==null||oiPrice==null) return out;

                sql = "SELECT TOP 200 o.id, "+(userId==null?"NULL":("o."+userId))+" uid, "+
                        (userNameCol==null?"NULL":("u."+userNameCol))+" uname, "+
                        (status==null?"NULL":("o."+status))+" st, o."+created+" cr, "+
                        "SUM(oi."+oiQty+"*oi."+oiPrice+") tt " +
                        "FROM "+oTable+" o JOIN "+iTable+" oi ON oi."+oiOid+"=o.id "+
                        (userId==null||userNameCol==null? "":"LEFT JOIN "+uTable+" u ON u.id=o."+userId+" ")+
                        "GROUP BY o.id,"+(userId==null?"":("o."+userId+","))+(userNameCol==null?"":("u."+userNameCol+","))+
                        (status==null?"":("o."+status+","))+" o."+created+" ORDER BY o.id DESC";
            }
            try (PreparedStatement ps=c.prepareStatement(sql); ResultSet rs=ps.executeQuery()){
                while(rs.next()){
                    out.add(new Row(
                            rs.getInt(1),
                            (Integer)rs.getObject(2),
                            rs.getString(3),
                            rs.getString(4),
                            rs.getTimestamp(5),
                            rs.getBigDecimal(6)
                    ));
                }
            }
        } catch (SQLException ignore) {}
        return out;
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
}
