package com.fairing.fairplay.ticket.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTicketVersion is a Querydsl query type for TicketVersion
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTicketVersion extends EntityPathBase<TicketVersion> {

    private static final long serialVersionUID = -603124918L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTicketVersion ticketVersion = new QTicketVersion("ticketVersion");

    public final StringPath snapshot = createString("snapshot");

    public final QTicket ticket;

    public final NumberPath<Long> ticketVersionId = createNumber("ticketVersionId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> updatedBy = createNumber("updatedBy", Long.class);

    public final NumberPath<Integer> versionNumber = createNumber("versionNumber", Integer.class);

    public QTicketVersion(String variable) {
        this(TicketVersion.class, forVariable(variable), INITS);
    }

    public QTicketVersion(Path<? extends TicketVersion> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTicketVersion(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTicketVersion(PathMetadata metadata, PathInits inits) {
        this(TicketVersion.class, metadata, inits);
    }

    public QTicketVersion(Class<? extends TicketVersion> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.ticket = inits.isInitialized("ticket") ? new QTicket(forProperty("ticket"), inits.get("ticket")) : null;
    }

}

