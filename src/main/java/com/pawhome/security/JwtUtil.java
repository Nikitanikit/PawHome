package com.pawhome.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Issues and validates the JWTs used for both user and admin sessions.
 * NOTE: the secret is hard-coded for simplicity, same as the Python
 * version - a real deployment would load it from an environment variable.
 */
@Component
public class JwtUtil {

    private static final String SECRET = "pawhome-college-project-secret-key-change-me-0123456789";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes());
    private static final long TTL_MILLIS = 12L * 60 * 60 * 1000; // 12 hours

    public String issueToken(long subjectId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + TTL_MILLIS);
        return Jwts.builder()
                .subject(String.valueOf(subjectId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(KEY)
                .compact();
    }

    /** Returns the parsed claims, or null if the token is missing/invalid/expired. */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(KEY).build()
                    .parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    public long getSubjectId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }
}
