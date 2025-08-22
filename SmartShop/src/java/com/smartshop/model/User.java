package com.smartshop.model;

import java.nio.charset.StandardCharsets;

public class User {

    private int id;
    private String username;
    private String email;
    private String phone;
    private String fullName;
    private String address;
    private String status;
    private byte[] passwordSalt;
    private byte[] passwordHash;
    private String passwordHashStr;
    private String role;
    private boolean active;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String v) {
        this.username = v;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        this.email = v;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String v) {
        this.phone = v;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String v) {
        this.fullName = v;
    }
    
    public String getAddress() {
        return address;
    }

    public void setAddress(String v) {
        this.address = v;
    }    

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        this.status = v;
    }

    public byte[] getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(byte[] v) {
        this.passwordSalt = v;
    }

    public byte[] getPasswordHash() {
        return passwordHash;
    }
    
    public String getPasswordHashStr() {
        String passwordHashStr = new String(getPasswordHash(), StandardCharsets.UTF_8);
        return passwordHashStr;
    }

    public void setPasswordHash(byte[] v) {
        this.passwordHash = v;
    }
    
    public void setPasswordHashStr(String v) {
        this.passwordHashStr = v;
    }
    public String getRole(){return role;} public void setRole(String r){this.role=r;}
    public boolean isActive(){return active;} public void setActive(boolean a){this.active=a;}
}
