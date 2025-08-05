package com.fairing.fairplay.booth.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBoothPaymentStatusCode is a Querydsl query type for BoothPaymentStatusCode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBoothPaymentStatusCode extends EntityPathBase<BoothPaymentStatusCode> {

    private static final long serialVersionUID = 1497397833L;

    public static final QBoothPaymentStatusCode boothPaymentStatusCode = new QBoothPaymentStatusCode("boothPaymentStatusCode");

    public final StringPath code = createString("code");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath name = createString("name");

    public QBoothPaymentStatusCode(String variable) {
        super(BoothPaymentStatusCode.class, forVariable(variable));
    }

    public QBoothPaymentStatusCode(Path<? extends BoothPaymentStatusCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBoothPaymentStatusCode(PathMetadata metadata) {
        super(BoothPaymentStatusCode.class, metadata);
    }

}

