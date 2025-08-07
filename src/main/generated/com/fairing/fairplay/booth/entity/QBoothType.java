package com.fairing.fairplay.booth.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBoothType is a Querydsl query type for BoothType
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBoothType extends EntityPathBase<BoothType> {

    private static final long serialVersionUID = 1561133590L;

    public static final QBoothType boothType = new QBoothType("boothType");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> maxApplicants = createNumber("maxApplicants", Integer.class);

    public final StringPath name = createString("name");

    public final NumberPath<Integer> price = createNumber("price", Integer.class);

    public final StringPath size = createString("size");

    public QBoothType(String variable) {
        super(BoothType.class, forVariable(variable));
    }

    public QBoothType(Path<? extends BoothType> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBoothType(PathMetadata metadata) {
        super(BoothType.class, metadata);
    }

}

