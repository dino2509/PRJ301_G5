package com.smartshop.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class ElFns {
    private static final Locale VI = new Locale("vi","VN");
    private ElFns(){}

    public static String vnd(BigDecimal v) {
        if (v == null) return "0 ₫";
        NumberFormat nf = NumberFormat.getCurrencyInstance(VI);
        return nf.format(v);
    }

    public static String discountPercent(BigDecimal original, BigDecimal sale) {
        if (original == null || sale == null || original.signum() <= 0) return "0%";
        BigDecimal off = original.subtract(sale);
        if (off.signum() <= 0) return "0%";
        BigDecimal pct = off.multiply(BigDecimal.valueOf(100)).divide(original, 0, BigDecimal.ROUND_HALF_UP);
        return pct.toPlainString() + "%";
    }

    // ISO-8601 string cho JS (đếm ngược)
    public static String toIso(Object ts) {
        if (ts == null) return "";
        if (ts instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) ts).toInstant().toString();
        }
        if (ts instanceof java.util.Date) {
            return ((java.util.Date) ts).toInstant().toString();
        }
        if (ts instanceof java.time.temporal.TemporalAccessor) {
            return java.time.format.DateTimeFormatter.ISO_INSTANT
                   .format(Instant.from((java.time.temporal.TemporalAccessor) ts));
        }
        return ts.toString();
    }

    // Chuỗi thời gian còn lại dạng "X ngày HH:MM:SS" (server-side fallback)
    public static String remaining(java.util.Date end) {
        if (end == null) return "";
        Instant now = Instant.now();
        Instant e = end.toInstant();
        if (!e.isAfter(now)) return "Hết hạn";
        long sec = ChronoUnit.SECONDS.between(now, e);
        long d = sec / 86400; sec %= 86400;
        long h = sec / 3600;  sec %= 3600;
        long m = sec / 60;    sec %= 60;
        return String.format("%02d ngày %02d:%02d:%02d", d, h, m, sec);
    }
}
