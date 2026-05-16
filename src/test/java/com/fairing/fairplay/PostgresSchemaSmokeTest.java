package com.fairing.fairplay;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=${SCHEMA_SMOKE_DB_URL:jdbc:postgresql://127.0.0.1:5432/fairplay}",
        "spring.datasource.username=${SCHEMA_SMOKE_DB_USERNAME:fairplay}",
        "spring.datasource.password=${SCHEMA_SMOKE_DB_PASSWORD:fairplay}",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.main.lazy-initialization=false",
        "app.postgres.auto-init-constraints=true",
        "rag.pgvector.auto-init=true"
})
@EnabledIfEnvironmentVariable(named = "FAIRPLAY_SCHEMA_SMOKE_ENABLED", matches = "true")
class PostgresSchemaSmokeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsRequiredPostgresSchemaOnFreshDatabase() {
        List<String> tables = existingTables();
        Integer tableCount = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = 'public'
                """, Integer.class);

        assertThat(tableCount).isNotNull().isGreaterThan(40);
        assertThat(tables)
                .as("existing public tables: %s", tables)
                .contains(
                "users",
                "event",
                "reservation",
                "payment",
                "qr_ticket");
    }

    private List<String> existingTables() {
        return jdbcTemplate.queryForList("""
                select table_name
                from information_schema.tables
                where table_schema = 'public'
                """, String.class);
    }
}
