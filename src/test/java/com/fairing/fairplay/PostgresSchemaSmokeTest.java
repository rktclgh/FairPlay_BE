package com.fairing.fairplay;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fairing.fairplay.event.repository.EventQueryRepository;

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

    @Autowired
    @Qualifier("eventQueryRepositoryImpl")
    private EventQueryRepository eventQueryRepository;

    @Test
    void createsRequiredPostgresSchemaOnFreshDatabase() {
        Integer tableCount = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = 'public'
                """, Integer.class);

        assertThat(tableCount).isNotNull().isGreaterThan(40);
    }

    @Test
    void eventSummaryQueryUsesPostgresCompatibleGrouping() {
        assertThat(eventQueryRepository.findEventSummariesWithFilters(
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                PageRequest.of(0, 10)
        ).getContent()).isEmpty();
    }
}
