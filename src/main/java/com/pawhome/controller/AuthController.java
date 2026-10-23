package com.pawhome.controller;

import com.pawhome.security.ApiException;
import com.pawhome.security.AuthGuard;
import com.pawhome.security.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JdbcTemplate jdbc;
    private final JwtUtil jwtUtil;
    private final AuthGuard authGuard;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AuthController(JdbcTemplate jdbc, JwtUtil jwtUtil, AuthGuard authGuard) {
        this.jdbc = jdbc;
        this.jwtUtil = jwtUtil;
        this.authGuard = authGuard;
    }

    private String required(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> body) {
        String fullName = required(body, "full_name");
        String email = required(body, "email").toLowerCase();
        String phone = required(body, "phone");
        String address = required(body, "address");
        String password = required(body, "password");
        String confirm = required(body, "confirm_password");

        StringBuilder missing = new StringBuilder();
        if (fullName.isEmpty()) missing.append("full_name, ");
        if (email.isEmpty()) missing.append("email, ");
        if (phone.isEmpty()) missing.append("phone, ");
        if (address.isEmpty()) missing.append("address, ");
        if (password.isEmpty()) missing.append("password, ");
        if (confirm.isEmpty()) missing.append("confirm_password, ");
        if (missing.length() > 0) {
            throw new ApiException(400, "Please fill in: " + missing.substring(0, missing.length() - 2));
        }
        if (!password.equals(confirm)) {
            throw new ApiException(400, "Passwords do not match.");
        }
        if (password.length() < 6) {
            throw new ApiException(400, "Password must be at least 6 characters.");
        }
        if (!email.contains("@")) {
            throw new ApiException(400, "Please enter a valid email address.");
        }

        List<Map<String, Object>> existing = jdbc.queryForList("SELECT id FROM users WHERE email = ?", email);
        if (!existing.isEmpty()) {
            throw new ApiException(409, "An account with this email already exists.");
        }

        String now = LocalDateTime.now().format(TS);
        jdbc.update("INSERT INTO users (full_name, email, phone, address, password_hash, created_at) VALUES (?,?,?,?,?,?)",
                fullName, email, phone, address, BCrypt.hashpw(password, BCrypt.gensalt()), now);

        Map<String, Object> user = jdbc.queryForMap(
                "SELECT id, full_name, email, phone, address FROM users WHERE email = ?", email);
        long userId = ((Number) user.get("id")).longValue();
        String token = jwtUtil.issueToken(userId, "user");

        return Map.of("message", "Account created successfully.", "token", token, "user", user);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        String email = required(body, "email").toLowerCase();
        String password = required(body, "password");

        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM users WHERE email = ?", email);
        if (rows.isEmpty() || !BCrypt.checkpw(password, (String) rows.get(0).get("password_hash"))) {
            throw new ApiException(401, "Incorrect email or password.");
        }
        Map<String, Object> user = rows.get(0);
        user.remove("password_hash");
        long userId = ((Number) user.get("id")).longValue();
        String token = jwtUtil.issueToken(userId, "user");

        return Map.of("message", "Logged in successfully.", "token", token, "user", user);
    }

    @PostMapping("/forgot-password")
    public Map<String, Object> forgotPassword(@RequestBody Map<String, Object> body) {
        String email = required(body, "email").toLowerCase();
        String newPassword = required(body, "new_password");
        if (email.isEmpty() || newPassword.isEmpty()) {
            throw new ApiException(400, "Email and a new password are required.");
        }
        if (newPassword.length() < 6) {
            throw new ApiException(400, "Password must be at least 6 characters.");
        }
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT id FROM users WHERE email=?", email);
        if (rows.isEmpty()) {
            throw new ApiException(404, "No account found with that email.");
        }
        jdbc.update("UPDATE users SET password_hash=? WHERE email=?",
                BCrypt.hashpw(newPassword, BCrypt.gensalt()), email);
        return Map.of("message", "Password updated. You can now log in.");
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        Claims claims = authGuard.requireUser(request);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, full_name, email, phone, address, created_at FROM users WHERE id=?",
                authGuard.userId(claims));
        if (rows.isEmpty()) {
            throw new ApiException(404, "Account not found.");
        }
        return Map.of("user", rows.get(0));
    }

    @PostMapping("/admin/login")
    public Map<String, Object> adminLogin(@RequestBody Map<String, Object> body) {
        String username = required(body, "username");
        String password = required(body, "password");

        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM admins WHERE username=?", username);
        if (rows.isEmpty() || !BCrypt.checkpw(password, (String) rows.get(0).get("password_hash"))) {
            throw new ApiException(401, "Incorrect username or password.");
        }
        Map<String, Object> admin = rows.get(0);
        admin.remove("password_hash");
        long adminId = ((Number) admin.get("id")).longValue();
        String token = jwtUtil.issueToken(adminId, "admin");

        return Map.of("message", "Welcome back.", "token", token, "admin", admin);
    }

    @PutMapping("/admin/settings/password")
    public Map<String, Object> adminChangePassword(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Claims claims = authGuard.requireAdmin(request);
        String current = required(body, "current_password");
        String newPassword = required(body, "new_password");

        Map<String, Object> admin = jdbc.queryForMap("SELECT * FROM admins WHERE id=?", authGuard.userId(claims));
        if (!BCrypt.checkpw(current, (String) admin.get("password_hash"))) {
            throw new ApiException(401, "Current password is incorrect.");
        }
        if (newPassword.length() < 6) {
            throw new ApiException(400, "New password must be at least 6 characters.");
        }
        jdbc.update("UPDATE admins SET password_hash=? WHERE id=?",
                BCrypt.hashpw(newPassword, BCrypt.gensalt()), admin.get("id"));
        return Map.of("message", "Password updated successfully.");
    }
}
