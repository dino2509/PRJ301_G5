package com.smartshop.model;
public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String email;
    private String phone;
    private String fullName;
    private String role;
    private boolean active;
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public String getUsername(){return username;} public void setUsername(String u){this.username=u;}
    public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String p){this.passwordHash=p;}
    public String getEmail(){return email;} public void setEmail(String e){this.email=e;}
    public String getPhone(){return phone;} public void setPhone(String p){this.phone=p;}
    public String getFullName(){return fullName;} public void setFullName(String f){this.fullName=f;}
    public String getRole(){return role;} public void setRole(String r){this.role=r;}
    public boolean isActive(){return active;} public void setActive(boolean a){this.active=a;}
}
