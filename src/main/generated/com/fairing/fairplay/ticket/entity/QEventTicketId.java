package com.fairing.fairplay.ticket.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEventTicketId is a Querydsl query type for EventTicketId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QEventTicketId extends BeanPath<EventTicketId> {

    private static final long serialVersionUID = -366246625L;

    public static final QEventTicketId eventTicketId = new QEventTicketId("eventTicketId");

    public final NumberPath<Long> eventId = createNumber("eventId", Long.class);

    public final NumberPath<Long> ticketId = createNumber("ticketId", Long.class);

    public QEventTicketId(String variable) {
        super(EventTicketId.class, forVariable(variable));
    }

    public QEventTicketId(Path<? extends EventTicketId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEventTicketId(PathMetadata metadata) {
        super(EventTicketId.class, metadata);
    }

}

