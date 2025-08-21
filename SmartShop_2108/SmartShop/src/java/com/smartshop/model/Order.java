package com.smartshop.model;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public class Order {
    private int id;
    private int userId;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public int getUserId(){return userId;} public void setUserId(int u){this.userId=u;}
    public BigDecimal getTotalAmount(){return totalAmount;} public void setTotalAmount(BigDecimal t){this.totalAmount=t;}
    public String getStatus(){return status;} public void setStatus(String s){this.status=s;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime c){this.createdAt=c;}
}
