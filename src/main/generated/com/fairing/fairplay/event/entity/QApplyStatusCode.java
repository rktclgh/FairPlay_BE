package com.fairing.fairplay.event.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QApplyStatusCode is a Querydsl query type for ApplyStatusCode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QApplyStatusCode extends EntityPathBase<ApplyStatusCode> {

    private static final long serialVersionUID = 328913199L;

    public static final QApplyStatusCode applyStatusCode = new QApplyStatusCode("applyStatusCode");

    public final StringPath code = createString("code");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath name = createString("name");

    public QApplyStatusCode(String variable) {
        super(ApplyStatusCode.class, forVariable(variable));
    }

    public QApplyStatusCode(Path<? extends ApplyStatusCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QApplyStatusCode(PathMetadata metadata) {
        super(ApplyStatusCode.class, metadata);
    }

}

