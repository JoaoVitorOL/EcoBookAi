package com.ecobook.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration
public class TestDatabaseConfig {

    @Bean
    public PostgreSQLContainer<?> postgreSQLContainer() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("ecobook_test")
                .withUsername("ecobook_test")
                .withPassword("test_password");

        container.start();

        // Set system properties for Spring to use
        System.setProperty("spring.datasource.url", container.getJdbcUrl());
        System.setProperty("spring.datasource.username", container.getUsername());
        System.setProperty("spring.datasource.password", container.getPassword());

        return container;
    }
}
