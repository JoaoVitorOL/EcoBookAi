package com.ecobook.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Database connection pooling configuration using HikariCP
 */
@Configuration
public class DataSourceConfig {

    /**
     * Configure HikariCP connection pool with optimized settings
     * - Maximum pool size: 20 connections
     * - Minimum idle: 5 connections
     * - Connection timeout: 20 seconds
     * - Idle timeout: 5 minutes
     * - Max lifetime: 20 minutes
     * - Validation query: SELECT 1
     *
     * @return Configured HikariDataSource
     */
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }
}
