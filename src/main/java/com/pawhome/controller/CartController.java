package com.pawhome.controller;

import com.pawhome.security.ApiException;
import com.pawhome.security.AuthGuard;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class CartController {

    private final JdbcTemplate jdbc;
    private final AuthGuard authGuard;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CartController(JdbcTemplate jdbc, AuthGuard authGuard) {
        this.jdbc = jdbc;
        this.authGuard = authGuard;
    }

    @GetMapping("/api/cart")
    public Map<String, Object> getCart(HttpServletRequest request) {
        Claims claims = authGuard.requireUser(request);
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT c.id AS cart_id, p.* FROM cart_items c JOIN pets p ON p.id = c.pet_id
                WHERE c.user_id = ? ORDER BY c.added_at DESC
                """, authGuard.userId(claims));
        double total = rows.stream().mapToDouble(r -> ((Number) r.get("adoption_fee")).doubleValue()).sum();
        return Map.of("items", rows, "total", total);
    }

    @PostMapping("/api/cart")
    public Map<String, Object> addToCart(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Claims claims = authGuard.requireUser(request);
        long petId = Long.parseLong(String.valueOf(body.get("pet_id")));

        List<Map<String, Object>> petRows = jdbc.queryForList("SELECT * FROM pets WHERE id=?", petId);
        if (petRows.isEmpty()) {
            throw new ApiException(404, "This pet could not be found.");
        }
        Map<String, Object> pet = petRows.get(0);
        if (!"available".equals(pet.get("status"))) {
            throw new ApiException(409, pet.get("name") + " is no longer available for adoption.");
        }
        try {
            jdbc.update("INSERT INTO cart_items (user_id, pet_id, added_at) VALUES (?,?,?)",
                    authGuard.userId(claims), petId, LocalDateTime.now().format(TS));
        } catch (Exception e) {
            throw new ApiException(409, pet.get("name") + " is already in your cart.");
        }
        return Map.of("message", pet.get("name") + " added to your cart.");
    }

    @DeleteMapping("/api/cart/{petId}")
    public Map<String, Object> removeFromCart(@PathVariable long petId, HttpServletRequest request) {
        Claims claims = authGuard.requireUser(request);
        jdbc.update("DELETE FROM cart_items WHERE user_id=? AND pet_id=?", authGuard.userId(claims), petId);
        return Map.of("message", "Removed from cart.");
    }

    /**
     * Mock payment gateway: no real card network is contacted (this is a
     * college project with no external payment provider wired in). We
     * validate the input, then record a 'success' payment so the flow,
     * database relationships and receipts all work end-to-end.
     */
    @PostMapping("/api/checkout")
    public Map<String, Object> checkout(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Claims claims = authGuard.requireUser(request);
        long userId = authGuard.userId(claims);
        String paymentMethod = body.getOrDefault("payment_method", "Card").toString();

        List<Map<String, Object>> cart = jdbc.queryForList(
                """
                SELECT c.id AS cart_id, p.* FROM cart_items c JOIN pets p ON p.id = c.pet_id
                WHERE c.user_id = ?
                """, userId);
        if (cart.isEmpty()) {
            throw new ApiException(400, "Your cart is empty.");
        }

        List<String> unavailable = cart.stream()
                .filter(p -> !"available".equals(p.get("status")))
                .map(p -> String.valueOf(p.get("name")))
                .collect(Collectors.toList());
        if (!unavailable.isEmpty()) {
            throw new ApiException(409, "No longer available: " + String.join(", ", unavailable) + ". Please remove and retry.");
        }

        List<Map<String, Object>> receipts = new ArrayList<>();
        for (Map<String, Object> pet : cart) {
            long petId = ((Number) pet.get("id")).longValue();
            String txnId = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
            double fee = ((Number) pet.get("adoption_fee")).doubleValue();
            String now = LocalDateTime.now().format(TS);

            jdbc.update("INSERT INTO payments (user_id, pet_id, amount, payment_method, transaction_id, status, created_at) VALUES (?,?,?,?,?, 'success', ?)",
                    userId, petId, fee, paymentMethod, txnId, now);
            jdbc.update("UPDATE pets SET status='pending' WHERE id=?", petId);
            jdbc.update("DELETE FROM cart_items WHERE id=?", pet.get("cart_id"));

            Map<String, Object> receipt = new LinkedHashMap<>();
            long paymentId = jdbc.queryForObject("SELECT id FROM payments WHERE transaction_id=?", Long.class, txnId);
            receipt.put("payment_id", paymentId);
            receipt.put("pet_id", petId);
            receipt.put("pet_name", pet.get("name"));
            receipt.put("amount", fee);
            receipt.put("transaction_id", txnId);
            receipts.add(receipt);
        }

        return Map.of("message", "Payment successful. Please complete an adoption application for each pet.",
                "receipts", receipts);
    }
}
