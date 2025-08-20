package com.smartshop.model;

import java.math.BigDecimal;

public class Product {

    private int id;
    private Integer categoryId;
    private String name;
    private String brand;
    private String color;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private int stock;
    private int sold;
    private Double rating;
    private boolean active;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer v) {
        this.categoryId = v;
    }

    public String getName() {
        return name;
    }

    public void setName(String v) {
        this.name = v;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String v) {
        this.brand = v;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String v) {
        this.color = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        this.description = v;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String v) {
        this.imageUrl = v;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal v) {
        this.price = v;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int v) {
        this.stock = v;
    }

    public int getSold() {
        return sold;
    }

    public void setSold(int v) {
        this.sold = v;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double v) {
        this.rating = v;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean v) {
        this.active = v;
    }
}
