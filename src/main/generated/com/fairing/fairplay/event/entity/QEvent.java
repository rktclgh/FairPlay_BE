package com.fairing.fairplay.event.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEvent is a Querydsl query type for Event
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEvent extends EntityPathBase<Event> {

    private static final long serialVersionUID = 870321980L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEvent event = new QEvent("event");

    public final SetPath<com.fairing.fairplay.booth.entity.Booth, com.fairing.fairplay.booth.entity.QBooth> booths = this.<com.fairing.fairplay.booth.entity.Booth, com.fairing.fairplay.booth.entity.QBooth>createSet("booths", com.fairing.fairplay.booth.entity.Booth.class, com.fairing.fairplay.booth.entity.QBooth.class, PathInits.DIRECT2);

    public final StringPath eventCode = createString("eventCode");

    public final QEventDetail eventDetail;

    public final NumberPath<Long> eventId = createNumber("eventId", Long.class);

    public final SetPath<com.fairing.fairplay.ticket.entity.EventTicket, com.fairing.fairplay.ticket.entity.QEventTicket> eventTickets = this.<com.fairing.fairplay.ticket.entity.EventTicket, com.fairing.fairplay.ticket.entity.QEventTicket>createSet("eventTickets", com.fairing.fairplay.ticket.entity.EventTicket.class, com.fairing.fairplay.ticket.entity.QEventTicket.class, PathInits.DIRECT2);

    public final SetPath<EventVersion, QEventVersion> eventVersions = this.<EventVersion, QEventVersion>createSet("eventVersions", EventVersion.class, QEventVersion.class, PathInits.DIRECT2);

    public final SetPath<ExternalLink, QExternalLink> externalLinks = this.<ExternalLink, QExternalLink>createSet("externalLinks", ExternalLink.class, QExternalLink.class, PathInits.DIRECT2);

    public final BooleanPath hidden = createBoolean("hidden");

    public final com.fairing.fairplay.user.entity.QEventAdmin manager;

    public final QEventStatusCode statusCode;

    public final StringPath titleEng = createString("titleEng");

    public final StringPath titleKr = createString("titleKr");

    public QEvent(String variable) {
        this(Event.class, forVariable(variable), INITS);
    }

    public QEvent(Path<? extends Event> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEvent(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEvent(PathMetadata metadata, PathInits inits) {
        this(Event.class, metadata, inits);
    }

    public QEvent(Class<? extends Event> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.eventDetail = inits.isInitialized("eventDetail") ? new QEventDetail(forProperty("eventDetail"), inits.get("eventDetail")) : null;
        this.manager = inits.isInitialized("manager") ? new com.fairing.fairplay.user.entity.QEventAdmin(forProperty("manager"), inits.get("manager")) : null;
        this.statusCode = inits.isInitialized("statusCode") ? new QEventStatusCode(forProperty("statusCode")) : null;
    }

}

