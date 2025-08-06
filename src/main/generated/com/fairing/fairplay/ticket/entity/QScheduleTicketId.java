package com.fairing.fairplay.ticket.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QScheduleTicketId is a Querydsl query type for ScheduleTicketId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QScheduleTicketId extends BeanPath<ScheduleTicketId> {

    private static final long serialVersionUID = -1423274720L;

    public static final QScheduleTicketId scheduleTicketId = new QScheduleTicketId("scheduleTicketId");

    public final NumberPath<Long> scheduleId = createNumber("scheduleId", Long.class);

    public final NumberPath<Long> ticketId = createNumber("ticketId", Long.class);

    public QScheduleTicketId(String variable) {
        super(ScheduleTicketId.class, forVariable(variable));
    }

    public QScheduleTicketId(Path<? extends ScheduleTicketId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QScheduleTicketId(PathMetadata metadata) {
        super(ScheduleTicketId.class, metadata);
    }

}

