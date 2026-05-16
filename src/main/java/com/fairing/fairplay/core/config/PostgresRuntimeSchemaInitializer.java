package com.fairing.fairplay.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostgresRuntimeSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.postgres.auto-init-constraints:true}")
    private boolean autoInitConstraints;

    @Override
    public void run(ApplicationArguments args) {
        if (!autoInitConstraints) {
            log.info("PostgreSQL runtime constraint auto-init is disabled.");
            return;
        }

        createUniqueIndexIfTableExists(
                "admin_kpi_statistics",
                "uk_admin_kpi_statistics_stat_date",
                "admin_kpi_statistics (stat_date)");
        createUniqueIndexIfTableExists(
                "event_comparison_statistics",
                "uk_event_comparison_statistics_event_period",
                "event_comparison_statistics (event_id, start_date, end_date)");
        createUniqueIndexIfTableExists(
                "event_popularity_statistics",
                "uk_event_pop_stats_event_id",
                "event_popularity_statistics (event_id)");
    }

    private void createUniqueIndexIfTableExists(String tableName, String indexName, String indexTarget) {
        Boolean tableExists = jdbcTemplate.queryForObject(
                "SELECT to_regclass(?) IS NOT NULL",
                Boolean.class,
                tableName);

        if (!Boolean.TRUE.equals(tableExists)) {
            log.debug("Skipping PostgreSQL index {} because table {} does not exist.", indexName, tableName);
            return;
        }

        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS " + indexName + " ON " + indexTarget);
    }
}
