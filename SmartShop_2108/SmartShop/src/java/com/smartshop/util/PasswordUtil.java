package com.smartshop.util;

import java.security.MessageDigest;
import java.security.SecureRandom;

public class PasswordUtil {
    private static final SecureRandom RNG = new SecureRandom();

    public static byte[] newSalt() {
        byte[] s = new byte[16];
        RNG.nextBytes(s);
        return s;
    }

    public static byte[] sha256(byte[] salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            md.update(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return md.digest();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static boolean matches(byte[] salt, byte[] hash, String raw) {
        byte[] h = sha256(salt, raw);
        if (h.length != hash.length) return false;
        int res = 0; for (int i=0;i<h.length;i++) res |= h[i]^hash[i];
        return res == 0;
    }
}
