package com.pawhome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PawHome backend entry point.
 *
 * Run with:  mvn spring-boot:run
 * Then open: http://localhost:5000
 *
 * On first run, the app seeds sample pets + an admin account
 * (see util.DataSeeder). Admin login: admin / Admin@123
 */
@SpringBootApplication
public class PawhomeApplication {
    public static void main(String[] args) {
        SpringApplication.run(PawhomeApplication.class, args);
    }
}
