package com.fairing.fairplay.event.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEventDetail is a Querydsl query type for EventDetail
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEventDetail extends EntityPathBase<EventDetail> {

    private static final long serialVersionUID = 689813165L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEventDetail eventDetail = new QEventDetail("eventDetail");

    public final StringPath bio = createString("bio");

    public final BooleanPath checkOutAllowed = createBoolean("checkOutAllowed");

    public final StringPath contactInfo = createString("contactInfo");

    public final StringPath content = createString("content");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final DatePath<java.time.LocalDate> endDate = createDate("endDate", java.time.LocalDate.class);

    public final QEvent event;

    public final NumberPath<Long> eventDetailId = createNumber("eventDetailId", Long.class);

    public final NumberPath<Integer> eventTime = createNumber("eventTime", Integer.class);

    public final StringPath hostName = createString("hostName");

    public final QLocation location;

    public final StringPath locationDetail = createString("locationDetail");

    public final QMainCategory mainCategory;

    public final StringPath officialUrl = createString("officialUrl");

    public final StringPath policy = createString("policy");

    public final BooleanPath reentryAllowed = createBoolean("reentryAllowed");

    public final QRegionCode regionCode;

    public final DatePath<java.time.LocalDate> startDate = createDate("startDate", java.time.LocalDate.class);

    public final QSubCategory subCategory;

    public final StringPath thumbnailUrl = createString("thumbnailUrl");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QEventDetail(String variable) {
        this(EventDetail.class, forVariable(variable), INITS);
    }

    public QEventDetail(Path<? extends EventDetail> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEventDetail(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEventDetail(PathMetadata metadata, PathInits inits) {
        this(EventDetail.class, metadata, inits);
    }

    public QEventDetail(Class<? extends EventDetail> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.event = inits.isInitialized("event") ? new QEvent(forProperty("event"), inits.get("event")) : null;
        this.location = inits.isInitialized("location") ? new QLocation(forProperty("location")) : null;
        this.mainCategory = inits.isInitialized("mainCategory") ? new QMainCategory(forProperty("mainCategory")) : null;
        this.regionCode = inits.isInitialized("regionCode") ? new QRegionCode(forProperty("regionCode")) : null;
        this.subCategory = inits.isInitialized("subCategory") ? new QSubCategory(forProperty("subCategory"), inits.get("subCategory")) : null;
    }

}

