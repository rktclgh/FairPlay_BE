package com.fairing.fairplay.banner.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBannerActionCode is a Querydsl query type for BannerActionCode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBannerActionCode extends EntityPathBase<BannerActionCode> {

    private static final long serialVersionUID = 2036062673L;

    public static final QBannerActionCode bannerActionCode = new QBannerActionCode("bannerActionCode");

    public final StringPath code = createString("code");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath name = createString("name");

    public QBannerActionCode(String variable) {
        super(BannerActionCode.class, forVariable(variable));
    }

    public QBannerActionCode(Path<? extends BannerActionCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBannerActionCode(PathMetadata metadata) {
        super(BannerActionCode.class, metadata);
    }

}

