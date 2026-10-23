package com.pawhome.controller;

import com.pawhome.security.ApiException;
import com.pawhome.security.AuthGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final JdbcTemplate jdbc;
    private final AuthGuard authGuard;

    public AdminController(JdbcTemplate jdbc, AuthGuard authGuard) {
        this.jdbc = jdbc;
        this.authGuard = authGuard;
    }

    private long count(String sql, Object... params) {
        Long v = jdbc.queryForObject(sql, Long.class, params);
        return v == null ? 0 : v;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(HttpServletRequest request) {
        authGuard.requireAdmin(request);
        Double revenueRaw = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount),0) FROM payments WHERE status='success'", Double.class);
        double revenue = revenueRaw == null ? 0.0 : revenueRaw;

        return Map.of(
                "total_pets", count("SELECT COUNT(*) FROM pets"),
                "available", count("SELECT COUNT(*) FROM pets WHERE status='available'"),
                "pending_pets", count("SELECT COUNT(*) FROM pets WHERE status='pending'"),
                "adopted", count("SELECT COUNT(*) FROM pets WHERE status='adopted'"),
                "total_users", count("SELECT COUNT(*) FROM users"),
                "pending_apps", count("SELECT COUNT(*) FROM adoption_applications WHERE status='pending'"),
                "revenue", revenue
        );
    }

    @GetMapping("/users")
    public Map<String, Object> listUsers(HttpServletRequest request) {
        authGuard.requireAdmin(request);
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT id, full_name, email, phone, address, created_at,
                   (SELECT COUNT(*) FROM adoption_applications WHERE user_id = users.id) AS application_count
            FROM users ORDER BY created_at DESC
            """);
        return Map.of("users", rows);
    }

    @DeleteMapping("/users/{id}")
    public Map<String, Object> deleteUser(@PathVariable long id, HttpServletRequest request) {
        authGuard.requireAdmin(request);
        List<Map<String, Object>> existing = jdbc.queryForList("SELECT id FROM users WHERE id=?", id);
        if (existing.isEmpty()) {
            throw new ApiException(404, "User not found.");
        }
        jdbc.update("DELETE FROM users WHERE id=?", id);
        return Map.of("message", "User account removed.");
    }

    @GetMapping("/reports")
    public Map<String, Object> reports(HttpServletRequest request) {
        authGuard.requireAdmin(request);

        List<Map<String, Object>> bySpecies = jdbc.queryForList(
                "SELECT species, COUNT(*) AS total FROM pets GROUP BY species");
        List<Map<String, Object>> byStatus = jdbc.queryForList(
                "SELECT status, COUNT(*) AS total FROM pets GROUP BY status");
        List<Map<String, Object>> appsByStatus = jdbc.queryForList(
                "SELECT status, COUNT(*) AS total FROM adoption_applications GROUP BY status");
        // H2 equivalent of SQLite's strftime('%Y-%m', ...): substring the "yyyy-MM-dd HH:mm:ss" string
        List<Map<String, Object>> appsByMonth = jdbc.queryForList("""
            SELECT SUBSTRING(applied_at, 1, 7) AS month, COUNT(*) AS total
            FROM adoption_applications GROUP BY month ORDER BY month
            """);
        List<Map<String, Object>> revenueByMonth = jdbc.queryForList("""
            SELECT SUBSTRING(created_at, 1, 7) AS month, COALESCE(SUM(amount),0) AS total
            FROM payments WHERE status='success' GROUP BY month ORDER BY month
            """);

        return Map.of(
                "by_species", bySpecies,
                "by_status", byStatus,
                "apps_by_status", appsByStatus,
                "applications_by_month", appsByMonth,
                "revenue_by_month", revenueByMonth
        );
    }
}
