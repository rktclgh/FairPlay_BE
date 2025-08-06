package com.fairing.fairplay.attendee.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAttendee is a Querydsl query type for Attendee
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAttendee extends EntityPathBase<Attendee> {

    private static final long serialVersionUID = 1470867018L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAttendee attendee = new QAttendee("attendee");

    public final QAttendeeTypeCode attendeeTypeCode;

    public final DatePath<java.time.LocalDate> birth = createDate("birth", java.time.LocalDate.class);

    public final BooleanPath checkedIn = createBoolean("checkedIn");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final StringPath phone = createString("phone");

    public final com.fairing.fairplay.reservation.entity.QReservation reservation;

    public QAttendee(String variable) {
        this(Attendee.class, forVariable(variable), INITS);
    }

    public QAttendee(Path<? extends Attendee> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAttendee(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAttendee(PathMetadata metadata, PathInits inits) {
        this(Attendee.class, metadata, inits);
    }

    public QAttendee(Class<? extends Attendee> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.attendeeTypeCode = inits.isInitialized("attendeeTypeCode") ? new QAttendeeTypeCode(forProperty("attendeeTypeCode")) : null;
        this.reservation = inits.isInitialized("reservation") ? new com.fairing.fairplay.reservation.entity.QReservation(forProperty("reservation"), inits.get("reservation")) : null;
    }

}

