package com.fairing.fairplay.qr.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QQrTicket is a Querydsl query type for QrTicket
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QQrTicket extends EntityPathBase<QrTicket> {

    private static final long serialVersionUID = 1210410052L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QQrTicket qrTicket = new QQrTicket("qrTicket");

    public final BooleanPath active = createBoolean("active");

    public final com.fairing.fairplay.attendee.entity.QAttendee attendee;

    public final com.fairing.fairplay.ticket.entity.QEventTicket eventTicket;

    public final DateTimePath<java.time.LocalDateTime> expiredAt = createDateTime("expiredAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> issuedAt = createDateTime("issuedAt", java.time.LocalDateTime.class);

    public final StringPath manualCode = createString("manualCode");

    public final StringPath qrCode = createString("qrCode");

    public final BooleanPath reentryAllowed = createBoolean("reentryAllowed");

    public final StringPath ticketNo = createString("ticketNo");

    public QQrTicket(String variable) {
        this(QrTicket.class, forVariable(variable), INITS);
    }

    public QQrTicket(Path<? extends QrTicket> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QQrTicket(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QQrTicket(PathMetadata metadata, PathInits inits) {
        this(QrTicket.class, metadata, inits);
    }

    public QQrTicket(Class<? extends QrTicket> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.attendee = inits.isInitialized("attendee") ? new com.fairing.fairplay.attendee.entity.QAttendee(forProperty("attendee"), inits.get("attendee")) : null;
        this.eventTicket = inits.isInitialized("eventTicket") ? new com.fairing.fairplay.ticket.entity.QEventTicket(forProperty("eventTicket"), inits.get("eventTicket")) : null;
    }

}

