package com.smartshop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletTx {
    private int id;
    private int userId;
    private String type; // TOPUP, DEBIT, REFUND
    private BigDecimal amount;
    private String status; // PENDING, APPROVED, REJECTED
    private Integer refOrderId;
    private LocalDateTime createdAt;

    public int getId(){return id;} public void setId(int id){this.id=id;}
    public int getUserId(){return userId;} public void setUserId(int userId){this.userId=userId;}
    public String getType(){return type;} public void setType(String type){this.type=type;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal amount){this.amount=amount;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public Integer getRefOrderId(){return refOrderId;} public void setRefOrderId(Integer refOrderId){this.refOrderId=refOrderId;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
}
