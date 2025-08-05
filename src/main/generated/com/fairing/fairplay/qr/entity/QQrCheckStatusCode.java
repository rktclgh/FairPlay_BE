package com.fairing.fairplay.qr.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QQrCheckStatusCode is a Querydsl query type for QrCheckStatusCode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QQrCheckStatusCode extends EntityPathBase<QrCheckStatusCode> {

    private static final long serialVersionUID = -1126324753L;

    public static final QQrCheckStatusCode qrCheckStatusCode = new QQrCheckStatusCode("qrCheckStatusCode");

    public final StringPath code = createString("code");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath name = createString("name");

    public QQrCheckStatusCode(String variable) {
        super(QrCheckStatusCode.class, forVariable(variable));
    }

    public QQrCheckStatusCode(Path<? extends QrCheckStatusCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QQrCheckStatusCode(PathMetadata metadata) {
        super(QrCheckStatusCode.class, metadata);
    }

}

