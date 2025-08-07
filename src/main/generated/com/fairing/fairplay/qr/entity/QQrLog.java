package com.fairing.fairplay.qr.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QQrLog is a Querydsl query type for QrLog
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QQrLog extends EntityPathBase<QrLog> {

    private static final long serialVersionUID = -2098072308L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QQrLog qrLog = new QQrLog("qrLog");

    public final QQrActionCode actionCode;

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QQrTicket qrTicket;

    public QQrLog(String variable) {
        this(QrLog.class, forVariable(variable), INITS);
    }

    public QQrLog(Path<? extends QrLog> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QQrLog(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QQrLog(PathMetadata metadata, PathInits inits) {
        this(QrLog.class, metadata, inits);
    }

    public QQrLog(Class<? extends QrLog> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.actionCode = inits.isInitialized("actionCode") ? new QQrActionCode(forProperty("actionCode")) : null;
        this.qrTicket = inits.isInitialized("qrTicket") ? new QQrTicket(forProperty("qrTicket"), inits.get("qrTicket")) : null;
    }

}

