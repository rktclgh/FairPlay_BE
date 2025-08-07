package com.fairing.fairplay.banner.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBannerLog is a Querydsl query type for BannerLog
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBannerLog extends EntityPathBase<BannerLog> {

    private static final long serialVersionUID = -973520266L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBannerLog bannerLog = new QBannerLog("bannerLog");

    public final QBannerActionCode actionCode;

    public final QBanner banner;

    public final DateTimePath<java.time.LocalDateTime> changedAt = createDateTime("changedAt", java.time.LocalDateTime.class);

    public final com.fairing.fairplay.admin.entity.QAdminAccount changedBy;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public QBannerLog(String variable) {
        this(BannerLog.class, forVariable(variable), INITS);
    }

    public QBannerLog(Path<? extends BannerLog> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBannerLog(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBannerLog(PathMetadata metadata, PathInits inits) {
        this(BannerLog.class, metadata, inits);
    }

    public QBannerLog(Class<? extends BannerLog> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.actionCode = inits.isInitialized("actionCode") ? new QBannerActionCode(forProperty("actionCode")) : null;
        this.banner = inits.isInitialized("banner") ? new QBanner(forProperty("banner"), inits.get("banner")) : null;
        this.changedBy = inits.isInitialized("changedBy") ? new com.fairing.fairplay.admin.entity.QAdminAccount(forProperty("changedBy"), inits.get("changedBy")) : null;
    }

}

