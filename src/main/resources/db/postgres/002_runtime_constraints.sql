CREATE UNIQUE INDEX IF NOT EXISTS uk_admin_kpi_statistics_stat_date
    ON admin_kpi_statistics (stat_date);

CREATE UNIQUE INDEX IF NOT EXISTS uk_event_comparison_statistics_event_period
    ON event_comparison_statistics (event_id, start_date, end_date);

CREATE UNIQUE INDEX IF NOT EXISTS uk_event_pop_stats_event_id
    ON event_popularity_statistics (event_id);
