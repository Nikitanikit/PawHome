package com.pawhome.controller;

import com.pawhome.security.ApiException;
import com.pawhome.security.AuthGuard;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final JdbcTemplate jdbc;
    private final AuthGuard authGuard;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> REQUIRED = List.of("pet_id", "full_name", "email", "phone", "address",
            "housing_type", "previous_pet_experience", "reason_for_adoption", "family_members", "employment_info",
            "agreement");

    public ApplicationController(JdbcTemplate jdbc, AuthGuard authGuard) {
        this.jdbc = jdbc;
        this.authGuard = authGuard;
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Claims claims = authGuard.requireUser(request);
        long userId = authGuard.userId(claims);

        List<String> missing = new ArrayList<>();
        for (String f : REQUIRED) {
            Object v = body.get(f);
            if (v == null || String.valueOf(v).isBlank()) missing.add(f);
        }
        if (!missing.isEmpty()) {
            throw new ApiException(400, "Please complete: " + String.join(", ", missing));
        }
        boolean agreement = Boolean.TRUE.equals(body.get("agreement")) || "true".equals(String.valueOf(body.get("agreement")));
        if (!agreement) {
            throw new ApiException(400, "Please accept the adoption agreement to continue.");
        }

        long petId = Long.parseLong(String.valueOf(body.get("pet_id")));
        List<Map<String, Object>> petRows = jdbc.queryForList("SELECT * FROM pets WHERE id=?", petId);
        if (petRows.isEmpty()) {
            throw new ApiException(404, "This pet could not be found.");
        }
        Map<String, Object> pet = petRows.get(0);
        if ("adopted".equals(pet.get("status"))) {
            throw new ApiException(409, pet.get("name") + " has already been adopted.");
        }

        List<Map<String, Object>> existing = jdbc.queryForList(
                "SELECT id FROM adoption_applications WHERE user_id=? AND pet_id=? AND status='pending'",
                userId, petId);
        if (!existing.isEmpty()) {
            throw new ApiException(409, "You already have a pending application for this pet.");
        }

        List<Map<String, Object>> paymentRows = jdbc.queryForList(
                "SELECT id FROM payments WHERE user_id=? AND pet_id=? ORDER BY created_at DESC LIMIT 1", userId, petId);
        Long paymentId = paymentRows.isEmpty() ? null : ((Number) paymentRows.get(0).get("id")).longValue();

        String now = LocalDateTime.now().format(TS);
        jdbc.update("""
            INSERT INTO adoption_applications
                (user_id, pet_id, payment_id, full_name, email, phone, address, housing_type,
                 previous_pet_experience, reason_for_adoption, family_members, employment_info,
                 agreement, status, applied_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,1,'pending',?)
            """,
            userId, petId, paymentId, body.get("full_name"), body.get("email"), body.get("phone"),
            body.get("address"), body.get("housing_type"), body.get("previous_pet_experience"),
            body.get("reason_for_adoption"), Integer.parseInt(String.valueOf(body.get("family_members"))),
            body.get("employment_info"), now);

        Map<String, Object> application = jdbc.queryForList(
                "SELECT * FROM adoption_applications ORDER BY id DESC LIMIT 1").get(0);
        return Map.of("message", "Your adoption application has been submitted successfully.",
                "application", application);
    }

    @GetMapping("/mine")
    public Map<String, Object> mine(HttpServletRequest request) {
        Claims claims = authGuard.requireUser(request);
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT a.*, p.name AS pet_name, p.image_url AS pet_image, p.species AS pet_species
            FROM adoption_applications a JOIN pets p ON p.id = a.pet_id
            WHERE a.user_id = ? ORDER BY a.applied_at DESC
            """, authGuard.userId(claims));
        return Map.of("applications", rows);
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String status, HttpServletRequest request) {
        authGuard.requireAdmin(request);
        StringBuilder sql = new StringBuilder("""
            SELECT a.*, p.name AS pet_name, p.image_url AS pet_image, p.species AS pet_species,
                   u.email AS user_email
            FROM adoption_applications a
            JOIN pets p ON p.id = a.pet_id
            JOIN users u ON u.id = a.user_id
            WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql.append(" AND a.status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY a.applied_at DESC");
        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());
        return Map.of("applications", rows);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOne(@PathVariable long id, HttpServletRequest request) {
        Claims claims = authGuard.requireAny(request);
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT a.*, p.name AS pet_name, p.image_url AS pet_image
            FROM adoption_applications a JOIN pets p ON p.id = a.pet_id WHERE a.id=?
            """, id);
        if (rows.isEmpty()) {
            throw new ApiException(404, "Application not found.");
        }
        Map<String, Object> row = rows.get(0);
        if ("user".equals(authGuard.role(claims)) && ((Number) row.get("user_id")).longValue() != authGuard.userId(claims)) {
            throw new ApiException(403, "You don't have permission to view this application.");
        }
        return Map.of("application", row);
    }

    @PutMapping("/{id}/approve")
    public Map<String, Object> approve(@PathVariable long id, HttpServletRequest request) {
        authGuard.requireAdmin(request);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM adoption_applications WHERE id=?", id);
        if (rows.isEmpty()) {
            throw new ApiException(404, "Application not found.");
        }
        Map<String, Object> application = rows.get(0);
        if (!"pending".equals(application.get("status"))) {
            throw new ApiException(409, "This application has already been reviewed.");
        }

        String now = LocalDateTime.now().format(TS);
        jdbc.update("UPDATE adoption_applications SET status='approved', reviewed_at=? WHERE id=?", now, id);
        // The pet is officially adopted: lock the listing so nobody else can apply.
        jdbc.update("UPDATE pets SET status='adopted' WHERE id=?", application.get("pet_id"));
        // Any other pending applications for the same pet are now moot.
        jdbc.update("""
            UPDATE adoption_applications SET status='rejected', reviewed_at=?,
                admin_note='Pet was adopted by another applicant.'
            WHERE pet_id=? AND status='pending' AND id != ?
            """, now, application.get("pet_id"), id);

        return Map.of("message", "Application approved. The pet is now marked as adopted.");
    }

    @PutMapping("/{id}/reject")
    public Map<String, Object> reject(@PathVariable long id, @RequestBody(required = false) Map<String, Object> body,
                                       HttpServletRequest request) {
        authGuard.requireAdmin(request);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM adoption_applications WHERE id=?", id);
        if (rows.isEmpty()) {
            throw new ApiException(404, "Application not found.");
        }
        Map<String, Object> application = rows.get(0);
        if (!"pending".equals(application.get("status"))) {
            throw new ApiException(409, "This application has already been reviewed.");
        }
        String note = (body != null && body.get("admin_note") != null) ? String.valueOf(body.get("admin_note")) : "";
        String now = LocalDateTime.now().format(TS);
        jdbc.update("UPDATE adoption_applications SET status='rejected', reviewed_at=?, admin_note=? WHERE id=?",
                now, note, id);

        // Free the pet back up if nothing else has claimed it.
        List<Map<String, Object>> stillPending = jdbc.queryForList(
                "SELECT id FROM adoption_applications WHERE pet_id=? AND status='pending'", application.get("pet_id"));
        if (stillPending.isEmpty()) {
            jdbc.update("UPDATE pets SET status='available' WHERE id=? AND status != 'adopted'", application.get("pet_id"));
        }
        return Map.of("message", "Application rejected.");
    }
}
