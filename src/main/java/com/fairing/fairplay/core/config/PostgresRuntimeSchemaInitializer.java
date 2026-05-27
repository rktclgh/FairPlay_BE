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

        dropEventManagerUniqueConstraintIfExists();
        relaxLegacyPaymentTargetConstraintIfExists();
        normalizeBannerApplicationCommentTypeIfExists();

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

    private void dropEventManagerUniqueConstraintIfExists() {
        Boolean tableExists = jdbcTemplate.queryForObject(
                "SELECT to_regclass(?) IS NOT NULL",
                Boolean.class,
                "event");

        if (!Boolean.TRUE.equals(tableExists)) {
            log.debug("Skipping event.manager_id uniqueness cleanup because event table does not exist.");
            return;
        }

        jdbcTemplate.execute("""
                DO $$
                DECLARE
                    constraint_name text;
                BEGIN
                    SELECT tc.constraint_name
                    INTO constraint_name
                    FROM information_schema.table_constraints tc
                    JOIN information_schema.key_column_usage kcu
                      ON tc.constraint_name = kcu.constraint_name
                     AND tc.table_schema = kcu.table_schema
                    WHERE tc.table_schema = 'public'
                      AND tc.table_name = 'event'
                      AND tc.constraint_type = 'UNIQUE'
                    GROUP BY tc.constraint_name
                    HAVING array_agg(kcu.column_name::text ORDER BY kcu.ordinal_position) = ARRAY['manager_id']::text[];

                    IF constraint_name IS NOT NULL THEN
                        EXECUTE format('ALTER TABLE "event" DROP CONSTRAINT %I', constraint_name);
                    END IF;
                END $$;
                """);
    }

    private void relaxLegacyPaymentTargetConstraintIfExists() {
        Boolean tableExists = jdbcTemplate.queryForObject(
                "SELECT to_regclass(?) IS NOT NULL",
                Boolean.class,
                "payment");

        if (!Boolean.TRUE.equals(tableExists)) {
            log.debug("Skipping payment.target_id nullability cleanup because payment table does not exist.");
            return;
        }

        jdbcTemplate.execute("ALTER TABLE payment ALTER COLUMN target_id DROP NOT NULL");
    }

    private void normalizeBannerApplicationCommentTypeIfExists() {
        Boolean columnExists = jdbcTemplate.queryForObject(
                """
                        SELECT EXISTS (
                            SELECT 1
                            FROM information_schema.columns
                            WHERE table_schema = 'public'
                              AND table_name = 'banner_application'
                              AND column_name = 'admin_comment'
                        )
                        """,
                Boolean.class);

        if (!Boolean.TRUE.equals(columnExists)) {
            log.debug("Skipping banner_application.admin_comment type cleanup because column does not exist.");
            return;
        }

        jdbcTemplate.execute("ALTER TABLE banner_application ALTER COLUMN admin_comment TYPE text USING admin_comment::text");
    }
}
