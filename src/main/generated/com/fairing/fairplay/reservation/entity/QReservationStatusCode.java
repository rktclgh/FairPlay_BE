package com.fairing.fairplay.reservation.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QReservationStatusCode is a Querydsl query type for ReservationStatusCode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReservationStatusCode extends EntityPathBase<ReservationStatusCode> {

    private static final long serialVersionUID = 1670248411L;

    public static final QReservationStatusCode reservationStatusCode = new QReservationStatusCode("reservationStatusCode");

    public final StringPath code = createString("code");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath name = createString("name");

    public QReservationStatusCode(String variable) {
        super(ReservationStatusCode.class, forVariable(variable));
    }

    public QReservationStatusCode(Path<? extends ReservationStatusCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QReservationStatusCode(PathMetadata metadata) {
        super(ReservationStatusCode.class, metadata);
    }

}

