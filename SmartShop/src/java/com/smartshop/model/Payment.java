package com.smartshop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {
    private int id;
    private int orderId;
    private String provider; // WALLET, FAKE
    private String txnId;
    private BigDecimal amount;
    private String status; // PENDING, PAID, FAILED
    private String rawPayload;
    private LocalDateTime createdAt;

    public int getId(){return id;} public void setId(int id){this.id=id;}
    public int getOrderId(){return orderId;} public void setOrderId(int orderId){this.orderId=orderId;}
    public String getProvider(){return provider;} public void setProvider(String provider){this.provider=provider;}
    public String getTxnId(){return txnId;} public void setTxnId(String txnId){this.txnId=txnId;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal amount){this.amount=amount;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
    public String getRawPayload(){return rawPayload;} public void setRawPayload(String rawPayload){this.rawPayload=rawPayload;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
}
