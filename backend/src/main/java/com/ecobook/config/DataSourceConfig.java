package com.ecobook.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

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
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(20000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1200000);
        config.setAutoCommit(true);
        config.setConnectionTestQuery("SELECT 1");
        config.setLeakDetectionThreshold(60000);

        return new HikariDataSource(config);
    }
}
