package com.smartshop.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;

public class Product {
    private int id;
    private String name;
    private String brand;
    private String color;
    private String description;
    private String imageUrl;
    private boolean active;

    /** Giá gốc */
    private BigDecimal price;

    /** % giảm giá, 0..100, có thể null */
    private BigDecimal sale;

    /** Giá sau giảm, có thể null */
    private BigDecimal salePrice;

    /** Giữ lại nếu DB đã có (không dùng cũng không xóa để tương thích) */
    private BigDecimal originalPrice;

    private Integer categoryId;
    private Integer stock;
    private Integer sold;
    private Double rating;
    private Timestamp promoEndAt;

    // ===== getters/setters =====
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

    public BigDecimal getSale() { return sale; }
    public void setSale(BigDecimal sale) { this.sale = sale; }

    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }

    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Integer getSold() { return sold; }
    public void setSold(Integer sold) { this.sold = sold; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Timestamp getPromoEndAt() { return promoEndAt; }
    public void setPromoEndAt(Timestamp promoEndAt) { this.promoEndAt = promoEndAt; }

    // ===== tiện ích tính toán, không phá vỡ mã cũ =====

    /** Giá hiệu lực: ưu tiên salePrice, nếu null thì tính từ sale, nếu vẫn null thì trả về price. */
    public BigDecimal getEffectivePrice() {
        if (salePrice != null) return salePrice;
        if (sale != null && price != null) {
            return price.multiply(BigDecimal.ONE.subtract(sale.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)))
                        .setScale(2, RoundingMode.HALF_UP);
        }
        return price;
    }

    /** Đồng bộ lại salePrice theo sale, chỉ trong RAM. Không ghi DB. */
    public void recalcBySale() {
        if (price != null && sale != null) {
            this.salePrice = price.multiply(BigDecimal.ONE.subtract(sale.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)))
                                  .setScale(2, RoundingMode.HALF_UP);
        }
    }

    /** Đồng bộ lại sale theo salePrice, chỉ trong RAM. Không ghi DB. */
    public void recalcBySalePrice() {
        if (price != null && salePrice != null && price.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = BigDecimal.ONE.subtract(salePrice.divide(price, 6, RoundingMode.HALF_UP));
            this.sale = ratio.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        }
    }
    
        public boolean isActive() {
        return active;
    }

    public void setActive(boolean v) {
        this.active = v;
    }
}
