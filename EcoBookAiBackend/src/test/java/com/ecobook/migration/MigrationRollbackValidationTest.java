package com.ecobook.migration;

import com.ecobook.config.TestDatabaseConfig;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MigrationRollbackValidationTest {

    private static final Path MIGRATION_DIR = Path.of("src", "main", "resources", "db", "migration");
    private static final Path ROLLBACK_DIR = Path.of("src", "main", "resources", "db", "rollback");
    private static final List<MigrationScenario> REVERSIBLE_SCENARIOS = List.of(
            new MigrationScenario("V1__initial_schema.sql", RollbackKind.SQL),
            new MigrationScenario("V2__allow_incomplete_usuario_profile.sql", RollbackKind.SQL),
            new MigrationScenario("V3__normalize_material_confidence_type.sql", RollbackKind.SQL),
            new MigrationScenario("V5__extend_upload_tracking_for_phase3.sql", RollbackKind.SQL),
            new MigrationScenario("V6__add_usuario_fcm_token.sql", RollbackKind.SQL),
            new MigrationScenario("V7__add_material_author_and_publisher.sql", RollbackKind.SQL),
            new MigrationScenario("V8__add_phase4_search_indexes.sql", RollbackKind.SQL),
            new MigrationScenario("V9__phase5_request_workflow_support.sql", RollbackKind.SQL),
            new MigrationScenario("V10__add_failed_notification_queue.sql", RollbackKind.SQL),
            new MigrationScenario("V11__add_disciplina_todas.sql", RollbackKind.SQL),
            new MigrationScenario("V13__add_user_notification_inbox.sql", RollbackKind.SQL),
            new MigrationScenario("V14__add_non_receipt_reports.sql", RollbackKind.SQL),
            new MigrationScenario("V15__add_extended_sistema_ensino_values.sql", RollbackKind.SQL),
            new MigrationScenario("V16__align_non_receipt_report_schema_with_runtime.sql", RollbackKind.SQL),
            new MigrationScenario("V17__phase8_lgpd_security_foundation.sql", RollbackKind.SQL),
            new MigrationScenario("V18__add_cpf_and_profile_photo_to_usuario.sql", RollbackKind.SQL)
    );
    private static final List<MigrationScenario> SNAPSHOT_ONLY_SCENARIOS = List.of(
            new MigrationScenario("V4__replace_google_auth_with_local_credentials.sql", RollbackKind.SNAPSHOT_REQUIRED),
            new MigrationScenario("V12__add_material_back_cover_and_align_school_year_rules.sql", RollbackKind.SNAPSHOT_REQUIRED)
    );

    @jakarta.annotation.Resource
    private DataSource dataSource;

    @DynamicPropertySource
    static void configureTestDatabase(DynamicPropertyRegistry registry) {
        TestDatabaseConfig.registerProperties(registry);
    }

    @Test
    @DisplayName("T229 should keep a rollback artifact for every Flyway migration")
    void shouldProvideRollbackArtifactsForEveryMigration() throws IOException {
        List<String> migrations = listSqlFileNames(MIGRATION_DIR);
        List<String> rollbacks = listSqlFileNames(ROLLBACK_DIR);

        assertThat(rollbacks)
                .containsExactlyElementsOf(migrations);
    }

    @Test
    @DisplayName("T229 should rollback and reapply every SQL-backed migration on PostgreSQL")
    void shouldRollbackAndReapplyEverySqlBackedMigration() throws Exception {
        assumeTrue(isPostgreSql(), "Rollback validation requires PostgreSQL enum and schema semantics.");

        for (MigrationScenario scenario : REVERSIBLE_SCENARIOS) {
            String schema = temporarySchemaName(scenario.fileName());
            recreateSchema(schema);
            try {
                migrateToVersion(schema, scenario.version());
                executeRollbackScript(schema, scenario.fileName());
                deleteFlywayHistoryRow(schema, scenario.version());
                migrateToVersion(schema, scenario.version());
                assertThat(hasFlywayHistoryEntry(schema, scenario.version()))
                        .as("Flyway history should contain %s after re-apply", scenario.version())
                        .isTrue();
            } finally {
                dropSchema(schema);
            }
        }
    }

    @Test
    @DisplayName("T229 should require snapshot restore for historically irreversible migrations")
    void shouldRequireSnapshotRestoreForIrreversibleMigrations() throws Exception {
        assumeTrue(isPostgreSql(), "Rollback validation requires PostgreSQL enum and schema semantics.");

        for (MigrationScenario scenario : SNAPSHOT_ONLY_SCENARIOS) {
            String schema = temporarySchemaName(scenario.fileName());
            recreateSchema(schema);
            try {
                migrateToVersion(schema, scenario.version());
                assertThatThrownBy(() -> executeRollbackScript(schema, scenario.fileName()))
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("snapshot");
            } finally {
                dropSchema(schema);
            }
        }
    }

    private void migrateToVersion(String schema, String version) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .defaultSchema(schema)
                .schemas(schema)
                .cleanDisabled(true)
                .target(MigrationVersion.fromVersion(version))
                .load();
        flyway.migrate();
    }

    private void executeRollbackScript(String schema, String fileName) throws IOException, SQLException {
        String script = Files.readString(ROLLBACK_DIR.resolve(fileName), StandardCharsets.UTF_8);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO \"" + schema + "\", public");
            statement.execute(script);
        }
    }

    private void deleteFlywayHistoryRow(String schema, String version) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM \"" + schema + "\".flyway_schema_history WHERE version = '" + version + "'");
        }
    }

    private boolean hasFlywayHistoryEntry(String schema, String version) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) FROM \"" + schema + "\".flyway_schema_history WHERE version = '" + version + "'")) {
            resultSet.next();
            return resultSet.getInt(1) == 1;
        }
    }

    private boolean isPostgreSql() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getURL().startsWith("jdbc:postgresql:");
        }
    }

    private void recreateSchema(String schema) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS \"" + schema + "\" CASCADE");
            statement.execute("CREATE SCHEMA \"" + schema + "\"");
        }
    }

    private void dropSchema(String schema) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS \"" + schema + "\" CASCADE");
        }
    }

    private String temporarySchemaName(String fileName) {
        String base = fileName.substring(0, fileName.indexOf("__"))
                .replace('V', 'v')
                .toLowerCase(Locale.ROOT);
        return "rb_" + base + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private static List<String> listSqlFileNames(Path directory) throws IOException {
        try (var stream = Files.list(directory)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private record MigrationScenario(String fileName, RollbackKind rollbackKind) {

        private String version() {
            int separatorIndex = fileName.indexOf("__");
            return fileName.substring(1, separatorIndex);
        }
    }

    private enum RollbackKind {
        SQL,
        SNAPSHOT_REQUIRED
    }
}
