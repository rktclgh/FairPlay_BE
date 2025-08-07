package com.fairing.fairplay.event.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QEventStatusCode is a Querydsl query type for EventStatusCode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEventStatusCode extends EntityPathBase<EventStatusCode> {

    private static final long serialVersionUID = -1383745189L;

    public static final QEventStatusCode eventStatusCode = new QEventStatusCode("eventStatusCode");

    public final StringPath code = createString("code");

    public final NumberPath<Integer> eventStatusCodeId = createNumber("eventStatusCodeId", Integer.class);

    public final StringPath name = createString("name");

    public QEventStatusCode(String variable) {
        super(EventStatusCode.class, forVariable(variable));
    }

    public QEventStatusCode(Path<? extends EventStatusCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QEventStatusCode(PathMetadata metadata) {
        super(EventStatusCode.class, metadata);
    }

}

