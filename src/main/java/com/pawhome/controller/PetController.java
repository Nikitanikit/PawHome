package com.pawhome.controller;

import com.pawhome.security.ApiException;
import com.pawhome.security.AuthGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    private final JdbcTemplate jdbc;
    private final AuthGuard authGuard;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> UPDATABLE_FIELDS = List.of(
            "name", "species", "breed", "age_years", "gender", "color", "location",
            "vaccination_status", "health_status", "description", "personality",
            "adoption_requirements", "adoption_fee", "image_url", "status");

    public PetController(JdbcTemplate jdbc, AuthGuard authGuard) {
        this.jdbc = jdbc;
        this.authGuard = authGuard;
    }

    @GetMapping
    public Map<String, Object> listPets(@RequestParam(required = false) String search,
                                         @RequestParam(required = false) String species,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String location) {
        StringBuilder sql = new StringBuilder("SELECT * FROM pets WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (name LIKE ? OR breed LIKE ? OR description LIKE ?)");
            String like = "%" + search + "%";
            params.add(like); params.add(like); params.add(like);
        }
        if (species != null && !species.isBlank()) {
            sql.append(" AND species = ?");
            params.add(species);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (location != null && !location.isBlank()) {
            sql.append(" AND location LIKE ?");
            params.add("%" + location + "%");
        }
        sql.append(" ORDER BY created_at DESC");

        List<Map<String, Object>> pets = jdbc.queryForList(sql.toString(), params.toArray());
        return Map.of("pets", pets, "count", pets.size());
    }

    @GetMapping("/meta/species")
    public Map<String, Object> speciesOptions() {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT DISTINCT species FROM pets ORDER BY species");
        List<Object> species = rows.stream().map(r -> r.get("species")).toList();
        return Map.of("species", species);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getPet(@PathVariable long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM pets WHERE id=?", id);
        if (rows.isEmpty()) {
            throw new ApiException(404, "This pet could not be found.");
        }
        return Map.of("pet", rows.get(0));
    }

    @PostMapping
    public Map<String, Object> createPet(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        authGuard.requireAdmin(request);

        List<String> required = List.of("name", "species", "breed", "age_years", "gender", "color", "location",
                "vaccination_status", "health_status", "description", "personality",
                "adoption_requirements", "adoption_fee", "image_url");
        List<String> missing = new ArrayList<>();
        for (String f : required) {
            Object v = body.get(f);
            if (v == null || String.valueOf(v).isBlank()) missing.add(f);
        }
        if (!missing.isEmpty()) {
            throw new ApiException(400, "Missing fields: " + String.join(", ", missing));
        }

        String now = LocalDateTime.now().format(TS);
        jdbc.update("""
            INSERT INTO pets (name, species, breed, age_years, gender, color, location,
                vaccination_status, health_status, description, personality,
                adoption_requirements, adoption_fee, image_url, status, created_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """,
            body.get("name"), body.get("species"), body.get("breed"),
            Double.parseDouble(String.valueOf(body.get("age_years"))), body.get("gender"), body.get("color"),
            body.get("location"), body.get("vaccination_status"), body.get("health_status"),
            body.get("description"), body.get("personality"), body.get("adoption_requirements"),
            Double.parseDouble(String.valueOf(body.get("adoption_fee"))), body.get("image_url"),
            body.getOrDefault("status", "available"), now);

        Map<String, Object> pet = jdbc.queryForList(
                "SELECT * FROM pets ORDER BY id DESC LIMIT 1", new Object[]{}).get(0);
        return Map.of("message", "Pet added successfully.", "pet", pet);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updatePet(@PathVariable long id, @RequestBody Map<String, Object> body,
                                          HttpServletRequest request) {
        authGuard.requireAdmin(request);

        List<Map<String, Object>> existing = jdbc.queryForList("SELECT id FROM pets WHERE id=?", id);
        if (existing.isEmpty()) {
            throw new ApiException(404, "This pet could not be found.");
        }

        List<String> updates = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (String f : UPDATABLE_FIELDS) {
            if (body.containsKey(f)) {
                updates.add(f + " = ?");
                params.add(body.get(f));
            }
        }
        if (updates.isEmpty()) {
            throw new ApiException(400, "No fields to update.");
        }
        params.add(id);
        jdbc.update("UPDATE pets SET " + String.join(", ", updates) + " WHERE id = ?", params.toArray());

        Map<String, Object> pet = jdbc.queryForList("SELECT * FROM pets WHERE id=?", id).get(0);
        return Map.of("message", "Pet updated successfully.", "pet", pet);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deletePet(@PathVariable long id, HttpServletRequest request) {
        authGuard.requireAdmin(request);
        List<Map<String, Object>> existing = jdbc.queryForList("SELECT id FROM pets WHERE id=?", id);
        if (existing.isEmpty()) {
            throw new ApiException(404, "This pet could not be found.");
        }
        jdbc.update("DELETE FROM pets WHERE id=?", id);
        return Map.of("message", "Pet listing deleted.");
    }
}
