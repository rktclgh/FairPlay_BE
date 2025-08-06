package com.fairing.fairplay.ticket.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBoothTicket is a Querydsl query type for BoothTicket
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBoothTicket extends EntityPathBase<BoothTicket> {

    private static final long serialVersionUID = 811176192L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBoothTicket boothTicket = new QBoothTicket("boothTicket");

    public final com.fairing.fairplay.booth.entity.QBooth booth;

    public final QBoothTicketId id;

    public final QTicket ticket;

    public QBoothTicket(String variable) {
        this(BoothTicket.class, forVariable(variable), INITS);
    }

    public QBoothTicket(Path<? extends BoothTicket> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBoothTicket(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBoothTicket(PathMetadata metadata, PathInits inits) {
        this(BoothTicket.class, metadata, inits);
    }

    public QBoothTicket(Class<? extends BoothTicket> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.booth = inits.isInitialized("booth") ? new com.fairing.fairplay.booth.entity.QBooth(forProperty("booth"), inits.get("booth")) : null;
        this.id = inits.isInitialized("id") ? new QBoothTicketId(forProperty("id")) : null;
        this.ticket = inits.isInitialized("ticket") ? new QTicket(forProperty("ticket"), inits.get("ticket")) : null;
    }

}

