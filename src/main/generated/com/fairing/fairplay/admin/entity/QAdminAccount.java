package com.fairing.fairplay.admin.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAdminAccount is a Querydsl query type for AdminAccount
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAdminAccount extends EntityPathBase<AdminAccount> {

    private static final long serialVersionUID = -1427897039L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAdminAccount adminAccount = new QAdminAccount("adminAccount");

    public final BooleanPath active = createBoolean("active");

    public final QAdminRoleCode adminRoleCode;

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final StringPath password = createString("password");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QAdminAccount(String variable) {
        this(AdminAccount.class, forVariable(variable), INITS);
    }

    public QAdminAccount(Path<? extends AdminAccount> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAdminAccount(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAdminAccount(PathMetadata metadata, PathInits inits) {
        this(AdminAccount.class, metadata, inits);
    }

    public QAdminAccount(Class<? extends AdminAccount> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.adminRoleCode = inits.isInitialized("adminRoleCode") ? new QAdminRoleCode(forProperty("adminRoleCode")) : null;
    }

}

