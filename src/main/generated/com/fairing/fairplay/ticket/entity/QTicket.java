package com.fairing.fairplay.ticket.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTicket is a Querydsl query type for Ticket
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTicket extends EntityPathBase<Ticket> {

    private static final long serialVersionUID = 446761390L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTicket ticket = new QTicket("ticket");

    public final SetPath<BoothTicket, QBoothTicket> boothTickets = this.<BoothTicket, QBoothTicket>createSet("boothTickets", BoothTicket.class, QBoothTicket.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final BooleanPath deleted = createBoolean("deleted");

    public final StringPath description = createString("description");

    public final SetPath<EventTicket, QEventTicket> eventTickets = this.<EventTicket, QEventTicket>createSet("eventTickets", EventTicket.class, QEventTicket.class, PathInits.DIRECT2);

    public final NumberPath<Integer> maxPurchase = createNumber("maxPurchase", Integer.class);

    public final StringPath name = createString("name");

    public final NumberPath<Integer> price = createNumber("price", Integer.class);

    public final SetPath<ScheduleTicket, QScheduleTicket> scheduleTickets = this.<ScheduleTicket, QScheduleTicket>createSet("scheduleTickets", ScheduleTicket.class, QScheduleTicket.class, PathInits.DIRECT2);

    public final NumberPath<Integer> stock = createNumber("stock", Integer.class);

    public final NumberPath<Long> ticketId = createNumber("ticketId", Long.class);

    public final QTicketStatusCode ticketStatusCode;

    public final SetPath<TicketVersion, QTicketVersion> ticketVersions = this.<TicketVersion, QTicketVersion>createSet("ticketVersions", TicketVersion.class, QTicketVersion.class, PathInits.DIRECT2);

    public final EnumPath<TypesEnum> types = createEnum("types", TypesEnum.class);

    public final BooleanPath visible = createBoolean("visible");

    public QTicket(String variable) {
        this(Ticket.class, forVariable(variable), INITS);
    }

    public QTicket(Path<? extends Ticket> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTicket(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTicket(PathMetadata metadata, PathInits inits) {
        this(Ticket.class, metadata, inits);
    }

    public QTicket(Class<? extends Ticket> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.ticketStatusCode = inits.isInitialized("ticketStatusCode") ? new QTicketStatusCode(forProperty("ticketStatusCode")) : null;
    }

}

