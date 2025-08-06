package com.fairing.fairplay.ticket.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEventSchedule is a Querydsl query type for EventSchedule
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEventSchedule extends EntityPathBase<EventSchedule> {

    private static final long serialVersionUID = 1293432431L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEventSchedule eventSchedule = new QEventSchedule("eventSchedule");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final DatePath<java.time.LocalDate> date = createDate("date", java.time.LocalDate.class);

    public final TimePath<java.time.LocalTime> endTime = createTime("endTime", java.time.LocalTime.class);

    public final com.fairing.fairplay.event.entity.QEvent event;

    public final NumberPath<Long> scheduleId = createNumber("scheduleId", Long.class);

    public final SetPath<ScheduleTicket, QScheduleTicket> scheduleTickets = this.<ScheduleTicket, QScheduleTicket>createSet("scheduleTickets", ScheduleTicket.class, QScheduleTicket.class, PathInits.DIRECT2);

    public final TimePath<java.time.LocalTime> startTime = createTime("startTime", java.time.LocalTime.class);

    public final EnumPath<TypesEnum> types = createEnum("types", TypesEnum.class);

    public final NumberPath<Integer> weekday = createNumber("weekday", Integer.class);

    public QEventSchedule(String variable) {
        this(EventSchedule.class, forVariable(variable), INITS);
    }

    public QEventSchedule(Path<? extends EventSchedule> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEventSchedule(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEventSchedule(PathMetadata metadata, PathInits inits) {
        this(EventSchedule.class, metadata, inits);
    }

    public QEventSchedule(Class<? extends EventSchedule> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.event = inits.isInitialized("event") ? new com.fairing.fairplay.event.entity.QEvent(forProperty("event"), inits.get("event")) : null;
    }

}

