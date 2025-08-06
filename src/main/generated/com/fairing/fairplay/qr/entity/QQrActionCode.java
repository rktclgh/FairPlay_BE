package com.fairing.fairplay.qr.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QQrActionCode is a Querydsl query type for QrActionCode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QQrActionCode extends EntityPathBase<QrActionCode> {

    private static final long serialVersionUID = 612840059L;

    public static final QQrActionCode qrActionCode = new QQrActionCode("qrActionCode");

    public final StringPath code = createString("code");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath name = createString("name");

    public QQrActionCode(String variable) {
        super(QrActionCode.class, forVariable(variable));
    }

    public QQrActionCode(Path<? extends QrActionCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QQrActionCode(PathMetadata metadata) {
        super(QrActionCode.class, metadata);
    }

}

