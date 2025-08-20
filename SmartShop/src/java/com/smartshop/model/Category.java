package com.smartshop.model;

public class Category {

    private int id;
    private String name;
    private String slug;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String s) {
        this.slug = s;
    }
}
