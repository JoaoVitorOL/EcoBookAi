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

    public static void main(String[] args) {
        SpringApplication.run(EcoBookApplication.class, args);
    }

}
