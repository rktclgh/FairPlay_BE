package com.fairing.fairplay.reservation.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReservation is a Querydsl query type for Reservation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReservation extends EntityPathBase<Reservation> {

    private static final long serialVersionUID = 1558998460L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReservation reservation = new QReservation("reservation");

    public final BooleanPath canceled = createBoolean("canceled");

    public final DateTimePath<java.time.LocalDateTime> canceled_at = createDateTime("canceled_at", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final com.fairing.fairplay.event.entity.QEvent event;

    public final NumberPath<Integer> price = createNumber("price", Integer.class);

    public final NumberPath<Integer> quantity = createNumber("quantity", Integer.class);

    public final NumberPath<Long> reservationId = createNumber("reservationId", Long.class);

    public final QReservationStatusCode reservationStatusCode;

    public final com.fairing.fairplay.ticket.entity.QEventSchedule schedule;

    public final com.fairing.fairplay.ticket.entity.QTicket ticket;

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final com.fairing.fairplay.user.entity.QUsers user;

    public QReservation(String variable) {
        this(Reservation.class, forVariable(variable), INITS);
    }

    public QReservation(Path<? extends Reservation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReservation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReservation(PathMetadata metadata, PathInits inits) {
        this(Reservation.class, metadata, inits);
    }

    public QReservation(Class<? extends Reservation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.event = inits.isInitialized("event") ? new com.fairing.fairplay.event.entity.QEvent(forProperty("event"), inits.get("event")) : null;
        this.reservationStatusCode = inits.isInitialized("reservationStatusCode") ? new QReservationStatusCode(forProperty("reservationStatusCode")) : null;
        this.schedule = inits.isInitialized("schedule") ? new com.fairing.fairplay.ticket.entity.QEventSchedule(forProperty("schedule"), inits.get("schedule")) : null;
        this.ticket = inits.isInitialized("ticket") ? new com.fairing.fairplay.ticket.entity.QTicket(forProperty("ticket"), inits.get("ticket")) : null;
        this.user = inits.isInitialized("user") ? new com.fairing.fairplay.user.entity.QUsers(forProperty("user"), inits.get("user")) : null;
    }

}

