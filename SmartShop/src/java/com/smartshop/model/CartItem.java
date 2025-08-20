package com.smartshop.model;

import java.math.BigDecimal;

public class CartItem {

    private int id;
    private int productId;
    private String productName;
    private String imageUrl;
    private BigDecimal unitPrice;
    private int qty;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int v) {
        this.productId = v;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String v) {
        this.productName = v;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String v) {
        this.imageUrl = v;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal v) {
        this.unitPrice = v;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int v) {
        this.qty = v;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(new java.math.BigDecimal(qty));
    }
}
