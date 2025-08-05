package com.fairing.fairplay.event.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRegionCode is a Querydsl query type for RegionCode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRegionCode extends EntityPathBase<RegionCode> {

    private static final long serialVersionUID = -1957910625L;

    public static final QRegionCode regionCode = new QRegionCode("regionCode");

    public final StringPath code = createString("code");

    public final StringPath name = createString("name");

    public final NumberPath<Integer> regionCodeId = createNumber("regionCodeId", Integer.class);

    public QRegionCode(String variable) {
        super(RegionCode.class, forVariable(variable));
    }

    public QRegionCode(Path<? extends RegionCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRegionCode(PathMetadata metadata) {
        super(RegionCode.class, metadata);
    }

}

