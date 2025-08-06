package com.fairing.fairplay.statistics.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEventSessionStatistics is a Querydsl query type for EventSessionStatistics
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEventSessionStatistics extends EntityPathBase<EventSessionStatistics> {

    private static final long serialVersionUID = 247061208L;

    public static final QEventSessionStatistics eventSessionStatistics = new QEventSessionStatistics("eventSessionStatistics");

    public final NumberPath<Integer> cancellation = createNumber("cancellation", Integer.class);

    public final NumberPath<Integer> checkins = createNumber("checkins", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> eventId = createNumber("eventId", Long.class);

    public final NumberPath<Integer> noShows = createNumber("noShows", Integer.class);

    public final NumberPath<Integer> reservations = createNumber("reservations", Integer.class);

    public final NumberPath<Long> sessionId = createNumber("sessionId", Long.class);

    public final DatePath<java.time.LocalDate> statDate = createDate("statDate", java.time.LocalDate.class);

    public final NumberPath<Long> statsSessionId = createNumber("statsSessionId", Long.class);

    public final StringPath ticketType = createString("ticketType");

    public QEventSessionStatistics(String variable) {
        super(EventSessionStatistics.class, forVariable(variable));
    }

    public QEventSessionStatistics(Path<? extends EventSessionStatistics> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEventSessionStatistics(PathMetadata metadata) {
        super(EventSessionStatistics.class, metadata);
    }

}

