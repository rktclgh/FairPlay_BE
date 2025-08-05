package com.fairing.fairplay.booth.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBoothApplication is a Querydsl query type for BoothApplication
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBoothApplication extends EntityPathBase<BoothApplication> {

    private static final long serialVersionUID = -1903435020L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBoothApplication boothApplication = new QBoothApplication("boothApplication");

    public final StringPath adminComment = createString("adminComment");

    public final DateTimePath<java.time.LocalDateTime> applyAt = createDateTime("applyAt", java.time.LocalDateTime.class);

    public final QBoothApplicationStatusCode boothApplicationStatusCode;

    public final StringPath boothDescription = createString("boothDescription");

    public final StringPath boothEmail = createString("boothEmail");

    public final QBoothPaymentStatusCode boothPaymentStatusCode;

    public final StringPath boothTitle = createString("boothTitle");

    public final StringPath contactNumber = createString("contactNumber");

    public final StringPath email = createString("email");

    public final DatePath<java.time.LocalDate> endDate = createDate("endDate", java.time.LocalDate.class);

    public final com.fairing.fairplay.event.entity.QEvent event;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath managerName = createString("managerName");

    public final StringPath officialUrl = createString("officialUrl");

    public final DatePath<java.time.LocalDate> startDate = createDate("startDate", java.time.LocalDate.class);

    public final DateTimePath<java.time.LocalDateTime> statusUpdatedAt = createDateTime("statusUpdatedAt", java.time.LocalDateTime.class);

    public QBoothApplication(String variable) {
        this(BoothApplication.class, forVariable(variable), INITS);
    }

    public QBoothApplication(Path<? extends BoothApplication> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBoothApplication(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBoothApplication(PathMetadata metadata, PathInits inits) {
        this(BoothApplication.class, metadata, inits);
    }

    public QBoothApplication(Class<? extends BoothApplication> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.boothApplicationStatusCode = inits.isInitialized("boothApplicationStatusCode") ? new QBoothApplicationStatusCode(forProperty("boothApplicationStatusCode")) : null;
        this.boothPaymentStatusCode = inits.isInitialized("boothPaymentStatusCode") ? new QBoothPaymentStatusCode(forProperty("boothPaymentStatusCode")) : null;
        this.event = inits.isInitialized("event") ? new com.fairing.fairplay.event.entity.QEvent(forProperty("event"), inits.get("event")) : null;
    }

}

