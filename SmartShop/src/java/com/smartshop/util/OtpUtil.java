package com.smartshop.util;

import java.security.SecureRandom;

public final class OtpUtil {
    private static final SecureRandom RND = new SecureRandom();
    private OtpUtil(){}
    public static String numeric6(){
        int n = RND.nextInt(1_000_000); // 0..999999
        return String.format("%06d", n);
    }
}
