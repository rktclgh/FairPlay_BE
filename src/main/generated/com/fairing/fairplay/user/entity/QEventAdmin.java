package com.fairing.fairplay.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEventAdmin is a Querydsl query type for EventAdmin
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEventAdmin extends EntityPathBase<EventAdmin> {

    private static final long serialVersionUID = -1783756874L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEventAdmin eventAdmin = new QEventAdmin("eventAdmin");

    public final BooleanPath active = createBoolean("active");

    public final StringPath businessNumber = createString("businessNumber");

    public final StringPath contactEmail = createString("contactEmail");

    public final StringPath contactNumber = createString("contactNumber");

    public final QUsers user;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QEventAdmin(String variable) {
        this(EventAdmin.class, forVariable(variable), INITS);
    }

    public QEventAdmin(Path<? extends EventAdmin> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEventAdmin(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEventAdmin(PathMetadata metadata, PathInits inits) {
        this(EventAdmin.class, metadata, inits);
    }

    public QEventAdmin(Class<? extends EventAdmin> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUsers(forProperty("user"), inits.get("user")) : null;
    }

}

