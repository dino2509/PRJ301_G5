package com.smartshop.model;

import java.math.BigDecimal;

public class CartItem {

    private int id;
    private int productId;
    private String productName;
    private String imageUrl;
    private BigDecimal unitPrice;
    private int qty;
    private String name;
    private BigDecimal price;
    private int quantity;   
    
    public CartItem() {}

    public CartItem(int productId, String name, BigDecimal price, int quantity, String imageUrl) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
    }
    

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
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public BigDecimal getSubtotal() { return price.multiply(BigDecimal.valueOf(quantity)); }
}
