package com.fairing.fairplay.attendee.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAttendeeTypeCode is a Querydsl query type for AttendeeTypeCode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAttendeeTypeCode extends EntityPathBase<AttendeeTypeCode> {

    private static final long serialVersionUID = -1812945743L;

    public static final QAttendeeTypeCode attendeeTypeCode = new QAttendeeTypeCode("attendeeTypeCode");

    public final StringPath code = createString("code");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath name = createString("name");

    public QAttendeeTypeCode(String variable) {
        super(AttendeeTypeCode.class, forVariable(variable));
    }

    public QAttendeeTypeCode(Path<? extends AttendeeTypeCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAttendeeTypeCode(PathMetadata metadata) {
        super(AttendeeTypeCode.class, metadata);
    }

}

