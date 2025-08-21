package com.smartshop.model;

import java.math.BigDecimal;

public class Wallet {
    private int userId;
    private BigDecimal balance;

    public int getUserId(){return userId;}
    public void setUserId(int userId){this.userId=userId;}
    public BigDecimal getBalance(){return balance;}
    public void setBalance(BigDecimal balance){this.balance=balance;}
}
