package com.fairing.fairplay.event.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEventVersion is a Querydsl query type for EventVersion
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEventVersion extends EntityPathBase<EventVersion> {

    private static final long serialVersionUID = -1296741892L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEventVersion eventVersion = new QEventVersion("eventVersion");

    public final QEvent event;

    public final NumberPath<Long> eventVersionId = createNumber("eventVersionId", Long.class);

    public final StringPath snapshot = createString("snapshot");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> updatedBy = createNumber("updatedBy", Long.class);

    public final NumberPath<Integer> versionNumber = createNumber("versionNumber", Integer.class);

    public QEventVersion(String variable) {
        this(EventVersion.class, forVariable(variable), INITS);
    }

    public QEventVersion(Path<? extends EventVersion> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEventVersion(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEventVersion(PathMetadata metadata, PathInits inits) {
        this(EventVersion.class, metadata, inits);
    }

    public QEventVersion(Class<? extends EventVersion> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.event = inits.isInitialized("event") ? new QEvent(forProperty("event"), inits.get("event")) : null;
    }

}

