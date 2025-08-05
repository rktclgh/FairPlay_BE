package com.fairing.fairplay.ticket.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QTicketStatusCode is a Querydsl query type for TicketStatusCode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTicketStatusCode extends EntityPathBase<TicketStatusCode> {

    private static final long serialVersionUID = -928133811L;

    public static final QTicketStatusCode ticketStatusCode = new QTicketStatusCode("ticketStatusCode");

    public final StringPath code = createString("code");

    public final StringPath name = createString("name");

    public final NumberPath<Integer> ticketStatusCodeId = createNumber("ticketStatusCodeId", Integer.class);

    public QTicketStatusCode(String variable) {
        super(TicketStatusCode.class, forVariable(variable));
    }

    public QTicketStatusCode(Path<? extends TicketStatusCode> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTicketStatusCode(PathMetadata metadata) {
        super(TicketStatusCode.class, metadata);
    }

}

