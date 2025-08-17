package com.smartshop.model;
import java.math.BigDecimal;
public class Product {
    private int id;
    private String name;
    private String brand;
    private String color;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private int categoryId;
    private int stock;
    private int sold;
    private double rating;
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public String getName(){return name;} public void setName(String n){this.name=n;}
    public String getBrand(){return brand;} public void setBrand(String b){this.brand=b;}
    public String getColor(){return color;} public void setColor(String c){this.color=c;}
    public String getDescription(){return description;} public void setDescription(String d){this.description=d;}
    public String getImageUrl(){return imageUrl;} public void setImageUrl(String i){this.imageUrl=i;}
    public java.math.BigDecimal getPrice(){return price;} public void setPrice(java.math.BigDecimal p){this.price=p;}
    public int getCategoryId(){return categoryId;} public void setCategoryId(int c){this.categoryId=c;}
    public int getStock(){return stock;} public void setStock(int s){this.stock=s;}
    public int getSold(){return sold;} public void setSold(int s){this.sold=s;}
    public double getRating(){return rating;} public void setRating(double r){this.rating=r;}
}
