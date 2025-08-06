package com.fairing.fairplay.ticket.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEventTicket is a Querydsl query type for EventTicket
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEventTicket extends EntityPathBase<EventTicket> {

    private static final long serialVersionUID = -572447516L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEventTicket eventTicket = new QEventTicket("eventTicket");

    public final com.fairing.fairplay.event.entity.QEvent event;

    public final QEventTicketId id;

    public final QTicket ticket;

    public QEventTicket(String variable) {
        this(EventTicket.class, forVariable(variable), INITS);
    }

    public QEventTicket(Path<? extends EventTicket> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEventTicket(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEventTicket(PathMetadata metadata, PathInits inits) {
        this(EventTicket.class, metadata, inits);
    }

    public QEventTicket(Class<? extends EventTicket> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.event = inits.isInitialized("event") ? new com.fairing.fairplay.event.entity.QEvent(forProperty("event"), inits.get("event")) : null;
        this.id = inits.isInitialized("id") ? new QEventTicketId(forProperty("id")) : null;
        this.ticket = inits.isInitialized("ticket") ? new QTicket(forProperty("ticket"), inits.get("ticket")) : null;
    }

}

