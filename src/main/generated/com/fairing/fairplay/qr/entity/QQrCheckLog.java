package com.fairing.fairplay.qr.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QQrCheckLog is a Querydsl query type for QrCheckLog
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QQrCheckLog extends EntityPathBase<QrCheckLog> {

    private static final long serialVersionUID = -1132571564L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QQrCheckLog qrCheckLog = new QQrCheckLog("qrCheckLog");

    public final QQrCheckStatusCode checkStatusCode;

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QQrTicket qrTicket;

    public QQrCheckLog(String variable) {
        this(QrCheckLog.class, forVariable(variable), INITS);
    }

    public QQrCheckLog(Path<? extends QrCheckLog> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QQrCheckLog(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QQrCheckLog(PathMetadata metadata, PathInits inits) {
        this(QrCheckLog.class, metadata, inits);
    }

    public QQrCheckLog(Class<? extends QrCheckLog> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.checkStatusCode = inits.isInitialized("checkStatusCode") ? new QQrCheckStatusCode(forProperty("checkStatusCode")) : null;
        this.qrTicket = inits.isInitialized("qrTicket") ? new QQrTicket(forProperty("qrTicket"), inits.get("qrTicket")) : null;
    }

}

