package com.fairing.fairplay.statistics.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEventHourlyStatistics is a Querydsl query type for EventHourlyStatistics
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEventHourlyStatistics extends EntityPathBase<EventHourlyStatistics> {

    private static final long serialVersionUID = -1578879979L;

    public static final QEventHourlyStatistics eventHourlyStatistics = new QEventHourlyStatistics("eventHourlyStatistics");

    public final NumberPath<Integer> checkins = createNumber("checkins", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> eventId = createNumber("eventId", Long.class);

    public final NumberPath<Integer> hour = createNumber("hour", Integer.class);

    public final NumberPath<Long> reservations = createNumber("reservations", Long.class);

    public final DatePath<java.time.LocalDate> statDate = createDate("statDate", java.time.LocalDate.class);

    public final NumberPath<Long> statsHourlyId = createNumber("statsHourlyId", Long.class);

    public QEventHourlyStatistics(String variable) {
        super(EventHourlyStatistics.class, forVariable(variable));
    }

    public QEventHourlyStatistics(Path<? extends EventHourlyStatistics> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEventHourlyStatistics(PathMetadata metadata) {
        super(EventHourlyStatistics.class, metadata);
    }

}

