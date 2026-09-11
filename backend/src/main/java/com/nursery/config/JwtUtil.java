package com.nursery.config;

import com.nursery.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(@Value("${app.jwt-secret}") String secret,
                   @Value("${app.jwt-expire-hours}") long expireHours) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        // HS256 要求密钥至少 32 字节，不足则循环填充
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            for (int i = 0; i < 32; i++) padded[i] = bytes[i % bytes.length];
            bytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expireMillis = expireHours * 3600_000L;
    }

    public String generate(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("name", user.getName())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    public AuthUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        Long uid = claims.get("uid", Number.class).longValue();
        String name = claims.get("name", String.class);
        String role = claims.get("role", String.class);
        return new AuthUser(uid, claims.getSubject(), name, User.Role.valueOf(role));
    }
}
