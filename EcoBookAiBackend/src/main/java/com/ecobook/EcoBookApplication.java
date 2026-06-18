package com.ecobook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * EcoBook IA Backend Application
 * AI-Powered Material Donation Matching Platform
 */
@SpringBootApplication
@EnableScheduling
public class EcoBookApplication {

    /**
     * Starts the EcoBook backend application.
     * @param args application startup arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(EcoBookApplication.class, args);
    }

}
