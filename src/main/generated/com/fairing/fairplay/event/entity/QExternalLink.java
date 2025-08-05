package com.fairing.fairplay.event.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QExternalLink is a Querydsl query type for ExternalLink
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QExternalLink extends EntityPathBase<ExternalLink> {

    private static final long serialVersionUID = 352187267L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QExternalLink externalLink = new QExternalLink("externalLink");

    public final StringPath displayText = createString("displayText");

    public final QEvent event;

    public final NumberPath<Long> linkId = createNumber("linkId", Long.class);

    public final StringPath url = createString("url");

    public QExternalLink(String variable) {
        this(ExternalLink.class, forVariable(variable), INITS);
    }

    public QExternalLink(Path<? extends ExternalLink> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QExternalLink(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QExternalLink(PathMetadata metadata, PathInits inits) {
        this(ExternalLink.class, metadata, inits);
    }

    public QExternalLink(Class<? extends ExternalLink> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.event = inits.isInitialized("event") ? new QEvent(forProperty("event"), inits.get("event")) : null;
    }

}

