package com.fairing.fairplay.statistics.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEventDailyStatistics is a Querydsl query type for EventDailyStatistics
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEventDailyStatistics extends EntityPathBase<EventDailyStatistics> {

    private static final long serialVersionUID = -374767013L;

    public static final QEventDailyStatistics eventDailyStatistics = new QEventDailyStatistics("eventDailyStatistics");

    public final NumberPath<Integer> cancellationCount = createNumber("cancellationCount", Integer.class);

    public final NumberPath<Integer> checkinsCount = createNumber("checkinsCount", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> eventId = createNumber("eventId", Long.class);

    public final NumberPath<Integer> noShowsCount = createNumber("noShowsCount", Integer.class);

    public final NumberPath<Integer> reservationCount = createNumber("reservationCount", Integer.class);

    public final DatePath<java.time.LocalDate> statDate = createDate("statDate", java.time.LocalDate.class);

    public final NumberPath<Long> statsId = createNumber("statsId", Long.class);

    public QEventDailyStatistics(String variable) {
        super(EventDailyStatistics.class, forVariable(variable));
    }

    public QEventDailyStatistics(Path<? extends EventDailyStatistics> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEventDailyStatistics(PathMetadata metadata) {
        super(EventDailyStatistics.class, metadata);
    }

}

