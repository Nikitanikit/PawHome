package com.pawhome.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Mirrors the @auth_required decorator from the Python version.
 * Call requireUser() / requireAdmin() / requireAny() at the top of a
 * controller method that needs a logged-in user, admin, or either.
 */
@Component
public class AuthGuard {

    private final JwtUtil jwtUtil;

    public AuthGuard(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    private Claims parseOrThrow(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ApiException(401, "Authentication required. Please log in.");
        }
        Claims claims = jwtUtil.parse(header.substring(7));
        if (claims == null) {
            throw new ApiException(401, "Session expired. Please log in again.");
        }
        return claims;
    }

    public Claims requireUser(HttpServletRequest request) {
        Claims claims = parseOrThrow(request);
        if (!"user".equals(jwtUtil.getRole(claims))) {
            throw new ApiException(403, "You don't have permission to do that.");
        }
        return claims;
    }

    public Claims requireAdmin(HttpServletRequest request) {
        Claims claims = parseOrThrow(request);
        if (!"admin".equals(jwtUtil.getRole(claims))) {
            throw new ApiException(403, "You don't have permission to do that.");
        }
        return claims;
    }

    /** Either a logged-in user or an admin - used for viewing a single application. */
    public Claims requireAny(HttpServletRequest request) {
        return parseOrThrow(request);
    }

    public long userId(Claims claims) {
        return jwtUtil.getSubjectId(claims);
    }

    public String role(Claims claims) {
        return jwtUtil.getRole(claims);
    }
}
