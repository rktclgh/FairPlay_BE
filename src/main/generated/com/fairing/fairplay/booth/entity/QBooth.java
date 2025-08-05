package com.fairing.fairplay.booth.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBooth is a Querydsl query type for Booth
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBooth extends EntityPathBase<Booth> {

    private static final long serialVersionUID = 97107132L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBooth booth = new QBooth("booth");

    public final com.fairing.fairplay.user.entity.QBoothAdmin boothAdmin;

    public final StringPath boothDescription = createString("boothDescription");

    public final SetPath<com.fairing.fairplay.ticket.entity.BoothTicket, com.fairing.fairplay.ticket.entity.QBoothTicket> boothTickets = this.<com.fairing.fairplay.ticket.entity.BoothTicket, com.fairing.fairplay.ticket.entity.QBoothTicket>createSet("boothTickets", com.fairing.fairplay.ticket.entity.BoothTicket.class, com.fairing.fairplay.ticket.entity.QBoothTicket.class, PathInits.DIRECT2);

    public final StringPath boothTitle = createString("boothTitle");

    public final QBoothType boothType;

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final DatePath<java.time.LocalDate> endDate = createDate("endDate", java.time.LocalDate.class);

    public final com.fairing.fairplay.event.entity.QEvent event;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath location = createString("location");

    public final DatePath<java.time.LocalDate> startDate = createDate("startDate", java.time.LocalDate.class);

    public QBooth(String variable) {
        this(Booth.class, forVariable(variable), INITS);
    }

    public QBooth(Path<? extends Booth> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBooth(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBooth(PathMetadata metadata, PathInits inits) {
        this(Booth.class, metadata, inits);
    }

    public QBooth(Class<? extends Booth> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.boothAdmin = inits.isInitialized("boothAdmin") ? new com.fairing.fairplay.user.entity.QBoothAdmin(forProperty("boothAdmin"), inits.get("boothAdmin")) : null;
        this.boothType = inits.isInitialized("boothType") ? new QBoothType(forProperty("boothType")) : null;
        this.event = inits.isInitialized("event") ? new com.fairing.fairplay.event.entity.QEvent(forProperty("event"), inits.get("event")) : null;
    }

}

