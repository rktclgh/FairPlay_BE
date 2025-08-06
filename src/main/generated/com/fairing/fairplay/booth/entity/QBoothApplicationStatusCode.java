package com.fairing.fairplay.booth.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBoothApplicationStatusCode is a Querydsl query type for BoothApplicationStatusCode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBoothApplicationStatusCode extends EntityPathBase<BoothApplicationStatusCode> {

    private static final long serialVersionUID = 1915111187L;

    public static final QBoothApplicationStatusCode boothApplicationStatusCode = new QBoothApplicationStatusCode("boothApplicationStatusCode");

    public final StringPath code = createString("code");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath name = createString("name");

    public QBoothApplicationStatusCode(String variable) {
        super(BoothApplicationStatusCode.class, forVariable(variable));
    }

    public QBoothApplicationStatusCode(Path<? extends BoothApplicationStatusCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBoothApplicationStatusCode(PathMetadata metadata) {
        super(BoothApplicationStatusCode.class, metadata);
    }

}

