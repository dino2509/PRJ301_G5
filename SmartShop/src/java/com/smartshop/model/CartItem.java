package com.smartshop.model;
import java.math.BigDecimal;
public class CartItem {
    private Product product;
    private int quantity;
    public CartItem(Product p,int q){this.product=p;this.quantity=q;}
    public Product getProduct(){return product;}
    public int getQuantity(){return quantity;} public void setQuantity(int q){this.quantity=q;}
    public BigDecimal getSubtotal(){ return product.getPrice().multiply(new BigDecimal(quantity)); }
}
