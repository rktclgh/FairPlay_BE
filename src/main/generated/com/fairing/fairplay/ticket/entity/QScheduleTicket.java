package com.fairing.fairplay.ticket.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QScheduleTicket is a Querydsl query type for ScheduleTicket
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QScheduleTicket extends EntityPathBase<ScheduleTicket> {

    private static final long serialVersionUID = 794048805L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QScheduleTicket scheduleTicket = new QScheduleTicket("scheduleTicket");

    public final QEventSchedule eventSchedule;

    public final QScheduleTicketId id;

    public final NumberPath<Integer> remainingStock = createNumber("remainingStock", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> salesEndAt = createDateTime("salesEndAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> salesStartAt = createDateTime("salesStartAt", java.time.LocalDateTime.class);

    public final QTicket ticket;

    public final BooleanPath visible = createBoolean("visible");

    public QScheduleTicket(String variable) {
        this(ScheduleTicket.class, forVariable(variable), INITS);
    }

    public QScheduleTicket(Path<? extends ScheduleTicket> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QScheduleTicket(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QScheduleTicket(PathMetadata metadata, PathInits inits) {
        this(ScheduleTicket.class, metadata, inits);
    }

    public QScheduleTicket(Class<? extends ScheduleTicket> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.eventSchedule = inits.isInitialized("eventSchedule") ? new QEventSchedule(forProperty("eventSchedule"), inits.get("eventSchedule")) : null;
        this.id = inits.isInitialized("id") ? new QScheduleTicketId(forProperty("id")) : null;
        this.ticket = inits.isInitialized("ticket") ? new QTicket(forProperty("ticket"), inits.get("ticket")) : null;
    }

}

