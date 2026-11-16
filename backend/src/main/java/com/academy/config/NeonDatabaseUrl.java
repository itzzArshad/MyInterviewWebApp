package com.academy.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts a libpq-style URL ({@code postgresql://user:pass@host/db?sslmode=require}) — the format Neon,
 * Render and most hosts hand out — into what the JDBC driver needs: a {@code jdbc:postgresql://} URL
 * plus separate username and password.
 */
public record NeonDatabaseUrl(String jdbcUrl, String username, String password, boolean pooled) {

    public static boolean isLibpqUrl(String value) {
        String v = unquote(value);
        return v != null && (v.startsWith("postgresql://") || v.startsWith("postgres://"));
    }

    public static NeonDatabaseUrl parse(String value) {
        String v = unquote(value);
        URI uri = URI.create(v);
        if (uri.getHost() == null) throw new IllegalArgumentException("Database URL has no host");
        String userInfo = uri.getRawUserInfo();
        String user = null, pass = null;
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            user = decode(colon < 0 ? userInfo : userInfo.substring(0, colon));
            pass = colon < 0 ? null : decode(userInfo.substring(colon + 1));
        }
        boolean pooled = uri.getHost().contains("-pooler");

        List<String> params = new ArrayList<>();
        if (uri.getRawQuery() != null) {
            for (String p : uri.getRawQuery().split("&")) {
                if (p.isBlank()) continue;
                String key = p.contains("=") ? p.substring(0, p.indexOf('=')) : p;
                String val = p.contains("=") ? p.substring(p.indexOf('=') + 1) : "";
                // libpq spells it channel_binding; pgjdbc calls the same option channelBinding
                params.add(("channel_binding".equals(key) ? "channelBinding" : key) + "=" + val);
            }
        }
        if (params.stream().noneMatch(p -> p.startsWith("sslmode="))) params.add("sslmode=require");
        // PgBouncer (transaction pooling) cannot keep server-side prepared statements between transactions.
        if (pooled && params.stream().noneMatch(p -> p.startsWith("prepareThreshold="))) params.add("prepareThreshold=0");

        String port = uri.getPort() > 0 ? ":" + uri.getPort() : "";
        String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/postgres" : uri.getRawPath();
        String jdbc = "jdbc:postgresql://" + uri.getHost() + port + path + "?" + String.join("&", params);
        return new NeonDatabaseUrl(jdbc, user, pass, pooled);
    }

    /** Neon's direct endpoint is the pooled hostname without "-pooler". */
    public NeonDatabaseUrl direct() {
        if (!pooled) return this;
        String url = jdbcUrl.replaceFirst("-pooler\\.", ".").replace("&prepareThreshold=0", "").replace("?prepareThreshold=0&", "?");
        return new NeonDatabaseUrl(url, username, password, false);
    }

    static String unquote(String v) {
        if (v == null) return null;
        String s = v.strip();
        if (s.length() >= 2 && (s.startsWith("\"") && s.endsWith("\"") || s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return "NeonDatabaseUrl[pooled=" + pooled + "]"; // never log credentials
    }
}
