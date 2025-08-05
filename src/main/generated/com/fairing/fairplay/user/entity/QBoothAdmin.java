package com.fairing.fairplay.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBoothAdmin is a Querydsl query type for BoothAdmin
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBoothAdmin extends EntityPathBase<BoothAdmin> {

    private static final long serialVersionUID = -492197862L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBoothAdmin boothAdmin = new QBoothAdmin("boothAdmin");

    public final StringPath contactNumber = createString("contactNumber");

    public final StringPath email = createString("email");

    public final StringPath managerName = createString("managerName");

    public final StringPath officialUrl = createString("officialUrl");

    public final QUsers user;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QBoothAdmin(String variable) {
        this(BoothAdmin.class, forVariable(variable), INITS);
    }

    public QBoothAdmin(Path<? extends BoothAdmin> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBoothAdmin(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBoothAdmin(PathMetadata metadata, PathInits inits) {
        this(BoothAdmin.class, metadata, inits);
    }

    public QBoothAdmin(Class<? extends BoothAdmin> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUsers(forProperty("user"), inits.get("user")) : null;
    }

}

