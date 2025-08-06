package com.fairing.fairplay.shareticket.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QShareTicket is a Querydsl query type for ShareTicket
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QShareTicket extends EntityPathBase<ShareTicket> {

    private static final long serialVersionUID = 87832444L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QShareTicket shareTicket = new QShareTicket("shareTicket");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final BooleanPath expired = createBoolean("expired");

    public final DateTimePath<java.time.LocalDateTime> expiredAt = createDateTime("expiredAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath linkToken = createString("linkToken");

    public final com.fairing.fairplay.reservation.entity.QReservation reservation;

    public final NumberPath<Integer> submittedCount = createNumber("submittedCount", Integer.class);

    public final NumberPath<Integer> totalAllowed = createNumber("totalAllowed", Integer.class);

    public QShareTicket(String variable) {
        this(ShareTicket.class, forVariable(variable), INITS);
    }

    public QShareTicket(Path<? extends ShareTicket> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QShareTicket(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QShareTicket(PathMetadata metadata, PathInits inits) {
        this(ShareTicket.class, metadata, inits);
    }

    public QShareTicket(Class<? extends ShareTicket> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.reservation = inits.isInitialized("reservation") ? new com.fairing.fairplay.reservation.entity.QReservation(forProperty("reservation"), inits.get("reservation")) : null;
    }

}

