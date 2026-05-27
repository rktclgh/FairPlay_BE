DO $$
BEGIN
    IF to_regclass('public.admin_kpi_statistics') IS NOT NULL THEN
        CREATE UNIQUE INDEX IF NOT EXISTS uk_admin_kpi_statistics_stat_date
            ON admin_kpi_statistics (stat_date);
    END IF;

    IF to_regclass('public.event_comparison_statistics') IS NOT NULL THEN
        CREATE UNIQUE INDEX IF NOT EXISTS uk_event_comparison_statistics_event_period
            ON event_comparison_statistics (event_id, start_date, end_date);
    END IF;

    IF to_regclass('public.event_popularity_statistics') IS NOT NULL THEN
        CREATE UNIQUE INDEX IF NOT EXISTS uk_event_pop_stats_event_id
            ON event_popularity_statistics (event_id);
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.refund_status_code') IS NOT NULL THEN
        INSERT INTO refund_status_code (code, name)
        VALUES
            ('PROCESSING', '처리중'),
            ('FAILED', '환불실패'),
            ('RECONCILIATION_REQUIRED', '대사필요')
        ON CONFLICT (code) DO NOTHING;
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.payment_status_code') IS NOT NULL THEN
        INSERT INTO payment_status_code (code, name)
        VALUES
            ('REFUNDED', '환불 완료'),
            ('PARTIAL_REFUNDED', '부분 환불')
        ON CONFLICT (code) DO NOTHING;
    END IF;
END $$;
