package com.fairing.fairplay.reservation.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReservationLog is a Querydsl query type for ReservationLog
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReservationLog extends EntityPathBase<ReservationLog> {

    private static final long serialVersionUID = -1653140504L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReservationLog reservationLog = new QReservationLog("reservationLog");

    public final DateTimePath<java.time.LocalDateTime> changedAt = createDateTime("changedAt", java.time.LocalDateTime.class);

    public final com.fairing.fairplay.user.entity.QUsers changedBy;

    public final QReservation reservation;

    public final NumberPath<Long> reservationLogId = createNumber("reservationLogId", Long.class);

    public final QReservationStatusCode reservationStatusCode;

    public QReservationLog(String variable) {
        this(ReservationLog.class, forVariable(variable), INITS);
    }

    public QReservationLog(Path<? extends ReservationLog> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReservationLog(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReservationLog(PathMetadata metadata, PathInits inits) {
        this(ReservationLog.class, metadata, inits);
    }

    public QReservationLog(Class<? extends ReservationLog> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.changedBy = inits.isInitialized("changedBy") ? new com.fairing.fairplay.user.entity.QUsers(forProperty("changedBy"), inits.get("changedBy")) : null;
        this.reservation = inits.isInitialized("reservation") ? new QReservation(forProperty("reservation"), inits.get("reservation")) : null;
        this.reservationStatusCode = inits.isInitialized("reservationStatusCode") ? new QReservationStatusCode(forProperty("reservationStatusCode")) : null;
    }

}

