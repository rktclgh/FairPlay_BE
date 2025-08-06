package com.fairing.fairplay.banner.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBannerStatusCode is a Querydsl query type for BannerStatusCode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBannerStatusCode extends EntityPathBase<BannerStatusCode> {

    private static final long serialVersionUID = 432205197L;

    public static final QBannerStatusCode bannerStatusCode = new QBannerStatusCode("bannerStatusCode");

    public final StringPath code = createString("code");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath name = createString("name");

    public QBannerStatusCode(String variable) {
        super(BannerStatusCode.class, forVariable(variable));
    }

    public QBannerStatusCode(Path<? extends BannerStatusCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBannerStatusCode(PathMetadata metadata) {
        super(BannerStatusCode.class, metadata);
    }

}

