package com.fairing.fairplay.statistics.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEventTicketStatistics is a Querydsl query type for EventTicketStatistics
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEventTicketStatistics extends EntityPathBase<EventTicketStatistics> {

    private static final long serialVersionUID = -1810514192L;

    public static final QEventTicketStatistics eventTicketStatistics = new QEventTicketStatistics("eventTicketStatistics");

    public final NumberPath<Integer> checkins = createNumber("checkins", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> eventId = createNumber("eventId", Long.class);

    public final NumberPath<Integer> reservations = createNumber("reservations", Integer.class);

    public final DatePath<java.time.LocalDate> statDate = createDate("statDate", java.time.LocalDate.class);

    public final NumberPath<Long> statsTicketId = createNumber("statsTicketId", Long.class);

    public final StringPath ticketType = createString("ticketType");

    public QEventTicketStatistics(String variable) {
        super(EventTicketStatistics.class, forVariable(variable));
    }

    public QEventTicketStatistics(Path<? extends EventTicketStatistics> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEventTicketStatistics(PathMetadata metadata) {
        super(EventTicketStatistics.class, metadata);
    }

}

