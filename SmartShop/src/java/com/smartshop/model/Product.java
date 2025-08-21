package com.smartshop.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Product {
    private int id;
    private String name;
    private String brand;
    private String color;
    private String description;
    private String imageUrl;
    private BigDecimal price;          // giá sau khuyến mãi
    private BigDecimal originalPrice;  // giá gốc
    private Timestamp promoEndAt;      // hạn khuyến mãi
    private int categoryId;
    private int stock;
    private int sold;
    private double rating;
    private boolean active;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public Timestamp getPromoEndAt() { return promoEndAt; }
    public void setPromoEndAt(Timestamp promoEndAt) { this.promoEndAt = promoEndAt; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public int getSold() { return sold; }
    public void setSold(int sold) { this.sold = sold; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean v) {
        this.active = v;
    }
}
