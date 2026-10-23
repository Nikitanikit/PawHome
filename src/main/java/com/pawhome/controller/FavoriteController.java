package com.pawhome.controller;

import com.pawhome.security.ApiException;
import com.pawhome.security.AuthGuard;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final JdbcTemplate jdbc;
    private final AuthGuard authGuard;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FavoriteController(JdbcTemplate jdbc, AuthGuard authGuard) {
        this.jdbc = jdbc;
        this.authGuard = authGuard;
    }

    @GetMapping
    public Map<String, Object> list(HttpServletRequest request) {
        Claims claims = authGuard.requireUser(request);
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT p.* FROM favorites f JOIN pets p ON p.id = f.pet_id
                WHERE f.user_id = ? ORDER BY f.created_at DESC
                """, authGuard.userId(claims));
        return Map.of("favorites", rows);
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Claims claims = authGuard.requireUser(request);
        long petId = Long.parseLong(String.valueOf(body.get("pet_id")));

        List<Map<String, Object>> pet = jdbc.queryForList("SELECT id FROM pets WHERE id=?", petId);
        if (pet.isEmpty()) {
            throw new ApiException(404, "This pet could not be found.");
        }
        try {
            jdbc.update("INSERT INTO favorites (user_id, pet_id, created_at) VALUES (?,?,?)",
                    authGuard.userId(claims), petId, LocalDateTime.now().format(TS));
        } catch (Exception ignored) {
            // already a favorite - idempotent, same as the Python version
        }
        return Map.of("message", "Added to favorites.");
    }

    @DeleteMapping("/{petId}")
    public Map<String, Object> remove(@PathVariable long petId, HttpServletRequest request) {
        Claims claims = authGuard.requireUser(request);
        jdbc.update("DELETE FROM favorites WHERE user_id=? AND pet_id=?", authGuard.userId(claims), petId);
        return Map.of("message", "Removed from favorites.");
    }
}
