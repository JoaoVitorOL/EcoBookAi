package com.ecobook.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

public final class TestDatabaseConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestDatabaseConfig.class);
    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:15-alpine");
    private static final String TEST_DB_MODE = envOrDefault("ECOBOOK_TEST_DB_MODE", "auto").toLowerCase(Locale.ROOT);
    private static final String EXTERNAL_HOST = envOrDefault("ECOBOOK_TEST_DB_HOST", "localhost");
    private static final String EXTERNAL_PORT = envOrDefault("ECOBOOK_TEST_DB_PORT", "5432");
    private static final String EXTERNAL_ADMIN_DB = envOrDefault("ECOBOOK_TEST_ADMIN_DB", "postgres");
    private static final String EXTERNAL_DB_NAME = envOrDefault("ECOBOOK_TEST_DB_NAME", "ecobook_test");
    private static final String EXTERNAL_USERNAME = envOrDefault("ECOBOOK_TEST_DB_USER", "ecobook");
    private static final String EXTERNAL_PASSWORD = envOrDefault("ECOBOOK_TEST_DB_PASSWORD", envOrDefault("DB_PASSWORD", "dev_password_123"));
    private static final String H2_JDBC_URL =
            "jdbc:h2:mem:ecobook-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DEFAULT_NULL_ORDERING=HIGH;"
                    + "INIT=RUNSCRIPT FROM 'classpath:db/test/h2-domains.sql'";

    private static volatile DatabaseProperties databaseProperties;

    private TestDatabaseConfig() {
    }

    public static void registerProperties(DynamicPropertyRegistry registry) {
        DatabaseProperties properties = getDatabaseProperties();
        registry.add("spring.datasource.url", properties::jdbcUrl);
        registry.add("spring.datasource.username", properties::username);
        registry.add("spring.datasource.password", properties::password);
        registry.add("spring.datasource.driver-class-name", properties::driverClassName);
        registry.add("spring.flyway.enabled", properties::flywayEnabled);
        registry.add("spring.jpa.hibernate.ddl-auto", properties::ddlAuto);
        if (properties.databasePlatform() != null) {
            registry.add("spring.jpa.database-platform", properties::databasePlatform);
        }
    }

    private static synchronized DatabaseProperties getDatabaseProperties() {
        if (databaseProperties != null) {
            return databaseProperties;
        }

        databaseProperties = switch (TEST_DB_MODE) {
            case "external" -> initializeExternalPostgres(null);
            case "testcontainers" -> initializeWithTestcontainers();
            case "h2" -> initializeWithH2(null);
            default -> initializeAutomatically();
        };

        return databaseProperties;
    }

    private static DatabaseProperties initializeAutomatically() {
        RuntimeException containerFailure = null;
        try {
            return initializeWithTestcontainers();
        } catch (RuntimeException testcontainersFailure) {
            LOGGER.warn(
                    "Testcontainers indisponivel neste ambiente; usando PostgreSQL externo de teste em {}:{} ({})",
                    EXTERNAL_HOST,
                    EXTERNAL_PORT,
                    testcontainersFailure.getMessage()
            );
            LOGGER.debug("Detalhes da falha do Testcontainers local", testcontainersFailure);
            containerFailure = testcontainersFailure;
        }

        try {
            return initializeExternalPostgres(containerFailure);
        } catch (RuntimeException externalFailure) {
            LOGGER.warn(
                    "PostgreSQL externo de teste indisponivel em {}:{}; usando fallback H2 em memoria ({})",
                    EXTERNAL_HOST,
                    EXTERNAL_PORT,
                    externalFailure.getMessage()
            );
            LOGGER.debug("Detalhes da falha do PostgreSQL externo de teste", externalFailure);
            return initializeWithH2(externalFailure);
        }
    }

    private static DatabaseProperties initializeWithTestcontainers() {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(POSTGRES_IMAGE)
                .withDatabaseName(EXTERNAL_DB_NAME)
                .withUsername(EXTERNAL_USERNAME)
                .withPassword(EXTERNAL_PASSWORD);
        container.start();

        LOGGER.info("Testcontainers PostgreSQL iniciado para a suite de testes.");
        return new DatabaseProperties(
                container.getJdbcUrl(),
                container.getUsername(),
                container.getPassword(),
                container.getDriverClassName(),
                true,
                "validate",
                null
        );
    }

    private static DatabaseProperties initializeExternalPostgres(Throwable testcontainersFailure) {
        String databaseName = requireSafeIdentifier(EXTERNAL_DB_NAME, "ECOBOOK_TEST_DB_NAME");
        String username = requireSafeIdentifier(EXTERNAL_USERNAME, "ECOBOOK_TEST_DB_USER");
        String adminUrl = "jdbc:postgresql://" + EXTERNAL_HOST + ":" + EXTERNAL_PORT + "/" + EXTERNAL_ADMIN_DB;
        String jdbcUrl = "jdbc:postgresql://" + EXTERNAL_HOST + ":" + EXTERNAL_PORT + "/" + databaseName;

        try {
            recreateDatabase(adminUrl, databaseName, username, EXTERNAL_PASSWORD);
            LOGGER.info("Banco de teste externo {} preparado em {}:{}.", databaseName, EXTERNAL_HOST, EXTERNAL_PORT);
            return new DatabaseProperties(
                    jdbcUrl,
                    username,
                    EXTERNAL_PASSWORD,
                    "org.postgresql.Driver",
                    true,
                    "validate",
                    null
            );
        } catch (SQLException sqlException) {
            String message = "Nao foi possivel preparar o banco externo de testes em "
                    + EXTERNAL_HOST + ":" + EXTERNAL_PORT
                    + ". Rode `docker compose up -d postgres` na raiz do repositorio ou corrija as variaveis ECOBOOK_TEST_DB_*.";
            if (testcontainersFailure != null) {
                IllegalStateException failure = new IllegalStateException(message, sqlException);
                failure.addSuppressed(testcontainersFailure);
                throw failure;
            }
            throw new IllegalStateException(message, sqlException);
        }
    }

    private static DatabaseProperties initializeWithH2(Throwable previousFailure) {
        if (previousFailure == null) {
            LOGGER.info("Usando banco H2 em memoria para a suite de testes.");
        } else {
            LOGGER.warn("Usando banco H2 em memoria para a suite de testes como fallback final.");
            LOGGER.debug("Motivo do fallback final para H2", previousFailure);
        }

        return new DatabaseProperties(
                H2_JDBC_URL,
                "sa",
                "",
                "org.h2.Driver",
                false,
                "create-drop",
                "com.ecobook.config.LocalH2Dialect"
        );
    }

    private static void recreateDatabase(String adminUrl,
                                         String databaseName,
                                         String username,
                                         String password) throws SQLException {
        try (Connection connection = DriverManager.getConnection(adminUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" + databaseName + "' AND pid <> pg_backend_pid()");
            statement.execute("DROP DATABASE IF EXISTS \"" + databaseName + "\"");
            statement.execute("CREATE DATABASE \"" + databaseName + "\" OWNER \"" + username + "\"");
        }
    }

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String requireSafeIdentifier(String value, String key) {
        if (value.matches("[A-Za-z0-9_]+")) {
            return value;
        }
        throw new IllegalStateException(key + " deve conter apenas letras, numeros e underscore.");
    }

    private record DatabaseProperties(String jdbcUrl,
                                      String username,
                                      String password,
                                      String driverClassName,
                                      boolean flywayEnabled,
                                      String ddlAuto,
                                      String databasePlatform) {
    }
}
