package com.pawhome.util;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mirrors backend/seed.py: on a completely empty database, inserts the
 * admin account and a small shelter's worth of sample pets so the site
 * looks real out of the box. Safe to restart - only runs when the pets
 * table is empty, so it never duplicates data.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DataSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM pets", Integer.class);
        if (count != null && count > 0) {
            return; // already seeded
        }
        String now = LocalDateTime.now().format(TS);

        jdbc.update("INSERT INTO admins (username, password_hash, full_name, created_at) VALUES (?,?,?,?)",
                "admin", BCrypt.hashpw("Admin@123", BCrypt.gensalt()), "Shelter Administrator", now);

        String insertPet = """
            INSERT INTO pets (name, species, breed, age_years, gender, color, location,
                vaccination_status, health_status, description, personality,
                adoption_requirements, adoption_fee, image_url, status, created_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?, 'available', ?)
            """;

        Object[][] pets = {
            {"Buddy", "Dog", "Labrador Retriever", 2.0, "Male", "Golden", "Mumbai, MH",
             "Fully Vaccinated", "Healthy",
             "Buddy is a joyful Labrador who greets everyone like an old friend. He loves fetch, long walks, and dozing in a sunny spot afterwards.",
             "Friendly, playful, great with kids", "Fenced yard preferred; needs daily exercise", 1500.0,
             "https://commons.wikimedia.org/wiki/Special:FilePath/Yellow_Labrador.jpg?width=800"},
            {"Luna", "Cat", "Domestic Shorthair", 1.0, "Female", "Black & White", "Pune, MH",
             "Fully Vaccinated", "Healthy",
             "Luna is a curious, quiet cat who loves warm windowsills and gentle chin scratches. She purrs the moment you sit down.",
             "Calm, independent, affectionate on her terms", "Indoor home, no other dominant cats", 800.0,
             "https://commons.wikimedia.org/wiki/Special:FilePath/Domestic_cat.jpg?width=800"},
            {"Rocky", "Dog", "Indian Pariah (Desi)", 3.0, "Male", "Brown", "Nagpur, MH",
             "Fully Vaccinated", "Healthy",
             "Rocky was rescued as a stray and has grown into a loyal, watchful companion. Calm indoors but alert outside.",
             "Loyal, alert, good guard instincts", "Experienced owner preferred", 1000.0,
             "https://commons.wikimedia.org/wiki/Special:FilePath/Yellow_Labrador.jpg?width=800"},
            {"Mochi", "Cat", "Persian", 2.0, "Female", "Cream", "Thane, MH",
             "Partially Vaccinated", "Healthy",
             "Mochi is a fluffy Persian who loves being brushed and napping in laundry baskets.",
             "Gentle, sleepy, loves grooming sessions", "Regular grooming needed; indoor only", 2000.0,
             "https://commons.wikimedia.org/wiki/Special:FilePath/Female_cat_sitting_down_on_sofa.jpg?width=800"},
            {"Simba", "Dog", "Golden Retriever", 1.0, "Male", "Golden", "Mumbai, MH",
             "Fully Vaccinated", "Healthy",
             "Simba is a bouncy puppy who is still learning the rules but is eager to please.",
             "Energetic, eager to learn, very social", "Active family, puppy-proofed home", 2500.0,
             "https://commons.wikimedia.org/wiki/Special:FilePath/Golden_Retriever_Puppy.JPG?width=800"},
            {"Coco", "Rabbit", "Dutch Rabbit", 1.0, "Female", "Brown & White", "Pune, MH",
             "Not Applicable", "Healthy",
             "Coco hops around investigating everything and loves fresh coriander as a treat. Litter-trained.",
             "Curious, gentle, litter-trained", "Enclosure/hutch and rabbit-safe space", 600.0,
             "https://commons.wikimedia.org/wiki/Special:FilePath/Dobby_a_pet_rabbit.JPG?width=800"},
            {"Max", "Dog", "Beagle", 4.0, "Male", "Tricolor", "Nashik, MH",
             "Fully Vaccinated", "Under Treatment",
             "Max is a gentle senior Beagle finishing a course of medication for a minor skin condition.",
             "Calm, food-motivated, great with other dogs", "Patience for ongoing vet follow-up", 500.0,
             "https://commons.wikimedia.org/wiki/Special:FilePath/Yellow_Labrador.jpg?width=800"},
            {"Whiskers", "Cat", "Domestic Shorthair", 5.0, "Male", "Grey Tabby", "Mumbai, MH",
             "Fully Vaccinated", "Healthy",
             "Whiskers is a laid-back senior cat who asks for little more than a warm lap.",
             "Easy-going, low energy, very affectionate", "Quiet household ideal", 400.0,
             "https://commons.wikimedia.org/wiki/Special:FilePath/Domestic_cat.jpg?width=800"},
            {"Bruno", "Dog", "Rottweiler Mix", 3.0, "Male", "Black & Tan", "Pune, MH",
             "Fully Vaccinated", "Healthy",
             "Bruno looks intimidating but is a certified softie who loves belly rubs. Basic obedience trained.",
             "Confident, trained, affectionate with family", "Experienced large-breed owner, secure fencing", 1200.0,
             "https://commons.wikimedia.org/wiki/Special:FilePath/Golden_Retriever_Puppy.JPG?width=800"},
            {"Kiwi", "Bird", "Indian Ringneck Parrot", 2.0, "Male", "Green", "Thane, MH",
             "Not Applicable", "Healthy",
             "Kiwi is a chatty ringneck learning new words every week and enjoys millet spray as a reward.",
             "Chatty, intelligent, bonds closely with one person", "Spacious cage, daily out-of-cage time", 1800.0,
             "https://commons.wikimedia.org/wiki/Special:FilePath/Green-irn.jpg?width=800"},
            {"Daisy", "Dog", "Shih Tzu", 6.0, "Female", "White & Brown", "Nagpur, MH",
             "Fully Vaccinated", "Special Needs",
             "Daisy has mild hip stiffness common in senior small breeds but still enjoys short strolls.",
             "Sweet, cuddly, low exercise needs", "Someone home most of the day, no stairs ideally", 300.0,
             "https://commons.wikimedia.org/wiki/Special:FilePath/Yellow_Labrador.jpg?width=800"},
            {"Milo", "Cat", "Bombay Cat", 1.0, "Male", "Black", "Mumbai, MH",
             "Fully Vaccinated", "Healthy",
             "Milo is a sleek, playful kitten who turns any string or cap into a toy. Litter-trained.",
             "Playful, social, mischievous", "Kitten-proofed home", 900.0,
             "https://commons.wikimedia.org/wiki/Special:FilePath/Female_cat_sitting_down_on_sofa.jpg?width=800"},
        };

        for (Object[] p : pets) {
            jdbc.update(insertPet, p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10], p[11], p[12], p[13], now);
        }

        System.out.println("Seeded " + pets.length + " pets and 1 admin account (username: admin / password: Admin@123)");
    }
}
