package com.fairing.fairplay.event.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEventApply is a Querydsl query type for EventApply
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEventApply extends EntityPathBase<EventApply> {

    private static final long serialVersionUID = 989637010L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEventApply eventApply = new QEventApply("eventApply");

    public final StringPath adminComment = createString("adminComment");

    public final DateTimePath<java.time.LocalDateTime> applyAt = createDateTime("applyAt", java.time.LocalDateTime.class);

    public final StringPath businessNumber = createString("businessNumber");

    public final StringPath contactNumber = createString("contactNumber");

    public final StringPath email = createString("email");

    public final StringPath eventEmail = createString("eventEmail");

    public final StringPath fileUrl = createString("fileUrl");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath managerName = createString("managerName");

    public final QApplyStatusCode statusCode;

    public final DateTimePath<java.time.LocalDateTime> statusUpdatedAt = createDateTime("statusUpdatedAt", java.time.LocalDateTime.class);

    public final StringPath titleEng = createString("titleEng");

    public final StringPath titleKr = createString("titleKr");

    public final BooleanPath verified = createBoolean("verified");

    public QEventApply(String variable) {
        this(EventApply.class, forVariable(variable), INITS);
    }

    public QEventApply(Path<? extends EventApply> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEventApply(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEventApply(PathMetadata metadata, PathInits inits) {
        this(EventApply.class, metadata, inits);
    }

    public QEventApply(Class<? extends EventApply> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.statusCode = inits.isInitialized("statusCode") ? new QApplyStatusCode(forProperty("statusCode")) : null;
    }

}

